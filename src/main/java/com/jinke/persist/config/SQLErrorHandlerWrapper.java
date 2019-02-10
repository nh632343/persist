package com.jinke.persist.config;

import java.util.List;

public class SQLErrorHandlerWrapper {
    private SQLErrorHandler sqlErrorHandler;

    public SQLErrorHandlerWrapper(SQLErrorHandler sqlErrorHandler) {
        this.sqlErrorHandler = sqlErrorHandler;
    }


    public void handleError(String msg, List beanList) {
        handleError(msg, beanList, null);
    }

    public void handleError(String msg, List beanList, String sql) {
        if (sqlErrorHandler == null) return;
        sqlErrorHandler.handleError(new Exception(msg), beanList, sql);
    }

    public void handleError(Exception e, List beanList) {
        handleError(e, beanList, null);
    }

    public void handleError(Exception e, List beanList, String sql) {
        if (sqlErrorHandler == null) return;
        sqlErrorHandler.handleError(e, beanList, sql);
    }
}
