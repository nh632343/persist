package com.jinke.persist.collector;

import com.jinke.persist.annotation.ColumnGroup;
import com.jinke.persist.annotation.ColumnGroups;
import com.jinke.persist.utils.ArrayUtils;
import com.jinke.persist.utils.ColumnUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * column group 信息收集类
 * 只用于单线程
 */
class ColumnGroupBuilder {
    //key为group名称
    //value为对应的field列表
    private Map<String, List<Field>> groupToFieldListMap;

    ColumnGroupBuilder() {

    }

    /**
     * 查看该field的ColumnGroups注解
     * 根据group名称归类保存field
     * @param field field
     * @throws Exception
     */
    void addField(Field field) throws Exception {
        ColumnGroups columnGroupsAno = field.getAnnotation(ColumnGroups.class);
        if (columnGroupsAno == null) return;
        ColumnGroup[] columnGroupAnoArray = columnGroupsAno.value();
        if (ArrayUtils.isEmpty(columnGroupAnoArray)) return;

        if (groupToFieldListMap == null) {
            groupToFieldListMap = new HashMap<>();
        }

        for (ColumnGroup columnGroupAno : columnGroupAnoArray) {
            String columnGroup = columnGroupAno.value();
            if (ColumnUtil.empty(columnGroup)) {
                throw new Exception(field + " 's ColumnGroup is empty");
            }

            List<Field> fieldList = groupToFieldListMap.get(columnGroup);
            if (fieldList == null) {
                fieldList = new ArrayList<>();
                groupToFieldListMap.put(columnGroup, fieldList);
            }

            if ( !fieldList.contains(field) ) {
                fieldList.add(field);
            }
        }

    }

    Map<String, List<Field>> getGroupToFieldListMap() {
        return groupToFieldListMap;
    }
}
