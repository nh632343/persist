package com.jinke.persist.config;


public class PersistConfiguration {
    private EmptyValueHandler defaultEmptyHandler = new DefaultEmptyValueHandler();
    private EmptyValueHandler customEmptyHandler;
    private SQLErrorHandler sqlErrorHandler;
    private InfoLogger infoLogger;
    private TableNameOverride tableNameOverride;


    public PersistConfiguration withEmptyValueHandler(EmptyValueHandler emptyValueHandler) {
        this.customEmptyHandler = emptyValueHandler;
        return this;
    }


    public PersistConfiguration withSQLErrorHandler(SQLErrorHandler errorHandler) {
        this.sqlErrorHandler = errorHandler;
        return this;
    }

    public PersistConfiguration withInfoLogger(InfoLogger infoLogger) {
        this.infoLogger = infoLogger;
        return this;
    }

    public PersistConfiguration withTableNameOverride(TableNameOverride tableNameOverride) {
        this.tableNameOverride = tableNameOverride;
        return this;
    }

    public EmptyValueHandler getDefaultEmptyHandler() {
        return defaultEmptyHandler;
    }

    public EmptyValueHandler getCustomEmptyHandler() {
        return customEmptyHandler;
    }

    public SQLErrorHandler getSqlErrorHandler() {
        return sqlErrorHandler;
    }

    public InfoLogger getInfoLogger() {
        return infoLogger;
    }

    public TableNameOverride getTableNameOverride() {
        return tableNameOverride;
    }
}
