package org.gms.activity;

import org.gms.client.Character;
import org.gms.dao.entity.ActivityMonsterConfigDO;
import org.gms.dao.entity.ActivityMonsterMobDO;

import java.util.Collection;
import java.util.List;

/**
 * 活动怪物事件基类。
 *
 * <p>子类只需要实现两个方法：
 * <ul>
 *   <li>{@link #shouldSummon(Character)} —— 什么时候该召唤（"是否召唤"的判断，不进数据库）</li>
 *   <li>{@link #candidateMapIds()} —— 从哪些地图里随机挑一张</li>
 * </ul>
 *
 * <p>其余的（计时、找合格玩家、清场、换图、取刷怪点、召唤多种怪、发通知）全部由
 * {@link ActivityMonsterManager} 负责。
 *
 * <p>召唤几种怪、每种几只，来自数据库 {@code activity_monster_mob} 表，
 * 通过 {@link #mobs()} 读取。
 *
 * <p>实现类必须提供 {@code public XxxEvent(ActivityMonsterConfigDO)} 构造器，
 * 供 {@link ActivityMonsterRegistry} 反射实例化。
 *
 * @author dwang
 * @version 1.0
 */
public abstract class ActivityMonsterEvent {

    private final ActivityMonsterConfigDO config;

    /** 下一次触发时刻（毫秒时间戳） */
    private long nextFireTime;

    /** 上一回合刷在哪张图，用于"换一张新图" */
    private Integer lastMapId;

    protected ActivityMonsterEvent(ActivityMonsterConfigDO config) {
        this.config = config;
        // 启动即开始计时：首次触发 = 现在 + interval
        this.nextFireTime = System.currentTimeMillis() + intervalMs();
    }

    public String key() {
        return config.getEventKey();
    }

    public ActivityMonsterConfigDO config() {
        return config;
    }

    /** 本活动要召唤的怪物清单（来自数据库 activity_monster_mob） */
    public List<ActivityMonsterMobDO> mobs() {
        return config.getMobs();
    }

    /** 本回合总共要召唤多少只（各种怪数量之和） */
    public int totalSpawnCount() {
        List<ActivityMonsterMobDO> list = mobs();
        if (list == null) {
            return 0;
        }
        int total = 0;
        for (ActivityMonsterMobDO m : list) {
            Integer c = m.getSpawnCount();
            total += (c == null || c <= 0) ? 1 : c;
        }
        return total;
    }

    // ==================== 子类必须实现 ====================

    /**
     * 这名玩家满足召唤条件吗？
     * 返回 true 的玩家会触发本回合召唤，并收到通知。
     *
     * @param chr 在线玩家
     * @return true = 满足条件，可以召唤并通知他
     */
    public abstract boolean shouldSummon(Character chr);

    /**
     * 候选地图 ID 集合。
     * 例如返回 {@code HiddenMapAchievementManager.getHiddenMapIds()}。
     * 框架会在里面随机挑一张（优先排除上一回合用过的那张），
     * 并且只挑"有怪物刷怪点"的图。
     *
     * @return 候选地图 ID；返回空集合则本回合跳过
     */
    public abstract Collection<Integer> candidateMapIds();

    // ==================== 子类可选覆盖 ====================

    /**
     * 通知文案。默认用数据库模板渲染占位符：
     * <pre>
     *   {event}     活动名
     *   {monsters}  怪物摘要，例如 "蜗牛×5、红蜗牛×5"
     *   {monster}   {monsters} 的别名
     *   {map}       地图名
     *   {count}     本回合总只数
     * </pre>
     * 需要按玩家定制文案时重写本方法即可。
     */
    public String buildNotice(Character chr, String monstersText, String mapName, int totalCount) {
        String tpl = config.getNoticeText();
        if (tpl == null || tpl.isEmpty()) {
            tpl = "【{event}】{monsters} 出现在了「{map}」，请速去攻略！";
        }
        return tpl.replace("{event}", nullToEmpty(config.getName()))
                .replace("{monsters}", nullToEmpty(monstersText))
                .replace("{monster}", nullToEmpty(monstersText))
                .replace("{map}", nullToEmpty(mapName))
                .replace("{count}", String.valueOf(totalCount));
    }

    /** 清场完成后的回调（需要发奖 / 记进度时重写） */
    public void onCleared(Character chr, int mapId, int killedCount) {
    }

    // ==================== 框架内部使用 ====================

    private long intervalMs() {
        Integer sec = config.getIntervalSec();
        return (sec == null || sec <= 0 ? 600 : sec) * 1000L;
    }

    long nextFireTime() {
        return nextFireTime;
    }

    void scheduleNext(long now) {
        this.nextFireTime = now + intervalMs();
    }

    Integer lastMapId() {
        return lastMapId;
    }

    void setLastMapId(Integer mapId) {
        this.lastMapId = mapId;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}