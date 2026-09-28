## 20260928
1. 修复复活节任务
2. 烧烤任务
### Server
1. 迁移东方神州（CN）地图组：新增 37 张地图（红鸾宫/上海/大擂台，Map\Map7 段），配套 Tile/Obj/Back 素材、BGM、怪物 9619999、NPC 9310004-9310007 / 9310013-9310019 / 9310021 / 9330042 / 9900007，并补 String.wz 的 Npc/Mob 名称。
2. 迁移任务 4100-4109（QuestInfo/Say/Check/Act 全套），随后去掉 4107、4108，换成原版兑换任务 8515。
3. 地图 102000000 新增 NPC 9310000（勇士部落飞上海）与旁边的飞机元件。
4. 任务冷却提示改中文：这个任务可在xx时xx分xx秒后重新开始。
### Client
1. client-dist 1.1.6：客户端 Data 同步新增上述地图、素材、任务、NPC、字符串（Data\Map、Data\Quest、Data\Npc、Data\String、Data\Sound\BgmCN 等）。
2. client-dist 1.1.7：插件 Hook.dll 修复商城 / ITC 按账号性别过滤商品的问题（账号性别与角色性别不一致时买不到本性别装备）；config.ini 新增 MouseWheelCursorFix（滚轮不再把光标甩到右下角，默认开）。


## 20260927
### Server
1. 设计怪物系统任务相关逻辑：随机地图，实现随机怪物在指定条件召唤。
2. 实现1周年庆、 2周年庆任务
3. 新增部分CN的道具，为任务做准备
4. 新增 盛大易宝 掉落，作为点卷。 爆率10%。
### Client
1. 新增客户端插件，object可能出现在地图前景导致覆盖地图。例如军营地图
2. 优化部分地图因视角看不到地图最下面控制

## 20260926
1.修复商城商品永久
2.宠物可复活和转移亲密度

## 20260925
1. 修复商城无法购买8开头SN物品（任务道具）
2. 修复重复提示问题
3. 修复寂寞的吉夫任务

## 20260924
1. 修复任务：红桃A的秘密能力、情人节：巧克力篮 导致的游戏闪退。
2. 修复打怪任务提示
3. 修复创建账号性别选择
4. hint改为dropmessage
5. 事件发布更新，从springContextUtils发
6. 修复技能书无法使用问题
7. 屏蔽黄波普、绿波普召唤
8. 部分任务任务道具没有回收问题

## 20260919
1. 更新完成就任务。
2. 打TAG1.0.0


## 20260831
1. 修复完整的圣诞节雪球活动
![](../asset/Snipaste_2026-08-27_23-28-46.png)
2. 修复九灵龙蛋任务因版本差异无法兑换黑龙项链使用的卷轴问题
3. 从BMS的掉落文件同步掉落。详情查看：``` origin.DropServiceTest```
```sql
-- 需要修改表结构。当前表结构是单个droperId和itemId是组合键。无法适配同一个DropperId掉落多个相同ItemId的情况，而且这多个掉落概率是不一样的，不知道官方为什么这么设计
-- 需要修改表结构如下：
ALTER TABLE drop_data DROP INDEX dropperid;
-- 2. 删除重复/冗余的索引
ALTER TABLE drop_data DROP INDEX dropperid_2;
ALTER TABLE drop_data DROP INDEX mobid;
-- 3. 新建普通的联合索引 (dropperid, itemid)
CREATE INDEX idx_dropper_item ON drop_data (dropperid, itemid);
```
4. 修复怪物掉落位置没有更新问题


## 20260824
### 任务相关
1. 修复Quest.wz相关的解析缺失。
2. 重构Quest相关的处理
3. 新增任务管理后台显示
![](../asset/Snipaste_2026-08-24_10-57-46.png)

### 事件相关
1. 检查053可以开启的的事件
2. 修复部分事件因版本问题无法获取item的问题、开船码头显示问题
3. 组队任务重大BUG！部分Portal可以直接通过，被处理像普通过图一样，代码逻辑错误。当前解决方式也有问题```org.gms.server.maps.GenericPortal.checkEventCantEnter```
4. 新增任务管理器管理。（后续功能，添加奖励和经验配置等）
5. Boss测试，都可以攻略。建议画质调节到最低，打BOSS前重启客户端，否则容易炸
![](../asset/event-config.png)
![](../asset/Snipaste_2026-08-23_16-30-57.png)

### 其他
1. 修复账号自动创建。默认的生日```2005-05-11```, 在文件：```org.gms.property.DefaultDates```修改
2. 修复丢物品无法触发reactor,如果state不为0的type = 100无法触发任务 ```org.gms.server.maps.MapleMap.activateItemReactors```。
3. 新增```天空组队```女神日记本掉落



## 20260817
### 初步重构记录
详情查看： [重构记录](log/20260817项目重构整理.md)

## 20260810
### 重构
1. 拆分项目，```gms-data-provider``` 、``` gms-ui-admin```项目解耦
2. 打算迁移admin的管理和实际使用拆分



## 20260727
### 修复技能
所有主动技能效果和伤害修复

详情查看： [技能修复](log/skill_log.md)

## 20260721
### 修复商城
1. 修复商城操作相关包头内容
2. 修复商城物品在背包无法正常显示问题
3. 修复商城装备穿戴无法正常显示问题
4. 修复宠物相关的包头
### 修复怪物
1. 修复怪物移动相关包
2. 修复技能给怪物上Debuff完全失效问题
3. 修复怪物释放技能给玩家Debuff失效的问题
### 修复召唤物
1. 修复召唤物相关包、召唤、移动攻击
![](../asset/Snipaste_2026-07-21_14-39-56.png)



## 20260713
1. 冷却时间包修复
2. BUFF显示修复
3. GM隐身技能修复
4. quest = 1028 修复 -> nextQuest执行修复
5. 多个角色加载修复
6. 仓库使用修复

## 20260706
1. 修复登录