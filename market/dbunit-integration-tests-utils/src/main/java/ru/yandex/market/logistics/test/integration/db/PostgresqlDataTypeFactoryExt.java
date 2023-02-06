package ru.yandex.market.logistics.test.integration.db;

import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;

public class PostgresqlDataTypeFactoryExt extends PostgresqlDataTypeFactory {

    private Map<String, Supplier<DataType>> creators = ImmutableMap.of(
        "jsonb", JsonBDataType::new,
        "_int8", ArrayDataType::new,
        "_text", TextArrayDataType::new
    );

    static final String JSONB = "jsonb";
    static final String INT_ARRAY = "_int8";
    static final String TEXT_ARRAY = "_text";

    @Override
    public DataType createDataType(int sqlType, String sqlTypeName, String tableName, String columnName) {
        return creators
            .getOrDefault(sqlTypeName, () -> defaultType(sqlType, sqlTypeName, tableName, columnName))
            .get();
    }

    private DataType defaultType(
        int sqlType,
        String sqlTypeName,
        String tableName,
        String columnName
    ) {
        try {
            return super.createDataType(sqlType, sqlTypeName, tableName, columnName);
        } catch (DataTypeException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
