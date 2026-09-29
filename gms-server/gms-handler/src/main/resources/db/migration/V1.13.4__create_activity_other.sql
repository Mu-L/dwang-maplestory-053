-- =============================================================================
-- 感恩节
-- 8821：金银岛  训话的火鸡（9400505）  黄色火鸡蛋（4031416）
-- 8822：神秘岛  训话的火鸡（9400505）  绿色火鸡蛋（4031417）
-- 8823：全域怪物掉落下面四个
-- 4031418  4031419 4031420 4031421

INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (9400505, 4031416, 1, 1, 8821, 5000);
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (9400505, 4031417, 1, 1, 8822, 5000);


INSERT INTO `drop_data_global` (`continent`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`, `comments`) VALUES (-1, 4031418, 1, 1, 8823, 50000, '感恩节');
INSERT INTO `drop_data_global` (`continent`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`, `comments`) VALUES (-1, 4031419, 1, 1, 8823, 50000, '感恩节');
INSERT INTO `drop_data_global` (`continent`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`, `comments`) VALUES (-1, 4031420, 1, 1, 8823, 50000, '感恩节');
INSERT INTO `drop_data_global` (`continent`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`, `comments`) VALUES (-1, 4031421, 1, 1, 8823, 50000, '感恩节');

-- 刷新周期 半个小时把
INSERT INTO `activity_monster_config` (`event_key`, `event_class`, `name`, `enabled`, `interval_sec`, `notice_type`, `notice_text`, `remark`) VALUES ('THANKE_GIVING_ACTIVITY', 'org.gms.activity.ThanksgivingEvent', '感恩节活动', 1, 600, 6, '【{event}】{monsters} 出现在了「{map}」，请速去攻略！', '玩家进行感恩节活动，换图重刷');
SET @current_activity_monster_id = LAST_INSERT_ID();

INSERT INTO `activity_monster_mob` (`config_id`, `mob_id`, `spawn_count`, `sort_order`) VALUES (@current_activity_monster_id, 9400505, 10, 0);


-- =============================================================================
-- 光明节 8829 - 8830
INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES ( 1130100, 4031445, 1, 1, 8829, 300000);
INSERT INTO `drop_data` ( `dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES ( 2130100, 4031445, 1, 1, 8829, 300000);


-- 8832 奇巧先生的饼干屋
INSERT INTO `drop_data_global` (`continent`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`, `comments`) VALUES (-1, 4031446, 1, 1, 8832, 50000, 'quest_8832');


-- Spot 8869-8870
INSERT INTO `drop_data_global` (`continent`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`, `comments`) VALUES (-1, 4031542, 1, 1, 8869, 10000, 'quest_8869');

-- 官方自定义春节新年活动 获取红包
INSERT INTO `drop_data_global` (`continent`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`, `comments`) VALUES (-1, 4031249, 1, 1, 8208, 1000, 'quest_8208');