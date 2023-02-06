package ru.yandex.market.logistics.pechkin.app.configuration;

import java.util.Map;

import javax.sql.DataSource;

import com.google.common.collect.ImmutableMap;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.org.apache.commons.lang.SystemUtils;

@Configuration
public class DatabaseTestConfiguration {

    private static final String IMAGE = "postgres";
    private static final String TAG = "10.7";

    private static final String FSYNC_OFF_COMMAND = "postgres -c fsync=off";
    private static final Map<String, String> TMPFS_MAP = ImmutableMap.of(
        "/var/lib/postgresql/data", "rw,noexec,nosuid,size=1024M",
        "/temp_pgdata", "rw,noexec,nosuid,size=1024M"
    );

    @Bean
    public PostgreSQLContainer postgreSQLContainer() {
        PostgreSQLContainer container = new PostgreSQLContainer(String.format("%s:%s", IMAGE, TAG));
        container.withCommand(FSYNC_OFF_COMMAND);
        if (SystemUtils.IS_OS_LINUX) {
            container.setTmpFsMapping(TMPFS_MAP);
        }
        container.start();
        return container;
    }

    @Primary
    @Bean
    public DataSource dataSource(PostgreSQLContainer postgres) {
        return createDataSource(postgres);
    }

    private static DataSource createDataSource(PostgreSQLContainer postgres) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        dataSource.setConnectionTestQuery("SELECT 1");
        return dataSource;
    }
}
