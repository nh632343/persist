package com.jinke.persist.collector;

import com.jinke.persist.OPInfo;
import com.jinke.persist.annotation.*;
import com.jinke.persist.config.SQLErrorHandlerWrapper;
import com.jinke.persist.constant.Constant;
import com.jinke.persist.enums.OPType;
import com.jinke.persist.utils.ColumnUtil;
import com.sun.scenario.effect.impl.prism.PrImage;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OPInfoCollector {
    private static final String PLACE_HOLDER_PATTERN_STRING = "\\{(.*?)\\}";
    private static final Pattern TABLE_NAME_PLACE_HOLDER_PATTERN = Pattern.compile(PLACE_HOLDER_PATTERN_STRING);
    private static final String INTERNAL_PLACE_HOLDER = "%s";

    private SQLErrorHandlerWrapper errorHandler;

    private ThreadLocal<List> beanListHolder = new ThreadLocal<>();

    public OPInfoCollector(SQLErrorHandlerWrapper errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * 生成OPInfo
     * @param clazz
     * @return   null表示这个class的注解信息有错误
     */
    public OPInfo generateOPInfo(Class clazz, List beanList) {
        beanListHolder.set(beanList);
        OPInfo result = generateOPInfoInner(clazz, beanList);
        beanListHolder.remove();
        return result;
    }

    private OPInfo generateOPInfoInner(Class clazz, List beanList) {
        String tableName = getTableName(clazz);
        if (ColumnUtil.empty(tableName)) return null;


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

        String tableName = tableNameAno.value();
        if (ColumnUtil.empty(tableName)) {
            onError("Class:" + clazz.getName() + "  TableName Annotation is empty");
            return null;
        }
        return Constant.SQL_TRANSFER + tableName + Constant.SQL_TRANSFER;
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

    /**
     *
     * @param clazz
     * @param opInfo
     * @return
     */
    private OPInfo collectInfo(Class clazz, OPInfo opInfo) {
        List<Field> insertFieldList = new ArrayList<>();
        InsertBuilder insertBuilder = new InsertBuilder();

        //createBuilder 和 autoCreateAno 同时为空或同时不为空
        //建表语句
        CreateTableBuilder createBuilder = null;
        //判断是否有自动建表注解
        AutoCreate autoCreateAno = (AutoCreate) clazz.getAnnotation(AutoCreate.class);
        if (autoCreateAno != null) {
            createBuilder = new CreateTableBuilder(autoCreateAno);
        }

        //遍历field -------------------------------------------
        //考虑继承，还要遍历所有父类
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            //
            for (Field field : clazz.getDeclaredFields()) {
                //ignore Transient field
                if (field.getAnnotation(NotSql.class) != null) continue;

                field.setAccessible(true);
                insertFieldList.add(field);
                opInfo.putNameField(field.getName(), field);

                //find insert ColumnName
                String columnName = ColumnUtil.getColumnName(field);
                opInfo.putNameColumnName(field.getName(), columnName);

                //收集自动建表信息
                if (createBuilder != null) {
                    try {
                        createBuilder.addField(clazz, field, columnName);
                    } catch (Exception e) {
                        onError(e.getMessage());
                        return null;
                    }
                }

                insertBuilder.addColumn(columnName);
            }
        }
        //遍历field 结束--------------------------------------------------

        if (createBuilder != null) {
            opInfo.setCreateStatement(createBuilder.toString());
        }

        opInfo.setInsertStatement(insertBuilder.toString());
        opInfo.setInsertFieldList(insertFieldList);
        return opInfo;
    }

    private void onError(String msg) {
        errorHandler.handleError(msg, beanListHolder.get(), OPType.CHECK);
    }
}
