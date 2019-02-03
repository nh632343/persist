package com.jinke.persist.utils;

import com.jinke.persist.annotation.ColumnName;
import com.jinke.persist.constant.Constant;

import java.lang.reflect.Field;

public class ColumnUtil {

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
     * @return  例如 appId 转为 app_id
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

    /**
     * 根据field获取数据库列名
     * 如果field没有ColumnName注解 或 注解值为空，将field的名字转为下划线形式
     * 如果ColumnName注解的值有效，使用注解的值
     * @param field 需要获取列名的field
     * @return !!!note: 列名的左右会带有 ` 这个字符, 为了避免与mysql关键字冲突, 进行转义.   例如 `app_id`
     */
    public static String getColumnName(Field field) {
        ColumnName insertColumnAno = field.getAnnotation(ColumnName.class);
        String column;
        if (insertColumnAno == null || ColumnUtil.empty(insertColumnAno.name())) {
            column = ColumnUtil.toUnderlineFormat(field.getName());
        } else {
            column = insertColumnAno.name();
        }
        return Constant.SQL_TRANSFER + column + Constant.SQL_TRANSFER;
    }
}
