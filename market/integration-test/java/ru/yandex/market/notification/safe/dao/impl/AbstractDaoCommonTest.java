package ru.yandex.market.notification.safe.dao.impl;

import java.sql.Connection;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Ignore;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Базовый класс тестов для проверки DAO.
 *
 * @author Vladislav Bauer
 */
@Ignore
abstract class AbstractDaoCommonTest<D extends AbstractDao> {

    private static final String DRIVER_CLASS_NAME = "oracle.jdbc.driver.OracleDriver";
    private static final String CONNECTION_INIT_SQL = "select * from dual";
    private static final String JDBC_URL = "jdbc:oracle:thin:@//marketdevdb03h-vip.yandex.ru:1521/billingdb";
    private static final String USERNAME = "sysdev";
    private static final String PASSWORD = "sysdev";

    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
    private static final long VALIDATION_TIMEOUT = TimeUnit.SECONDS.toMillis(60);
    private static final int MAX_POOL_SIZE = 1;


    @Nonnull
    final NamedParameterJdbcTemplate jdbcTemplate = createJdbcTemplate();


    @Nonnull
    abstract D createDao();

    @Nonnull
    <T> T check(final Function<D, T> operation) {
        final D dao = createDao();
        final T result = operation.apply(dao);

        assertThat(result, notNullValue());

        return result;
    }


    @Nonnull
    private NamedParameterJdbcTemplate createJdbcTemplate() {
        try {
            final DataSource dataSource = createDataSource();
            final NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

            try (Connection connection = dataSource.getConnection()) {
                assertThat(connection, notNullValue());
            }

            return template;
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    private DataSource createDataSource() {
        final HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(DRIVER_CLASS_NAME);
        dataSource.setJdbcUrl(JDBC_URL);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);
        dataSource.setMaximumPoolSize(MAX_POOL_SIZE);
        dataSource.setConnectionTimeout(CONNECTION_TIMEOUT);
        dataSource.setValidationTimeout(VALIDATION_TIMEOUT);
        dataSource.setReadOnly(true);
        dataSource.setInitializationFailTimeout(1L);
        dataSource.setConnectionInitSql(CONNECTION_INIT_SQL);
        return dataSource;
    }

}
