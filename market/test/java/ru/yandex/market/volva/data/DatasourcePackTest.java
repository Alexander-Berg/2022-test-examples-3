package ru.yandex.market.volva.data;

import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class DatasourcePackTest {

    private static final String JDBC_URL =
            "jdbc:postgresql://" +
                    "sas-f1rl8lmudw0r8q94.db.yandex.net:6432," +
                    "vla-ok1mnkaznqx9j175.db.yandex.net:6432" +
                    "/marketstat_antifraud_orders_test?&targetServerType=master&ssl=true&sslmode=verify-full";
    private static final String JDBC_DRIVER = "org.postgresql.Driver";
    private static final String USER_NAME = "test_user";
    private static final String PASSWORD = "password";
    private static final String JDBC_SCHEMA = "public";

    @Test
    public void getMasterDataSource() {
        DatasourcePack pack = new DatasourcePack(credentials());
        DataSource masterDatasource = pack.getMasterDataSource();
        HikariDataSource hikariDataSource = extractDatasource(masterDatasource);

        assertThat(hikariDataSource.getJdbcUrl()).isEqualTo(JDBC_URL);
        assertThat(hikariDataSource.getDriverClassName()).isEqualTo(JDBC_DRIVER);
        assertThat(hikariDataSource.getUsername()).isEqualTo(USER_NAME);
        assertThat(hikariDataSource.getPassword()).isEqualTo(PASSWORD);
    }

    @Test
    public void getReadDataSources() {
        DatasourcePack pack = new DatasourcePack(credentials());
        List<DataSource> readDataSources = pack.getReadDataSources();
        Set<String> urls = Set.of(
                "jdbc:postgresql://sas-f1rl8lmudw0r8q94.db.yandex.net:6432/marketstat_antifraud_orders_test?&ssl=true&sslmode=verify-full",
                "jdbc:postgresql://vla-ok1mnkaznqx9j175.db.yandex.net:6432/marketstat_antifraud_orders_test?&ssl=true&sslmode=verify-full"
        );
        for (var ds : readDataSources){
            HikariDataSource hDs = extractDatasource(ds);
            assertThat(urls).contains(hDs.getJdbcUrl());
            assertThat(hDs.getDriverClassName()).isEqualTo(JDBC_DRIVER);
            assertThat(hDs.getUsername()).isEqualTo(USER_NAME);
            assertThat(hDs.getPassword()).isEqualTo(PASSWORD);
        }
    }

    @SneakyThrows
    private HikariDataSource extractDatasource(DataSource proxyDataSource) {
        return proxyDataSource.unwrap(HikariDataSource.class);
    }

    private DbCredentials credentials() {
        return DbCredentials.builder()
                .jdbcUrl(JDBC_URL)
                .jdbcDriver(JDBC_DRIVER)
                .jdbcUsername(USER_NAME)
                .jdbcPassword(PASSWORD)
                .jdbcSchema(JDBC_SCHEMA)
                .build();
    }
}
