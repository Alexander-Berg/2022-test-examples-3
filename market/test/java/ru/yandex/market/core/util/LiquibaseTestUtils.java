package ru.yandex.market.core.util;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Утилитный класс для запуска Liquibase changelog'ов в тестах.
 *
 * @see liquibase.integration.spring.SpringLiquibase
 */
public final class LiquibaseTestUtils {
    private static final Logger log = LoggerFactory.getLogger(LiquibaseTestUtils.class);

    private LiquibaseTestUtils() {
    }

    public static void runLiquibase(DataSource dataSource, Iterable<String> changelogFiles) {
        try (Connection connection = dataSource.getConnection()) {
            runLiquibase(connection, changelogFiles);
        } catch (SQLException ex) {
            throw new RuntimeException("Could not apply Liquibase changelog(s)", ex);
        }
    }

    public static void runLiquibase(Connection connection, Iterable<String> changelogFiles) {
        var jdbcConnection = new JdbcConnection(connection);
        var resourceAccessor = new ClassLoaderResourceAccessor();
        var ctx = new Contexts("functionalTest");
        var labels = new LabelExpression();
        for (String changelogFile : changelogFiles) {
            log.info("Running liquibase for [{}]...", changelogFile);
            try {
                var liquibase = new Liquibase(changelogFile, resourceAccessor, jdbcConnection);
                liquibase.update(ctx, labels);
            } catch (LiquibaseException e) {
                throw new RuntimeException(e);
            }
            log.info("...done liquibase for [{}]", changelogFile);
        }
    }
}
