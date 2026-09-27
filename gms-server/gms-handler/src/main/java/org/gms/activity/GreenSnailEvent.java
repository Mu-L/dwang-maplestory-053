package org.gms.activity;

import org.gms.client.Character;
import org.gms.dao.entity.ActivityMonsterConfigDO;
import org.gms.server.achievement.HiddenMapAchievementManager;

import java.util.Collection;

/**
 * 示例活动：玩家等级 &gt; 10 时，在隐藏地图里召唤一批绿蜗牛。
 *
 * <p>这个类就是"活动怪物事件"的全部业务逻辑 —— 只有两件事：
 * <ol>
 *   <li>条件：玩家等级 &gt; 10</li>
 *   <li>地图池：本服已登记的所有隐藏地图</li>
 * </ol>
 * 召唤几只、多久一轮、刷什么怪、喊什么话，全部来自数据库配置。
 *
 * @author dwang
 * @version 1.0
 */
public class GreenSnailEvent extends ActivityMonsterEvent {

    public GreenSnailEvent(ActivityMonsterConfigDO config) {
        super(config);
    }

    /** 条件：玩家等级大于 10 级 */
    @Override
    public boolean shouldSummon(Character chr) {
        return chr != null && chr.getLevel() > 10;
    }

    /** 地图池：本服的隐藏地图 */
    @Override
    public Collection<Integer> candidateMapIds() {
        return HiddenMapAchievementManager.getHiddenMapIds();
    }
}
