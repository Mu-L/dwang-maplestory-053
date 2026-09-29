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
package org.gms.net.server.channel.handlers;

import org.gms.client.Character;
import org.gms.client.Client;
import org.gms.client.character.keybind.KeyBinding;
import org.gms.client.character.skill.Skill;
import org.gms.client.character.skill.SkillFactory;
import org.gms.client.inventory.InventoryType;
import org.gms.constants.game.GameConstants;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 处理客户端发来的快捷键/自动喝药配置（RecvOpcode.CHANGE_KEYMAP，v0.53 = 0x75）。
 *
 * <h2>v0.53 与 v0.83+ 的封包差异（本次修复的核心）</h2>
 * 两个客户端都在 {@code COutPacket} 里先 {@code Encode4(mode)}，但 <b>mode 1 的字段数不同</b>：
 * <pre>
 * v0.53  GMSv53.exe   CFuncKeyMappedMan::SendAutoPotion @ 0x4F6475
 *     Encode4(1);                        // mode
 *     Encode4(*(int*)(this+0x380));      // 自动喝HP药水的道具ID
 *     Encode4(*(int*)(this+0x384));      // 自动喝MP药水的道具ID   &lt;-- v0.83 没有这一项
 *
 * v0.83  `Angel.exe     CFuncKeyMappedMan::OnSetAutoHpPot @ 0x58DE2D
 *     mode 1 -> 只有一个 int（HP 药品ID），MP 走独立的 mode 2（OnSetAutoMpPot @ 0x58DE53）
 * </pre>
 * 旧代码按 v0.83 只 {@code readInt()} 一次，于是：
 * <ul>
 *   <li>MP 药品ID 永远读不到，自动喝MP药水这一格从没被保存过；</li>
 *   <li>mode 2 分支在 v0.53 客户端上永远不会被触发（死代码）。</li>
 * </ul>
 * 现在按 {@code p.available()} 判断是否还有第二个 int，v0.53 / v0.83 两种布局都能正确解析。
 *
 * <h2>mode 0（普通快捷键）</h2>
 * 两种客户端布局一致：{@code Encode4(0); Encode4(count); {Encode4(key); byte type; int action;} * count}，
 * 只覆盖下标 0..88 的普通快捷键，不包含自动喝药。
 */
public final class KeymapChangeHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(KeymapChangeHandler.class);

    /** 普通快捷键列表（两个版本布局一致） */
    private static final int MODE_KEYMAP_LIST = 0;
    /** 自动喝药：v0.53 带 (HP,MP) 两个ID；v0.83+ 只带 HP 一个ID */
    private static final int MODE_AUTO_POTION = 1;
    /** 自动喝MP药水，只有 v0.83+ 客户端才会单独发 */
    private static final int MODE_AUTO_MP_POTION = 2;

    /** 自动喝药在服务端 keymap 里使用的绑定类型：7 = 使用消耗道具 */
    private static final int AUTO_POT_BINDING_TYPE = 7;

    /** 标记“客户端这个版本没有携带该字段”，用于区分“没发”与“发了 0（清空）” */
    private static final int FIELD_ABSENT = Integer.MIN_VALUE;

    @Override
    public final void handlePacket(InPacket p, Client c) {
        if (p.available() < Integer.BYTES) {
            return;
        }
        final Character player = c.getPlayer();
        final int mode = p.readInt();

        if (mode == MODE_KEYMAP_LIST) {
            handleKeymapList(p, player);
        } else if (mode == MODE_AUTO_POTION) {
            // v0.53: 一次带齐 HP、MP 两个药品ID；v0.83+: 只有一个 HP
            if (p.available() < Integer.BYTES) {
                return;
            }
            int hpItemId = p.readInt();
            int mpItemId = p.available() >= Integer.BYTES ? p.readInt() : FIELD_ABSENT;

            applyAutoPot(player, Character.AUTO_HP_POT_KEY, hpItemId);
            if (mpItemId != FIELD_ABSENT) {
                applyAutoPot(player, Character.AUTO_MP_POT_KEY, mpItemId);
            }
        } else if (mode == MODE_AUTO_MP_POTION) {
            // v0.83+ 兼容分支
            if (p.available() >= Integer.BYTES) {
                applyAutoPot(player, Character.AUTO_MP_POT_KEY, p.readInt());
            }
        }
    }

    private void handleKeymapList(InPacket p, Character player) {
        if (p.available() < Integer.BYTES) {
            return;
        }
        int numChanges = p.readInt();
        for (int i = 0; i < numChanges; i++) {
            if (p.available() < 2 * Integer.BYTES + 1) {
                return;     // 包被截断，丢弃剩余部分
            }
            int key = p.readInt();
            int type = p.readByte();
            int action = p.readInt();

            if (type == 1) {
                Skill skill = SkillFactory.getSkill(action);
                if (skill != null) {
                    boolean isBannedSkill = GameConstants.bannedBindSkills(skill.getId());
                    if (isBannedSkill || (!player.isGM() && GameConstants.isGMSkills(skill.getId()))
                            || (!GameConstants.isInJobTree(skill.getId(), player.getJob().getId()) && !player.isGM())) {
                        //for those skills are are "technically" in the beginner tab, like bamboo rain in Dojo or skills you find in PYPQ
                        continue;   // fk that
                    }
                }
            }

            player.changeKeybinding(key, new KeyBinding(type, action));
        }
    }

    /**
     * 写入或清空一个“自动喝药”槽位。
     *
     * @param key    服务端用于保存该槽位的约定键位（{@link Character#AUTO_HP_POT_KEY} / {@link Character#AUTO_MP_POT_KEY}）
     * @param itemId 道具ID；0 表示客户端清空了该格
     */
    private void applyAutoPot(Character player, int key, int itemId) {
        if (itemId == 0) {
            player.getKeymap().remove(key);
            return;
        }
        if (player.getInventory(InventoryType.USE).findById(itemId) == null) {
            // 原本这里直接 disconnect（防止玩家上报背包里没有的消耗道具）。
            // v0.53 的 mode 1 会把 (HP,MP) 一起发上来，其中一格可能是客户端从本地
            // CConfig 默认值填进来的、玩家其实并不持有的药水，直接踢下线会误伤，
            // 因此这里只忽略该格：Character.updateHpMp() 用 findById() 取道具，
            // 无效ID自然什么都不会触发，不存在可利用面。
            log.warn("{} tried to set auto potion to item {} which is not in the USE inventory, ignored",
                    player.getName(), itemId);
            return;
        }
        player.changeKeybinding(key, new KeyBinding(AUTO_POT_BINDING_TYPE, itemId));
    }
}
