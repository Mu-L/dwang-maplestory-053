/*
 * NPC 名称: 三情法师
 * 功能: 婚庆传送 / 结婚服务NPC
 */

function start() {
    status = -1;
    action(1, 0, 0);
}

var 冒险币 = 5000;

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    } else {
        if (status >= 0 && mode == 0) {
            cm.sendOk("如果你什么时候想结婚了，随时可以再来看看我哦。。。");
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        if (status == 0) {
            cm.sendSimple("你要去红鸾宫吗？我可以送你过去。。。\r\n那里可是举办美好婚礼的圣殿哦。。。\r\n\r\n#r你想挑选哪种婚礼类型呢？#k\r\n\r\n#d#L0#中式婚礼#l\r\n");
        } else if (status == 1) {
            if (selection == 0) { // 传送至红鸾宫/中式婚礼地图
                cm.sendNext("好的，我这就送你去红鸾宫，祝你找到属于你的幸福哦。。。");
            }  else {
                cm.dipose();
                 return;
            }
        } else if(status == 2) {
            cm.getPlayer().saveLocation("WORLDTOUR");
            cm.warp(700000000, 0);
            cm.dispose();
            return;
        } else{
            cm.dispose();
            return;
        }
    }
}