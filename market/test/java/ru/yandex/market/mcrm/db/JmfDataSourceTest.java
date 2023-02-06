package ru.yandex.market.mcrm.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.mcrm.db.test.TestMasterReadOnlyDataSourceConfiguration;

@SpringJUnitConfig(classes = TestMasterReadOnlyDataSourceConfiguration.class)
public class JmfDataSourceTest {

    @Inject
    private DataSource dataSource;

    /**
     * Должны получить ошибку "cannot execute CREATE TABLE in a read-only transaction"
     */
    @Test
    @Transactional(readOnly = true)
    public void createReadOnly() {
        Assertions.assertThrows(SQLException.class, () -> {
            Connection connection = dataSource.getConnection();
            connection.createStatement().execute("CREATE TABLE readOnly_true_test (id int8)");
        });
    }

    /**
     * Не должны получить ошибку т.к. транзакция не readOnly
     */
    @Test
    @Transactional(readOnly = false)
    public void createMaster() throws Exception {
        Connection connection = dataSource.getConnection();
        connection.createStatement().execute("CREATE TABLE readOnly_false_test (id int8)");
    }
}
