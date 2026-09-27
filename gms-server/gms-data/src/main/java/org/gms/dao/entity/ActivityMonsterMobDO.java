package org.gms.dao.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 活动怪物-怪物清单。一个活动可以召唤多种怪，一种怪一行。
 *
 * @author dwang
 * @version 1.0
 * @since 2026/9/13 14:26
 */
@Data
@Table("activity_monster_mob")
public class ActivityMonsterMobDO {

    @Id(keyType = KeyType.Auto)
    private Integer id;

    /** 关联 activity_monster_config.id */
    private Integer configId;

    /** 怪物 ID */
    private Integer mobId;

    /** 这一种怪召唤几只 */
    private Integer spawnCount;

    /** 显示顺序 */
    private Integer sortOrder;
}