package ru.yandex.market.checkout.checkouter.degradation;

import java.util.concurrent.ExecutorService;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.market.antifraud.orders.client.MstatAntifraudCrmClient;
import ru.yandex.market.antifraud.orders.client.MstatAntifraudOrdersCheckouterClient;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.config.degradation.ManagedDegradationAspectConfiguration;
import ru.yandex.market.checkout.checkouter.degradation.client.DegradableAntifraudCrmClientDecorator;
import ru.yandex.market.checkout.checkouter.degradation.client.DegradableAntifraudOrdersClientDecorator;
import ru.yandex.market.checkout.checkouter.degradation.client.DegradableGeocoderClientDecorator;
import ru.yandex.market.checkout.checkouter.degradation.client.DegradableLoyaltyClientDecorator;
import ru.yandex.market.checkout.checkouter.degradation.client.DegradableStockStorageOrderClientDecorator;
import ru.yandex.market.checkout.checkouter.degradation.client.DegradableStockStoragePreOrderClientDecorator;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.checkouter.pay.TrustRequestConverter;
import ru.yandex.market.checkout.checkouter.service.TrustPayDegradationService;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderClient;
import ru.yandex.market.fulfillment.stockstorage.client.StockStoragePreOrderClient;
import ru.yandex.market.fulfillment.stockstorage.client.service.RequestEntityService;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.monitoring.thread.pool.InstrumentedExecutors;
import ru.yandex.market.queuedcalls.QueuedCallService;
import ru.yandex.market.request.context.IContext;
import ru.yandex.market.request.context.impl.MarketRequestContextFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.degradation.DegradationStage.CHECKOUT;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"test", "degradation"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ContextConfiguration(classes = {
        ManagedDegradationAspectConfiguration.class,
        AbstractDegradationTest.DegradationConfiguration.class
})

@TestPropertySource(properties = "market.checkout.managed-degradation.enable=true")
public abstract class AbstractDegradationTest {

    @Autowired
    private MarketRequestContextFactory marketRequestContextFactory;

    protected static final Logger log = (Logger) LoggerFactory.getLogger(Loggers.KEY_VALUE_LOG);
    protected final InMemoryAppender appender = new InMemoryAppender();
    private Level oldLevel;

    protected void assertOnErrorLog(String callName) {
        assertLog(callName, DegradationReason.FAILURE);
    }

    protected void assertOnTimeoutLog(String callName) {
        assertLog(callName, DegradationReason.TIMEOUT);
    }

    private void assertLog(String callName, DegradationReason reason) {
        assertNotNull(appender.getRaw().stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains(
                        reason.getReasonKey() + "\t" + CHECKOUT.getSubkey() + "_" + callName + "\t1.0"
                ))
                .findFirst()
                .orElse(null));
    }

    @BeforeEach
    void initContext() {
        IContext context = mock(IContext.class);
        when(marketRequestContextFactory.save()).thenReturn(context);
        doNothing().when(context).restore();
        oldLevel = log.getLevel();
        log.setLevel(Level.INFO);
    }

    @AfterEach
    void closeContext() {
        log.setLevel(oldLevel);
    }

    @TestConfiguration
    @Profile("degradation")
    protected static class DegradationConfiguration {

        @Bean
        public MarketRequestContextFactory marketRequestContextFactory() {
            return mock(MarketRequestContextFactory.class);
        }

        @Bean
        public TrustPayDegradationService trustPayDegradationServiceMock() {
            return mock(TrustPayDegradationService.class);
        }

        @Bean
        public MarketLoyaltyClient marketLoyaltyClient(RestTemplate loyaltyRestTemplate) {
            return new DegradableLoyaltyClientDecorator(loyaltyRestTemplate, new ObjectMapper(), "http://localhost");
        }

        @Bean
        public StockStorageOrderClient stockStorageOrderClient(RestTemplate stockStorageRestTemplate) {
            return new DegradableStockStorageOrderClientDecorator(
                    stockStorageRestTemplate,
                    "",
                    mock(RequestEntityService.class));
        }

        @Bean
        public MstatAntifraudCrmClient mstatAntifraudCrmClient(RestTemplate antifraudRestTemplate) {
            return new DegradableAntifraudCrmClientDecorator(antifraudRestTemplate, "");
        }

        @Bean
        public MstatAntifraudOrdersCheckouterClient mstatAntifraudOrdersCheckouterClient(
                RestTemplate antifraudRestTemplate
        ) {
            return new DegradableAntifraudOrdersClientDecorator(antifraudRestTemplate, "");
        }

        @Bean
        public StockStoragePreOrderClient stockStoragePreOrderClient(RestTemplate stockStorageRestTemplate) {
            return new DegradableStockStoragePreOrderClientDecorator(
                    stockStorageRestTemplate,
                    "",
                    mock(RequestEntityService.class));
        }

        @Bean
        public RestTemplate loyaltyRestTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean
        public RestTemplate stockStorageRestTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean
        public RestTemplate antifraudRestTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean
        public RestTemplate yaLavkaRestTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean
        public RestTemplate yaLavkaCheckRestTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean
        public RetryTemplate loyaltyRetryTemplate() {
            RetryPolicy loyaltyRetryPolicy = new SimpleRetryPolicy(1) {
                @Override
                public boolean canRetry(RetryContext context) {
                    if (context.getLastThrowable() instanceof MarketLoyaltyException) {
                        MarketLoyaltyException exception = (MarketLoyaltyException) context.getLastThrowable();
                        return super.canRetry(context)
                                && exception.getMarketLoyaltyErrorCode() == MarketLoyaltyErrorCode.OTHER_ERROR;
                    }
                    return super.canRetry(context);
                }
            };

            final RetryTemplate retryTemplate = new RetryTemplate();
            retryTemplate.setRetryPolicy(loyaltyRetryPolicy);

            return retryTemplate;
        }

        @Bean
        public TrustRequestConverter trustRequestConverter() {
            return mock(TrustRequestConverter.class);
        }

        @Bean
        public QueuedCallService queuedCallService() {
            return mock(QueuedCallService.class);
        }

        @Bean
        public TransactionTemplate transactionTemplate() {
            return mock(TransactionTemplate.class);
        }

        @Bean
        public CheckouterProperties checkouterProperties() {
            return mock(CheckouterProperties.class);
        }

        @Bean
        public CheckouterFeatureReader checkouterFeatureReader() {
            return mock(CheckouterFeatureReader.class);
        }

        @Bean
        public GeoClient degradableGeocoderClientDecorator() {
            return DegradableGeocoderClientDecorator.decorate(mock(GeoClient.class));
        }

        @Bean
        public ExecutorService requestsExecutor(DegradationContextFactory degradationContextFactory) {
            return InstrumentedExecutors.contextCachedThreadPoolBuilder("RequestsExecutor")
                    .setDaemon(false)
                    .setContextFactory(degradationContextFactory)
                    .build();
        }
    }
}
