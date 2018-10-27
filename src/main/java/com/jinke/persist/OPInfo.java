package com.jinke.persist;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OPInfo {
    private String tableName;

    private List<Field> tableNameParam;

    private List<Field> insertFieldList;

    private String insertStatement;

    private String createStatement;

    //保存属性名字和属性的映射
    //多线程环境下，只会进行get操作，所以不用concurrentHashMap
    private Map<String, Field> nameToFieldMap = new HashMap<>();

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

    public void put(String name, Field field) {
        nameToFieldMap.put(name, field);
    }
}
