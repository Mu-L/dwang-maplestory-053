/*
 *  NPC 名称: 疯狂兔子 (Mad Bunny)
 *  功能: 复活节活动 NPC
 *  架构: OdinMS JavaScript
 */

var status = -1;
var questId = 8876;
var itemId = 4031284; // 金色彩蛋

function start(mode, type, selection) {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }

    if (mode == 0) {
        // 在 sendYesNo 界面点“否”或取消
        if (status == 0) {
            cm.sendNext("你简直是在浪费我的口舌。走开！");
            cm.dispose();
            return;
        }
        status--;
    } else {
        status++;
    }

    // 1. 基础条件检查（等级与时间）
    if (cm.getPlayer().getLevel() < 8) {
        cm.sendNext("哟。我是罗伊，但我的兄弟们都叫我‘疯狂兔子’。我讨厌复活节，今年我要彻底搞垮它。不过我觉得你现在帮不上什么忙，因为你看起来太弱了。去变强一点，也许到时候我们能谈谈正事。");
        cm.dispose();
        return;
    }

    // 2. 获取任务状态 (0 = 未接取, 1 = 进行中, 2 = 已完成)
    var qStatus = cm.getQuestStatus(questId);

    // --- 分支 A：任务已完成过 (qStatus == 2) ---
    if (qStatus == 2) {
        if (status == 0) {
            cm.sendYesNo("嘿，很高兴再见到你！最近怎么样？对了，你有帮我找到更多的 #b#t" + itemId + "##k 吗？");
        } else if (status == 1) {
            cm.sendNext("太棒了。听起来是个好消息。我会在这里等你的。");
            cm.forceStartQuest(questId); // 重新设置为进行中
            cm.dispose();
        }
        return;
    }

    // --- 分支 B：任务进行中 (qStatus == 1) ---
    if (qStatus == 1) {
        if (!cm.haveItem(itemId, 1)) {
            cm.sendNext("你根本没有金色彩蛋！！伙计……如果你要是找到了，记得来找我，好吗？");
            cm.dispose();
            return;
        }

        if (status == 0) {
            cm.sendYesNo("哇！你找到金色彩蛋了？那可是复活节彩蛋里最稀有的！你打算拿它怎么办？如果你给我，我会给你一些经验值！虽然我不常给经验，不能保证你能拿到多少，但生活就是一场赌博对吧？或者，我和所有商店老板都有约定，他们会花大价钱买下它。（这些经验值根据你的等级，最多可能让你升将近两级。）");
        } else if (status == 1) {
            if (cm.haveItem(itemId, 1)) {
                cm.gainItem(itemId, -1);

                // 随机计算经验值
                var rand = Math.floor(Math.random() * 10000) + 1;
                var exp = 100;

                if (rand <= 5000) {
                    exp = 100;
                } else if (rand <= 8500) {
                    exp = 1000;
                } else if (rand <= 9999) {
                    exp = 10000;
                } else {
                    exp = 100000;
                }

                cm.gainExp(exp);
                cm.forceCompleteQuest(questId);
                cm.sendNext("成交！希望你能拿到不少经验值！\r\n\r\n我给了你 " + exp + " 点经验。保重！");
            } else {
                cm.sendNext("你根本没有金色彩蛋！！伙计……如果你要是找到了，记得来找我，好吗？");
            }
            cm.dispose();
        }
        return;
    }

    // --- 分支 C：未接取任务 (qStatus == 0) ---
    if (status == 0) {
        cm.sendYesNo("明白了，所以这意味着你能通过把金色彩蛋交给我们来帮我们忙，对吗？");
    } else if (status == 1) {
        cm.sendNext("好的……祝你好运！！！");
        cm.forceStartQuest(questId);
        cm.dispose();
    }
}