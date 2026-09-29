
DELETE FROM `shops` WHERE `npcid` IN (9300005);
INSERT INTO `shops` (`shopid`, `npcid`) VALUES (9300005, 9300005);

DELETE FROM `shopitems` WHERE `shopid` IN (9300005);
INSERT INTO `shopitems` ( `shopid`, `itemid`, `price`, `pitch`, `position`) VALUES (9300005, 1112804, 20000, 0, 1);
INSERT INTO `shopitems` ( `shopid`, `itemid`, `price`, `pitch`, `position`) VALUES (9300005, 1051130, 100000, 0, 1);
INSERT INTO `shopitems` ( `shopid`, `itemid`, `price`, `pitch`, `position`) VALUES (9300005, 1050122, 100000, 0, 1);

