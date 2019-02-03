package com.jinke.persist;

import com.jinke.persist.collector.OPInfoCollector;
import com.jinke.persist.config.*;
import com.jinke.persist.enums.OPType;
import com.jinke.persist.utils.ArrayUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.*;


public class DbManager {

    private static final String SQL_HEAD_INSERT = "INSERT INTO ";
    private static final String SQL_HEAD_UPDATE = "UPDATE ";



    //注解信息收集器
    private OPInfoCollector opInfoCollector;

    private OPPrepare opPrepare;

    private PersistConfiguration persistConfiguration;

    private JdbcTemplate jdbcTemplate;

    private SQLErrorHandlerWrapper errorHandler;

    private InfoLogger infoLogger;


    public DbManager(JdbcTemplate jdbcTemplate, PersistConfiguration persistConfiguration) {
        this.persistConfiguration = persistConfiguration;
        errorHandler = new SQLErrorHandlerWrapper(persistConfiguration.getSqlErrorHandler());
        infoLogger = new InfoLoggerWrapper(persistConfiguration.getInfoLogger());

        opInfoCollector = new OPInfoCollector(errorHandler);
        opPrepare = new OPPrepare(this, persistConfiguration, errorHandler);
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
    public void insert(List beanList, OPOption opConfig) {
        OPInfo opInfo = opInfoCollector.getOPInfo(beanList);
        if (opInfo == null) return;

        //获取真正表名 和 创建表（如果配置了自动创建）
        String tableName = opPrepare.prepare(opInfo, beanList, OPType.INSERT, opConfig);
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
    public void update(List beanList, String[] updateItems, String[] conditionItems, OPOption opConfig) {
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
    public void update(List beanList, List<String> updateItems, List<String> conditionItems, OPOption opConfig) {
        if (ArrayUtils.isEmpty(beanList) || ArrayUtils.isEmpty(updateItems)) return;

        OPInfo opInfo = opInfoCollector.getOPInfo(beanList);
        if (opInfo == null) return;

        //获取真正表名 和 创建表（如果配置了自动创建）
        String tableName = opPrepare.prepare(opInfo, beanList, OPType.UPDATE, opConfig);
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




    boolean execute(String sql, List beanList, List<Field> fieldList, OPType opType) {
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

    boolean execute(String sql, List beanList ,OPType opType) {
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
