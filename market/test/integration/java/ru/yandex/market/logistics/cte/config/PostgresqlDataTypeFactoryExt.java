package ru.yandex.market.logistics.cte.config;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.dbunit.dataset.datatype.AbstractDataType;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.dataset.datatype.TypeCastException;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.postgresql.util.PGobject;

import static java.sql.Types.OTHER;

public class PostgresqlDataTypeFactoryExt extends PostgresqlDataTypeFactory {

    @Override
    public boolean isEnumType(String sqlTypeName) {
        var enumTypes = new ArrayList<String>();

        enumTypes.add("stock_type");
        enumTypes.add("quality_attribute_type");
        enumTypes.add("attribute_type");
        enumTypes.add("qattribute_assessment_type");
        enumTypes.add("unit_type");

        if (enumTypes.stream().anyMatch(s -> s.equalsIgnoreCase(sqlTypeName))) {
            return true;
        }
        return super.isEnumType(sqlTypeName);
    }

    @Override
    public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
        if (sqlTypeName.equals("jsonb")) {
            return new JsonbDataType();
        } else if (isEnumType(sqlTypeName)) {
            sqlType = OTHER;
        }
        return super.createDataType(sqlType, sqlTypeName);
    }

    public static class JsonbDataType extends AbstractDataType {

        public JsonbDataType() {
            super("jsonb", OTHER, String.class, false);
        }

        @Override
        public Object typeCast(Object obj) throws TypeCastException {
            if (obj != null) {
                return obj.toString();
            } else {
                return null;
            }
        }

        @Override
        public Object getSqlValue(int column, ResultSet resultSet) throws SQLException, TypeCastException {
            return resultSet.getString(column);
        }

        @Override
        public void setSqlValue(Object value,
                                int column,
                                PreparedStatement statement) throws SQLException, TypeCastException {
            final PGobject jsonObj = new PGobject();
            jsonObj.setType("json");
            jsonObj.setValue(value == null ? null : value.toString());

            statement.setObject(column, jsonObj);
        }
    }
}
