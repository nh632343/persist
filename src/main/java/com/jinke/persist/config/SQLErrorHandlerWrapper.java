package com.jinke.persist.config;

import com.jinke.persist.enums.OPType;

import java.util.List;

public class SQLErrorHandlerWrapper {
    private SQLErrorHandler sqlErrorHandler;

    public SQLErrorHandlerWrapper(SQLErrorHandler sqlErrorHandler) {
        this.sqlErrorHandler = sqlErrorHandler;
    }


    public void handleError(String msg, List beanList, OPType opType) {
        handleError(msg, beanList, opType, null);
    }

    public void handleError(String msg, List beanList, OPType opType, String sql) {
        if (sqlErrorHandler == null) return;
        sqlErrorHandler.handleError(new Exception(msg), beanList, opType, sql);
    }

    public void handleError(Exception e, List beanList, OPType opType) {
        handleError(e, beanList, opType, null);
    }

    public void handleError(Exception e, List beanList, OPType opType, String sql) {
        if (sqlErrorHandler == null) return;
        sqlErrorHandler.handleError(e, beanList, opType, sql);
    }
}
