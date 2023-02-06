package ru.yandex.edu.ora;

import org.junit.Test;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import ru.yandex.core.MailProvider;
import ru.yandex.core.Settings;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by nasyrov on 10.03.2016.
 */
public class Sql2oTests extends MailProvider {

    private Sql2o sql2o;
    public Sql2oTests() throws IOException {
        sql2o = new Sql2o(Settings.getOraString(),Settings.get("oracle.user"),Settings.get("oracle.password"));
    }


    @Test
    public void basic() throws SQLException {
        try (Connection con = sql2o.open()) {
            List<TestRow> rows = con.createQuery("select 'test' name, :pNum num from dual")
                    .addParameter("pNum", 123)
                    .executeAndFetch(TestRow.class);
            assertEquals(rows.size(), 1);
            TestRow r = rows.get(0);
            assertEquals("test", r.name);
            assertEquals(BigDecimal.valueOf(123), r.num);

            List<Map<String,Object>> list = con.createQuery("select 'test' name, 123 num from dual")
                    .executeAndFetchTable()
                    .asList();
            assertEquals(list.size(), 1);
            Map<String,Object> item = list.get(0);
            assertEquals("test", item.get("name"));
            BigDecimal num = (BigDecimal) item.get("num");
            assertEquals(BigDecimal.valueOf(123), item.get("num"));
        }
    }

    @Test
    public void paramsTest() {
        try  (Connection con = sql2o.open()) {
            List<Map<String,Object>> list =
                    con.createQuery("select \"id\" id from bpmonline.\"SysAdminUnit\" where \"Name\"=:pName")
                            .addParameter("pName", "nasyrov")
                            .executeAndFetchTable()
                            .asList();
            assertEquals(list.size(), 1);
            Map<String,Object> item = list.get(0);
            String id = (String)item.get("id");
            assertTrue(id.length() > 0);
        }
    }

    public void smth(){
        try  (Connection con = sql2o.open()) {
            List<Map<String, Object>> list =
                    con.createQuery("select \"id\" id from bpmonline.\"SysAdminUnit\" where \"Name\"=:pName")
                            .addParameter("pName", "nasyrov")
                            .executeAndFetchTable()
                            .asList();
        }
    }
}
