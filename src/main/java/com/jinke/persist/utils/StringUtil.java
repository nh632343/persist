package com.jinke.persist.utils;

import com.jinke.persist.annotation.ColumnName;

import java.lang.reflect.Field;

/**
 * @author: chenye
 * @description:
 * @createDate: 2018/1/29 20:38
 */
public class StringUtil {

    public static boolean empty(String str) {

        if(str == null) {
            return true;
        }
        if("".equals(str.trim())) {
            return true;
        }
        if("null".equals(str.trim())) {
            return true;
        }
        return false;
    }

    /**
     * 把大写字母转小写，并在前面加下划线
     * @return
     */
    public static String toUnderlineFormat(String name) {
        if (empty(name)) return "";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);
            if (!Character.isUpperCase(c)) {
                builder.append(c);
                continue;
            }
            c = Character.toLowerCase(c);
            builder.append('_');
            builder.append(c);
        }

        return builder.toString();
    }

    public static String getColumnName(Field field) {
        ColumnName insertColumnAno = field.getAnnotation(ColumnName.class);
        if (insertColumnAno == null || StringUtil.empty(insertColumnAno.name())) {
            return StringUtil.toUnderlineFormat(field.getName());
        } else {
            return insertColumnAno.name();
        }
    }
}
