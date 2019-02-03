package com.jinke.persist.config;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class DefaultEmptyValueHandler implements EmptyValueHandler {

    @Override
    public Object handle(Field field) {
        Class fieldClass = field.getType();
        if (Number.class.isAssignableFrom(fieldClass)) return 0;
        if (fieldClass == String.class) return "";
        if (fieldClass == Date.class) return new Date(System.currentTimeMillis());
        if (fieldClass == Timestamp.class) return new Timestamp(System.currentTimeMillis());

        return null;
    }
}
