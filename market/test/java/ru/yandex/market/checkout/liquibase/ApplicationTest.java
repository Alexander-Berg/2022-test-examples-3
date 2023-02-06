package ru.yandex.market.checkout.liquibase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.liquibase.config.DbMigrationConfig;
import ru.yandex.market.checkout.liquibase.config.TestDbConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(name = "root", classes = {TestDbConfig.class, DbMigrationConfig.class})
public class ApplicationTest {

    @Autowired
    private DataSource datasourceCheckouter;

    @Test
    public void checkPostgresVersion() throws Exception {
        try (Connection connection = datasourceCheckouter.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT version()")) {
            Assertions.assertTrue(resultSet.next(), "Can't execute simple query to db. ");
            final String pgVersion = resultSet.getString(1);
            Assertions.assertTrue(pgVersion.startsWith("PostgreSQL 12.8"), pgVersion);
        }
    }
}
