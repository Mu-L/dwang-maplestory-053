/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * @description 拍卖行中心脚本
 */
var OldTitle ="\t\t\t\t\t#e欢迎来到#rBeiDou#k脚本中心#n\t\t\t\t\r\n";
var status = -1;
var i = 0;
function start() {
    action(1, 0, 0)
}

function action(mode, type, selection) {


    cm.dispose();
    cm.openNpc(9900001, "achieve_成就完成");
    return;


}

function doSelect(selection) {
    switch (selection) {
        // 非GM功能
		case 999:
            openNpc("测试脚本");
            break;
        case 69:
            openNpc("快速转职");
            break;
        case 70:
            openNpc("技能学习");
            break;
        case 71:
            openNpc("万能传送");
            break;
        case 72:
            openNpc("转世重生");
            break;
        case 0:
            openNpc("新人福利");
            break;
        case 1:
            openNpc("achieve_显示成就");
            break;
        case 2:
            openNpc("achieve_自选枫叶");
            break;
        case 3:
            openNpc("_mobvac");
            break;
        case 4:
            cm.getPlayer().saveLocation("FREE_MARKET");
            cm.warp(910000000, "out00");
            break;
        case 5:
            openNpc("当前地图掉落");
            break;
         case 6:
            openNpc("在线奖励");
            break;
        // GM功能
        case 61:
            openNpc("万能传送");
            break;
        case 62:
            cm.dispose();
            cm.openShopNPC(9900001);
            cm.dispose();
            break;
        case 63:
            openNpc("Salon");
            break;
        case 64:
            openNpc("UI查询");
            break;	
        case 65:
            openNpc("一键删除道具");
            break;
        case 66:
            openNpc("一键刷道具");
            break;
        case 67:
            openNpc("Example1")
            break;
        case 68:
            openNpc("Example2")
            break;


        default:
            cm.sendOk("该功能暂不支持，敬请期待！");
            cm.dispose();
    }
}

function openNpc(scriptName) {
    cm.dispose();
    cm.openNpc(9900001, scriptName);
}