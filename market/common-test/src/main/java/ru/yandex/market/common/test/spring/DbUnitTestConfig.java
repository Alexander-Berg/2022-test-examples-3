package ru.yandex.market.common.test.spring;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.market.common.test.jdbc.InstrumentedDataSourceFactory;
import ru.yandex.market.common.test.mockito.MemCachedServiceMock;
import ru.yandex.market.common.test.transformer.StringTransformer;

/**
 * Базовый спринг конфиг для юнит-тестов базы данных.
 *
 * @author zoom
 */
@Configuration
@ParametersAreNonnullByDefault
public abstract class DbUnitTestConfig {

    @Bean(name = "dataSource")
    public EmbeddedDatabaseFactoryBean dataSourceFactory() throws Exception {
        final Resource[] resources = Stream.of(
                defaultDatabaseResources(),
                databaseResources(),
                extraDatabaseResources()
        )
                .flatMap(Collection::stream)
                .toArray(Resource[]::new);
        return createDataSourceFactory(resources);
    }

    @Bean
    public TransactionTemplate transactionTemplate(final PlatformTransactionManager txManager) {
        return new TransactionTemplate(txManager);
    }

    @Bean
    public PlatformTransactionManager txManager(final DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public MemCachingService memCachingService() {
        return new MemCachedServiceMock();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }


    @Nonnull
    protected EmbeddedDatabaseFactoryBean createDataSourceFactory(Resource... scripts) {
        final EmbeddedDatabaseFactoryBean factoryBean = new EmbeddedDatabaseFactoryBean();
        factoryBean.setDatabaseType(databaseType());
        factoryBean.setDataSourceFactory(new InstrumentedDataSourceFactory(createSqlTransformer()));
        factoryBean.setDatabaseName("testDataBase" + System.currentTimeMillis());
        factoryBean.setDatabasePopulator(new ResourceDatabasePopulator(scripts));
        return factoryBean;
    }

    /**
     * Тип базы, для которого задается конфиг.
     */
    @Nonnull
    protected abstract EmbeddedDatabaseType databaseType();

    /**
     * SQL-трансформер, который будет использоваться для запросов перед их исполненеим.
     */
    @Nonnull
    protected abstract StringTransformer createSqlTransformer();

    /**
     * Ресурсы, которые будут загружены в тестовую бд.
     */
    @Nonnull
    protected abstract List<Resource> databaseResources() throws Exception;

    /**
     * Возвращает дополнительным ресурсы, которые нужно накатить перед запуском теста.
     * Переопределите этот метод, чтобы он возвращал ресурсы, если вам не хватает ресурсов из
     * {@link #databaseResources()}, и вы не хотите добавлять новые ресурсы для всех
     */
    @Nonnull
    protected List<Resource> extraDatabaseResources() throws Exception {
        return Collections.emptyList();
    }

    /**
     * Ресурсы, которые будут накатываться всегда.
     * Можно использовать для того, чтобы у более частных конфигураций
     * (как {@link H2Config}, {@link HsqlConfig} и т.д.) можно было задать стандартные ресурсы.
     */
    @Nonnull
    protected List<Resource> defaultDatabaseResources() throws Exception {
        return Collections.emptyList();
    }
}
