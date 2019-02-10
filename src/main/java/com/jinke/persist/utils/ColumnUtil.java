package com.jinke.persist.utils;

import com.jinke.persist.BeanInfo;
import com.jinke.persist.annotation.ColumnName;

import java.lang.reflect.Field;
import java.util.List;

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
     * @return note: 列名的左右会带有转义符
     */
    public static String getColumnName(Field field, String sqlTransfer) {
        ColumnName insertColumnAno = field.getAnnotation(ColumnName.class);
        String column;
        if (insertColumnAno == null || ColumnUtil.empty(insertColumnAno.name())) {
            column = ColumnUtil.toUnderlineFormat(field.getName());
        } else {
            column = insertColumnAno.name();
        }
        return sqlTransfer + column + sqlTransfer;
    }

    public static String getAssignColumnSql(List<Field> groupFieldList, BeanInfo beanInfo, String separator) {
        if (ArrayUtils.isEmpty(groupFieldList)) return "";

        StringBuilder updateSqlBuilder = new StringBuilder(beanInfo.getColumnName(groupFieldList.get(0)));
        updateSqlBuilder.append("=? ");

        for (int i = 1; i < groupFieldList.size(); ++i) {
            updateSqlBuilder.append(separator);
            updateSqlBuilder.append(" ");
            updateSqlBuilder.append(beanInfo.getColumnName(groupFieldList.get(i)));
            updateSqlBuilder.append("=? ");
        }

        return updateSqlBuilder.toString();
    }

    public static String getAssignColumnSql(List<Field> groupFieldList, BeanInfo beanInfo) {
        return getAssignColumnSql(groupFieldList, beanInfo, ",");
    }
}
