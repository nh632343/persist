package com.jinke.persist;

import com.jinke.persist.utils.ReflectUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class ReflectBatchSetter implements BatchPreparedStatementSetter {
    private List beanList;
    private List<Field> fieldList;
    private DbManager dbManager;

    public ReflectBatchSetter(List beanList, List<Field> fieldList, DbManager dbManager) {
        this.beanList = beanList;
        this.fieldList = fieldList;
        this.dbManager = dbManager;
    }

    @Override
    public void setValues(PreparedStatement preparedStatement, int pos) throws SQLException {
        if (beanList.size() == 0) return;
        Object bean = beanList.get(pos);
        for (int i = 1; i <= fieldList.size(); ++i) {
            Field field = fieldList.get(i-1);
            preparedStatement.setObject(i, ReflectUtils.getFieldValue(field, bean, dbManager.getPersistConfiguration()));
        }
    }

    @Override
    public int getBatchSize() {
        return beanList.size();
    }
}
