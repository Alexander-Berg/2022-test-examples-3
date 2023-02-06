package ru.yandex.autotests.market.stat.dictionaries_yt.beans;

import com.google.common.base.Preconditions;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.records.DictionaryRecord;
import ru.yandex.autotests.market.stat.beans.tables.WithTableName;
import ru.yandex.autotests.market.stat.mappers.RowMapperUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;


/**
 * Created by entarrion on 22.09.16.
 */
public class DictType<T> implements WithTableName {

    private Class dataClass;
    private String table;


    public DictType(Class<T> clazz) {
        this.dataClass = clazz;
        DictTable tableInfo = getTableInfo(dataClass);
        this.table = tableInfo.name();
    }

    @Override
    public String getTableName() {
        return table;
    }

    private static <T extends DictionaryRecord> DictTable getTableInfo(Class<T> clazz) {
        Preconditions.checkState(clazz.isAnnotationPresent(DictTable.class), "No table annotation for dictionary!");
        return clazz.getAnnotation(DictTable.class);
    }

    public List<String> getIdFields() {
        List<String> result = Arrays.stream(getDataClass().getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(DictionaryIdField.class) && f.getAnnotation(DictionaryIdField.class).isForQuery())
            .map(RowMapperUtils::getFieldName)
            .collect(toList());

        Preconditions.checkState(result != null && result.size() > 0, "No id fields for dictionary " + getTableName() + " found!");
        return result;
    }

    public List<String> getRequiredFields() {
        List<String> result = new ArrayList<>();
        result.addAll(getIdFields());
        result.addAll(Arrays.stream(getDataClass().getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(RequiredField.class))
            .map(RowMapperUtils::getFieldName)
            .collect(toList()));

        Preconditions.checkState(result.size() > 0, "No required fields for dictionary " + getTableName() + " found!");
        return result;
    }

    public Class getDataClass() {
        return dataClass;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DictType && this.dataClass.equals(((DictType) o).getDataClass());
    }

    @Override
    public String toString() {
        return getTableName();
    }
}
