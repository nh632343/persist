package com.jinke.persist.config;

import java.util.List;

public interface TableNameOverride {
    String overrideTableName(String tableName, List beanList);
}
