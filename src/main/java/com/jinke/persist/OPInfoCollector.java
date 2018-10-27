package com.jinke.persist;

import com.jinke.persist.annotation.AutoCreate;
import com.jinke.persist.annotation.ColumnProps;
import com.jinke.persist.annotation.PrimaryKey;
import com.jinke.persist.annotation.TableName;
import com.jinke.persist.config.SQLErrorHandlerWrappper;
import com.jinke.persist.enums.OPType;
import com.jinke.persist.utils.StringUtil;


import java.beans.Transient;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OPInfoCollector {
    private static final String PLACE_HOLDER_PATTERN_STRING = "\\{(.*?)\\}";
    private static final Pattern TABLE_NAME_PLACE_HOLDER_PATTERN = Pattern.compile(PLACE_HOLDER_PATTERN_STRING);
    private static final String INTERNAL_PLACE_HOLDER = "%s";

    private SQLErrorHandlerWrappper errorHandler;

    private ThreadLocal<List> beanListHolder = new ThreadLocal<>();

    public OPInfoCollector(SQLErrorHandlerWrappper errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * 生成OPInfo
     * @param clazz
     * @return   null表示这个class的注解信息有错误
     */
    OPInfo generateOPInfo(Class clazz, List beanList) {
        beanListHolder.set(beanList);
        try {
            String tableName = getTableName(clazz);
            if (StringUtil.empty(tableName)) return null;


            //find placeholder in table name
            List<Field> paramList = getTableNameParam(tableName, clazz);
            if (paramList == null) {
                return null;
            }

            OPInfo opInfo = new OPInfo();
            //把原来的占位符换成%s
            opInfo.setTableName(tableName.replaceAll(PLACE_HOLDER_PATTERN_STRING, INTERNAL_PLACE_HOLDER));
            opInfo.setTableNameParam(paramList);

            return collectInfo(clazz, opInfo);

        } finally {
            beanListHolder.remove();
        }
    }

    /**
     * 从类的注解获取表名
     * @param clazz
     * @return null表示有错误
     */
    private String getTableName(Class clazz) {
        TableName tableNameAno = (TableName) clazz.getAnnotation(TableName.class);
        if (tableNameAno == null) {
            onError("Class:" + clazz.getName() + "  have no TableName Annotation");
            return null;
        }

        String tableName = tableNameAno.name();
        if (StringUtil.empty(tableName)) {
            onError("Class:" + clazz.getName() + "  TableName Annotation is empty");
            return null;
        }
        return tableName;
    }

    /**
     * 根据tableName中的占位符，找到类中对应的字段
     * @param tableName
     * @param clazz
     * @return 返回null表示解析出错，List可能为空
     */
    private List<Field> getTableNameParam(String tableName, Class clazz) {
        List<Field> paramList = new ArrayList<>();
        Matcher matcher = TABLE_NAME_PLACE_HOLDER_PATTERN.matcher(tableName);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            boolean find = false;
            for (Field field : clazz.getDeclaredFields()) {
                if (placeholder.equals(field.getName())) {
                    find = true;
                    paramList.add(field);
                    break;
                }
            }

            if (!find) {
                onError("Class:" + clazz.getName() +" can not find placeholder:" + placeholder);
                return null;
            }
        }

        return paramList;
    }

    private OPInfo collectInfo(Class clazz, OPInfo opInfo) {
        List<Field> insertFieldList = new ArrayList<>();
        StringBuilder insertStatementBuilder = new StringBuilder(" (");

        StringBuilder createBuilder = null;
        AutoCreate autoCreateAno = (AutoCreate) clazz.getAnnotation(AutoCreate.class);
        if (autoCreateAno != null) {
            createBuilder = new StringBuilder(" (");
        }

        boolean havePrimaryKey = false;    //only use for auto create table
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getAnnotation(Transient.class) != null) continue;

            //find insert ColumnName
            String columnName = StringUtil.getColumnName(field);


            if (autoCreateAno != null) {
                ColumnProps columnPropsAno = field.getAnnotation(ColumnProps.class);
                if (columnPropsAno == null) {
                    onError("Class:" + clazz.getName() + " is auto create table, but field:" + field.getName() + " not have ColumnProps annotation");
                    return null;
                }
                createBuilder.append(columnName);
                createBuilder.append(" ");
                createBuilder.append(columnPropsAno.type());
                createBuilder.append(" ");
                createBuilder.append(columnPropsAno.notNull() ? "NOT NULL" : "");

                if (field.getAnnotation(PrimaryKey.class) != null) {
                    if (havePrimaryKey) {
                        onError("Class:" + clazz.getName() + " have two primary key");
                        return null;
                    }
                    havePrimaryKey = true;
                    createBuilder.append(" PRIMARY KEY AUTO_INCREMENT");
                }
                createBuilder.append(" ");
                createBuilder.append( StringUtil.empty(columnPropsAno.defaultValue()) ? "" : ("default " + columnPropsAno.defaultValue()) ); //如果是字符串类型，自己加单引号
                createBuilder.append(" ");
                createBuilder.append(columnPropsAno.other());
                createBuilder.append(" ");
                createBuilder.append( StringUtil.empty(columnPropsAno.comment()) ? "" : ("comment '" + columnPropsAno.comment() + "'") );
                createBuilder.append(',');
            }


            field.setAccessible(true);
            opInfo.put(field.getName(), field);
            if (field.getAnnotation(PrimaryKey.class) == null) {
                insertFieldList.add(field);
                insertStatementBuilder.append(columnName);
                insertStatementBuilder.append(',');
            }
        }

        if (autoCreateAno != null) {
            if (StringUtil.empty(autoCreateAno.otherProps())) {
                createBuilder.deleteCharAt(createBuilder.length() - 1);
            } else {
                createBuilder.append(autoCreateAno.otherProps());
            }
            createBuilder.append(") ");
            createBuilder.append(autoCreateAno.tableProps());
            opInfo.setCreateStatement(createBuilder.toString());
        }

        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(") VALUES (");
        for (int i = 0; i < insertFieldList.size(); ++i) {
            insertStatementBuilder.append("?,");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(")");

        opInfo.setInsertStatement(insertStatementBuilder.toString());
        opInfo.setInsertFieldList(insertFieldList);
        return opInfo;
    }

    private void onError(String msg) {
        errorHandler.handleError(msg, beanListHolder.get(), OPType.CHECK);
    }
}
