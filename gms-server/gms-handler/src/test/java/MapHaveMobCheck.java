import org.gms.ServerApplication;
import org.gms.constants.id.MapExistMob;
import org.gms.model.dto.QuestSearchReqDTO;
import org.gms.provider.Data;
import org.gms.provider.DataTool;
import org.gms.provider.wz.XMLWZFile;
import org.gms.server.MapStrInfo;
import org.gms.server.StringInfoProvider;
import org.gms.server.quest.QuestActionType;
import org.gms.server.quest.QuestRepository;
import org.gms.server.quest.QuestRequirementType;
import org.gms.server.quest.QuestV2;
import org.gms.server.quest.actions.AbstractQuestActionData;
import org.gms.server.quest.actions.ext.BuffActionData;
import org.gms.server.quest.actions.ext.ItemActionData;
import org.gms.server.quest.actions.ext.SkillActionData;
import org.gms.server.quest.requirements.AbstractQuestRequirementData;
import org.gms.server.quest.requirements.imp.ItemRequirementData;
import org.gms.server.quest.requirements.imp.MobRequirementData;
import org.gms.server.quest.requirements.imp.NpcRequirementData;
import org.gms.util.PathUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


/**
 * 检查是否是野外地图
 *
 * @author dwang
 * @version 2.0
 * @since 2026/9/1 15:30
 */
@SpringBootTest(classes = ServerApplication.class)// 可选：指定测试用的 profile（例如 application-test.yml）
@ActiveProfiles("test")
public class MapHaveMobCheck {

    private static final String DEFAULT_PACKAGE = "string.gen";


    /**
     * 删除不存在的物品
     */
    @Test
    public void checkMapIsMobMap() {
        Map<Integer, List<MapStrInfo>> mobExistMap = StringInfoProvider.getMOB_EXIST_MAP();
        Set<Integer> haveMobSet = new HashSet<>();
        Collection<List<MapStrInfo>> values = mobExistMap.values();
        for (List<MapStrInfo> value : values) {
            for (MapStrInfo mapStrInfo : value) {
                haveMobSet.add(mapStrInfo.mapId);
            }
        }


        for (Integer victoriaMap : MapExistMob.VICTORIA_ISLAND_FIELD_MAPS) {
            if (!haveMobSet.contains(victoriaMap)) {
                System.out.println("没有怪物的地图：" + victoriaMap);
            }
        }

        for (Integer elNathFieldMap : MapExistMob.EL_NATH_FIELD_MAPS) {
            if (!haveMobSet.contains(elNathFieldMap)) {
                System.out.println("没有怪物的地图：" + elNathFieldMap);
            }
        }


    }

}