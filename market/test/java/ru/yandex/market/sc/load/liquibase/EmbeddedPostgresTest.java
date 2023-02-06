package ru.yandex.market.sc.load.liquibase;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.sc.load.TestDatabaseConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(classes = {TestDatabaseConfiguration.class})
public class EmbeddedPostgresTest {

    @Autowired
    DataSource dataSource;

    @Test
    public void testTablesMade() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT * FROM spok");
            rs.next();
            assertEquals("test liquibase", rs.getString(1));
        }
    }
}
