package ru.yandex.market.rg.outlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@Disabled
class ExportOutletIdsExecutorManualTest {

    ExportOutletIdsExecutor executor;

    @BeforeEach
    void setUp() {
        if (executor == null) {
            var dataSource = new PGSimpleDataSource();
            dataSource.setUser(System.getProperty("PG_USER"));
            dataSource.setPassword(System.getProperty("PG_PASSWORD"));
            dataSource.setUrl("jdbc:postgresql://" +
                    "sas-niw5fd3mmmjsrac3.db.yandex.net:6432," +
                    "vla-z90mih98uiw5fvdc.db.yandex.net:6432" +
                    "/mbi?&targetServerType=secondary");
            executor = new ExportOutletIdsExecutor(
                    new JdbcTemplate(dataSource),
                    null
            );
        }
    }

    @Test
    void test() throws IOException {
        Path path = Files.createTempFile("outlet", ".pbsn");
        System.out.println(path);
        executor.makeFile(path);
    }

}
