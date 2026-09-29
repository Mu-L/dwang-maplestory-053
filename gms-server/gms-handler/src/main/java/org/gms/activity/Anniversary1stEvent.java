package org.gms.activity;

import org.gms.client.Character;
import org.gms.constants.id.QuestId;
import org.gms.dao.entity.ActivityMonsterConfigDO;
import org.gms.server.achievement.HiddenMapAchievementManager;
import org.gms.server.quest.QuestStatus;

import java.util.Collection;

/**
 * 一周年活动任务
 * @author dwang
 * @version 1.0
 */
public class Anniversary1stEvent extends ActivityMonsterEvent {

    public Anniversary1stEvent(ActivityMonsterConfigDO config) {
        super(config);
    }

    /** 条件：如果接了1周年庆 */
    @Override
    public boolean shouldSummon(Character chr) {
        boolean q8800 = chr.getQuest(QuestId.ANNIVERSARY_BIRTHDAY_PRESENT_RED_8800).getStatus() == QuestStatus.Status.STARTED;
        boolean q8801 = chr.getQuest(QuestId.ANNIVERSARY_CODY_S_QUEST_8801).getStatus() == QuestStatus.Status.STARTED;
        boolean q8802 = chr.getQuest(QuestId.ANNIVERSARY_BIRTHDAY_PRESENT_BLUE_8802).getStatus() == QuestStatus.Status.STARTED;


        return q8800 || q8801 || q8802;

    }

    /** 地图池：本服的隐藏地图 */
    @Override
    public Collection<Integer> candidateMapIds(Character character) {
        return HiddenMapAchievementManager.getHiddenMapIds();
    }
}
