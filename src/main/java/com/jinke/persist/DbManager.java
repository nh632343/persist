package com.jinke.persist;

import com.jinke.persist.collector.BeanInfoCollector;
import com.jinke.persist.config.*;
import com.jinke.persist.dialect.AbstractDialect;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.*;


public class DbManager {

    private interface ExecSqlArgsFunc {
        AbstractDialect.ExecSqlArgs get(List beanList, String tableName, BeanInfo beanInfo, OPOption opOption);
    }


    private ThreadLocal<List> beanListHolder = new ThreadLocal<>();

    //注解信息收集器
    private BeanInfoCollector beanInfoCollector;

    private OPPrepare opPrepare;

    private PersistConfiguration persistConfiguration;

    private JdbcTemplate jdbcTemplate;

    private SQLErrorHandlerWrapper errorHandler;

    private InfoLogger infoLogger;


    public DbManager(JdbcTemplate jdbcTemplate, PersistConfiguration persistConfiguration) {
        this.persistConfiguration = persistConfiguration;
        errorHandler = new SQLErrorHandlerWrapper(persistConfiguration.getSqlErrorHandler());
        infoLogger = new InfoLoggerWrapper(persistConfiguration.getInfoLogger());

        beanInfoCollector = new BeanInfoCollector(errorHandler);
        opPrepare = new OPPrepare(this, persistConfiguration, errorHandler);

        this.persistConfiguration.getDialect().setErrorHandler(new AbstractDialect.IErrorHandler() {
            @Override
            public void onError(String msg) {
                errorHandler.handleError(msg, beanListHolder.get());
            }

            @Override
            public void onError(Exception e) {
                errorHandler.handleError(e, beanListHolder.get());
            }
        });

        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(List beanList) {
        insert(beanList, null);
    }


    /**
     * 插入，除了primary key和transient注解的属性
     * @param beanList 插入的bean
     * @param opOption 额外参数，可为null
     */
    public void insert(List beanList, OPOption opOption) {
        handleInner(beanList, opOption,
                new ExecSqlArgsFunc() {
                    @Override
                    public AbstractDialect.ExecSqlArgs get(List beanList, String tableName, BeanInfo beanInfo, OPOption opOption) {
                        return persistConfiguration.getDialect().insert(
                                tableName, beanInfo, beanList, opOption);
                    }
                });

    }


    public void update(List beanList, String updateGroup, String conditionGroup) {
        update(beanList, updateGroup, conditionGroup, null);
    }

    public void update(List beanList, final String updateGroup, final String conditionGroup, OPOption opOption) {
        handleInner(beanList, opOption,
                new ExecSqlArgsFunc() {
            @Override
            public AbstractDialect.ExecSqlArgs get(List beanList, String tableName, BeanInfo beanInfo, OPOption opOption) {
                return persistConfiguration.getDialect().update(
                        tableName, updateGroup, conditionGroup, beanInfo, beanList, opOption);
            }
        });

    }

    private void handleInner(List beanList, OPOption opOption, ExecSqlArgsFunc execSqlArgsFunc) {
        beanListHolder.set(beanList);
        BeanInfo beanInfo = beanInfoCollector.getBeanInfo(beanList, persistConfiguration);
        if (beanInfo == null) return;

        //获取真正表名 和 创建表（如果配置了自动创建）
        String tableName = opPrepare.prepare(beanInfo, beanList, opOption);
        if (tableName == null) return;

        AbstractDialect.ExecSqlArgs args = execSqlArgsFunc.get(beanList, tableName, beanInfo, opOption);
        if (args == null) return;

        execute(args.sql, beanList, args.fieldList);
        beanListHolder.remove();
    }


    public PersistConfiguration getPersistConfiguration() {
        return persistConfiguration;
    }




    boolean execute(String sql, List beanList, List<Field> fieldList) {
        infoLogger.info(sql);
        try {
            jdbcTemplate.batchUpdate(sql, new ReflectBatchSetter(beanList, fieldList, this));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            errorHandler.handleError(e, beanList, sql);
            return false;
        }
    }

    boolean execute(String[] sql, List beanList) {

        try {
            jdbcTemplate.batchUpdate(sql);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            errorHandler.handleError(e, beanList, Arrays.toString(sql));
            return false;
        }
    }
}
