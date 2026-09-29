package cmp;

import org.gms.provider.Data;
import org.gms.provider.DataTool;
import org.gms.provider.wz.XMLWZFile;

import java.nio.file.Path;
import java.util.*;

/**
 * TODO
 *
 * @author dwang
 * @version 1.0
 * @since 2026/8/11 10:27
 */
public class QuestCmp {

    public static void main(String[] args) {
        String nowQuest = "E:\\game\\ms\\gms053\\server\\gms53-Server\\gms-server\\gms-handler\\wz-zh-CN\\Quest.wz";
        Map<Integer, String> questNames = getQuestMap(nowQuest);
        Set<Integer> questIds = questNames.keySet();

        String quest48Path = "E:\\game\\ms\\gms053\\server\\gms53-Server\\cms48\\Quest.wz";
        Map<Integer, String> questNames48 = getQuestMap(quest48Path);

        Set<Integer> quest48Ids = questNames48.keySet();
        List<Integer> needAddQuestIds = new ArrayList<>();
        Map<Integer, String> needAddMap = new TreeMap<>();


        for (Integer quest48Id : quest48Ids) {
            if (!questIds.contains(quest48Id)) {
                needAddQuestIds.add(quest48Id);
            }
        }
        needAddQuestIds.stream().sorted().forEach(id -> {
            needAddMap.put(id, questNames48.get(id));
        });

        needAddMap.forEach((id, str) -> {
            System.out.println("任务：" + id + "(" + str + ")");
        });
    }

    private static Map<Integer, String> getQuestMap(String nowQuest) {
        Path enPath = Path.of(nowQuest);
        XMLWZFile xmlwzFile = new XMLWZFile(enPath);
        Data questInfoData = xmlwzFile.getData("QuestInfo.img");
        Map<Integer, String> questNames = new HashMap<>();

        for (Data quest : questInfoData.getChildren()) {
            int questID = Integer.parseInt(quest.getName());
            Data reqInfo = questInfoData.getChildByPath(quest.getName());
            if (reqInfo != null) {
                String name = DataTool.getString("name", reqInfo, "");
                questNames.put(questID, name);

            }
        }
        return questNames;
    }
}
