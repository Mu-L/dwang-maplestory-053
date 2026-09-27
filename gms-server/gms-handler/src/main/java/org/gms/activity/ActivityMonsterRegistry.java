package org.gms.activity;

import org.gms.dao.entity.ActivityMonsterConfigDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件注册表：用数据库里配置的类名，反射创建 {@link ActivityMonsterEvent} 实例。
 *
 * <p>数据库 {@code activity_monster_config.event_class} 直接填 Java 类全限定名，例如
 * {@code org.gms.activity.GreenSnailEvent}。因此新增活动只需要：
 * <ol>
 *   <li>写一个 {@link ActivityMonsterEvent} 子类，提供 {@code public XxxEvent(ActivityMonsterConfigDO)} 构造器</li>
 *   <li>在数据库插一行配置，event_class 填这个类的全限定名</li>
 * </ol>
 * 不需要改这个类。是否启动由数据库 {@code enabled} 字段控制。
 *
 * @author dwang
 * @version 1.0
 */
public final class ActivityMonsterRegistry {

    private static final Logger log = LoggerFactory.getLogger(ActivityMonsterRegistry.class);

    /** 类名 -> Class 缓存，避免每次都反射查找 */
    private static final Map<String, Class<? extends ActivityMonsterEvent>> CLASS_CACHE =
            new ConcurrentHashMap<>();

    private ActivityMonsterRegistry() {
    }

    /**
     * 反射创建事件实例。
     * 约定：实现类必须有一个 {@code public XxxEvent(ActivityMonsterConfigDO)} 构造器。
     *
     * @param config 数据库配置行（含怪物清单）
     * @return 创建失败返回 null，调用方跳过该配置
     */
    public static ActivityMonsterEvent create(ActivityMonsterConfigDO config) {
        String className = config.getEventClass();
        if (className == null || className.trim().isEmpty()) {
            log.warn("[活动怪物] 配置 {} 没有填 event_class，已跳过", config.getEventKey());
            return null;
        }
        className = className.trim();

        try {
            Class<? extends ActivityMonsterEvent> clazz = CLASS_CACHE.get(className);
            if (clazz == null) {
                clazz = Class.forName(className).asSubclass(ActivityMonsterEvent.class);
                CLASS_CACHE.put(className, clazz);
            }

            Constructor<? extends ActivityMonsterEvent> ctor =
                    clazz.getConstructor(ActivityMonsterConfigDO.class);
            return ctor.newInstance(config);
        } catch (ClassNotFoundException e) {
            log.error("[活动怪物] 配置 {} 的类 {} 不存在，请检查 event_class", config.getEventKey(), className);
        } catch (ClassCastException e) {
            log.error("[活动怪物] 配置 {} 的类 {} 不是 ActivityMonsterEvent 的子类",
                    config.getEventKey(), className);
        } catch (NoSuchMethodException e) {
            log.error("[活动怪物] 配置 {} 的类 {} 缺少 public 构造器({})",
                    config.getEventKey(), className, ActivityMonsterConfigDO.class.getSimpleName());
        } catch (Exception e) {
            log.error("[活动怪物] 配置 {} 反射创建 {} 失败", config.getEventKey(), className, e);
        }
        return null;
    }
}