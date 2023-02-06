package ru.yandex.market.mbo.integration.test.initializers;

import com.google.common.collect.ImmutableMap;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.mbo.integration.test.config.MasterDatabaseConfig;
import ru.yandex.market.mbo.integration.test.orchestrator.DatabaseGroup;
import ru.yandex.market.mbo.integration.test.orchestrator.DatabaseGroupOrchestrator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates databases for tests and drops them after tests finished.
 *
 * @author s-ermakov
 */
public class DatabasesInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Logger log = LoggerFactory.getLogger(DatabasesInitializer.class);

    private static final Map<String, String> SCHEMA_NAMES = ImmutableMap.<String, String>builder()
        .put("site_catalog", "site_catalog")
        .put("market_content", "market_content")
        .put("market_content_draft", "market_content_draft")
        .put("mbo_tms", "mbo_tms")
        .put("watches", "market_depot")
        .build();

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        HikariDataSource masterDataSource = createMasterDataSource();
        JdbcTemplate masterJDBCTemplate = new JdbcTemplate(masterDataSource);

        DatabaseGroupOrchestrator databaseCreator = new DatabaseGroupOrchestrator(
            masterJDBCTemplate,
            masterDataSource.getUsername()
        );
        DatabaseGroup databaseGroup = databaseCreator.createGroup(SCHEMA_NAMES.keySet());
        log.info("Created database group: " + databaseGroup);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            databaseCreator.dropGroup(databaseGroup);
            log.info("Drop database group: " + databaseGroup);
        }));

        Map<String, Object> connectionParams = new LinkedHashMap<>();
        SCHEMA_NAMES.forEach((schema, propertyPrefix) -> {
            String jdbcUrl = masterDataSource.getJdbcUrl();
            String schemeName = databaseGroup.getSchemeName(schema);
            String username = databaseGroup.getSchemeUserName(schema);
            String password = databaseGroup.getSchemePassword(schema);
            Map<String, String> params = schemaParams(propertyPrefix, jdbcUrl, schemeName, username, password);
            connectionParams.putAll(params);
        });

        applicationContext.getEnvironment().getPropertySources().addFirst(
            new MapPropertySource("dynamic-params", connectionParams));

        log.debug("Injected datasource params:\n" +
            connectionParams.entrySet().stream()
                .map(kv -> kv.getKey() + "=" + kv.getValue())
                .collect(Collectors.joining("\n")));
    }

    private Map<String, String> schemaParams(String propertyPrefix, String url,
                                             String schemaName, String userName, String password) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(propertyPrefix + ".scat.default_schema", schemaName);
        map.put(propertyPrefix + ".scat.jdbc.driverClassName", "oracle.jdbc.OracleDriver");
        map.put(propertyPrefix + ".scat.jdbc.url", url);
        map.put(propertyPrefix + ".scat.username", userName);
        map.put(propertyPrefix + ".scat.password", password);
        map.put(propertyPrefix + ".username", userName);
        map.put(propertyPrefix + ".password", password);
        return map;
    }

    private HikariDataSource createMasterDataSource() {
        ClassPathResource masterProperties = new ClassPathResource("/mbo-functional-tests-oaas/master-db.properties");
        ResourcePropertySource resourcePropertySource = null;
        try {
            resourcePropertySource = new ResourcePropertySource(masterProperties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MasterDatabaseConfig masterDatabaseConfig = new MasterDatabaseConfig();
        return masterDatabaseConfig.masterDataSource(
            (String) resourcePropertySource.getProperty("master.scat.jdbc.driverClassName"),
            (String) resourcePropertySource.getProperty("master.scat.jdbc.url"),
            (String) resourcePropertySource.getProperty("master.scat.username"),
            (String) resourcePropertySource.getProperty("master.scat.password")
        );
    }
}
