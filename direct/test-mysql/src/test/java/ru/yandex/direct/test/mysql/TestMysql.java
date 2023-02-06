package ru.yandex.direct.test.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.direct.mysql.MySQLInstance;

public class TestMysql {
    @Test
    public void test() throws SQLException, InterruptedException {
        try (
                MySQLInstance mysql = new DirectMysqlDb(TestMysqlConfig.directConfig()).start();
                Connection conn = mysql.connect();
                PreparedStatement st = conn.prepareStatement("show databases");
                ResultSet result = st.executeQuery()
        ) {
            List<String> dbNames = new ArrayList<>();
            conn.setCatalog("mysql");
            while (result.next()) {
                dbNames.add(result.getString(1));
            }
            Assert.assertFalse(dbNames.contains("ppc"));
            Assert.assertTrue(dbNames.contains("ppc1"));
            Assert.assertTrue(dbNames.contains("ppc2"));
        }
    }
}
