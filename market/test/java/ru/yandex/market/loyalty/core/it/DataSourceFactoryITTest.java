package ru.yandex.market.loyalty.core.it;


import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.loyalty.core.config.DatasourceType;
import ru.yandex.market.loyalty.core.config.ITConfig;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

/**
 * @author fonar101
 */
@Ignore
@ContextConfiguration(classes = ITConfig.class)
@RunWith(LoyaltySpringTestRunner.class)
@WebAppConfiguration
public class DataSourceFactoryITTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ITConfig.MasterTestManager masterTestManager;

    @Test
    public void testAllModes() {
        Boolean transactionReadOnly = withinReadOnlyAsyncContext(stat -> {
            try (ResultSet resultSet = stat.executeQuery("show transaction_read_only")) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        });
        assertEquals(true, transactionReadOnly);

        transactionReadOnly = withinReadOnlyAsyncContext(stat -> {
            try (ResultSet resultSet = stat.executeQuery("show transaction_read_only")) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        });
        assertEquals(true, transactionReadOnly);

        transactionReadOnly = withinReadWriteContext(stat -> {
            try (ResultSet resultSet = stat.executeQuery("show transaction_read_only")) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        });
        assertEquals(false, transactionReadOnly);


        try {
            withinReadWriteContext(stat -> stat.execute("DROP TABLE IF EXISTS temp123"));
            withinReadWriteContext(stat -> stat.execute("CREATE TABLE temp123(id bigint)"));
            for (long id = 1; id < 3; id++) {
                final long i = id;
                withinReadWriteContext(stat -> stat.execute("INSERT INTO temp123(id) VALUES(" + i + ")"));
            }

            for (int t = 0; t < 10; t++) {
                withinReadOnlyContext(stat -> {
                    try (ResultSet resultSet = stat.executeQuery("SELECT id FROM temp123")) {
                        int i = 1;
                        while (resultSet.next()) {
                            long id = resultSet.getLong("id");
                            System.out.println("read-only async row " + i++ + ": id=" + id);
                        }
                    }
                    return null;
                });
            }

            for (int t = 0; t < 10; t++) {
                withinReadOnlyAsyncContext(stat -> {
                    try (ResultSet resultSet = stat.executeQuery("SELECT id FROM temp123")) {
                        int i = 1;
                        while (resultSet.next()) {
                            long id = resultSet.getLong("id");
                            System.out.println("read-only row " + i++ + ": id=" + id);
                        }
                    }
                    return null;
                });
            }

            masterTestManager.setMasterFail(true);
            for (int t = 0; t < 10; t++) {
                withinReadOnlyContext(stat -> {
                    try (ResultSet resultSet = stat.executeQuery("SELECT id FROM temp123")) {
                        int i = 1;
                        while (resultSet.next()) {
                            long id = resultSet.getLong("id");
                            System.out.println("read-only async row " + i++ + ": id=" + id);
                        }
                    }
                    return null;
                });
                assertThrows(RuntimeException.class, () ->
                        withinReadWriteContext(stat -> stat.execute("INSERT INTO temp123(id) VALUES(100)"))
                );
            }
        } finally {
            masterTestManager.setMasterFail(false);
            withinReadWriteContext(statement -> statement.execute("DROP TABLE IF EXISTS temp123"));
        }
    }

    private <S> S withinReadWriteContext(StatementConsumer<S, Statement> call) {
        return DatasourceType.READ_WRITE.within(() -> {
            try (Connection connection = dataSource.getConnection(); Statement statement =
                    connection.createStatement()) {
                return call.apply(statement);
            }
        });
    }

    private <S> S withinReadOnlyContext(StatementConsumer<S, Statement> call) {
        return DatasourceType.READ_ONLY.within(() -> {
            try (Connection connection = dataSource.getConnection(); Statement statement =
                    connection.createStatement()) {
                return call.apply(statement);
            }
        });
    }

    private <S> S withinReadOnlyAsyncContext(StatementConsumer<S, Statement> call) {
        return DatasourceType.READ_ONLY_ASYNC.within(() -> {
            try (Connection connection = dataSource.getConnection(); Statement statement =
                    connection.createStatement()) {
                return call.apply(statement);
            }
        });
    }

    private interface StatementConsumer<S, T extends Statement> {
        S apply(T stat) throws SQLException;
    }
}

