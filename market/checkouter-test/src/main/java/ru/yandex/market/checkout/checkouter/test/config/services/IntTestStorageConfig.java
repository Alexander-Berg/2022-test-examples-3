package ru.yandex.market.checkout.checkouter.test.config.services;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import io.zonky.test.db.postgres.embedded.PreparedDbProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.ZooKeeperMain;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checkout.common.util.ZooPropertiesSetter;
import ru.yandex.market.checkout.liquibase.config.DbMigrationCheckouterArchiveConfig;
import ru.yandex.market.checkout.liquibase.config.DbMigrationCheckouterConfig;
import ru.yandex.market.checkout.liquibase.config.OmsServiceDbMigrationConfiguration;
import ru.yandex.market.checkout.liquibase.zk.ZooMigrationParser;
import ru.yandex.market.checkout.liquibase.zk.ZooMigrationsDao;
import ru.yandex.market.checkout.liquibase.zk.ZooMigrator;
import ru.yandex.market.checkout.liquibase.zk.ZooMigratorSpring;
import ru.yandex.market.checkout.storage.ZooScriptExecutor;

@Import({
        DbMigrationCheckouterConfig.class,
        DbMigrationCheckouterArchiveConfig.class,
        OmsServiceDbMigrationConfiguration.class,
})
@Configuration
public class IntTestStorageConfig {

    @Bean
    public ZooKeeperMain zooKeeperMain(
            CuratorFramework curator
    ) throws Exception {
        return new ZooKeeperMain(curator.getZookeeperClient().getZooKeeper());
    }

    @DependsOn("testZK")
    @Bean(initMethod = "init")
    public ZooScriptExecutor zooScriptExecutor(
            ZooKeeperMain zooKeeperMain,
            @Value("classpath:/files/zookeeper-init.zk")
                    Resource resource
    ) {
        return new ZooScriptExecutor(zooKeeperMain, resource);
    }

    @Bean(initMethod = "start")
    public ZooMigrationsDao zooMigrationsDao(CuratorFramework curator) {
        return new ZooMigrationsDao(curator, "/checkout/migrations");
    }

    @Bean
    public InterProcessMutex migratorLock(CuratorFramework curator) {
        return new InterProcessMutex(curator, "/checkout/migrations-lock");
    }

    @Bean
    public ZooMigrator migrator(
            ZooMigrationsDao zooMigrationsDao,
            InterProcessMutex migratorLock,
            CuratorFramework curator
    ) {
        return new ZooMigrator(zooMigrationsDao, migratorLock, curator);
    }

    @Bean
    public ZooMigrationParser parser() {
        return new ZooMigrationParser();
    }

    @DependsOn("zooScriptExecutor")
    @Bean
    public ZooMigratorSpring zooMigratorSpring(
            ZooMigrator migrator,
            @Value("${zookeeper.changelog}") Resource changelog
    ) throws Exception {
        return new ZooMigratorSpring(migrator, parser().parse(changelog));
    }

    @DependsOn("testZK")
    @Bean(initMethod = "init")
    public ZooScriptExecutor zooPostScriptExeuctor(
            ZooKeeperMain zooKeeperMain,
            @Value("classpath:/files/zookeeper-postinit.zk") Resource postMigrations
    ) {
        return new ZooScriptExecutor(zooKeeperMain, postMigrations);
    }

    @Bean
    public ZooPropertiesSetter zooPropertiesSetter() {
        return new ZooPropertiesSetter();
    }

    @Bean(initMethod = "start", destroyMethod = "close", name = {"testZK2", "testZK"})
    public TestingServer testZK(
            @Value("${market.checkout.zookeeper.connectTimeout}") int connectTimeout
    ) throws Exception {
        InstanceSpec spec = new InstanceSpec(
                null, -1, -1, -1, true, -1, -1, -1,
                Map.of(
                        "maxSessionTimeout", String.valueOf(2 * connectTimeout)
                )
        );
        return new TestingServer(spec, false);
    }


    @ConditionalOnExpression("systemEnvironment['MARKET_CHECKOUTER_USE_LOCAL_POSTGRES'] == 'true'")
    @Bean("databaseUrlsProvider")
    public CacheLoader<Object, String> localDatabaseUrlsProvider() {
        return CacheLoader.from(dbName -> String.format("jdbc:pgcluster://localhost:5432/%s?user=postgres", dbName));
    }

    @ConditionalOnMissingBean
    @Bean("databaseUrlsProvider")
    public CacheLoader<Object, String> embeddedDatabaseUrlsProvider() {
        PreparedDbProvider embeddedDatabaseProvider = PreparedDbProvider.forPreparer(
                (ds) -> {
                },
                List.of(
                        builder -> {
                            builder.setPGStartupWait(Duration.ofMinutes(1));
                            builder.setServerConfig("unix_socket_directories", "");
                        }
                )
        );
        return new CacheLoader<>() {
            @Override
            public String load(@Nonnull Object key) throws Exception {
                var connectionString = embeddedDatabaseProvider.createDatabase();
                return "market_checkouter_local".equals(key)
                        ? connectionString.replace("jdbc:postgresql:", "jdbc:pgcluster:")
                        : connectionString;
            }
        };
    }

    @Bean
    public Cache<Object, String> databaseUrls(CacheLoader<Object, String> databaseUrlsProvider) {
        return CacheBuilder.newBuilder().build(databaseUrlsProvider);
    }

    @Bean(value = {"jdbcTemplate", "slaveJdbcTemplate", "masterJdbcTemplate"})
    public JdbcTemplate jdbcTemplate(DataSource datasource) {
        return new JdbcTemplate(datasource);
    }

    @Bean(value = {"archiveJdbcTemplates", "archiveSlaveJdbcTemplates", "archiveMasterJdbcTemplates"})
    public List<JdbcTemplate> archiveJdbcTemplates(
            @Qualifier("archiveDataSources") List<DataSource> archiveDataSources
    ) {
        return archiveDataSources.stream()
                .map(JdbcTemplate::new)
                .collect(Collectors.toList());
    }
}
