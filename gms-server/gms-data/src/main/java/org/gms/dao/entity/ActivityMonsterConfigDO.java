package org.gms.dao.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.util.List;

/**
 * 活动怪物事件配置（一个活动一行）。
 *
 * <p>只存静态数据（实现类、周期、文案、启停）。"是否召唤"的业务条件在 Java 子类里。
 *
 * @author dwang
 * @version 1.0
 * @since 2026/9/13 14:26
 */
@Data
@Table("activity_monster_config")
public class ActivityMonsterConfigDO {

    @Id(keyType = KeyType.Auto)
    private Integer id;

    /** 活动标识（唯一），日志/控制台识别用 */
    private String eventKey;

    /** Java 实现类全限定名，反射实例化 ActivityMonsterEvent */
    private String eventClass;

    /** 活动名，文案占位符 {event} */
    private String name;

    /** 是否启用 —— 由数据库控制，控制台改这里即可启停 */
    private Boolean enabled;

    /** 回合周期（秒），600 = 10 分钟 */
    private Integer intervalSec;

    /** dropMessage type，6 = 蓝字 */
    private Integer noticeType;

    /** 文案模板：{event} {monsters} {monster} {map} {count} */
    private String noticeText;

    private String remark;

    /** 本活动要召唤的怪物清单，来自 activity_monster_mob，非本表字段 */
    @Column(ignore = true)
    private List<ActivityMonsterMobDO> mobs;
}