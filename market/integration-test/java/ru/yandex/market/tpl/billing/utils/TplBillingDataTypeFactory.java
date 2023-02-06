package ru.yandex.market.tpl.billing.utils;

import java.sql.Types;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;

public class TplBillingDataTypeFactory extends PostgresqlDataTypeFactory {

    static final String JSONB = "jsonb";

    private static final Set<String> ENUM_TYPE_NAMES = Set.of(
        "courier_pickup_point_type",
        "courier_service_type",
        "partner_type",
        "pickup_point_branding_type",
        "routing_vehicle_type",
        "tariff_value_type",
        "transaction_scope"
    );

    private Map<String, Supplier<DataType>> creators = Map.of(
        "jsonb", JsonBDataType::new
    );

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

    @Override
    public boolean isEnumType(String sqlTypeName) {
        return ENUM_TYPE_NAMES.contains(sqlTypeName.toLowerCase());
    }

    @Override
    public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
        if (isEnumType(sqlTypeName)) {
            sqlType = Types.OTHER;
        }

        return super.createDataType(sqlType, sqlTypeName);
    }
}
