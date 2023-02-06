package ru.yandex.market.mcrm.db.test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.sql.DataSource;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.apache.commons.dbcp2.BasicDataSource;
import org.postgresql.xa.PGXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.support.ResourcePatternResolver;

import ru.yandex.embedded.postgresql.EmbeddedPostgresRunner;
import ru.yandex.embedded.postgresql.PostgresRunner;
import ru.yandex.embedded.postgresql.RecipePostgresRunner;
import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.mcrm.db.ChangelogProvider;
import ru.yandex.market.mcrm.db.Constants;
import ru.yandex.market.mcrm.db.CreatePartitionStrategy;
import ru.yandex.market.mcrm.db.DataSourceConfiguration;
import ru.yandex.market.mcrm.db.MasterReadOnlyDataSource;
import ru.yandex.market.mcrm.db.impl.CreatePartitionService;
import ru.yandex.market.mcrm.tx.TxConfiguration;
import ru.yandex.market.mcrm.tx.TxService;
import ru.yandex.market.mcrm.utils.TopologicalMixedSort;

/**
 * Используется в тестах mCRM
 */
@Configuration
@Import({
        DataSourceConfiguration.class,
        TxConfiguration.class
})
@PropertySource("classpath:test_db_support.properties")
public class TestMasterReadOnlyDataSourceConfiguration {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private static final Logger LOG = LoggerFactory.getLogger(TestMasterReadOnlyDataSourceConfiguration.class);

    private static final List<DataSource> dataSources = new ArrayList<>();

