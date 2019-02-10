package com.jinke.persist.collector;

import com.jinke.persist.BeanInfo;
import com.jinke.persist.annotation.*;
import com.jinke.persist.config.PersistConfiguration;
import com.jinke.persist.config.SQLErrorHandlerWrapper;
import com.jinke.persist.utils.ArrayUtils;
import com.jinke.persist.utils.ColumnUtil;
import org.springframework.lang.Nullable;


import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BeanInfoCollector {
    private static final String PLACE_HOLDER_PATTERN_STRING = "\\{(.*?)\\}";
    private static final Pattern TABLE_NAME_PLACE_HOLDER_PATTERN = Pattern.compile(PLACE_HOLDER_PATTERN_STRING);
    private static final String INTERNAL_PLACE_HOLDER = "%s";

    private SQLErrorHandlerWrapper errorHandler;

    private ThreadLocal<List> beanListHolder = new ThreadLocal<>();

    //所有注解有错误的类，都存在这里
    private final Set<Class> errorClassSet = Collections.synchronizedSet(new HashSet<Class>());

    private final ConcurrentHashMap<Class, BeanInfo> classToInfoMap = new ConcurrentHashMap<>();

    public BeanInfoCollector(SQLErrorHandlerWrapper errorHandler) {
        this.errorHandler = errorHandler;
    }



    /**
     * 获取该类的表信息
     * @param beanList beanList
     * @return OPInfo, 如果出现错误, 返回null
     */
    public @Nullable BeanInfo getBeanInfo(List beanList, PersistConfiguration persistConfiguration) {
        if (ArrayUtils.isEmpty(beanList) || ArrayUtils.haveNullObject(beanList, errorHandler)) return null;

        Class clazz = beanList.get(0).getClass();
        //检查beanList中的对象，是否都是同一个类型
        if (!ArrayUtils.isSameClass(beanList, clazz, errorHandler)) {
            return null;
        }

        if (errorClassSet.contains(clazz)) return null;


        BeanInfo beanInfo = classToInfoMap.get(clazz);
        if (beanInfo == null) {
            beanInfo = generateOPInfo(clazz, beanList, persistConfiguration);
            if (beanInfo == null) {
                //error
                errorClassSet.add(clazz);
                return null;
            }
            classToInfoMap.put(clazz, beanInfo);
        }
        return beanInfo;
    }

    /**
     * 生成OPInfo
     * @param clazz
     * @return   null表示这个class的注解信息有错误
     */
    private BeanInfo generateOPInfo(Class clazz, List beanList, PersistConfiguration persistConfiguration) {
        beanListHolder.set(beanList);
        BeanInfo result = generateOPInfoInner(clazz, beanList, persistConfiguration);
        beanListHolder.remove();
        return result;
    }

    private BeanInfo generateOPInfoInner(Class clazz, List beanList, PersistConfiguration persistConfiguration) {
        String tableName = getTableName(clazz, persistConfiguration);
        if (ColumnUtil.empty(tableName)) return null;


        //find placeholder in table name
        List<Field> paramList = getTableNameParam(tableName, clazz);
        if (paramList == null) {
            return null;
        }

        BeanInfo beanInfo = new BeanInfo();
        //把原来的占位符换成%s
        beanInfo.setTableName(tableName.replaceAll(PLACE_HOLDER_PATTERN_STRING, INTERNAL_PLACE_HOLDER));
        beanInfo.setTableNameParam(paramList);

        return collectInfo(clazz, beanInfo, persistConfiguration);
    }

    /**
     * 从类的注解获取表名
     * @param clazz
     * @return null表示有错误
     */
    private String getTableName(Class clazz, PersistConfiguration persistConfiguration) {
        TableName tableNameAno = (TableName) clazz.getAnnotation(TableName.class);
        if (tableNameAno == null) {
            onError("Class:" + clazz.getName() + "  have no TableName Annotation");
            return null;
        }

        String tableName = tableNameAno.value();
        if (ColumnUtil.empty(tableName)) {
            onError("Class:" + clazz.getName() + "  TableName Annotation is empty");
            return null;
        }

        String sqlTransfer = persistConfiguration.getDialect().getRealSQLTransfer();
        return sqlTransfer + tableName + sqlTransfer;
    }

    /**
     * 根据tableName中的占位符，找到类中对应的字段
     * @param tableName
     * @param clazz
     * @return 返回null表示解析出错，List可能为空
     */
    private List<Field> getTableNameParam(String tableName, Class clazz) {
        List<Field> paramList = new ArrayList<>();
        Matcher matcher = TABLE_NAME_PLACE_HOLDER_PATTERN.matcher(tableName);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            boolean find = false;
            for (Field field : clazz.getDeclaredFields()) {
                if (placeholder.equals(field.getName())) {
                    find = true;
                    paramList.add(field);
                    break;
                }
            }

            if (!find) {
                onError("Class:" + clazz.getName() +" can not find placeholder:" + placeholder);
                return null;
            }
        }

        return paramList;
    }

    /**
     *
     * @param clazz
     * @param beanInfo
     * @return
     */
    private BeanInfo collectInfo(Class clazz, BeanInfo beanInfo, PersistConfiguration persistConfiguration) {
        List<Field> insertFieldList = new ArrayList<>();
        InsertBuilder insertBuilder = new InsertBuilder();
        ColumnGroupBuilder columnGroupBuilder = new ColumnGroupBuilder();


        //判断是否有自动建表注解
        AutoCreate autoCreateAno = (AutoCreate) clazz.getAnnotation(AutoCreate.class);
        if (autoCreateAno != null) {
            String[] createStatements = autoCreateAno.value();
            if (ArrayUtils.isEmpty(createStatements)) {
                onError("Class:" + clazz.getName() + "'s AutoCreate Annotation has empty String Array");
                return null;
            }
            beanInfo.setCreateStatements(createStatements);
        }

        String sqlTransfer = persistConfiguration.getDialect().getRealSQLTransfer();

        //遍历field -------------------------------------------
        //考虑继承，还要遍历所有父类
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            //
            for (Field field : clazz.getDeclaredFields()) {
                //ignore NotSql field
                if (field.getAnnotation(NotSql.class) != null) continue;

                field.setAccessible(true);
                insertFieldList.add(field);

                //find insert ColumnName
                String columnName = ColumnUtil.getColumnName(field, sqlTransfer);
                beanInfo.putFieldToColumnName(field, columnName);

                insertBuilder.addColumn(columnName);
                try {
                    columnGroupBuilder.addField(field);
                } catch (Exception e) {
                    onError(e.getMessage());
                    return null;
                }
            }
        }

        //遍历field 结束--------------------------------------------------
        beanInfo.setInsertStatement(insertBuilder.toString());
        beanInfo.setInsertFieldList(insertFieldList);
        beanInfo.setGroupToFieldListMap(columnGroupBuilder.getGroupToFieldListMap());
        return beanInfo;
    }

    private void onError(String msg) {
        errorHandler.handleError(msg, beanListHolder.get());
    }
}
