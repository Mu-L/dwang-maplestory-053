package org.gms.activity;

import org.gms.client.Character;
import org.gms.constants.id.QuestId;
import org.gms.dao.entity.ActivityMonsterConfigDO;
import org.gms.server.achievement.HiddenMapAchievementManager;
import org.gms.server.quest.QuestStatus;

import java.util.Collection;

/**
 * 2周年活动任务
 * @author dwang
 * @version 1.0
 */
public class Anniversary2stEvent extends ActivityMonsterEvent {

    public Anniversary2stEvent(ActivityMonsterConfigDO config) {
        super(config);
    }

    /** 条件：如果接了2周年庆 */
    @Override
    public boolean shouldSummon(Character chr) {
        boolean q8879 = chr.getQuest(QuestId.QUEST_2ND_ANNIVERSARY_BIRTHDAY_PRESENT_RED_8879).getStatus() == QuestStatus.Status.STARTED;
        boolean q8880 = chr.getQuest(QuestId.QUEST_2ND_ANNIVERSARY_BIRTHDAY_PRESENT_BLUE_8880).getStatus() == QuestStatus.Status.STARTED;
        boolean q8881 = chr.getQuest(QuestId.QUEST_2ND_ANNIVERSARY_CODY_S_QUEST_8881).getStatus() == QuestStatus.Status.STARTED;


        return q8879 || q8880 || q8881;

    }

    /** 地图池：本服的隐藏地图 */
    @Override
    public Collection<Integer> candidateMapIds(Character character) {
        return HiddenMapAchievementManager.getHiddenMapIds();
    }
}
