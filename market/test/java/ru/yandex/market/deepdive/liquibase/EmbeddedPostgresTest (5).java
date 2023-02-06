package ru.yandex.market.deepdive.liquibase;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.deepdive.configuration.DbConfig;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DbConfig.class)
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
