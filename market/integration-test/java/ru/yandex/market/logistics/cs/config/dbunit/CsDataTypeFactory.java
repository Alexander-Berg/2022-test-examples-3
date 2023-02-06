package ru.yandex.market.logistics.cs.config.dbunit;

import java.sql.Types;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;

import ru.yandex.market.logistics.test.integration.db.PostgresqlDataTypeFactoryExt;

@ParametersAreNonnullByDefault
public class CsDataTypeFactory extends PostgresqlDataTypeFactoryExt {
    private static final Set<String> ENUM_TYPE_NAMES = Set.of("capacity_counting_type");

    @Override
    public boolean isEnumType(String sqlTypeName) {
        return ENUM_TYPE_NAMES.contains(sqlTypeName.toLowerCase());
    }

    @Override
    public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
        if (isEnumType(sqlTypeName)) {
            sqlType = Types.OTHER;
        }
        if (LTreeDataType.LTREE_TYPE.equalsIgnoreCase(sqlTypeName)) {
            return new LTreeDataType();
        }
        return super.createDataType(sqlType, sqlTypeName);
    }
}
