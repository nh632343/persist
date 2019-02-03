package com.jinke.persist;

import com.jinke.persist.collector.OPInfoCollector;
import com.jinke.persist.config.*;
import com.jinke.persist.constant.Constant;
import com.jinke.persist.enums.OPType;
import com.jinke.persist.utils.ArrayUtils;
import com.jinke.persist.utils.ColumnUtil;
import com.jinke.persist.utils.ReflectUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class DbManager {

    private static final String SQL_HEAD_INSERT = "INSERT INTO ";
    private static final String SQL_HEAD_UPDATE = "UPDATE ";

    //所有注解有错误的类，都存在这里
    private final Set<Class> errorClassSet = Collections.synchronizedSet(new HashSet<Class>());

    //已经创建的表名, 避免重复执行建表sql语句
    private final Set<String> createdTableSet = Collections.synchronizedSet(new HashSet<String>());

    private final ConcurrentHashMap<Class, OPInfo> classToInfoMap = new ConcurrentHashMap<>();

    //注解信息收集器
    private OPInfoCollector opInfoCollector;

    private PersistConfiguration persistConfiguration;

    private JdbcTemplate jdbcTemplate;

    private SQLErrorHandlerWrapper errorHandler;

    private InfoLogger infoLogger;


    public DbManager(JdbcTemplate jdbcTemplate, PersistConfiguration persistConfiguration) {
        this.persistConfiguration = persistConfiguration;
        errorHandler = new SQLErrorHandlerWrapper(persistConfiguration.getSqlErrorHandler());
        infoLogger = new InfoLoggerWrapper(persistConfiguration.getInfoLogger());

        opInfoCollector = new OPInfoCollector(errorHandler);
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(List beanList) {
        insert(beanList, null);
    }


    /**
     * 插入，除了primary key和transient注解的属性
     * @param beanList 插入的bean
     * @param opConfig 额外参数，可为null
     */
    public void insert(List beanList, OPConfig opConfig) {
        OPInfo opInfo = getOPInfo(beanList);
        if (opInfo == null) return;

        //获取真正表名 和 创建表（如果配置了自动创建）
        String tableName = prepare(opInfo, beanList, OPType.INSERT, opConfig);
        if (tableName == null) return;

        String sql = SQL_HEAD_INSERT + tableName + opInfo.getInsertStatement();

        execute(sql, beanList, opInfo.getInsertFieldList(), OPType.INSERT);
    }

    public void update(List beanList, String[] updateItems, String[] conditionItems) {
        update(beanList, updateItems, conditionItems, null);
    }

    /**
     *
     * @param beanList  插入的bean
     * @param updateItems   需要更新的属性，string为属性名
     * @param conditionItems  作为条件的属性，string为属性名, 条件为并且 (and)         可为null
     * @param opConfig 额外参数，可为null
     */
    public void update(List beanList, String[] updateItems, String[] conditionItems, OPConfig opConfig) {
        if (ArrayUtils.isEmpty(beanList) || ArrayUtils.isEmpty(updateItems)) return;
        update(beanList, Arrays.asList(updateItems), conditionItems == null ? null : Arrays.asList(conditionItems), opConfig);
    }

    public void update(List beanList, List<String> updateItems, List<String> conditionItems) {
        update(beanList, updateItems, conditionItems, null);
    }

    /**
     *
     * @param beanList  插入的bean
     * @param updateItems   需要更新的属性，string为属性名
     * @param conditionItems  作为条件的属性，string为属性名, 条件为并且 (and)         可为null
     * @param opConfig 额外参数，可为null
     */
    public void update(List beanList, List<String> updateItems, List<String> conditionItems, OPConfig opConfig) {
        if (ArrayUtils.isEmpty(beanList) || ArrayUtils.isEmpty(updateItems)) return;

        OPInfo opInfo = getOPInfo(beanList);
        if (opInfo == null) return;

        //获取真正表名 和 创建表（如果配置了自动创建）
        String tableName = prepare(opInfo, beanList, OPType.UPDATE, opConfig);
        if (tableName == null) return;

        StringBuilder sqlBuilder = new StringBuilder(SQL_HEAD_UPDATE + tableName + " SET ");

        //fieldList为需要替换的field的集合
        List<Field> fieldList = new ArrayList<>();
        //根据属性名找到属性和列名
        for (int i = 0; i < updateItems.size(); ++i) {
            String name = updateItems.get(i);
            Field updateField = opInfo.getField(name);

            if (updateField == null) {
                errorHandler.handleError("can not find property:" + name + " in Class:" + beanList.get(0).getClass(), beanList, OPType.UPDATE);
                return;
            }
            fieldList.add(updateField);
            sqlBuilder.append(opInfo.getColumnName(name));
            sqlBuilder.append("=?,");
        }
        sqlBuilder.deleteCharAt(sqlBuilder.length() - 1);


        if (!ArrayUtils.isEmpty(conditionItems)) {
            sqlBuilder.append(" where ");
            boolean isFirst = true;
            for (int i = 0; i < conditionItems.size(); ++i) {
                String name = conditionItems.get(i);
                Field condField = opInfo.getField(name);

                if (condField == null) {
                    errorHandler.handleError("can not find property:" + name + " in Class:" + beanList.get(0).getClass(), beanList, OPType.UPDATE);
                    return;
                }
                fieldList.add(condField);
                if (isFirst) {
                    isFirst = false;
                } else {
                    sqlBuilder.append(" and ");
                }
                sqlBuilder.append(opInfo.getColumnName(name));
                sqlBuilder.append("=?");
            }
        }

        String sql = sqlBuilder.toString();

        execute(sql, beanList, fieldList, OPType.UPDATE);

    }

    public PersistConfiguration getPersistConfiguration() {
        return persistConfiguration;
    }

    /**
     * 获取该类的表信息
     * @param beanList beanList
     * @return OPInfo, 如果出现错误, 返回null
     */
    private @Nullable OPInfo getOPInfo(List beanList) {
        if (ArrayUtils.isEmpty(beanList) || ArrayUtils.haveNullObject(beanList, errorHandler)) return null;

        Class clazz = beanList.get(0).getClass();
        //检查beanList中的对象，是否都是同一个类型
        if (!ArrayUtils.isSameClass(beanList, clazz, errorHandler)) {
            return null;
        }

        if (errorClassSet.contains(clazz)) return null;


        OPInfo opInfo = classToInfoMap.get(clazz);
        if (opInfo == null) {
            opInfo = opInfoCollector.generateOPInfo(clazz, beanList);
            if (opInfo == null) {
                //error
                errorClassSet.add(clazz);
                return null;
            }
            classToInfoMap.put(clazz, opInfo);
        }
        return opInfo;
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
    private @Nullable String getRealTableName(OPInfo opInfo, List beanList, OPType opType, OPConfig opConfig) {
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
    private String handleTableNameOverride(TableNameOverride tableNameOverride, String tableName, List beanList, OPConfig opConfig) {
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

    private String prepare(OPInfo opInfo, List beanList, OPType opType, OPConfig opConfig) {
        String tableName = getRealTableName(opInfo, beanList, opType, opConfig);
        if (tableName == null) return null;

        if (opInfo.getCreateStatement() == null || createdTableSet.contains(tableName)) return tableName;
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + opInfo.getCreateStatement();
        if (!execute(sql, beanList, OPType.CREATE)) {
            //execute fail
            return null;
        }
        createdTableSet.add(tableName);
        return tableName;
    }


    private boolean execute(String sql, List beanList, List<Field> fieldList, OPType opType) {
        infoLogger.info(sql);
        try {
            jdbcTemplate.batchUpdate(sql, new ReflectBatchSetter(beanList, fieldList, this));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            errorHandler.handleError(e, beanList, opType, sql);
            return false;
        }
    }

    private boolean execute(String sql, List beanList ,OPType opType) {
        infoLogger.info(sql);
        try {
            jdbcTemplate.update(sql);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            errorHandler.handleError(e, beanList, opType, sql);
            return false;
        }
    }
}
