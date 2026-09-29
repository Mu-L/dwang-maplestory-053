package cmp;


import io.micrometer.common.util.StringUtils;
import org.gms.provider.*;
import org.gms.provider.wz.XMLWZFile;
import org.gms.server.MapStrInfo;
import org.gms.util.PathUtils;
import org.gms.util.StringUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * ID与常量生成器（支持新旧WZ格式兼容）
 *
 * @author dwang
 * @version 2.0
 * @since 2026/9/1 15:30
 */
public class MapCmp {

    private static final String DEFAULT_PACKAGE = "string.gen";

    public static void main(String[] args) {
        Path root = PathUtils.getRootPath("bms");
        Path cnPath =      Path.of(root + "\\gms-server\\gms-handler\\wz-zh-CN");
        Path cnStrPath =      Path.of(root + "\\gms-server\\gms-handler\\wz-zh-CN\\String.wz");

        Path enPath =      Path.of(root + "\\gms-server\\gms-handler\\wz");
        Path pathcms48 =      Path.of(root + "\\cms48");
        Path pathcms48Str =      Path.of(root + "\\cms48\\String.wz");


        Map<Integer, String> cnNames = StringCmp.WzResolver.MAP_RESOLVER.resolve(pathcms48Str, "Map.img", null, "cn");

        List<Integer> maps = loadMap(enPath);
        List<Integer> map48 = loadMap(pathcms48);
        List<Integer> needAddIds = new ArrayList<>();
        Map<Integer, String> needAddMap = new TreeMap<>();
        for (Integer npdId : map48) {
            if (!maps.contains(npdId)) {
                needAddIds.add(npdId);
            }
        }
        needAddIds.stream().sorted().forEach( id -> {
            String s = cnNames.get(id);
            if (!StringUtils.isEmpty(s)) {
                needAddMap.put(id, s);
            }
        });

        needAddMap.forEach((id, str) -> {
            System.out.println("Map：" + id + "(" + str + ")");
        });

    }

    private static List<Integer> loadMap(Path enPath) {
        List<Integer> maps = new ArrayList<>();
        Path wzPath =      Path.of(enPath + "\\Map.wz");
        DataProvider mapSource = new XMLWZFile(wzPath);
        DataDirectoryEntry root = mapSource.getRoot();

        for (DataDirectoryEntry objData : root.getSubdirectories()) {
            if (!"Map".contentEquals(objData.getName())) {
                continue;
            }

            // 遍历 Map/Map0, Map/Map1 等子目录
            for (DataDirectoryEntry mapFileDir : objData.getSubdirectories()) {
                if (!mapFileDir.getName().contains("Map")) {
                    continue;
                }

                List<DataFileEntry> files = mapFileDir.getFiles();
                for (DataFileEntry file : files) {
                    String path = objData.getName() + "/" + mapFileDir.getName() + "/" + file.getName();
                    Data mapData = mapSource.getData(path);
                    if (mapData == null) {
                        System.err.println("无法读取文件，跳过该地图：" + path);
                        continue; // 改为 continue，避免单文件错误导致整个引擎初始化终止
                    }

                    String fileName = file.getName();
                    int dotIndex = fileName.indexOf('.');
                    if (dotIndex == -1) {
                        continue;
                    }

                    int mapId;
                    try {
                        mapId = Integer.parseInt(fileName.substring(0, dotIndex));
                    } catch (NumberFormatException e) {
                        // 忽略非数字名称的 img 文件
                        continue;
                    }
                    maps.add(mapId);
                }
            }
        }
        return maps;
    }

    private static Map<Integer, String> cmpStringMap(Path cnPath, Path pathcms48, String subImg, WzResolver resolver) {
        Map<Integer, String> cnNames = resolver.resolve(cnPath, subImg, null, "cn");
        Map<Integer, String> cms48Names = resolver.resolve(pathcms48, subImg, null, "cn");

        Set<Integer> cnIds = cnNames.keySet();
        Set<Integer> cn48Ids = cms48Names.keySet();

        List<Integer> needAddQuestIds = new ArrayList<>();
        Map<Integer, String> needAddMap = new TreeMap<>();


        for (Integer quest48Id : cn48Ids) {
            if (!cnIds.contains(quest48Id)) {
                needAddQuestIds.add(quest48Id);
            }
        }
        needAddQuestIds.stream().sorted().forEach( id -> {
            needAddMap.put(id, cms48Names.get(id));
        });

        return needAddMap;
    }

    /**
     * WZ节点解析策略函数接口
     */
    @FunctionalInterface
    interface WzResolver {
        Map<Integer, String> resolve(Path wzPath, String imgFileName, String subNodeName, String langType);

