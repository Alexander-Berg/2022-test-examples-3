package ru.yandex.market;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import net.ttddyy.dsproxy.listener.logging.CommonsLogLevel;
import net.ttddyy.dsproxy.listener.logging.SystemOutQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.tool.schema.Action;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.jdbc.H2SqlTransformer;
import ru.yandex.market.common.test.jdbc.InstrumentedDataSourceFactory;
import ru.yandex.market.common.test.transformer.PatternStringTransformer;
import ru.yandex.market.common.test.transformer.StringTransformer;
import ru.yandex.market.core.database.EmbeddedPostgresConfig;

import static java.util.function.Predicate.not;

@Configuration
@Import(EmbeddedPostgresConfig.class)
public class VendorDbUnitTestConfigH2AndPg {
    @Autowired
    private ResourceLoader resourceLoader;
    @Value("classpath:unittest/script.cs_billing.list")
    private Resource resourceFile;

    // pg part

    @Bean(name = {
            "pgDataSource",
            "vendorDataSource",
    })
    public DataSource pgDataSource(
            @Qualifier("dataSource") DataSource dataSource
    ) {
        return dataSource;
    }

    @Primary // see vendorTransactionManager definition in vendor-datasource.xml
    @Bean(name = {
            "pgTxManager",
            "vendorTransactionManager",
    })
    public PlatformTransactionManager pgTxManager(
            @Qualifier("pgDataSource") DataSource dataSource
    ) {
        return EmbeddedPostgresConfig.txManager(dataSource);
    }

    @Bean(name = {
            "pgTransactionTemplate",
            "vendorTransactionTemplate",
    })
    public TransactionTemplate pgTransactionTemplate(
            @Qualifier("pgTxManager") PlatformTransactionManager txManager
    ) {
        return new TransactionTemplate(txManager);
    }

    @Bean(name = {
            "pgJdbcTemplate",
            "vendorJdbcTemplate",
    })
    public JdbcTemplate pgJdbcTemplate(
            @Qualifier("pgDataSource") DataSource dataSource
    ) {
        return EmbeddedPostgresConfig.jdbcTemplate(dataSource);
    }

    @Bean(name = {
            "pgNamedParameterJdbcTemplate",
            "vendorNamedParameterJdbcTemplate",
    })
    public NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate(
            @Qualifier("pgJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Bean
    public Properties hibernateProperties() {
        Properties settings = new Properties();
        settings.put(Environment.DIALECT, PostgreSQL10Dialect.class);
        settings.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        settings.put(Environment.HBM2DDL_AUTO, Action.VALIDATE);
        settings.put(Environment.ORDER_INSERTS, "true");
        settings.put(Environment.ORDER_UPDATES, "true");
        settings.put(Environment.BATCH_VERSIONED_DATA, "true");
        settings.put(Environment.STATEMENT_BATCH_SIZE, "100");
        settings.put(Environment.SHOW_SQL, "true");
        return settings;
    }

    // h2/oracle part

    @Bean
    public EmbeddedDatabaseFactoryBean oracleDataSourceFactory() throws Exception {
        var factoryBean = new EmbeddedDatabaseFactoryBean();
        factoryBean.setDataSourceFactory(new InstrumentedDataSourceFactory(getH2ToOracleTransformer()));
        factoryBean.setDatabaseName("testDataBase" + System.currentTimeMillis());
        factoryBean.setDatabasePopulator(new ResourceDatabasePopulator(getOracleResources()));
        factoryBean.setDatabaseType(EmbeddedDatabaseType.H2);
        return factoryBean;
    }

    private StringTransformer getH2ToOracleTransformer() {
        return new H2SqlTransformer() {
            protected void customizeTransformers(List<StringTransformer> transformers) {
                super.customizeTransformers(transformers);
                transformers.add(new PatternStringTransformer("last_day\\(", "my_last_day("));
                transformers.add(new PatternStringTransformer("nocache", ""));
            }
        };
    }

    private Resource[] getOracleResources() throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(resourceFile.getInputStream()))) {
            return reader.lines()
                    .filter(not(String::isBlank))
                    .filter(not(line -> line.trim().matches("/{2,}.*")))
                    .map(filePath -> String.format("classpath:%s", filePath))
                    .map(resourceLoader::getResource)
                    .toArray(Resource[]::new);
        }
    }

    @Bean(name = {
            "oracleDataSource",
            "csBillingDataSource",
            "mbiStatsDataSource",
            "tmsDataSource",
    })
    public DataSource oracleDataSource(
            @Qualifier("oracleDataSourceFactory") DataSource dataSource
    ) {
        return ProxyDataSourceBuilder
                .create(dataSource)
                .name("DATASOURCE_PROXY_ORACLE")
                .listener(new SystemOutQueryLoggingListener())
                .logQueryByCommons(CommonsLogLevel.TRACE)
                .build();
    }

    @Bean(name = {
            "oracleTxManager",
            "csBillingTransactionManager",
            "mstApiClientTransactionManager",
            "tmsTransactionManager",
    })
    public PlatformTransactionManager oracleTxManager(
            @Qualifier("oracleDataSource") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = {
            "oracleTransactionTemplate",
            "csBillingTransactionTemplate",
            "mbiStatsTransactionTemplate",
            "tmsTransactionTemplate",
    })
    public TransactionTemplate oracleTransactionTemplate(
            @Qualifier("oracleTxManager") PlatformTransactionManager txManager
    ) {
        return new TransactionTemplate(txManager);
    }

    @Bean(name = {
            "oracleJdbcTemplate",
            "csBillingJdbcTemplate",
            "mbiStatsJdbcTemplate",
            "tmsJdbcTemplate",
    })
    public JdbcTemplate oracleJdbcTemplate(
            @Qualifier("oracleDataSource") DataSource dataSource
    ) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = {
            "oracleNamedParameterJdbcTemplate",
            "csBillingNamedParameterJdbcTemplate",
            "mbiStatsNamedParameterJdbcTemplate",
            "tmsNamedParameterJdbcTemplate",
            // non-db
            "clickhouseNamedJdbcTemplate",
            "marketVendorsClickHouseNamedJdbcTemplate",
            "ytNamedParameterJdbcTemplate",
    })
    public NamedParameterJdbcTemplate oracleNamedParameterJdbcTemplate(
            @Qualifier("oracleJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }
}
