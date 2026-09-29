package org.gms.activity;

import org.gms.client.Character;
import org.gms.constants.id.QuestId;
import org.gms.dao.entity.ActivityMonsterConfigDO;
import org.gms.server.achievement.HiddenMapAchievementManager;
import org.gms.server.quest.QuestStatus;

import java.util.Collection;

/**
 * 复活节任务
 * @author dwang
 * @version 1.0
 */
public class EasterEvent extends ActivityMonsterEvent {

    public EasterEvent(ActivityMonsterConfigDO config) {
        super(config);
    }

    /** 条件：如果接了2周年庆 */
    @Override
    public boolean shouldSummon(Character chr) {
        boolean q8701 = chr.getQuest(QuestId.QUEST_2006_EASTER_MAD_BUNNY_S_EASTER_YELLOW_8701).getStatus() == QuestStatus.Status.STARTED;
        boolean q8713 = chr.getQuest(QuestId.QUEST_2006_EASTER_MAD_BUNNY_S_EASTER_GREEN_8713).getStatus() == QuestStatus.Status.STARTED;
        boolean q8874 = chr.getQuest(QuestId.EASTER_MAD_BUNNY_S_EASTER_YELLOW_8874).getStatus() == QuestStatus.Status.STARTED;
        boolean q8875 = chr.getQuest(QuestId.EASTER_MAD_BUNNY_S_EASTER_GREEN_8875).getStatus() == QuestStatus.Status.STARTED;
        boolean q8876 = chr.getQuest(QuestId.EASTER_MAD_BUNNY_S_EASTER_GREEN_8876).getStatus() == QuestStatus.Status.STARTED;


        return q8701 || q8713 || q8874 || q8875 || q8876;

    }

    /** 地图池：本服的隐藏地图 */
    @Override
    public Collection<Integer> candidateMapIds(Character character) {
        return HiddenMapAchievementManager.getHiddenMapIds();
    }
}
