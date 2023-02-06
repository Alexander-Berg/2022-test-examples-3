package ru.yandex.market.common.test.db.ddl.datatype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.dbunit.dataset.datatype.StringDataType;
import org.dbunit.dataset.datatype.TypeCastException;
import org.postgresql.jdbc.PgSQLXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @implNote переопределяем только {@link #setSqlValue(Object, int, PreparedStatement)}
 * тк нам надо засунуть в xml-колонку текстовое значение из before.
 * при этом {@link #getSqlValue(int, ResultSet)} оставляем как есть,
 * тк уже будем сравнивать значение в xml-колонке с текстом из after.
 */
public class XmlDataType extends StringDataType {
    private static final Logger logger = LoggerFactory.getLogger(XmlDataType.class);

    XmlDataType() {
        super("xml", Types.SQLXML);
    }

    @Override
    public void setSqlValue(
            Object value,
            int column,
            PreparedStatement statement
    ) throws SQLException, TypeCastException {
        try {
            logger.debug("setSqlValue(value={}, column={}, statement={}) - start", value, column, statement);
            String v = asString(value);
            statement.setSQLXML(column, new PgSQLXML(null, v));
        } catch (SQLException e) {
            // fallback на старое поведение
            super.setSqlValue(value, column, statement);
        }
    }
}
