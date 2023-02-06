package ru.yandex.market.stat.dicts.config.integration_conf;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.health.HealthCheckRegistry;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.curator.framework.CuratorFramework;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.stat.dicts.config.AnaplanConfig;
import ru.yandex.market.stat.dicts.config.AnaplanLoadTasksConfig;
import ru.yandex.market.stat.dicts.config.JdbcConfig;
import ru.yandex.market.stat.dicts.config.ParsersDictsConfig;
import ru.yandex.market.stat.dicts.config.SupportConfig;
import ru.yandex.market.stat.dicts.config.loaders.DictionariesHolder;
import ru.yandex.market.stat.dicts.loaders.anaplan.AnaplanApiV2;
import ru.yandex.market.stat.dicts.loaders.anaplan.AnaplanTokenProvider;
import ru.yandex.market.stat.dicts.loaders.tvm.TvmTicketSupplier;
import ru.yandex.market.stat.dicts.services.DictionaryYtService;
import ru.yandex.market.stat.yt.YtClusterProvider;

import javax.sql.DataSource;

/**
 * @author Kate Lebedeva <kateleb@yandex-team.ru>
 */
@Configuration
@Profile({"integration-tests"})
@Import({
        PropertiesDictionariesITestConfig.class,
        ParsersDictsConfig.class,
        AnaplanConfig.class,
        AnaplanLoadTasksConfig.class,
        SupportConfig.class,
        JdbcConfig.class
})
@ComponentScan(value = {"ru.yandex.market.stat.dicts.services"})
public class DictionariesIntTestConfig {

    @Bean
    public YtClusterProvider ytClusterProvider() {
        return Mockito.mock(YtClusterProvider.class);
    }

    @Bean
    public DictionaryYtService ytService() {
        return Mockito.mock(DictionaryYtService.class);
    }

    @Bean
    public AnaplanApiV2 anaplanApi() {
        return Mockito.mock(AnaplanApiV2.class);
    }

    @Bean
    public AnaplanTokenProvider tokenProvider() {
        return Mockito.mock(AnaplanTokenProvider.class);
    }

    @Bean
    public DictionariesHolder parseService() {
        return Mockito.mock(DictionariesHolder.class);
    }

    @Bean
    public DataSource metadataDataSource() {
        return Mockito.mock(DataSource.class);
    }

    @Bean
    public NamedParameterJdbcTemplate metadataTemplate() {
        return Mockito.mock(NamedParameterJdbcTemplate.class);
    }

    @Bean(autowire = Autowire.BY_NAME)
    public DataSourceTransactionManager metadataTransactionManager() {
        return Mockito.mock(DataSourceTransactionManager.class);
    }

    @Bean
    public TransactionTemplate metadataTransactionTemplate() {
        return Mockito.mock(TransactionTemplate.class);
    }

    @Bean
    public SpringLiquibase liquibase() {
        return Mockito.mock(SpringLiquibase.class);
    }

    @Bean
    public CuratorFramework curator() {
        return Mockito.mock(CuratorFramework.class);
    }

    @Bean
    public JdbcTemplate clickHouseUploadJdbcTemplate() {
        return Mockito.mock(JdbcTemplate.class);
    }

    @Bean
    public DataSource clickHouseUploadDataSource() {
        return Mockito.mock(DataSource.class);
    }

    @Bean
    public TvmTicketSupplier tvmTicketSupplier() {
        return Mockito.mock(TvmTicketSupplier.class);
    }

    @Bean
    public MetricRegistry metricRegistry() {
        return Mockito.mock(MetricRegistry.class);
    }

    @Bean
    public HealthCheckRegistry healthCheckRegistry() {
        return Mockito.mock(HealthCheckRegistry.class);
    }

    @Bean
    public GraphiteReporter graphiteReporter() {
        return Mockito.mock(GraphiteReporter.class);
    }
}
