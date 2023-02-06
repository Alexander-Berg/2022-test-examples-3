package ru.yandex.market;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.schema.Action;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.jdbc.H2SqlTransformer;
import ru.yandex.market.common.test.spring.DbUnitTestConfig;
import ru.yandex.market.common.test.transformer.PatternStringTransformer;
import ru.yandex.market.common.test.transformer.StringTransformer;

import static java.util.function.Predicate.not;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

@Deprecated(forRemoval = true, since = "use VendorDbUnitTestConfigH2AndPg")
@Configuration
public class VendorDbUnitTestConfigH2 extends DbUnitTestConfig {
    private final ResourceLoader resourceLoader;
    private final Resource resourceFile;

    public VendorDbUnitTestConfigH2(
            ResourceLoader resourceLoader,
            @Value("classpath:unittest/script.list") Resource resourceFile
    ) {
        this.resourceLoader = resourceLoader;
        this.resourceFile = resourceFile;
    }

    @Bean(name = {
            "oracleDataSource",
            "vendorDataSource",
            "csBillingDataSource",
            "mbiStatsDataSource",
            "tmsDataSource",
    })
    public DataSource oracleDataSource(@Qualifier("dataSource") DataSource dataSource) {
        return dataSource;
//        return ProxyDataSourceBuilder
//                .create(dataSource)
//                .name("DATASOURCE_PROXY")
//                .listener(new SystemOutQueryLoggingListener())
//                .logQueryByCommons(CommonsLogLevel.TRACE)
//                .build();
    }

    @Override
    @Bean(name = {
            "vendorTransactionTemplate",
            "csBillingTransactionTemplate",
            "mbiStatsTransactionTemplate",
            "tmsTransactionTemplate",
    })
    public TransactionTemplate transactionTemplate(PlatformTransactionManager txManager) {
        return super.transactionTemplate(txManager);
    }

    @Override
    @Bean(name = {
            "csBillingTransactionManager",
            "mstApiClientTransactionManager",
            "tmsTransactionManager",
    })
    public PlatformTransactionManager txManager(DataSource h2DataSource) {
        return super.txManager(h2DataSource);
    }

    @Override
    @Bean(name = {
            "vendorJdbcTemplate",
            "csBillingJdbcTemplate",
            "mbiStatsJdbcTemplate",
            "tmsJdbcTemplate",
    })
    public JdbcTemplate jdbcTemplate(DataSource h2DataSource) {
        return super.jdbcTemplate(h2DataSource);
    }

    @Bean(name = {
            "vendorNamedParameterJdbcTemplate",
            "csBillingNamedParameterJdbcTemplate",
            "mbiStatsNamedParameterJdbcTemplate",
            "tmsNamedParameterJdbcTemplate",
            // non-db
            "clickhouseNamedJdbcTemplate",
            "marketVendorsClickHouseNamedJdbcTemplate",
            "ytNamedParameterJdbcTemplate",
    })
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate vendorJdbcTemplate) {
        return super.namedParameterJdbcTemplate(vendorJdbcTemplate);
    }

    @Bean
    public Properties hibernateProperties() {
        Properties settings = new Properties();
        settings.put(Environment.DIALECT, H2Dialect.class);
        settings.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        settings.put(Environment.HBM2DDL_AUTO, Action.VALIDATE);
        settings.put(Environment.ORDER_INSERTS, "true");
        settings.put(Environment.ORDER_UPDATES, "true");
        settings.put(Environment.BATCH_VERSIONED_DATA, "true");
        settings.put(Environment.STATEMENT_BATCH_SIZE, "50");
        settings.put(Environment.SHOW_SQL, "true");
        return settings;
    }

    @Nonnull
    @Override
    protected EmbeddedDatabaseType databaseType() {
        return H2;
    }

    @Nonnull
    @Override
    protected StringTransformer createSqlTransformer() {
        return new H2SqlTransformer() {
            protected void customizeTransformers(List<StringTransformer> transformers) {
                super.customizeTransformers(transformers);

                transformers.add(new PatternStringTransformer("last_day\\(", "my_last_day("));
                transformers.add(new PatternStringTransformer("nocache", ""));
            }
        };
    }

    @Nonnull
    @Override
    protected List<Resource> databaseResources() throws Exception {
        try (final var reader = new BufferedReader(new InputStreamReader(resourceFile.getInputStream()))) {
            return reader.lines()
                    .filter(not(String::isBlank))
                    .filter(not(line -> line.trim().matches("/{2,}.*")))
                    .map(filePath -> String.format("classpath:%s", filePath))
                    .map(resourceLoader::getResource)
                    .collect(Collectors.toList());
        }
    }
}
