package org.gms.activity;

import org.gms.server.life.Monster;
import org.gms.server.maps.MapleMap;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次召唤的运行时句柄：记住这一轮刷在哪张图、刷了哪些怪。
 * "重置事件"就是调用 {@link #wipe(boolean)}。
 *
 * @author dwang
 * @version 1.0
 */
public class ActivityMonsterInstance {

    private final String eventKey;
    private final MapleMap map;
    private final List<Monster> monsters;

    /** 本轮的怪物摘要，例如 "蜗牛×5、红蜗牛×5" */
    private final String monstersText;

    ActivityMonsterInstance(String eventKey, MapleMap map, List<Monster> monsters, String monstersText) {
        this.eventKey = eventKey;
        this.map = map;
        this.monsters = new ArrayList<>(monsters);
        this.monstersText = monstersText;
    }

    public String getEventKey() {
        return eventKey;
    }

    public MapleMap getMap() {
        return map;
    }

    public int getMapId() {
        return map.getId();
    }

    public String getMapName() {
        return map.getMapName();
    }

    /** 例如 "蜗牛×5、红蜗牛×5" */
    public String getMonstersText() {
        return monstersText;
    }

    /** 本回合总共召唤了几只 */
    public int getSpawnedCount() {
        return monsters.size();
    }

    /** 当前还活着几只 */
    public int countAlive() {
        int n = 0;
        for (Monster m : monsters) {
            if (map.getMonsterByOid(m.getObjectId()) != null) {
                n++;
            }
        }
        return n;
    }

    /**
     * 清场：杀掉本回合召唤的所有怪。
     *
     * @param withDrops true = 掉落物品；false = 不掉落（"不报装备"用 false）
     * @return 实际清掉的数量
     */
    public int wipe(boolean withDrops) {
        int killed = 0;
        for (Monster m : monsters) {
            if (map.getMonsterByOid(m.getObjectId()) != null) {
                map.killMonster(m, null, withDrops);
                killed++;
            }
        }
        monsters.clear();
        return killed;
    }

    @Override
    public String toString() {
        return "ActivityMonsterInstance{event=" + eventKey + ", map=" + getMapId()
                + "「" + getMapName() + "」, monsters=" + monstersText
                + ", spawned=" + getSpawnedCount() + ", alive=" + countAlive() + "}";
    }
}