    static void closeAll() {
        LOG.info("TestSqlDataSourcesConfiguration::closeAll()");
        for (DataSource ds : dataSources) {
            try {
                if (ds instanceof BasicDataSource) {
                    ((BasicDataSource) ds).close();
                } else if (ds instanceof AtomikosDataSourceBean) {
                    ((AtomikosDataSourceBean) ds).close();
                }
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }
        }
        dataSources.clear();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public PostgresRunner postgres() {
        if (Boolean.parseBoolean(System.getenv("YA_TESTS"))) {
            return new RecipePostgresRunner();
        }
        return new EmbeddedPostgresRunner();
    }

    /**
     * Нужен для возможности инжектить лист бинов
     */
    @Bean
    public ChangelogProvider emptyChangelogProvider() {
        return () -> null;
    }

    @Bean(name = Constants.NESTED_DATA_SOURCE)
    public DataSource dataSource(
            @Named(Constants.MASTER_DATA_SOURCE) DataSource master,
            @Named(Constants.READ_ONLY_DATA_SOURCE) DataSource readOnly,
            TxService txService) {
        return new MasterReadOnlyDataSource(master, readOnly, txService);
    }

    @Bean(destroyMethod = "close")
    @Named(Constants.MASTER_DATA_SOURCE)
    public DataSource masterDataSource(List<ChangelogProvider> changelogs,
                                       @Named(Constants.LIQUIBASE_DATA_SOURCE) DataSource liquibaseDataSource,
                                       @Nullable List<CreatePartitionStrategy> strategies,
                                       ResourcePatternResolver resolver,
                                       PostgresRunner postgres,
                                       @Value("${sql.test.log.enabled:false}") boolean needSqlLog) {
        runCommonMigrations(liquibaseDataSource, resolver);
        runLiquibase(liquibaseDataSource, changelogs, resolver);
        DataSource dataSource = createManagedPool(postgres.getUrl(), needSqlLog);
        if (!CrmCollections.isEmpty(strategies)) {
            new CreatePartitionService(liquibaseDataSource, strategies).init();
        }
        return dataSource;
    }

    @Bean(destroyMethod = "close")
    @Named(Constants.READ_ONLY_DATA_SOURCE)
    public DataSource readOnlyDataSource(PostgresRunner postgres) {
        return createPool("readOnlyDataSource", postgres.getUrl(), 25,
                "SET default_transaction_read_only TO true;");
    }

    @Bean(destroyMethod = "close")
    @Named(Constants.LIQUIBASE_DATA_SOURCE)
    public DataSource liquibaseDataSource(PostgresRunner postgres) {
        return createPool("liquibaseDataSource", postgres.getUrl(), 2, "SELECT 1");
    }

    @Bean
    public DbTestTool dbTestTool(@Named(Constants.DEFAULT_DATA_SOURCE) DataSource dataSource,
                                 @Value("${sql.clear.script}") String[] clearScriptPaths) {
        if (clearScriptPaths.length == 1 && CrmStrings.isSpringPlaceHolder(clearScriptPaths[0])) {
            // Зарезолвить значение по умолчанию через аннотацию, почему-то, не получается
            // Конструкция "${sql.clear.script:/sql/clearDatabase.sql}" и её вариации
            // всегда возврещают "/sql/clearDatabase.sql"
            clearScriptPaths = new String[]{"/sql/clearDatabase.sql"};
        }

        return new DbTestTool(dataSource, clearScriptPaths);
    }

    private void runCommonMigrations(DataSource liquibaseDataSource, ResourcePatternResolver resolver) {
        ChangelogProvider changelogProvider = () -> "/sql/test_changelog.xml";
        runLiquibase(liquibaseDataSource, List.of(changelogProvider), resolver);
    }

    private void runLiquibase(DataSource liquibaseDataSource, List<ChangelogProvider> changelogs,
                              ResourcePatternResolver resolver) {
        for (ChangelogProvider provider : TopologicalMixedSort.sort(changelogs)) {
            String changelogPath = provider.changelog();
            if (!"${sql.changelog}".equals(changelogPath) && null != changelogPath) {
                if (!changelogPath.startsWith("classpath:")) {
                    changelogPath = "classpath:" + changelogPath;
                }
                if (resolver.getResource(changelogPath).exists()) {
                    LOG.info("Run liquibase. Changelog: {}", changelogPath);
                    runLiquibase(changelogPath, liquibaseDataSource);
                } else {
                    LOG.info("Resource not found: {}", changelogPath);
                }
            }
        }
    }

    private void runLiquibase(String changelogPath, DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            JdbcConnection jdbcConnection = new JdbcConnection(connection);
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);

            if (changelogPath.startsWith("classpath:")) {
                changelogPath = changelogPath.substring("classpath:".length());
            }
            if (changelogPath.startsWith("/")) {
                changelogPath = changelogPath.substring(1);
            }

            new Liquibase(changelogPath, new ClassLoaderResourceAccessor(), database).update("");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DataSource createManagedPool(String url, boolean needSqlLog) {
        LOG.info("CREATE_POOLED_DATASOURCE, NAME: {}, URL: {}, MIN: {}, MAX: {}", "masterDataSource", url, 25, 25);
        try {
            Properties properties = new Properties();
            properties.setProperty("url", url);
            properties.setProperty("readOnly", "false");

            AtomikosDataSourceBean dataSource = new AtomikosDataSourceBean();
            dataSource.setXaDataSourceClassName(PGXADataSource.class.getName());
            dataSource.setXaProperties(properties);
            dataSource.setMaxPoolSize(25);
            dataSource.setMinPoolSize(0);
            dataSource.setUniqueResourceName("masterDataSource" + "@" + COUNTER.incrementAndGet());
            dataSource.setTestQuery("SELECT 1;");

            if (needSqlLog) {
                var queryListener = new SLF4JQueryLoggingListener();
                queryListener.setLogLevel(SLF4JLogLevel.INFO);
                queryListener.setLogger(LoggerFactory.getLogger("PROXY_DATA_SOURCE_LOG"));
                DataSource proxyDataSource = ProxyDataSourceBuilder
                        .create(dataSource)
                        .name("masterProxyDataSource")
                        .listener(queryListener)
                        .build();
                dataSources.add(proxyDataSource);
                return proxyDataSource;
            } else {
                dataSources.add(dataSource);
                return dataSource;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BasicDataSource createPool(String name, String url, int poolSize, String testQuery) {
        LOG.info("CREATE_POOLED_DATASOURCE, URL: {}, MIN: {}, MAX: {}", url, poolSize, poolSize);
        try {
            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setJmxName(name);
            dataSource.setDriverClassName(org.postgresql.Driver.class.getName());
            dataSource.setUrl(url);
            dataSource.setMaxIdle(0);
            dataSource.setMaxTotal(poolSize);

            dataSource.setConnectionInitSqls(Collections.singletonList(testQuery));

            dataSources.add(dataSource);

            return dataSource;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
