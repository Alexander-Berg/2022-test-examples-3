package ru.yandex.market.common.test.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class InstrumentedPreparedStatement extends PreparedStatementDelegate {

    public InstrumentedPreparedStatement(PreparedStatement delegate) {
        super(delegate);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        if (x instanceof Supplier) {
            x = ((Supplier) x).get();
        }
        super.setObject(parameterIndex, x, targetSqlType);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        if (x instanceof Supplier) {
            x = ((Supplier) x).get();
        }
        super.setObject(parameterIndex, x);
    }
}
