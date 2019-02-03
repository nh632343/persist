package com.jinke.persist.config;

import com.jinke.persist.enums.OPType;

import java.util.List;

public interface SQLErrorHandler {
    void handleError(Exception e, List beanList, OPType opType, String sql);
}
