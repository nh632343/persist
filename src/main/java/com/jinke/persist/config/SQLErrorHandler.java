package com.jinke.persist.config;

import java.util.List;

public interface SQLErrorHandler {
    void handleError(Exception e, List beanList, String sql);
}
