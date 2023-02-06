package ru.yandex.market.core.expimp.db;

import javax.sql.DataSource;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import ru.yandex.market.core.expimp.db.model.Schema;


/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class OracleSchemaReaderTest {
    /**
     * Тест читает из дев базы все таблицы схемы SHOPS_WEB. Игрорирован, т.к. долгий.
     */
    @Test
    @Ignore
    public void readTables() throws Exception {
        SchemaReader reader = createSchemaReader();
        Schema schema = reader.readSchema("SHOPS_WEB");
        System.out.println(schema);
    }

    /**
     * Тест читает из дев базы две таблицы таблицы схемы SHOPS_WEB.
     */
    @Test
    @Ignore
    public void readTable() throws Exception {
        SchemaReader reader = createSchemaReader();
        Schema schema = reader.readSchema("SHOPS_WEB", "DELIVERY_OPTION_GROUPS", "DATAFEED");
        System.out.println(schema);
    }

    protected SchemaReader createSchemaReader() {
        DataSource ds = createDataSource();
        return new OracleSchemaReader(new JdbcTemplate(ds), new TypeMapper());
    }

    protected DataSource createDataSource() {
        return new SingleConnectionDataSource(
                "jdbc:oracle:thin:@//marketdevdb03h-vip.yandex.ru/billingdb",
                "sysdev",
                "sysdev",
                true
        );
    }

}
