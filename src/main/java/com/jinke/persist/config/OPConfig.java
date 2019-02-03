package com.jinke.persist.config;

public class OPConfig<T> {
    private TableNameOverride tableNameOverride;

    public OPConfig<T> withTableNameOverride(TableNameOverride tableNameOverride) {
        this.tableNameOverride = tableNameOverride;
        return this;
    }

    public TableNameOverride getTableNameOverride() {
        return tableNameOverride;
    }
}
