package com.jinke.persist.utils;

import com.jinke.persist.config.PersistConfiguration;

import java.lang.reflect.Field;

public class ReflectUtils {
    /**
     * 获取field的value
     * @param field field
     * @param object object
     * @param convertNull 是否转换空值
     *                    false的话，直接返回field的值
     *                    true的话, 会使用EmptyValueHandler进行转换
     *                    空值判断条件：为null 或者 空字符串
     * @return
     */
    public static Object getFieldValue(Field field, Object object, boolean convertNull, PersistConfiguration persistConfiguration) {
        try {
            field.setAccessible(true);
            Object value = field.get(object);
            if (!convertNull) return value;

            if (value != null &&
                    (!(value instanceof String) || !ColumnUtil.empty((String) value)) ) return value;

            if (persistConfiguration.getCustomEmptyHandler() != null) {
                Object ret = persistConfiguration.getCustomEmptyHandler().handle(field);
                if (ret != null) return ret;
            }
            return persistConfiguration.getDefaultEmptyHandler().handle(field);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public static Object getFieldValue(Field field, Object object, PersistConfiguration persistConfiguration) {
        return getFieldValue(field, object, true, persistConfiguration);
    }
}
