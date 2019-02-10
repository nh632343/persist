package com.jinke.persist;

import com.jinke.persist.utils.ArrayUtils;

import java.lang.reflect.Field;
import java.util.*;

public class BeanInfo {
    //表名，左右有sql关键字转义符；如果有占位符，会被替换为%s
    private String tableName;

    //field的顺序就是表名里占位符的顺序
    private List<Field> tableNameParam;

    private List<Field> insertFieldList;

    private String insertStatement;

    //为null表示不需要自动建表
    private String[] createStatements;

    //保存属性名字和属性的映射
    //不包括NotSql注解的field
    //多线程环境下，只会进行get操作，所以不用concurrentHashMap
    //private Map<String, Field> nameToFieldMap = new HashMap<>();

    //属性名字和对应列名的映射
    //列名左右有 `
    //private Map<String, String> nameToColumnName = new HashMap<>();

    //根据group名称获取对应的column
    //多线程环境下，只会进行get操作，所以不用concurrentHashMap
    private Map<String, List<Field>> groupToFieldListMap;

    private Map<Field, String> fieldToColumnNameMap = new HashMap<>();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<Field> getTableNameParam() {
        return tableNameParam;
    }

    public void setTableNameParam(List<Field> tableNameParam) {
        this.tableNameParam = Collections.unmodifiableList(tableNameParam);
    }

    public List<Field> getInsertFieldList() {
        return insertFieldList;
    }

    public void setInsertFieldList(List<Field> insertFieldList) {
        this.insertFieldList = Collections.unmodifiableList(insertFieldList);
    }

    public String getInsertStatement() {
        return insertStatement;
    }

    public void setInsertStatement(String insertStatement) {
        this.insertStatement = insertStatement;
    }

    public String[] getCreateStatements() {
        if (ArrayUtils.isEmpty(createStatements)) return null;

        return Arrays.copyOf(createStatements, createStatements.length);
    }

    public void setCreateStatements(String[] createStatements) {
        this.createStatements = createStatements;
    }



    public List<Field> getFieldListByGroup(String groupName) {
        if (groupToFieldListMap == null) {
            return null;
        }
        return groupToFieldListMap.get(groupName);
    }

    public void setGroupToFieldListMap(Map<String, List<Field>> groupToFieldListMap) {
        if (groupToFieldListMap == null) return;

        //把map中的所有List设置为不可修改
        this.groupToFieldListMap = new HashMap<>();
        for (Map.Entry<String, List<Field>> entry : groupToFieldListMap.entrySet()) {
            this.groupToFieldListMap.put(
                    entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
    }

    public String getColumnName(Field field) {
        return fieldToColumnNameMap.get(field);
    }

    public void putFieldToColumnName(Field field, String columnName) {
        fieldToColumnNameMap.put(field, columnName);
    }
}
