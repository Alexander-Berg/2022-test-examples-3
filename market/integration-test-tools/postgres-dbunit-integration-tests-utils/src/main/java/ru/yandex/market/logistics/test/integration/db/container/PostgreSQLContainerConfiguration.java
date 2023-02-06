package ru.yandex.market.logistics.test.integration.db.container;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;

@Configuration
public class PostgreSQLContainerConfiguration {
    private static Logger log = LoggerFactory.getLogger(PostgreSQLContainerConfiguration.class);

    private static final String IMAGE = "postgres";
    private static final String FSYNC_OFF_COMMAND = "postgres -c fsync=off";
    private static final Map<String, String> TMPFS_MAP = ImmutableMap.of(
        "/var/lib/postgresql/data", "rw,noexec,nosuid,size=1024M",
        "/temp_pgdata", "rw,noexec,nosuid,size=1024M"
    );

    @Value("${postgres.version:10.7}")
    private String tag;

    @Bean
    public PostgreSQLContainer<?> postgreSQLContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(String.format("%s:%s", IMAGE, tag));
        container.withCommand(FSYNC_OFF_COMMAND);
        if (SystemUtils.IS_OS_LINUX) {
            log.info("TMPFS mapping enabled for: {}", SystemUtils.OS_NAME);
            container.setTmpFsMapping(TMPFS_MAP);
        }
        container.start();
        log.info("Postgres started: {}", container.getJdbcUrl());
        return container;
    }
}
