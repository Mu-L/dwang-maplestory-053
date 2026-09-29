package org.gms.activity;

import org.gms.client.Character;
import org.gms.dao.entity.ActivityMonsterConfigDO;
import org.gms.dao.entity.ActivityMonsterMobDO;
import org.gms.dao.mapper.ActivityMonsterConfigMapper;
import org.gms.dao.mapper.ActivityMonsterMobMapper;
import org.gms.net.server.PlayerStorage;
import org.gms.net.server.channel.Channel;
import org.gms.server.StringInfoProvider;
import org.gms.server.TimerManager;
import org.gms.server.life.LifeFactory;
import org.gms.server.life.Monster;
import org.gms.server.life.SpawnPoint;
import org.gms.server.maps.MapleMap;
import org.gms.util.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 活动怪物管理器（每个频道一个）。
 *
 * <p>职责：读数据库配置、启动计时、每回合执行一次
 * {@code 找人 → 选图 → 校验 → 清场 → 召唤多种怪 → 通知}。
 * 业务条件在 {@link ActivityMonsterEvent} 子类里；用哪个实现类由数据库
 * {@code activity_monster_config.event_class} 决定（反射创建）；是否启动由
 * {@code enabled} 字段决定。
 *
 * <p>回合流程（以"10 分钟一轮"为例）：
 * <pre>
 *   启动 ─────────────────────────────────────────────► t = 0
 *                                                        │ 计时 600s
 *   t = 600s  第 1 回合：找合格玩家 → 选隐藏图 → 清场(空) → 蜗牛×5 + 红蜗牛×5 → 通知
 *   t = 1200s 第 2 回合：找合格玩家 → 换一张新隐藏图 → 清场上轮(不掉落) → 再刷 → 通知
 *   ...
 * </pre>
 *
 * @author dwang
 * @version 1.0
 */
public class ActivityMonsterManager {

    private static final Logger log = LoggerFactory.getLogger(ActivityMonsterManager.class);

    /** 计时检查间隔：每秒扫一次，开销可忽略 */
    private static final long TICK_MS = 1000L;

    /** 为找"刷怪点够多"的图，最多额外从 WZ 加载几张图 */
    private static final int MAX_LOAD_PROBE = 1;

    private final Channel channel;
    private final List<ActivityMonsterEvent> events = new ArrayList<>();

    /** 每个活动当前存活的怪：eventKey -> instance */
    private final Map<String, ActivityMonsterInstance> live = new ConcurrentHashMap<>();

    /** 已确认没有怪物刷怪点的地图，避免反复从 WZ 加载 */
    private final Set<Integer> noSpawnPointMaps = ConcurrentHashMap.newKeySet();

    private ScheduledFuture<?> tickTask;

    public ActivityMonsterManager(Channel channel) {
        this.channel = channel;
    }

    // ==================================================================
    // 启动 / 重载
    // ==================================================================

    /** 启动：读数据库配置 + 开始计时 */
    public void start() {
        reload();
        tickTask = TimerManager.getInstance().register(this::tick, TICK_MS);
        log.info("[活动怪物] 频道 {} 管理器已启动，共 {} 个活动", channel.getId(), events.size());
    }

    /**
     * 热重载配置：重新读数据库并重建事件。
     * 已经刷出来的怪不受影响，下一回合才按新配置走。
     */
    public void reload() {
        events.clear();
        try {
            ActivityMonsterConfigMapper configMapper =
                    SpringContextUtil.getBean(ActivityMonsterConfigMapper.class);
            Map<Integer, List<ActivityMonsterMobDO>> mobsByConfig = loadMobsByConfig();

            int disabled = 0;
            for (ActivityMonsterConfigDO row : configMapper.selectAll()) {
                // ★ 是否启动完全由数据库 enabled 字段控制（控制台改这里即可启停）
                if (!Boolean.TRUE.equals(row.getEnabled())) {
                    disabled++;
                    continue;
                }

                List<ActivityMonsterMobDO> mobList = mobsByConfig.get(row.getId());
                if (mobList == null || mobList.isEmpty()) {
                    log.warn("[活动怪物] 活动 {}「{}」在 activity_monster_mob 里没有配怪物，已跳过",
                            row.getEventKey(), row.getName());
                    continue;
                }
                row.setMobs(mobList);

                // ★ 按数据库配置的类名反射创建事件实例
                ActivityMonsterEvent event = ActivityMonsterRegistry.create(row);
                if (event != null) {
                    events.add(event);
                }
            }
            log.info("[活动怪物] 频道 {} 载入 {} 个活动（数据库禁用 {} 个）",
                    channel.getId(), events.size(), disabled);
        } catch (Exception e) {
            log.error("[活动怪物] 频道 {} 读取数据库配置失败", channel.getId(), e);
        }
    }

