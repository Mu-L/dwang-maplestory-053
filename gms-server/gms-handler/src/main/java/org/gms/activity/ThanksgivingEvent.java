package org.gms.activity;

import org.gms.client.Character;
import org.gms.constants.id.MapExistMob;
import org.gms.constants.id.QuestId;
import org.gms.dao.entity.ActivityMonsterConfigDO;
import org.gms.server.achievement.HiddenMapAchievementManager;
import org.gms.server.quest.QuestStatus;

import java.util.Collection;

/**
 * 感恩节活动怪物
 *
 * @author dwang
 * @version 1.0
 * @since 2026/9/29 13:28
 */
public class ThanksgivingEvent extends ActivityMonsterEvent {

    public ThanksgivingEvent(ActivityMonsterConfigDO config) {
        super(config);
    }





    /** 条件：感恩节 */
    @Override
    public boolean shouldSummon(Character chr) {
        boolean q8821 = chr.getQuest(QuestId.THANKSGIVING_TURKEY_YELLOW_EGG_HUNT_8821).getStatus() == QuestStatus.Status.STARTED;
        boolean q8822 = chr.getQuest(QuestId.THANKSGIVING_TURKEY_GREEN_EGG_HUNT_8822).getStatus() == QuestStatus.Status.STARTED;


        return q8821 || q8822;

    }

    /** 地图池：金银岛或者神秘岛出现 */
    @Override
    public Collection<Integer> candidateMapIds(Character chr) {
        boolean q8821 = chr.getQuest(QuestId.THANKSGIVING_TURKEY_YELLOW_EGG_HUNT_8821).getStatus() == QuestStatus.Status.STARTED;
        if (q8821) {
            return MapExistMob.VICTORIA_ISLAND_FIELD_MAPS;
        }
        boolean q8822 = chr.getQuest(QuestId.THANKSGIVING_TURKEY_YELLOW_EGG_HUNT_8821).getStatus() == QuestStatus.Status.STARTED;
        if (q8822) {
            return MapExistMob.EL_NATH_FIELD_MAPS;

        }
        return HiddenMapAchievementManager.getHiddenMapIds();
    }
}
