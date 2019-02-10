package com.jinke.persist.dialect;

import com.jinke.persist.BeanInfo;
import com.jinke.persist.config.OPOption;
import com.jinke.persist.config.SQLErrorHandlerWrapper;

import java.lang.reflect.Field;
import java.util.List;

public abstract class AbstractDialect {
    public interface IErrorHandler {
        void onError(String msg);
        void onError(Exception e);
    }


    private IErrorHandler errorHandler;

    public void setErrorHandler(IErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    protected void onError(String msg) {
        if (errorHandler == null) return;
        errorHandler.onError(msg);
    }

    protected void onError(Exception e) {
        if (errorHandler == null) return;
        errorHandler.onError(e);
    }

    /**
     * 获取sql关键字转义符
     * 对表名和列名等使用该转义符包含
     * @return sql转义符
     */
    public abstract String getSQLTransfer();

    public String getRealSQLTransfer() {
        String transfer = getSQLTransfer();
        if (transfer == null) return "";
        return transfer;
    }

    /**
     * 获取创建表的sql语句集
     * note: 该表名可能已经存在，需确保在表已存在的情况下，执行sql不会抛异常
     *
     * 带有 {@link com.jinke.persist.annotation.AutoCreate} 注解的bean，
     * 第一次执行操作时都会先调用此方法获取建表语句
     *
     * @param tableName 真正的表名
     *                  经过占位符、{@link com.jinke.persist.config.TableNameOverride}处理
     *                  表名已经带有sql关键字转义符 {@link #getSQLTransfer()}
     * @param beanInfo beanInfo
     *     通过{@link com.jinke.persist.BeanInfo#getCreateStatements()}获取
     *     {@link com.jinke.persist.annotation.AutoCreate}注解中的内容
     *     String[] 至少有一个元素
     *
     * @return 需要执行的sql语句集, 返回null表示出现错误
     */
    public abstract String[] createTable(String tableName, BeanInfo beanInfo);

    /**
     * 获取插入的sql语句及参数
     * {@link com.jinke.persist.BeanInfo#getInsertStatement()}获取全部插入的列
     * {@link com.jinke.persist.BeanInfo#getInsertFieldList()}获取对应的field
     *
     * {@link com.jinke.persist.config.OPOption.ConflictOption} 如果有conflict时的操作选项
     * 当conflictOption为null 或者 conflictOption.conflictAction为null，不考虑conflict
     *
     * @param tableName 真正的表名
     *                  经过占位符、{@link com.jinke.persist.config.TableNameOverride}处理
     *                  表名已经带有sql关键字转义符 {@link #getSQLTransfer()}
     * @param beanInfo beanInfo
     * @param beanList beanList
     * @param opOption 可能为null
     * @return 返回null, 表示出现错误
     *          {@link ExecSqlArgs#sql} 需要执行的sql
     *          {@link ExecSqlArgs#fieldList} 填入的值对应的field
     */
    public abstract ExecSqlArgs insert(String tableName, BeanInfo beanInfo, List beanList, OPOption opOption);

    public abstract ExecSqlArgs update(String tableName, String updateGroup, String conditionGroup,
                                       BeanInfo beanInfo, List beanList, OPOption opOption);

    public static class ExecSqlArgs {
        public final String sql;
        public final List<Field> fieldList;

        public ExecSqlArgs(String sql, List<Field> fieldList) {
            this.sql = sql;
            this.fieldList = fieldList;
        }
    }


}
