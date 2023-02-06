package ru.yandex.cs.billing.tms.config;

import java.time.Clock;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.common.cache.memcached.client.MemCachedClientFactory;
import ru.yandex.common.cache.memcached.client.MemCachedClusterFactory;
import ru.yandex.common.cache.memcached.client.spy.SpyMemCachedClientFactory;
import ru.yandex.common.cache.memcached.impl.TransactionalMemCachedAgent;
import ru.yandex.common.util.terminal.CommandExecutor;
import ru.yandex.cs.billing.balance.BalanceService;
import ru.yandex.cs.billing.balance.DefaultBalanceService;
import ru.yandex.cs.billing.billing.BillingServiceSql;
import ru.yandex.cs.billing.history.impl.HistoryServiceSql;
import ru.yandex.cs.billing.invoice.overdraft.dao.OverdraftInvoiceYtDao;
import ru.yandex.market.common.test.mockito.MemCachedServiceMock;
import ru.yandex.market.mbi.msapi.clicks.BindersConfig;
import ru.yandex.market.mbi.msapi.clicks.RowLogbrokerLoaderConfig;
import ru.yandex.market.mbi.msapi.tvm.TvmSettings;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.mock;

@ImportResource({
        "classpath:core-services.xml"
})
@Import({
        BindersConfig.class,
        PropertySourcesConfig.class,
        ExecutorsConfig.class,
        RowLogbrokerLoaderConfig.class,
        CommonConfig.class,
        CsBillingTmsDatasourceExternalFunctionalTestConfig.class,
        PersPayConfig.class,
        LbkxLogbrokerExternalTestConfig.class
})
@Configuration
public class CsBillingTmsExternalFunctionalTestConfig {

    @Bean
    public BillingServiceSql billingServiceSql(NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate,
                                               SimpleJdbcCall updateActualBalanceJdbcCall,
                                               SimpleJdbcCall getAvgSpending,
                                               Clock clock) {
        return Mockito.spy(new BillingServiceSql(csBillingNamedParameterJdbcTemplate, updateActualBalanceJdbcCall, getAvgSpending, clock));
    }

    @Bean
    public HistoryServiceSql historySql(JdbcTemplate csBillingJdbcTemplate) {
        return Mockito.spy(new HistoryServiceSql(csBillingJdbcTemplate));
    }

    @Bean
    public SpringLiquibase springLiquibase(DataSource liquibaseDataSource,
                                           @Value("${liquibase.cs_billing.docker.changelog}") String dockerChangelog) {
        final SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(liquibaseDataSource);
        liquibase.setShouldRun(true);
        liquibase.setChangeLog(dockerChangelog);
        return liquibase;
    }

    @Bean(name = {"balanceService", "cachedBalanceService"})
    public BalanceService balanceService() {
        return Mockito.mock(DefaultBalanceService.class);
    }

    @Bean
    public CommandExecutor commandExecutor() {
        return Mockito.mock(CommandExecutor.class);
    }

    @Bean
    public MemCachedClientFactory memCachedClientFactory() {
        return mock(SpyMemCachedClientFactory.class);
    }

    @Bean
    public MemCachedClusterFactory memCachedClusterFactory() {
        return mock(MemCachedClusterFactory.class);
    }

    @Bean
    public MemCachingService memCachingService() {
        return new MemCachedServiceMock();
    }

    @Bean(name = {"memCachedAgent", "transactionalMemCachedAgent"})
    public MemCachedAgent memCachedAgent() {
        return mock(TransactionalMemCachedAgent.class);
    }

    @Bean
    public Clock clock() {
        return Mockito.mock(Clock.class);
    }

    @Bean
    public TvmClient mstApiTvmClient() {
        return mock(TvmClient.class);
    }

    @Bean
    public TvmSettings mstApiTvmConfiguration() {
        return mock(TvmSettings.class);
    }

    @Bean
    public OverdraftInvoiceYtDao overdraftInvoiceYtDao() {
        return mock(OverdraftInvoiceYtDao.class);
    }
}
