package com.jinke.persist.config;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class DefaultEmptyValueHandler implements EmptyValueHandler {

    @Override
    public Object handle(Field field) {
        Class fieldClass = field.getType();
        if (fieldClass == Integer.class || fieldClass == int.class) return Integer.valueOf(0);
        if (fieldClass == Byte.class || fieldClass == byte.class) return Byte.valueOf((byte) 0);
        if (fieldClass == Short.class || fieldClass == short.class) return Short.valueOf((short) 0);
        if (fieldClass == Long.class || fieldClass == long.class) return Long.valueOf(0);
        if (fieldClass == Double.class || fieldClass == double.class) return Double.valueOf(0);
        if (fieldClass == Float.class || fieldClass == float.class) return Float.valueOf(0);
        if (fieldClass == BigDecimal.class) return BigDecimal.valueOf(0);
        if (fieldClass == String.class) return "";
        if (fieldClass == Date.class) return new Date(System.currentTimeMillis());
        if (fieldClass == Timestamp.class) return new Timestamp(System.currentTimeMillis());

        return null;
    }
}
