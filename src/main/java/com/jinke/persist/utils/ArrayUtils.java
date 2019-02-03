package com.jinke.persist.utils;

import com.jinke.persist.config.SQLErrorHandlerWrapper;
import com.jinke.persist.enums.OPType;

import java.util.Collection;
import java.util.List;

public class ArrayUtils {
    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Object[] objects) {
        return objects == null || objects.length == 0;
    }

    public static boolean isSameClass(List beanList, Class clazz, SQLErrorHandlerWrapper errorHandler) {
        for (Object bean : beanList) {
            if (clazz != bean.getClass()) {
                errorHandler.handleError("beanList have different class:", beanList, OPType.CHECK);
                return false;
            }
        }
        return true;
    }

    public static boolean haveNullObject(List beanList, SQLErrorHandlerWrapper errorHandler) {
        for (Object bean : beanList) {
            if (bean == null) {
                errorHandler.handleError("beanList have null object", beanList, OPType.CHECK);
                return true;
            }
        }
        return false;
    }
}
