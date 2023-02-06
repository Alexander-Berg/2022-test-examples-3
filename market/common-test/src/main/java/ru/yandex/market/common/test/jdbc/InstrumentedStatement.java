package ru.yandex.market.common.test.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.common.test.transformer.StringTransformer;

/**
 * Проксурующий {@link Statement}, используемый в юнит-тестах, чтобы иметь возможность модифицировать sql-запросы перед
 * их отправкой в СУБД.
 *
 * @author zoom
 */
public class InstrumentedStatement extends StatementDelegate {

    private final StringTransformer stringTransformer;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public InstrumentedStatement(Statement delegate, StringTransformer stringTransformer) {
        super(delegate);
        this.stringTransformer = stringTransformer;
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        String transformedSql = stringTransformer.transform(sql);
        logger.debug(transformedSql);
        return super.execute(transformedSql);
    }

    @Override
    public ResultSet executeQuery(final String sql) throws SQLException {
        String transformedSql = stringTransformer.transform(sql);
        logger.debug(transformedSql);
        return super.executeQuery(transformedSql);
    }

    @Override
    public int executeUpdate(final String sql) throws SQLException {
        String transformedSql = stringTransformer.transform(sql);
        logger.debug(transformedSql);
        return super.executeUpdate(transformedSql);
    }
}
