package com.jinke.persist.collector;

import com.jinke.persist.annotation.AutoCreate;
import com.jinke.persist.annotation.AutoIncrement;
import com.jinke.persist.annotation.ColumnProps;
import com.jinke.persist.annotation.PrimaryKey;
import com.jinke.persist.utils.ColumnUtil;

import java.lang.reflect.Field;

/**
 * 建表信息收集类
 */
class CreateTableBuilder {
    private final StringBuilder createBuilder;
    private boolean havePrimaryKey = false;    //收集建表信息时使用，primaryKey只能有一个
    private final AutoCreate autoCreateAno;

    CreateTableBuilder(AutoCreate autoCreateAno) {
        this.createBuilder = new StringBuilder(" (");
        this.autoCreateAno = autoCreateAno;
    }

    /**
     * 添加建表的field, 必须添加完所有field之后 再调用toString
     * @throws Exception 如果解析出错, 抛异常, 信息通过 exception.getMessage()获取
     */
    void addField(Class clazz, Field field, String columnName) throws Exception {
        ColumnProps columnPropsAno = field.getAnnotation(ColumnProps.class);
        //自动建表，field必须有ColumnProps注解
        if (columnPropsAno == null) {
            throw new Exception("Class:" + clazz.getName() + " is auto create table, but field:" + field.getName() + " not have ColumnProps annotation");
        }
        //字段的信息
        //example:  `column name` int(11) NOT NULL PRIMARY KEY default 5 comment 'a aaa'
        createBuilder.append(columnName);
        createBuilder.append(" ");
        createBuilder.append(columnPropsAno.type());
        createBuilder.append(" ");
        createBuilder.append(columnPropsAno.notNull() ? "NOT NULL" : "");

        if (field.getAnnotation(PrimaryKey.class) != null) {
            if (havePrimaryKey) {
                throw new Exception("Class:" + clazz.getName() + " have more than one primary key");

            }
            havePrimaryKey = true;
            createBuilder.append(" PRIMARY KEY");
            if (field.getAnnotation(AutoIncrement.class) != null) {
                createBuilder.append(" AUTO_INCREMENT");
            }
        }
        createBuilder.append(" ");
        createBuilder.append( ColumnUtil.empty(columnPropsAno.defaultValue()) ? "" : ("default " + columnPropsAno.defaultValue()) ); //如果是字符串类型，自己加单引号
        createBuilder.append(" ");
        createBuilder.append(columnPropsAno.other());
        createBuilder.append(" ");
        createBuilder.append( ColumnUtil.empty(columnPropsAno.comment()) ? "" : ("comment '" + columnPropsAno.comment() + "'") );
        createBuilder.append(',');
    }

    /**
     * 必须添加完所有field之后 再调用toString
     * @return 建表语句的 字段部分, 不包括 'create table xxxxx'这部分
     */
    public String toString() {
        if (ColumnUtil.empty(autoCreateAno.otherProps())) {
            createBuilder.deleteCharAt(createBuilder.length() - 1);
        } else {
            createBuilder.append(autoCreateAno.otherProps());
        }
        createBuilder.append(") ");
        createBuilder.append(autoCreateAno.tableProps());
        return createBuilder.toString();
    }
}