    /** 读出怪物清单并按活动分组、按 sort_order 排序 */
    private Map<Integer, List<ActivityMonsterMobDO>> loadMobsByConfig() {
        ActivityMonsterMobMapper mobMapper = SpringContextUtil.getBean(ActivityMonsterMobMapper.class);
        Map<Integer, List<ActivityMonsterMobDO>> grouped = new HashMap<>();
        for (ActivityMonsterMobDO mob : mobMapper.selectAll()) {
            if (mob.getConfigId() == null) {
                continue;
            }
            grouped.computeIfAbsent(mob.getConfigId(), k -> new ArrayList<>()).add(mob);
        }
        for (List<ActivityMonsterMobDO> list : grouped.values()) {
            list.sort(Comparator.comparingInt(m -> m.getSortOrder() == null ? 0 : m.getSortOrder()));
        }
        return grouped;
    }

    // ==================================================================
    // 回合调度
    // ==================================================================

    /** 每秒检查：谁的计时到了就跑一回合 */
    private void tick() {
        if (events.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (ActivityMonsterEvent event : events) {
            if (now < event.nextFireTime()) {
                continue;
            }
            // 先把下一次时间推掉，避免回合执行异常导致每秒狂刷
            event.scheduleNext(now);
            try {
                runRound(event);
            } catch (Exception e) {
                log.error("[活动怪物] 活动 {} 回合执行异常", event.key(), e);
            }
        }
    }

    /**
     * 一个回合：
     * ① 找合格玩家 ② 选好新图 ③ 校验怪物 ④ 清场 ⑤ 召唤多种怪 ⑥ 通知
     *
     * <p>顺序上把"可能失败"的动作全部放在清场之前，避免出现"清掉了但没刷出来"。
     */
    private void runRound(ActivityMonsterEvent event) {
        ActivityMonsterConfigDO cfg = event.config();
        List<ActivityMonsterMobDO> mobList = event.mobs();

        // ① 找合格玩家。没人满足条件就什么都不做（不清场、不换图、不通知）
        List<Character> qualified = new ArrayList<>();
        for (Character chr : getOnlinePlayers()) {
            try {
                if (event.shouldSummon(chr)) {
                    qualified.add(chr);
                }
            } catch (Exception e) {
                log.error("[活动怪物] 活动 {} 条件判断异常，该玩家本轮视为不满足", event.key(), e);
            }
        }
        if (qualified.isEmpty()) {
            log.debug("[活动怪物] 活动 {} 本回合无人满足条件，跳过", event.key());
            return;
        }

        Character character = qualified.getFirst();

        // ② 换一张新图（优先挑刷怪点够放下本轮怪物的图）
        int needed = event.totalSpawnCount();
        MapleMap map = pickMap(character, event, needed);
        if (map == null) {
            log.warn("[活动怪物] 活动 {} 找不到可用的地图（候选池为空或都没有刷怪点），本回合跳过", event.key());
            return;
        }

        // ③ 校验配置的怪物都存在（配置错了就本轮跳过，不要先清场）
        for (ActivityMonsterMobDO entry : mobList) {
            if (LifeFactory.getMonster(entry.getMobId()) == null) {
                log.warn("[活动怪物] 活动 {} 配置的怪物 {} 不存在，本回合跳过", event.key(), entry.getMobId());
                return;
            }
        }

        // ④ 清场：杀掉上一回合剩下的怪，不掉落（"不报装备"）
        int wiped = clearLive(event.key(), false);

        // ⑤ 按怪物清单召唤（一个活动可以多种怪）
        List<Monster> spawned = spawnMobs(map, mobList);
        if (spawned.isEmpty()) {
            log.warn("[活动怪物] 活动 {} 地图 {}「{}」没有刷怪点，生成失败",
                    event.key(), map.getId(), map.getMapName());
            return;
        }

        String monstersText = describeMobs(mobList);
        event.setLastMapId(map.getId());
        live.put(event.key(), new ActivityMonsterInstance(event.key(), map, spawned, monstersText));

        // ⑥ 通知合格玩家（dropMessage type 默认 6 = 蓝字）
        int noticeType = cfg.getNoticeType() == null ? 6 : cfg.getNoticeType();
        for (Character chr : qualified) {
            String msg = event.buildNotice(chr, monstersText, map.getMapName(), spawned.size());
            try {
                chr.dropMessage(noticeType, msg);
            } catch (Exception e) {
                log.error("[活动怪物] 活动 {} 通知玩家 {} 失败", event.key(), chr.getName(), e);
            }
        }

        log.info("[活动怪物] 活动 {} 在 {}「{}」生成 {}（共 {} 只，坐标取自该图刷怪点），清掉上轮 {} 只，通知 {} 人",
                event.key(), map.getId(), map.getMapName(), monstersText,
                spawned.size(), wiped, qualified.size());
    }

    // ==================================================================
    // 地图选择
    // ==================================================================

    /**
     * 从候选地图里随机挑一张有刷怪点的，优先排除上一回合用过的那张（"换一张新图"）。
     *
//     * <p>先看已经加载过的图（零 WZ 读取开销），都没有才按需加载；
     * 已确认没有刷怪点的图会被记住，不会反复加载。
     * 同等条件下优先挑刷怪点数 {@code >= neededPoints} 的图，避免怪叠在一起。
     */
    private MapleMap pickMap(Character character, ActivityMonsterEvent event, int neededPoints) {
        Collection<Integer> candidates = event.candidateMapIds(character);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        List<Integer> pool = new ArrayList<>(candidates);
        Integer lastMapId = event.lastMapId();
        if (lastMapId != null && pool.size() > 1) {
            pool.remove(lastMapId);
        }
        Collections.shuffle(pool);

        // 随机加载
        MapleMap fallback = null;

        // ① 优先从已经加载过的图里挑×
//        for (Integer mapId : pool) {
//            MapleMap map = mapId == null ? null : loadedMaps.get(mapId);
//            if (map == null) {
//                continue;
//            }
//            List<SpawnPoint> points = map.getMonsterSpawnPoints();
//            if (points.isEmpty()) {
//                continue;
//            }
//            if (points.size() >= neededPoints) {
//                return map;
//            }
//            if (fallback == null) {
//                fallback = map;
//            }
//        }
//        if (fallback != null) {
//            return fallback;
//        }

        // ② 都没有，才按需从 WZ 加载（跳过已知没有刷怪点的，最多试 MAX_LOAD_PROBE 张）
        int probed = 0;
        for (Integer mapId : pool) {
            if (mapId == null || noSpawnPointMaps.contains(mapId)) {
                continue;
            }
            if (probed >= MAX_LOAD_PROBE) {
                break;
            }
            probed++;

            MapleMap map = channel.getMapFactory().getMap(mapId);
            if (map == null) {
                continue;
            }
            int size = map.getMonsterSpawnPoints().size();
            if (size == 0) {
                noSpawnPointMaps.add(mapId);
                log.debug("[活动怪物] 地图 {}「{}」没有刷怪点，已加入跳过列表", mapId, map.getMapName());
                continue;
            }
            if (size >= neededPoints) {
                return map;
            }
            if (fallback == null) {
                fallback = map;
            }
        }
        return fallback;
    }

    // ==================================================================
    // 召唤 / 清场
    // ==================================================================

    /**
     * 按怪物清单召唤。直接借用地图自带的怪物刷怪点坐标：
     * 这些点是 WZ 里标注好的，坐标一定在真实平台上，并且带着正确的 foothold 编号，
     * 所以怪不会悬空也不会掉出地图。
     *
     * <p>用 {@code map.spawnMonster(mob)} 召唤 —— 这是原生入口，会走
     * {@code MapleMap#updateMonsterHp} → {@code Character#calculateMonsterHp}
     * 的怪物血量计算，不做任何绕过。
     *
     * <p>各类怪按 sort_order 依次取不同的刷怪点；刷怪点总数不够时才会循环复用。
     */
    private List<Monster> spawnMobs(MapleMap map, List<ActivityMonsterMobDO> mobList) {
        List<Monster> spawned = new ArrayList<>();
        List<SpawnPoint> points = map.getMonsterSpawnPoints();
        if (points.isEmpty()) {
            return spawned;
        }

        Collections.shuffle(points);
        int cursor = 0;

        for (ActivityMonsterMobDO entry : mobList) {
            int mobId = entry.getMobId() == null ? 0 : entry.getMobId();
            int count = (entry.getSpawnCount() == null || entry.getSpawnCount() <= 0) ? 1 : entry.getSpawnCount();

            for (int i = 0; i < count; i++) {
                Monster mob = LifeFactory.getMonster(mobId);
                if (mob == null) {
                    log.warn("[活动怪物] 怪物 {} 不存在，跳过", mobId);
                    break;
                }

                SpawnPoint sp = points.get(cursor++ % points.size());
                mob.setPosition(new Point(sp.getPosition()));
                mob.setFh(sp.getFh());
                mob.setF(sp.getF());

                map.spawnMonster(mob);
                spawned.add(mob);
            }
        }
        return spawned;
    }

    /** 生成文案用的怪物摘要，例如 "蜗牛×5、红蜗牛×5" */
    private String describeMobs(List<ActivityMonsterMobDO> mobList) {
        StringBuilder sb = new StringBuilder();
        for (ActivityMonsterMobDO entry : mobList) {
            if (sb.length() > 0) {
                sb.append('、');
            }
            String name = StringInfoProvider.getMobNameFromId(entry.getMobId());
            if (name == null || name.trim().isEmpty()) {
                name = "怪物" + entry.getMobId();
            }
            int count = (entry.getSpawnCount() == null || entry.getSpawnCount() <= 0) ? 1 : entry.getSpawnCount();
//            sb.append(name.trim()).append('×').append(count);
            sb.append(name.trim());
        }
        return sb.toString();
    }

    /**
     * 清掉某个活动当前存活的全部怪。
     *
     * @param withDrops false = 不掉落物品
     * @return 实际清掉的数量
     */
    private int clearLive(String eventKey, boolean withDrops) {
        ActivityMonsterInstance instance = live.remove(eventKey);
        if (instance == null) {
            return 0;
        }
        int killed = instance.wipe(withDrops);
        log.info("[活动怪物] 活动 {} 已清场 {} 只（掉落={}）", eventKey, killed, withDrops);
        return killed;
    }

    // ==================================================================
    // 供外部（GM 命令 / 控制台）调用
    // ==================================================================

    /** 立即清掉某个活动的怪 */
    public int clear(String eventKey, boolean withDrops) {
        return clearLive(eventKey, withDrops);
    }

    /** 当前存活的活动怪数量 */
    public int aliveCount(String eventKey) {
        ActivityMonsterInstance instance = live.get(eventKey);
        return instance == null ? 0 : instance.countAlive();
    }

    /** 当前已载入的活动标识 */
    public List<String> loadedEventKeys() {
        List<String> keys = new ArrayList<>();
        for (ActivityMonsterEvent event : events) {
            keys.add(event.key());
        }
        return keys;
    }

    private List<Character> getOnlinePlayers() {
        PlayerStorage storage = channel.getPlayerStorage();
        return storage == null ? Collections.emptyList() : new ArrayList<>(storage.getAllCharacters());
    }

    /** 关闭频道：停掉计时 + 清掉所有活动怪（不掉落） */
    public void dispose() {
        if (tickTask != null) {
            tickTask.cancel(true);
            tickTask = null;
        }
        for (String eventKey : new ArrayList<>(live.keySet())) {
            clearLive(eventKey, false);
        }
        events.clear();
        log.info("[活动怪物] 频道 {} 管理器已关闭", channel.getId());
    }
}