        /**
         * 通用平铺节点解析 (Mob.img, Npc.img)
         */
        // 2. 静态常量（等价于 public static final WzResolver FLAT_NAME_RESOLVER = ...）
        WzResolver FLAT_NAME_RESOLVER = (wzPath, imgFileName, subNodeName, langType) -> {
            Data data = loadImgData(wzPath, imgFileName);
            if (data == null) return Collections.emptyMap();

            Map<Integer, String> result = new TreeMap<>();
            String defaultPrefix = imgFileName.replace(".img", "");

            for (Data child : data.getChildren()) {
                if (!isDigit(child.getName())) continue;
                int id = Integer.parseInt(child.getName());
                String name = DataTool.getString(child.getChildByPath("name"), "NO_NAME");

                if ("en".equals(langType)) {
                    result.put(id, formatConstantName(name, id, defaultPrefix));
                } else {
                    result.put(id, name);
                }
            }
            return result;
        };

        /**
         * 地图节点解析 (Map.img -> MapCategory -> MapId)
         */
        WzResolver MAP_RESOLVER = (wzPath, imgFileName, subNodeName, langType) -> {
            Data data = loadImgData(wzPath, imgFileName);
            if (data == null) return Collections.emptyMap();

            Map<Integer, String> result = new TreeMap<>();
            for (Data category : data.getChildren()) {
                for (Data child : category.getChildren()) {
                    if (!isDigit(child.getName())) continue;
                    int id = Integer.parseInt(child.getName());
                    String name = DataTool.getString(child.getChildByPath("mapName"), "NO_NAME");

                    if ("en".equals(langType)) {
                        result.put(id, formatConstantName(name, id, "Map"));
                    } else {
                        String streetName = DataTool.getString(child.getChildByPath("streetName"), "NO_NAME");
                        result.put(id, streetName + " - " + name);
                    }
                }
            }
            return result;
        };

        /**
         * 物品节点统一解析 (自动兼容：Img根节点包含还是内部二级子节点包含)
         */
        WzResolver ITEM_RESOLVER = (wzPath, imgFileName, subNodeName, langType) -> {
            Data root = loadImgData(wzPath, imgFileName);
            if (root == null) return Collections.emptyMap();

            // 归一化提取包含物品ID列表的容器节点
            Data itemContainer = locateItemContainer(root, subNodeName);
            if (itemContainer == null) return Collections.emptyMap();

            Map<Integer, String> result = new TreeMap<>();
            String defaultPrefix = (subNodeName != null ? subNodeName : imgFileName).replace(".img", "");

                for (Data child : itemContainer.getChildren()) {
                    if (!isDigit(child.getName())) continue;
                    int id = Integer.parseInt(child.getName());

                    String name = DataTool.getString(child.getChildByPath("name"), "NO_NAME");
                    if ("en".equals(langType)) {
                        result.put(id, formatConstantName(name, id, defaultPrefix));
                    } else {
                        String desc = DataTool.getString(child.getChildByPath("desc"), "NO_NAME");
                        if (!"NO_NAME".equals(desc) && !desc.isBlank()) {
                            name = name + " - " + desc;
                        }
                        result.put(id, name);
                    }
                }
            return result;
        };

        /**
         * 物品节点统一解析 (自动兼容：Img根节点包含还是内部二级子节点包含)
         */
        WzResolver EQP_RESOLVER = (wzPath, imgFileName, subNodeName, langType) -> {
            Data root = loadImgData(wzPath, imgFileName);
            if (root == null) return Collections.emptyMap();

            // 归一化提取包含物品ID列表的容器节点
            Data itemContainer = null;
            if ("en".equals(langType)) {
                itemContainer = locateItemContainer(root, "Eqp");
            } else {
                itemContainer = locateItemContainer(root, subNodeName);

            }
            if (itemContainer == null) return Collections.emptyMap();

            Map<Integer, String> result = new TreeMap<>();
            String defaultPrefix = (subNodeName != null ? subNodeName : imgFileName).replace(".img", "");

            for (Data child : itemContainer.getChildByPath(subNodeName)) {


                if (!isDigit(child.getName())) continue;
                int id = Integer.parseInt(child.getName());

                String name = DataTool.getString(child.getChildByPath("name"), "NO_NAME");
                if ("en".equals(langType)) {
                    result.put(id, formatConstantName(name, id, defaultPrefix));
                } else {
                    String desc = DataTool.getString(child.getChildByPath("desc"), "NO_NAME");
                    if (!"NO_NAME".equals(desc) && !desc.isBlank()) {
                        name = name + " - " + desc;
                    }
                    result.put(id, name);
                }
            }
            return result;
        };
        /**
         * 地图节点解析 (Map.img -> MapCategory -> MapId)
         */
        WzResolver QUEST_RESOLVER = (wzPath, imgFileName, subNodeName, langType) -> {
            Data data = loadImgData(wzPath, imgFileName);
            if (data == null) return Collections.emptyMap();

            Map<Integer, String> result = new TreeMap<>();
            for (Data child : data.getChildren()) {
                if (!isDigit(child.getName())) continue;
                int id = Integer.parseInt(child.getName());
                String name = DataTool.getString(child.getChildByPath("name"), "NO_NAME");

                if ("en".equals(langType)) {
                    result.put(id, formatConstantName(name, id, "Quest"));
                } else {
                    String parentName = DataTool.getString(child.getChildByPath("parent"), "NO_NAME");
                    if (!"NO_NAME".equals(parentName) && !parentName.isBlank()) {
                        name = parentName + " - " + name;
                    }
                    result.put(id, name);
                }
            }
            return result;
        };
    }

