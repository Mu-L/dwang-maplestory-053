INSERT INTO `activity_monster_config` (`event_key`, `event_class`, `name`, `enabled`, `interval_sec`, `notice_type`, `notice_text`, `remark`) VALUES ('EASTER_ACTIVITY', 'org.gms.activity.EasterEvent', '复活节活动', 1, 600, 6, '【{event}】{monsters} 出现在了「{map}」，请速去攻略！', '玩家进行复活节活动，换图重刷');
SET @current_activity_monster_id = LAST_INSERT_ID();

INSERT INTO `activity_monster_mob` (`config_id`, `mob_id`, `spawn_count`, `sort_order`) VALUES (@current_activity_monster_id, 9400510, 10, 0);
INSERT INTO `activity_monster_mob` (`config_id`, `mob_id`, `spawn_count`, `sort_order`) VALUES (@current_activity_monster_id, 9400511, 10, 1);


-- 1. 绿波普 (9400510) -> 掉落: 绿色复活节彩蛋 (2022066)
-- 对应任务: 8713 (绿色彩蛋任务)
delete from  `drop_data` where dropperid = 9400510;
INSERT INTO drop_data (dropperid, itemid, minimum_quantity, maximum_quantity, questid, chance) VALUES (9400510, 2022066, 1, 1, 8713, 50000);

-- 对应任务: 8875 (绿色彩蛋重复/扩展任务)
INSERT INTO drop_data (dropperid, itemid, minimum_quantity, maximum_quantity, questid, chance) VALUES (9400510, 2022066, 1, 1, 8875, 50000);


-- 2. 黄波普 (9400511) -> 掉落 1: 黄色复活节彩蛋 (2022065)
-- 对应任务: 8701 (黄色彩蛋任务)
delete from  `drop_data` where dropperid = 9400511;
INSERT INTO drop_data (dropperid, itemid, minimum_quantity, maximum_quantity, questid, chance) VALUES (9400511, 2022065, 1, 1, 8701, 50000);

-- 对应任务: 8874 (黄色彩蛋重复/扩展任务)
INSERT INTO drop_data (dropperid, itemid, minimum_quantity, maximum_quantity, questid, chance) VALUES (9400511, 2022065, 1, 1, 8874, 50000);


-- 3. 黄波普 (9400511) -> 掉落 2: 黄金鸡蛋 (4031284)
-- 对应任务: 8876 (疯狂兔子的黄金蛋任务)
-- 黄金蛋为稀有彩蛋，爆率设为 5000 (0.5%)
INSERT INTO drop_data (dropperid, itemid, minimum_quantity, maximum_quantity, questid, chance) VALUES (9400511, 4031284, 1, 1, 8876, 5000);