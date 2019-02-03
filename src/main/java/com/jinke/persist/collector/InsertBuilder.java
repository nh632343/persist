package com.jinke.persist.collector;

/**
 * 插入语句建造
 */
class InsertBuilder {
    private StringBuilder insertStatementBuilder;
    //总字段数量
    private int num = 0;

    InsertBuilder() {
        insertStatementBuilder = new StringBuilder(" (");
    }

    /**
     * 添加列
     */
    void addColumn(String columnName) {
        ++num;
        insertStatementBuilder.append(columnName);
        insertStatementBuilder.append(',');
    }

    /**
     * 必须添加完所有列，再调用此方法
     * @return insert语句字段部分，不包括 'insert into xxx'
     */
    public String toString() {
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(") VALUES (");
        for (int i = 0; i < num; ++i) {
            insertStatementBuilder.append("?,");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(")");
        return insertStatementBuilder.toString();
    }
}
