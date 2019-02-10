package com.jinke.persist.dialect;

import com.jinke.persist.BeanInfo;
import com.jinke.persist.config.OPOption;
import com.jinke.persist.enums.ConflictAction;
import com.jinke.persist.utils.ArrayUtils;
import com.jinke.persist.utils.ColumnUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class POSTGRESQLDialect extends AbstractDialect {

    private static final String SQLTransfer = "\"";

    @Override
    public String getSQLTransfer() {
        return SQLTransfer;
    }

    @Override
    public String[] createTable(String tableName, BeanInfo beanInfo) {
        String[] createSqls = beanInfo.getCreateStatements();
        createSqls[0] = "CREATE TABLE IF NOT EXISTS " + tableName + " " + createSqls[0];
        return createSqls;
    }

    @Override
    public ExecSqlArgs insert(String tableName, BeanInfo beanInfo, List beanList, OPOption opOption) {
        String sql = "INSERT INTO " + tableName + " " + beanInfo.getInsertStatement();
        if (opOption == null
                || opOption.getConflictOption() == null
                || opOption.getConflictOption().getConflictAction() == null) {
            return new ExecSqlArgs(sql, beanInfo.getInsertFieldList());
        }

        OPOption.ConflictOption conflictOption = opOption.getConflictOption();
        sql = sql + " ON CONFLICT ON CONSTRAINT " + conflictOption.getConstraint();

        ConflictAction conflictAction = conflictOption.getConflictAction();
        switch (conflictAction) {
            case DO_NOTHING:
                sql = sql + " DO NOTHING";
                return new ExecSqlArgs(sql, beanInfo.getInsertFieldList());
            case UPDATE:
                sql = sql + " DO UPDATE SET ";
                String groupName = conflictOption.getUpdateFieldGroup();
                List<Field> groupFieldList = beanInfo.getFieldListByGroup(groupName);
                if (!checkFieldGroup(groupFieldList, groupName)) {
                    return null;
                }

                List<Field> retFieldList = new ArrayList<>(beanInfo.getInsertFieldList());
                retFieldList.addAll(groupFieldList);

                return new ExecSqlArgs(
                        sql + ColumnUtil.getAssignColumnSql(groupFieldList, beanInfo), retFieldList);
        }

        //not reachable
        return null;
    }

    @Override
    public ExecSqlArgs update(String tableName, String updateGroup, String conditionGroup,
                              BeanInfo beanInfo, List beanList, OPOption opOption) {
        List<Field> updateFieldGroup = beanInfo.getFieldListByGroup(updateGroup);
        if (!checkFieldGroup(updateFieldGroup, updateGroup)) {
            return null;
        }

        String sql = "UPDATE " + tableName + " SET " + ColumnUtil.getAssignColumnSql(updateFieldGroup, beanInfo);
        if (ColumnUtil.empty(conditionGroup)) {
            return new ExecSqlArgs(sql, updateFieldGroup);
        }

        List<Field> conditionFieldGroup = beanInfo.getFieldListByGroup(conditionGroup);
        if (!checkFieldGroup(conditionFieldGroup, updateGroup)) {
            return null;
        }
        sql = sql + " WHERE " + ColumnUtil.getAssignColumnSql(conditionFieldGroup, beanInfo, "and");
        List<Field> retFieldList = new ArrayList<>(updateFieldGroup);
        retFieldList.addAll(conditionFieldGroup);
        return new ExecSqlArgs(sql, retFieldList);
    }

    private boolean checkFieldGroup(List<Field> fieldList, String groupName) {
        if (ArrayUtils.isEmpty(fieldList)) {
            onError("get field group fail, group:" + groupName);
            return false;
        }
        return true;
    }
}
