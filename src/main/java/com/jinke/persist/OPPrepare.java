package com.jinke.persist;

import com.jinke.persist.config.OPOption;
import com.jinke.persist.config.PersistConfiguration;
import com.jinke.persist.config.SQLErrorHandlerWrapper;
import com.jinke.persist.config.TableNameOverride;
import com.jinke.persist.utils.ArrayUtils;
import com.jinke.persist.utils.ReflectUtils;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OPPrepare {

    private DbManager dbManager;

    private PersistConfiguration persistConfiguration;

    private SQLErrorHandlerWrapper errorHandler;

    //已经创建的表名, 避免重复执行建表sql语句
    private final Set<String> createdTableSet = Collections.synchronizedSet(new HashSet<String>());

    public OPPrepare(DbManager dbManager, PersistConfiguration persistConfiguration, SQLErrorHandlerWrapper errorHandler) {
        this.dbManager = dbManager;
        this.persistConfiguration = persistConfiguration;
        this.errorHandler = errorHandler;
    }

    /**
     * 获取真正的表名，包括表名处理(tableNameOverride)
     * 首先是替换表名中的占位符，再表名处理
     * @param beanInfo opInfo
     * @param beanList beanList
     * @param opOption
     * @return
     */
    private @Nullable String getRealTableName(BeanInfo beanInfo, List beanList, OPOption opOption) {
        String tableName = beanInfo.getTableName();
        if (beanInfo.getTableNameParam().isEmpty())
            //没有占位符，跳过替换步骤
            return handleTableNameOverride(persistConfiguration.getTableNameOverride(), tableName, beanList, opOption);
        //return persistConfiguration.getTableNameOverride() == null ? tableName : persistConfiguration.getTableNameOverride().overrideTableName(tableName, beanList);

        //if have placeholder, replace it.
        Object bean = beanList.get(0);
        List<Field> paramFields = beanInfo.getTableNameParam();
        //从属性找到占位符的实际值
        String[] values = new String[paramFields.size()];
        for (int i = 0; i < paramFields.size(); ++i) {
            Field field = paramFields.get(i);
            Object value = ReflectUtils.getFieldValue(field, bean, false, persistConfiguration);
            if (value == null) {
                //属性不能为null
                errorHandler.handleError(field.getDeclaringClass() + "->" + field.getName() + " is null, can not replace table name", beanList);
                return null;
            }
            values[i] = value.toString();
        }

        try {
            tableName = String.format(tableName, values);
        } catch (Exception e) {
            errorHandler.handleError(e, beanList);
            return null;
        }

//        if (persistConfiguration.getTableNameOverride() != null) {
//            tableName = persistConfiguration.getTableNameOverride().overrideTableName(tableName, beanList);
//        }
        return handleTableNameOverride(persistConfiguration.getTableNameOverride(), tableName, beanList, opOption);
    }

    /**
     * tableNameOverride额外处理
     * 如果tableNameOverride为空 并且 opConfig的tableNameOverride为空, 不做处理
     * 处理顺序：
     *    首先是persistConfiguration的tableNameOverride
     *    然后是opConfig的tableNameOverride
     * @param tableNameOverride persistConfiguration的tableNameOverride, 可为空
     * @param tableName 带 ` 的tableName
     * @param opOption 额外参数
     * @return 返回的tableName依然带有 `
     */
    private String handleTableNameOverride(TableNameOverride tableNameOverride, String tableName, List beanList, OPOption opOption) {
        if (tableNameOverride == null && (opOption == null || opOption.getTableNameOverride() == null)) return tableName;

        //去除两边的 转义符
        int transferLen = persistConfiguration.getDialect().getRealSQLTransfer().length();
        String retName = tableName.substring(transferLen, tableName.length() - transferLen);
        if (tableNameOverride != null) {
            retName = tableNameOverride.overrideTableName(retName, beanList);
        }
        if (opOption != null && opOption.getTableNameOverride() != null) {
            retName = opOption.getTableNameOverride().overrideTableName(retName, beanList);
        }

        String transfer = persistConfiguration.getDialect().getSQLTransfer();
        return transfer  + retName + transfer;
    }

    public String prepare(BeanInfo beanInfo, List beanList, OPOption opOption) {
        String tableName = getRealTableName(beanInfo, beanList, opOption);
        if (tableName == null) return null;

        if (ArrayUtils.isEmpty(beanInfo.getCreateStatements()) || createdTableSet.contains(tableName)) {
            return tableName;
        }

        String[] createTableSqls = persistConfiguration.getDialect().createTable(tableName, beanInfo);
        if (ArrayUtils.isEmpty(createTableSqls)) return null;

        if (!dbManager.execute(createTableSqls, beanList)) {
            //execute fail
            return null;
        }
        createdTableSet.add(tableName);
        return tableName;
    }
}
