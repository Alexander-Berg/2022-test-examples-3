package ru.yandex.market.common.test.db.ddl.datatype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.dbunit.dataset.datatype.AbstractDataType;
import org.json.JSONException;
import org.postgresql.util.PGobject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Позволяет использовать тип данных jsonb при инициализации postgres-базы.
 *
 * @author fbokovikov
 */
class JsonbDataType extends AbstractDataType {
    static final String TYPE = "jsonb";
    private static final Logger logger = LoggerFactory.getLogger(JsonbDataType.class);

    JsonbDataType() {
        super(TYPE, Types.OTHER, String.class, false);
    }

    @Override
    public Object typeCast(Object value) {
        return value == null ? null : value.toString();
    }

    @Override
    public int compareNonNulls(Object first, Object second) {
        JSONCompareResult result;

        try {
            result = JSONCompare.compareJSON(first.toString(), second.toString(), JSONCompareMode.NON_EXTENSIBLE);
            logger.info("Json comparison result: {}", result.getMessage());
        } catch (JSONException e) {
            throw new RuntimeException("Failed to parse json: " + e.getMessage());
        }

        return result.failed() ? -1 : 0;
    }

    @Override
    public Object getSqlValue(int column, ResultSet resultSet) throws SQLException {
        return resultSet.getString(column);
    }

    @Override
    public void setSqlValue(Object value,
                            int column,
                            PreparedStatement statement) throws SQLException {
        PGobject jsonObj = new PGobject();
        jsonObj.setType("json"); // not jsonb?
        jsonObj.setValue(value == null ? null : value.toString());
        statement.setObject(column, jsonObj);
    }
}
