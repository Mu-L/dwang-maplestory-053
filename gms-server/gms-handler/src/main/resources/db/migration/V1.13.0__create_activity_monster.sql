-- ------------------------------------------------------------
-- 活动怪物事件配置
--   activity_monster_config : 一个活动一行。event_class 填 Java 类全限定名，反射实例化。
--                             enabled 由数据库控制，控制台改这里即可启停。
--   activity_monster_mob    : 一个活动可以召唤多种怪，一种怪一行。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `activity_monster_mob`;
DROP TABLE IF EXISTS `activity_monster_config`;

CREATE TABLE `activity_monster_config` (
  `id`           int NOT NULL AUTO_INCREMENT,
  `event_key`    varchar(64)  NOT NULL COMMENT '活动标识（唯一，日志/控制台识别用，与 Java 类无关）',
  `event_class`  varchar(255) NOT NULL COMMENT '★ Java 实现类全限定名，反射实例化 ActivityMonsterEvent',
  `name`         varchar(64)  NOT NULL COMMENT '活动名，文案占位符 {event}',
  `enabled`      tinyint(1)   NOT NULL DEFAULT 1 COMMENT '★ 数据库控制是否启动，控制台可改',
  `interval_sec` int          NOT NULL DEFAULT 600 COMMENT '回合周期（秒），600=10分钟',
  `notice_type`  int          NOT NULL DEFAULT 6 COMMENT 'dropMessage type，6=蓝字',
  `notice_text`  varchar(255) NOT NULL DEFAULT '' COMMENT '占位符 {event}{monsters}{monster}{map}{count}',
  `remark`       varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_event_key`(`event_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '活动怪物事件配置表' ROW_FORMAT = Dynamic;

CREATE TABLE `activity_monster_mob` (
  `id`          int NOT NULL AUTO_INCREMENT,
  `config_id`   int NOT NULL COMMENT '关联 activity_monster_config.id',
  `mob_id`      int NOT NULL COMMENT '怪物 ID',
  `spawn_count` int NOT NULL DEFAULT 1 COMMENT '这一种怪召唤几只',
  `sort_order`  int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_config`(`config_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '活动怪物-怪物清单（一个活动多种怪）' ROW_FORMAT = Dynamic;

-- 示例活动：玩家 > 10 级时，在隐藏地图里召唤 蜗牛×5 + 红蜗牛×5，10 分钟一轮，换图重刷
-- 100100 = 蜗牛     130101 = 红蜗牛
-- 想改怪 / 改数量：只改 activity_monster_mob 表
-- 想停掉活动：把 enabled 改成 0
-- 想换实现类：改 event_class
INSERT INTO `activity_monster_config`
  (`event_key`, `event_class`, `name`, `enabled`, `interval_sec`, `notice_type`, `notice_text`, `remark`)
VALUES
  ('SNAIL_ACTIVITY', 'org.gms.activity.GreenSnailEvent', '蜗牛活动', 1, 600, 6,
   '【{event}】{monsters} 出现在了「{map}」，请速去攻略！',
   '玩家>10级；隐藏地图随机一张；蜗牛×5 + 红蜗牛×5；10分钟一轮，换图重刷');

INSERT INTO `activity_monster_mob` (`config_id`, `mob_id`, `spawn_count`, `sort_order`)
SELECT `id`, 100100, 5, 0 FROM `activity_monster_config` WHERE `event_key` = 'SNAIL_ACTIVITY';

INSERT INTO `activity_monster_mob` (`config_id`, `mob_id`, `spawn_count`, `sort_order`)
SELECT `id`, 130101, 5, 1 FROM `activity_monster_config` WHERE `event_key` = 'SNAIL_ACTIVITY';


-- 一周年活动怪物
delete from  `drop_data` where dropperid = 9400507;
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (9400507, 0, 70, 70, 0, 650000);
--   红色礼盒
INSERT INTO `drop_data` ( `dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES ( 9400507, 4031306, 1, 1, 8800, 50000);
--   蓝色礼盒
INSERT INTO `drop_data` ( `dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (9400507, 4031307 , 1, 1, 8802, 50000);

delete from  `drop_data` where dropperid = 9400506;
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (9400506, 0, 70, 70, 0, 650000);
--   蜡烛
INSERT INTO `drop_data` ( `dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES ( 9400506, 4031305, 1, 1, 8801, 50000);


INSERT INTO `activity_monster_config` (`event_key`, `event_class`, `name`, `enabled`, `interval_sec`, `notice_type`, `notice_text`, `remark`) VALUES ('Anniversary_ACTIVITY', 'org.gms.activity.Anniversary1stEvent', '一周年活动', 1, 300, 6, '【{event}】{monsters} 出现在了「{map}」，请速去攻略！', '玩家进行一周年活动，换图重刷');
SET @current_activity_monster_id = LAST_INSERT_ID();

INSERT INTO `activity_monster_mob` (`config_id`, `mob_id`, `spawn_count`, `sort_order`) VALUES (@current_activity_monster_id, 9400506, 10, 0);
INSERT INTO `activity_monster_mob` (`config_id`, `mob_id`, `spawn_count`, `sort_order`) VALUES (@current_activity_monster_id, 9400507, 1, 1);



-- 两周年
delete from  `drop_data` where dropperid = 9400512;
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (9400512, 0, 70, 70, 0, 650000);
--   红色礼盒
INSERT INTO `drop_data` ( `dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (9400512, 4031306, 1, 1, 8879, 50000);
--   蓝色礼盒
INSERT INTO `drop_data` ( `dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (9400512, 4031307 , 1, 1, 8880, 50000);

delete from  `drop_data` where dropperid = 9400513;
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (9400513, 0, 70, 70, 0, 650000);
--   蜡烛
INSERT INTO `drop_data` ( `dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES ( 9400513, 4031590, 1, 1, 8881, 50000);

INSERT INTO `activity_monster_config` (`event_key`, `event_class`, `name`, `enabled`, `interval_sec`, `notice_type`, `notice_text`, `remark`) VALUES ('Anniversary_ACTIVITY2', 'org.gms.activity.Anniversary2stEvent', '二周年活动', 1, 300, 6, '【{event}】{monsters} 出现在了「{map}」，请速去攻略！', '玩家进行一周年活动，换图重刷');
SET @current_activity_monster_id = LAST_INSERT_ID();

INSERT INTO `activity_monster_mob` (`config_id`, `mob_id`, `spawn_count`, `sort_order`) VALUES (@current_activity_monster_id, 9400513, 10, 0);
INSERT INTO `activity_monster_mob` (`config_id`, `mob_id`, `spawn_count`, `sort_order`) VALUES (@current_activity_monster_id, 9400512, 1, 1);


--激战酷暑
-- =========================================================
-- 简单难度
-- =========================================================

-- 9052 - 战胜酷暑 <简单> - 第2阶段
-- 任务物品：4031167
-- 掉落怪物：1110100、1110101
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (1110100, 4031167, 1, 1, 9052, 50000);
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (1110101, 4031167, 1, 1, 9052, 50000);

-- 9053 - 战胜酷暑 <简单> - 第3阶段
-- 任务物品：4000037
-- 掉落怪物：1210103
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (1210103, 4000037, 1, 1, 9053, 50000);


-- =========================================================
-- 中等难度
-- =========================================================

-- 9055 - 战胜酷暑 <中等> - 第2阶段
-- 任务物品：4031168
-- 掉落怪物：5200002、5200001
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (5200002, 4031168, 1, 1, 9055, 50000);
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (5200001, 4031168, 1, 1, 9055, 50000);

-- 9056 - 战胜酷暑 <中等> - 第3阶段
-- 任务物品：4000086
-- 掉落怪物：5300001
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (5300001, 4000086, 1, 1, 9056, 50000);


-- =========================================================
-- 困难难度
-- =========================================================

-- 9058 - 战胜酷暑 <困难> - 第2阶段
-- 任务物品：4031169
-- 掉落怪物：4230104、4230115
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (4230104, 4031169, 1, 1, 9058, 50000);
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (4230115, 4031169, 1, 1, 9058, 50000);

-- 9059 - 战胜酷暑 <困难> - 第3阶段
-- 任务物品：4000072
-- 掉落怪物：5120003
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (5120003, 4000072, 1, 1, 9059, 50000);




-- 新增盛大易宝掉落
INSERT INTO drop_data_global (continent, itemid, minimum_quantity, maximum_quantity, questid, chance, comments)
VALUES
(- 1, 4031250, 1, 1, 0, 200000, 'NX Card 500 PTS');