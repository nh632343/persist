package com.jinke.persist;

import com.jinke.persist.config.OPOption;
import com.jinke.persist.config.PersistConfiguration;
import com.jinke.persist.config.SQLErrorHandlerWrapper;
import com.jinke.persist.config.TableNameOverride;
import com.jinke.persist.constant.Constant;
import com.jinke.persist.enums.OPType;
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
     * @param opInfo opInfo
     * @param beanList beanList
     * @param opType
     * @param opConfig
     * @return
     */
    private @Nullable String getRealTableName(OPInfo opInfo, List beanList, OPType opType, OPOption opConfig) {
        String tableName = opInfo.getTableName();
        if (opInfo.getTableNameParam().isEmpty())
            //没有占位符，跳过替换步骤
            return handleTableNameOverride(persistConfiguration.getTableNameOverride(), tableName, beanList, opConfig);
        //return persistConfiguration.getTableNameOverride() == null ? tableName : persistConfiguration.getTableNameOverride().overrideTableName(tableName, beanList);

        //if have placeholder, replace it.
        Object bean = beanList.get(0);
        List<Field> paramFields = opInfo.getTableNameParam();
        //从属性找到占位符的实际值
        String[] values = new String[paramFields.size()];
        for (int i = 0; i < paramFields.size(); ++i) {
            Field field = paramFields.get(i);
            Object value = ReflectUtils.getFieldValue(field, bean, false, persistConfiguration);
            if (value == null) {
                //属性不能为null
                errorHandler.handleError(field.getDeclaringClass() + "->" + field.getName() + " is null, can not replace table name", beanList, opType);
                return null;
            }
            values[i] = value.toString();
        }

        try {
            tableName = String.format(tableName, values);
        } catch (Exception e) {
            errorHandler.handleError(e, beanList, opType);
            return null;
        }

//        if (persistConfiguration.getTableNameOverride() != null) {
//            tableName = persistConfiguration.getTableNameOverride().overrideTableName(tableName, beanList);
//        }
        return handleTableNameOverride(persistConfiguration.getTableNameOverride(), tableName, beanList, opConfig);
    }

    /**
     * tableNameOverride额外处理
     * 如果tableNameOverride为空 并且 opConfig的tableNameOverride为空, 不做处理
     * 处理顺序：
     *    首先是persistConfiguration的tableNameOverride
     *    然后是opConfig的tableNameOverride
     * @param tableNameOverride persistConfiguration的tableNameOverride, 可为空
     * @param tableName 带 ` 的tableName
     * @param opConfig 额外参数
     * @return 返回的tableName依然带有 `
     */
    private String handleTableNameOverride(TableNameOverride tableNameOverride, String tableName, List beanList, OPOption opConfig) {
        if (tableNameOverride == null && (opConfig == null || opConfig.getTableNameOverride() == null)) return tableName;
        //去除两边的 `
        String retName = tableName.substring(1, tableName.length() - 1);
        if (tableNameOverride != null) {
            retName = tableNameOverride.overrideTableName(retName, beanList);
        }
        if (opConfig != null && opConfig.getTableNameOverride() != null) {
            retName = opConfig.getTableNameOverride().overrideTableName(retName, beanList);
        }
        return Constant.SQL_TRANSFER  + retName + Constant.SQL_TRANSFER;
    }

    public String prepare(OPInfo opInfo, List beanList, OPType opType, OPOption opConfig) {
        String tableName = getRealTableName(opInfo, beanList, opType, opConfig);
        if (tableName == null) return null;

        if (opInfo.getCreateStatement() == null || createdTableSet.contains(tableName)) return tableName;
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + opInfo.getCreateStatement();
        if (!dbManager.execute(sql, beanList, OPType.CREATE)) {
            //execute fail
            return null;
        }
        createdTableSet.add(tableName);
        return tableName;
    }
}