    // ==================== 辅助与工具方法 ====================

    /**
     * 加载 WZ 数据文件
     */
    private static Data loadImgData(Path wzPath, String imgName) {
        try {
            XMLWZFile xmlwzFile = new XMLWZFile(wzPath);
            return xmlwzFile.getData(imgName);
        } catch (Exception e) {
            System.err.println("读取 WZ 节点失败 [" + wzPath.getFileName() + " -> " + imgName + "]: " + e.getMessage());
            return null;
        }
    }

    /**
     * 自动寻找真实的物品ID节点容器（兼容新旧结构）
     * 1. 优先尝试直接在 root 节点寻找指定名称的子节点（旧版：Item.img -> Etc）
     * 2. 其次尝试直接在 root 节点下找与 imgFileName 同名的子节点（新版：Etc.img -> Etc）
     * 3. 若无该层嵌套，则 root 本身即为容器
     */
    private static Data locateItemContainer(Data root, String subNodeName) {
        if (subNodeName != null) {
            Data target = root.getChildByPath(subNodeName);
            if (target != null) return target;
        }

        // 去掉 .img 后缀尝试查找同名子目录
        String cleanName = root.getName().replace(".img", "");
        Data sameNameChild = root.getChildByPath(cleanName);
        if (sameNameChild != null) {
            return sameNameChild;
        }

        // 默认 root 即为挂载点
        return root;
    }

    private static boolean isDigit(String str) {
        return str != null && str.matches("\\d+");
    }

    private static void buildJava(Map<Integer, String> cnNames, Map<Integer, String> enNames, Path outputDir, String subImg) throws IOException {
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        String imgName = subImg.replace(".img", "");
        String className = toPascalCase(imgName) + "Id";
        String javaCode = generateJavaClassCode(DEFAULT_PACKAGE, className, imgName, enNames, cnNames);

        Path javaFilePath = outputDir.resolve(className + ".java");
        Files.writeString(javaFilePath, javaCode, StandardCharsets.UTF_8);
        System.out.println("Generated [" + DEFAULT_PACKAGE + "]: " + javaFilePath.toAbsolutePath());
    }

    private static String formatConstantName(String rawName, int id, String defaultSup) {
        if (rawName == null || rawName.equals("NO_NAME")) {
            return defaultSup + "_" + id;
        }

        String cleanName = rawName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", " ").trim();
        if (cleanName.isEmpty()) {
            return defaultSup + "_" + id;
        }

        String formatted = cleanName.replaceAll("\\s+", "_").toUpperCase();
        if (Character.isDigit(formatted.charAt(0))) {
            formatted = defaultSup + "_" + formatted;
        }

        return formatted;
    }

    private static String toPascalCase(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private static String generateJavaClassCode(String packageName, String className, String defaultSup, Map<Integer, String> engMap, Map<Integer, String> chMap) {
        StringBuilder sb = new StringBuilder();

        if (packageName != null && !packageName.isBlank()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        sb.append("public class ").append(className).append(" {\n\n");

        engMap.forEach((id, name) -> {
            String constantName = formatConstantName(name, id, defaultSup);
            String cnName = escapeJavadoc(chMap.getOrDefault(id, "未知名称"));

            sb.append("    /**\n");
            sb.append("     * [").append(cnName).append("]\n");
            sb.append("     */\n");
            sb.append("    public static final int ").append(constantName).append("_").append(id).append(" = ").append(id).append(";\n\n");
        });

        sb.append("}\n");
        return sb.toString();
    }

    private static String escapeJavadoc(String input) {
        if (input == null) return "";
        return input.replace("*/", "* /").replace("\n", "\n     * ");
    }

    // 辅助封装任务对象
    private static class ItemTask {
        String cnImgFile;
        String enImgFile;
        String cnSubNode;
        String enSubNode;
        String outputName;

        ItemTask(String cnImgFile, String enImgFile, String cnSubNode, String enSubNode) {
            this.cnImgFile = cnImgFile;
            this.enImgFile = enImgFile;
            this.cnSubNode = cnSubNode;
            this.enSubNode = enSubNode;
            this.outputName = cnSubNode + ".img";
        }
    }
}