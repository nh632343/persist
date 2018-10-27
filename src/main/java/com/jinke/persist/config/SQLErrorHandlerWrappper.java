package com.jinke.persist.config;

import com.jinke.persist.enums.OPType;

import java.util.List;

public class SQLErrorHandlerWrappper {
    private SQLErrorHandler sqlErrorHandler;

    public SQLErrorHandlerWrappper(SQLErrorHandler sqlErrorHandler) {
        this.sqlErrorHandler = sqlErrorHandler;
    }


    public void handleError(String msg, List beanList, OPType opType) {
        if (sqlErrorHandler == null) return;
        sqlErrorHandler.handleError(new Exception(msg), beanList, opType);
    }

    public void handleError(Exception e, List beanList, OPType opType) {
        if (sqlErrorHandler == null) return;
        sqlErrorHandler.handleError(e, beanList, opType);
    }
}
