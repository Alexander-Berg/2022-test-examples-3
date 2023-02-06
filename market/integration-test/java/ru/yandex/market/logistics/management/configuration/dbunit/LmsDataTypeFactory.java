package ru.yandex.market.logistics.management.configuration.dbunit;

import java.sql.Types;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;

import ru.yandex.market.logistics.test.integration.db.PostgresqlDataTypeFactoryExt;

import static ru.yandex.market.logistics.management.configuration.dbunit.LTreeDataType.LTREE_TYPE;

@ParametersAreNonnullByDefault
public class LmsDataTypeFactory extends PostgresqlDataTypeFactoryExt {
    private static final Set<String> ENUM_TYPE_NAMES = Set.of(
        "partner_status",
        "capacity_delivery_type",
        "capacity_type",
        "service_type",
        "capacity_counting_type",
        "logistic_segment_type",
        "service_status",
        "snapshot_status",
        "delivery_type"
    );

    @Override
    public boolean isEnumType(String sqlTypeName) {
        return ENUM_TYPE_NAMES.contains(sqlTypeName.toLowerCase());
    }

    @Override
    public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
        if (isEnumType(sqlTypeName)) {
            sqlType = Types.OTHER;
        }
        if (LTREE_TYPE.equalsIgnoreCase(sqlTypeName)) {
            return new LTreeDataType();
        }
        return super.createDataType(sqlType, sqlTypeName);
    }
}
