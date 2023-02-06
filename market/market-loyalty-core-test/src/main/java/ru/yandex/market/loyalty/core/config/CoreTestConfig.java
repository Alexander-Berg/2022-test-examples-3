package ru.yandex.market.loyalty.core.config;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import gumi.builders.UrlBuilder;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.HttpClient;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import uk.co.jemos.podam.api.AttributeMetadata;
import uk.co.jemos.podam.api.DataProviderStrategy;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;
import uk.co.jemos.podam.api.PodamUtils;
import uk.co.jemos.podam.typeManufacturers.AbstractTypeManufacturer;
import uk.co.jemos.podam.typeManufacturers.IntTypeManufacturerImpl;

import ru.yandex.market.antifraud.orders.client.MstatAntifraudOrdersLoyaltyClient;
import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.api.model.identity.Uuid;
import ru.yandex.market.loyalty.core.config.validation.DatabaseUseSqlValidator;
import ru.yandex.market.loyalty.core.dao.coin.CoinTestDataDao;
import ru.yandex.market.loyalty.core.dao.ydb.CashbackOrdersDao;
import ru.yandex.market.loyalty.core.mock.MarketLoyaltyCoreMockConfigurer;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderTerminationEvent;
import ru.yandex.market.loyalty.core.model.trigger.group.OrderTerminationEventsGroup;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredTransactionHooks;
import ru.yandex.market.loyalty.core.service.antifraud.AntifraudClient;
import ru.yandex.market.loyalty.core.service.antifraud.AntifraudClientImpl;
import ru.yandex.market.loyalty.core.service.laas.LaasClient;
import ru.yandex.market.loyalty.core.service.trigger.coordinator.OrderTerminationEventProcessingCoordinator;
import ru.yandex.market.loyalty.core.test.CacheLoaderState;
import ru.yandex.market.loyalty.core.utils.NoExternalCallsInTransactionChecker;
import ru.yandex.market.loyalty.core.utils.RulePayloads;
import ru.yandex.market.loyalty.db.config.EnvironmentOrEmbeddedPostgresDb;
import ru.yandex.market.loyalty.db.config.LiquibaseChangelog;
import ru.yandex.market.loyalty.monitoring.MonitorType;
import ru.yandex.market.loyalty.monitoring.PushMonitor;
import ru.yandex.market.loyalty.monitoring.juggler.JugglerInternalPushMonitor;
import ru.yandex.market.loyalty.monitoring.juggler.LoyaltyJugglerClient;
import ru.yandex.market.loyalty.monitoring.juggler.LoyaltyJugglerClientImpl;
import ru.yandex.market.loyalty.test.database.CompositeSQLValidator;
import ru.yandex.market.loyalty.test.database.DatabaseMetadata;
import ru.yandex.market.loyalty.test.database.IndexSQLValidator;
import ru.yandex.market.loyalty.test.database.ResultSetAccounter;
import ru.yandex.market.loyalty.test.database.SQLValidationConnection;
import ru.yandex.market.loyalty.test.database.SQLValidator;
import ru.yandex.market.request.trace.Module;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * Всё для тестов
 */
@Configurable
@Import({
        EnvironmentOrEmbeddedPostgresDb.class,      // поднимаем PG
        MarketLoyaltyCoreMockConfigurer.class,      // мокируем всё внешнее
        CoreConfigInternal.class,
})
@PropertySource("classpath:/test-application.properties")
public class CoreTestConfig {
    @Bean
    public CoinTestDataDao coinTestDataDao(JdbcTemplate jdbcTemplate, Clock clock) {
        return new CoinTestDataDao(jdbcTemplate, clock);
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new CustomSpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(LiquibaseChangelog.ONLY_SCHEMA.getChangelog());
        return liquibase;
    }

    @Bean
    public NoExternalCallsInTransactionChecker noExternalCallsInTransactionChecker() {
        try {
            Map<Method, Method> exclusions = Map.of(
                    TransactionTemplate.class.getMethod(
                            "execute", TransactionCallback.class),
                    JugglerInternalPushMonitor.class.getMethod("addTemporaryWarning", MonitorType.class, String.class,
                            long.class, TimeUnit.class),
                    OrderTerminationEventProcessingCoordinator.class.getMethod("processEvent",
                            OrderTerminationEvent.class, OrderTerminationEventsGroup.class, RulePayloads.class
                    ),
                    CashbackOrdersDao.class.getMethod(
                            "selectByUidWithBFCondition", Long.class, String.class, Instant.class
                    )
            );
            return new NoExternalCallsInTransactionChecker(invocationInTransaction -> {
                Method transactionMethod = invocationInTransaction.getTransactionMethod();
                Method mockedMethod = invocationInTransaction.getMockedMethod();
                return exclusions.containsKey(transactionMethod)
                        && exclusions.get(transactionMethod).equals(mockedMethod);
            });
        } catch (NoSuchMethodException e) {
            return new NoExternalCallsInTransactionChecker();
        }
    }

    @Bean
    public ComponentDescriptor componentDescriptor() {
        return new ComponentDescriptor(
                "localhost",
                80,
                Module.MARKET_LOYALTY
        );
    }

