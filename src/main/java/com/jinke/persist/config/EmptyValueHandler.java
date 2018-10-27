package com.jinke.persist.config;

import java.lang.reflect.Field;

/**
 * 用于处理：字段为null，或者为空字符串
 */
public interface EmptyValueHandler {
    /**
     * 写入数据库时，如果字段为null，或者为空字符串，会调用此方法
     * 返回的object会写入数据库, 如果返回null，使用内置处理器{@link DefaultEmptyValueHandler}
     * @param field  字段为空的field
     */
    Object handle(Field field);
}
