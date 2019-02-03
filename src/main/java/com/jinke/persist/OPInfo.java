package com.jinke.persist;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OPInfo {
    //表名，左右有 `；如果有占位符，会被替换为%s
    private String tableName;

    //field的顺序就是表名里占位符的顺序
    private List<Field> tableNameParam;

    private List<Field> insertFieldList;

    private String insertStatement;

    //为null表示不需要自动建表
    private String createStatement;

    //保存属性名字和属性的映射
    //不包括Transient注解的field
    //多线程环境下，只会进行get操作，所以不用concurrentHashMap
    private Map<String, Field> nameToFieldMap = new HashMap<>();

    //属性名字和对应列名的映射
    //列名左右有 `
    private Map<String, String> nameToColumnName = new HashMap<>();

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
        this.tableNameParam = tableNameParam;
    }

    public List<Field> getInsertFieldList() {
        return insertFieldList;
    }

    public void setInsertFieldList(List<Field> insertFieldList) {
        this.insertFieldList = insertFieldList;
    }

    public String getInsertStatement() {
        return insertStatement;
    }

    public void setInsertStatement(String insertStatement) {
        this.insertStatement = insertStatement;
    }

    public String getCreateStatement() {
        return createStatement;
    }

    public void setCreateStatement(String createStatement) {
        this.createStatement = createStatement;
    }

    public Field getField(String name) {
        return nameToFieldMap.get(name);
    }

    public void putNameField(String name, Field field) {
        nameToFieldMap.put(name, field);
    }

    public void putNameColumnName(String name, String columnName) {
        nameToColumnName.put(name, columnName);
    }

    public String getColumnName(String name) {
        return nameToColumnName.get(name);
    }
}