    @Bean
    public ConnectionInterceptorListener queryValidationSqlConnectionSupplier(
            SQLValidator sqlValidator, @Autowired ResultSetAccounter resultSetAccounter) {
        return c -> () -> new SQLValidationConnection(c.get(), sqlValidator, resultSetAccounter);
    }

    @Bean
    public ConnectionInterceptorListener readOnlySetConnectionSupplier() {
        return c -> () -> setReadOnlyConnectionIfNecessary(c);
    }

    private static Connection setReadOnlyConnectionIfNecessary(
            ConnectionInterceptorListener.ConnectionSupplier c
    ) throws Exception {
        final Connection connection = c.get();
        final Optional<DatasourceType> datasourceType = DatasourceType.get();
        if (datasourceType.isPresent()) {
            final DatasourceType type = datasourceType.get();
            if (type == DatasourceType.READ_WRITE) {
                connection.setReadOnly(false);
            } else {
                connection.setReadOnly(true);
            }
        }
        return connection;
    }

    @Bean
    public DatabaseMetadata databaseMetadata(JdbcTemplate jdbcTemplate) {
        return new DatabaseMetadata(jdbcTemplate);
    }

    @Bean
    public CompositeSQLValidator usedIndexesSQLValidator(@Lazy DatabaseMetadata databaseMetadata) {
        List<Pair<String, String>> excludedColumns = asList(
                Pair.of("coin_bunch_request", "status"),
                Pair.of("promo", "status"),
                Pair.of("promo", "platform"),
                Pair.of("discount", "platform"),
                Pair.of("action_hash", "promo_id"),
                Pair.of("promo_user_expectation", "expectation")
        );
        List<String> excludedTables = asList(// lower case
                "table_constraints", "key_column_usage", "columns", "promo_audit", "coupon_audit",
                "promo_trigger_audit", "trigger_restriction_audit", "account_audit", "promo_rule_audit_trigger",
                "trigger_action_audit", "bunch_generation_request_audit"
        );
        List<Pair<Pair<String, String>, List<Pair<String, String>>>> excludedColumnsPrecise = singletonList(
                IndexSQLValidator.excludeColumnPrecise("coupon", "status", asList(
                        Pair.of("promo", "status"),
                        Pair.of("promo", "end_date")
                ))
        );
        return new CompositeSQLValidator(
                new IndexSQLValidator(databaseMetadata, excludedColumns, excludedTables, excludedColumnsPrecise),
                new DatabaseUseSqlValidator()
        );
    }

    @Bean
    public PodamFactory podamFactory() {
        PodamFactory factory = new PodamFactoryImpl();
        factory.getStrategy().addOrReplaceTypeManufacturer(int.class, new CustomIntTypeManufacturerImpl());
        factory.getStrategy().addOrReplaceTypeManufacturer(Identity.class, new IdentityManufacturerImpl());
        return factory;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public DeferredTransactionHooks testDeferredTransactionForcePredicate() {
        return Mockito.spy(new TestDeferredTransactionHooks());
    }

    private static class CustomIntTypeManufacturerImpl extends IntTypeManufacturerImpl {
        @Override
        public Integer getInteger(AttributeMetadata attributeMetadata) {
            if (attributeMetadata.getAttributeType() == Timestamp.class ||
                    attributeMetadata.getAttributeType() == Instant.class) {
                return PodamUtils.getIntegerInRange(0, 999_999_999);
            }
            return super.getInteger(attributeMetadata);
        }
    }

    @Bean
    public CacheLoaderState cacheLoaderState() {
        return new CacheLoaderState();
    }

    private static class IdentityManufacturerImpl extends AbstractTypeManufacturer<Identity> {
        @Override
        public Identity getType(
                DataProviderStrategy strategy,
                AttributeMetadata attributeMetadata,
                Map<String, Type> genericTypesArgumentsMap
        ) {
            return new Uuid(UUID.randomUUID().toString());
        }
    }

    private static class CustomSpringLiquibase extends SpringLiquibase {
        @Override
        public void afterPropertiesSet() throws LiquibaseException {
            DatasourceType.READ_WRITE.within(super::afterPropertiesSet);
        }
    }

    @Bean
    public LoyaltyJugglerClient loyaltyJugglerClient(
            @Value("${market.loyalty.juggler.url}") String jugglerUrl,
            @Value("${ENVIRONMENT:local}") String environment,
            @Juggler HttpClient httpClient
    ) {
        return new LoyaltyJugglerClientImpl(
                "loyalty-core-test-" + environment.toLowerCase(), jugglerUrl, httpClient);
    }

    @Bean
    @Qualifier("failAntiFraudClient")
    public AntifraudClient failAntiFraudClient(
            @Default PushMonitor monitor
    ) {
        return new AntifraudClientImpl(
                new MstatAntifraudOrdersLoyaltyClient(null, UrlBuilder.fromString("antifraudUrl")),
                monitor
        );
    }

    @Bean
    @Qualifier("laasApiClient")
    public LaasClient laasApiClient(
            @Qualifier("laasRestTemplateIntegration") RestTemplate laasRestTemplate,
            @Value("${market.loyalty.laas.url}") String host
    ) {
        return new LaasClient(laasRestTemplate, host);
    }

    @Bean
    public ResultSetAccounter resultSetAccounter() {
        return new ResultSetAccounter();
    }
}
