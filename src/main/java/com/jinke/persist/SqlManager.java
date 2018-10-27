package com.jinke.persist;

import com.jinke.persist.config.InfoLogger;
import com.jinke.persist.config.InfoLoggerWrapper;
import com.jinke.persist.config.PersistConfiguration;
import com.jinke.persist.config.SQLErrorHandlerWrappper;
import com.jinke.persist.enums.OPType;
import com.jinke.persist.utils.ArrayUtils;
import com.jinke.persist.utils.StringUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class SqlManager {

    private static final String SQL_HEAD_INSERT = "INSERT INTO ";
    private static final String SQL_HEAD_UPDATE = "UPDATE ";

    //所有注解有错误的类，都存在这里
    private final Set<Class> errorClassSet = Collections.synchronizedSet(new HashSet<Class>());

    //已经创建的表名
    private final Set<String> createdTableSet = Collections.synchronizedSet(new HashSet<String>());

    private final ConcurrentHashMap<Class, OPInfo> classToInfoMap = new ConcurrentHashMap<>();

    private OPInfoCollector opInfoCollector;

    private PersistConfiguration persistConfiguration;

    private JdbcTemplate jdbcTemplate;

    private SQLErrorHandlerWrappper errorHandler;

    private InfoLogger infoLogger;


    public SqlManager(JdbcTemplate jdbcTemplate, PersistConfiguration persistConfiguration) {
        this.persistConfiguration = persistConfiguration;
        errorHandler = new SQLErrorHandlerWrappper(persistConfiguration.getSqlErrorHandler());
        infoLogger = new InfoLoggerWrapper(persistConfiguration.getInfoLogger());

        opInfoCollector = new OPInfoCollector(errorHandler);
        this.jdbcTemplate = jdbcTemplate;
    }


    public void insert(List beanList) {
        OPInfo opInfo = getOPInfo(beanList);
        if (opInfo == null) return;

        //get table name
        String tableName = prepare(opInfo, beanList, OPType.INSERT);
        if (tableName == null) return;

        String sql = SQL_HEAD_INSERT + tableName + opInfo.getInsertStatement();

        execute(sql, beanList, opInfo.getInsertFieldList(), OPType.INSERT);
    }


    public void update(List beanList, String[] updateItems, String[] conditionItems) {
        if (ArrayUtils.isEmpty(beanList) || ArrayUtils.isEmpty(updateItems)) return;
        update(beanList, Arrays.asList(updateItems), conditionItems == null ? null : Arrays.asList(conditionItems));
    }

    public void update(List beanList, List<String> updateItems, List<String> conditionItems) {
        if (ArrayUtils.isEmpty(beanList) || ArrayUtils.isEmpty(updateItems)) return;

        OPInfo opInfo = getOPInfo(beanList);
        if (opInfo == null) return;

        //get table name
        String tableName = prepare(opInfo, beanList, OPType.UPDATE);
        if (tableName == null) return;

        StringBuilder sqlBuilder = new StringBuilder(SQL_HEAD_UPDATE + tableName + " SET ");

        List<Field> fieldList = new ArrayList<>();
        for (int i = 0; i < updateItems.size(); ++i) {
            String name = updateItems.get(i);
            Field updateField = opInfo.getField(name);

            if (updateField == null) {
                errorHandler.handleError("can not find property:" + name + " in Class:" + beanList.get(0).getClass(), beanList, OPType.UPDATE);
                return;
            }
            fieldList.add(updateField);
            sqlBuilder.append(StringUtil.getColumnName(updateField));
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
                sqlBuilder.append(StringUtil.getColumnName(condField));
                sqlBuilder.append("=?");
            }
        }

        String sql = sqlBuilder.toString();

        execute(sql, beanList, fieldList, OPType.UPDATE);

    }

    private boolean isSameClass(List beanList, Class clazz) {
        for (Object bean : beanList) {
            if (clazz != bean.getClass()) {
                errorHandler.handleError("beanList have different class:", beanList, OPType.CHECK);
                return false;
            }
        }
        return true;
    }

    private @Nullable OPInfo getOPInfo(List beanList) {
        if (ArrayUtils.isEmpty(beanList)) return null;

        Class clazz = beanList.get(0).getClass();
        //检查beanList中的对象，是否都是同一个类型
        if (!isSameClass(beanList, clazz)) {
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


    Object getFieldValue(Field field, Object object, boolean convertNull) {
        try {
            field.setAccessible(true);
            Object value = field.get(object);
            if (!convertNull) return value;

            if (value != null &&
                    (!(value instanceof String) || !StringUtil.empty((String) value)) ) return value;

            if (persistConfiguration.getCustomEmptyHandler() != null) {
                Object ret = persistConfiguration.getCustomEmptyHandler().handle(field);
                if (ret != null) return ret;
            }
            return persistConfiguration.getDefaultEmptyHandler().handle(field);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }

    }

    Object getFieldValue(Field field, Object object) {
        return getFieldValue(field, object, true);
    }

    private @Nullable String getRealTableName(OPInfo opInfo, List beanList, OPType opType) {
        String tableName = opInfo.getTableName();
        if (opInfo.getTableNameParam().isEmpty())
            return persistConfiguration.getTableNameOverride() == null ? tableName : persistConfiguration.getTableNameOverride().overrideTableName(tableName, beanList);

        //if have placeholder, replace it.
        Object bean = beanList.get(0);
        List<Field> paramFields = opInfo.getTableNameParam();
        String[] values = new String[paramFields.size()];
        for (int i = 0; i < paramFields.size(); ++i) {
            Field field = paramFields.get(i);
            Object value = getFieldValue(field, bean, false);
            if (value == null) {
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

        if (persistConfiguration.getTableNameOverride() != null) {
            tableName = persistConfiguration.getTableNameOverride().overrideTableName(tableName, beanList);
        }
        return tableName;
    }

    private String prepare(OPInfo opInfo, List beanList, OPType opType) {
        String tableName = getRealTableName(opInfo, beanList, opType);
        if (tableName == null) return null;

        if (opInfo.getCreateStatement() == null || createdTableSet.contains(tableName)) return tableName;
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + opInfo.getCreateStatement();
        execute(sql, beanList, OPType.CREATE);
        createdTableSet.add(tableName);
        return tableName;
    }


    private void execute(String sql, List beanList, List<Field> fieldList, OPType opType) {
        infoLogger.info(sql);
        try {
            jdbcTemplate.batchUpdate(sql, new ReflectBatchSetter(beanList, fieldList, this));
        } catch (Exception e) {
            e.printStackTrace();
            errorHandler.handleError(e, beanList, opType);
        }
    }

    void execute(String sql, List beanList ,OPType opType) {
        infoLogger.info(sql);
        try {
            jdbcTemplate.update(sql);
        } catch (Exception e) {
            e.printStackTrace();
            errorHandler.handleError(e, beanList, opType);
        }
    }
}
