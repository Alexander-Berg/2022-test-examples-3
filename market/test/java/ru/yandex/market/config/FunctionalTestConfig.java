package ru.yandex.market.config;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.bolts.collection.Option;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.common.util.region.ExtendedRegionTreePlainTextBuilder;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionTreeBuilder;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.billing.categories.db.DbCategoryDao;
import ru.yandex.market.billing.categories.db.DbSupplierCategoryFeeService;
import ru.yandex.market.billing.checkout.EventProcessorSupportFactory;
import ru.yandex.market.billing.config.OldFirstPartySuppliersIds;
import ru.yandex.market.billing.fulfillment.orders.CrossRegionalDeliveryHelper;
import ru.yandex.market.billing.fulfillment.orders.DbOrderBilledAmountDao;
import ru.yandex.market.billing.fulfillment.orders.FulfillmentOrderBillingService;
import ru.yandex.market.billing.fulfillment.promo.SupplierPromoTariffDao;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsService;
import ru.yandex.market.billing.imports.deliveryservice.DeliveryServiceTypeCache;
import ru.yandex.market.billing.imports.deliveryservice.DeliveryServicesDao;
import ru.yandex.market.billing.service.environment.CompareAndUpdateEnvironmentService;
import ru.yandex.market.billing.service.environment.EnvironmentAwareDateValidationService;
import ru.yandex.market.billing.service.environment.EnvironmentAwareDatesProcessingService;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.billing.sorting.SortingDailyTariffDao;
import ru.yandex.market.billing.sorting.SortingOrdersTariffDao;
import ru.yandex.market.checkout.checkouter.jackson.ObjectMapperTimeZoneSetter;
import ru.yandex.market.core.delivery.DeliveryEventTypeService;
import ru.yandex.market.core.order.DbOrderDao;
import ru.yandex.market.core.order.OrderBilledAmountsDao;
import ru.yandex.market.core.order.OrderTrantimeDao;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.logbroker.db.LogbrokerMonitorExceptionsService;
import ru.yandex.market.logbroker.db.LogbrokerMonitorExceptionsServiceStub;
import ru.yandex.market.logbroker.model.LogbrokerCluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Основной конфиг для тестов
 */
@Configuration
@SuppressWarnings("ParameterNumber")
@ImportResource("classpath:WEB-INF/checkouter-client.xml")
public class FunctionalTestConfig {

    @Autowired
    private DeliveryServicesDao deliveryServicesDao;

    @Autowired
    private DbOrderDao dbOrderDao;

    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private OldFirstPartySuppliersIds oldFirstPartySuppliersIds;

    @Autowired
    private CompareAndUpdateEnvironmentService compareAndUpdateEnvironmentService;

    @Autowired
    private OrderTrantimeDao orderTrantimeDao;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreUnresolvablePlaceholders(true);
        return configurer;
    }

    @Bean
    public TestableClock clock() {
        return new TestableClock();
    }

    @Bean
    public EnvironmentAwareDatesProcessingService environmentAwareDaysProcessorService() {
        return new EnvironmentAwareDatesProcessingService(
                clock(), environmentService, compareAndUpdateEnvironmentService
        );
    }

    @Bean(name = "tvm2")
    public Tvm2 tvm2() {
        var tvm2 = Mockito.mock(Tvm2.class);
        when(tvm2.getServiceTicket(any(Integer.class))).thenReturn(Option.of("test-mdm-hidden-offer-service-ticket"));
        return tvm2;
    }

    @Bean
    public Clock logbrokerOrderEventsClock() {
        return Clock.fixed(DateTimes.toInstantAtDefaultTz(2020, 11, 17, 0, 0, 0), ZoneId.systemDefault());
    }

    @Bean
    public ObjectMapperTimeZoneSetter checkouterAnnotationObjectMapperTimeZoneSetter(
            @Qualifier("checkouterAnnotationObjectMapper") ObjectMapper checkouterAnnotationObjectMapper
    ) {
        var timeZoneSetter = new ObjectMapperTimeZoneSetter();
        timeZoneSetter.setObjectMapper(checkouterAnnotationObjectMapper);
        timeZoneSetter.setTimeZone(TimeZone.getDefault());
        timeZoneSetter.afterPropertiesSet();
        return timeZoneSetter;
    }

    @Bean
    DeliveryServiceTypeCache deliveryServiceTypeCache() {
        return new DeliveryServiceTypeCache(deliveryServicesDao);
    }

    @Bean
    DeliveryEventTypeService deliveryEventTypeService() {
        return new DeliveryEventTypeService(dbOrderDao, deliveryServiceTypeCache(), orderTrantimeDao);
    }

    @Bean
    public RetryTemplate getOrdersRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(new NoBackOffPolicy());
        RetryPolicy retryPolicy = new SimpleRetryPolicy(1);
        retryTemplate.setRetryPolicy(retryPolicy);
        return retryTemplate;
    }

    @Bean
    public EventProcessorSupportFactory eventProcessorSupportFactory() {
        return new EventProcessorSupportFactory();
    }

    @Bean
    @Primary
    public TariffsService clientTariffsService() {
        return Mockito.mock(TariffsService.class);
    }

    @Bean
    public LogbrokerCluster lbkxCluster() {
        return mock(LogbrokerCluster.class);
    }

    @Bean
    public LogbrokerMonitorExceptionsService logbrokerMonitorExceptionsService() {
        return new LogbrokerMonitorExceptionsServiceStub();
    }

    @Bean
    public FulfillmentOrderBillingService testFulfillmentOrderBillingService(
            DbCategoryDao dbCategoryDao,
            OrderBilledAmountsDao orderBilledAmountsDao,
            OrderTrantimeDao orderTrantimeDao,
            DbOrderBilledAmountDao dbOrderBilledAmountDao,
            DbOrderDao dbOrderDao,
            TransactionTemplate pgTransactionTemplate,
            EnvironmentAwareDateValidationService environmentAwareDateValidationService,
            SortingOrdersTariffDao sortingOrdersTariffDao,
            SortingDailyTariffDao sortingDailyTariffDao,
            EnvironmentService environmentService,
            SupplierPromoTariffDao supplierPromoTariffDao,
            CrossRegionalDeliveryHelper crossRegionalDeliveryHelper,
            OldFirstPartySuppliersIds oldFirstPartySuppliersIds
    ) {
        return new FulfillmentOrderBillingService(
                dbCategoryDao,
                testDbSupplierCategoryFeeDao(),
                orderBilledAmountsDao,
                orderTrantimeDao,
                dbOrderBilledAmountDao,
                dbOrderDao,
                pgTransactionTemplate,
                environmentAwareDateValidationService,
                sortingOrdersTariffDao,
                sortingDailyTariffDao,
                environmentService,
                clientTariffsService(),
                supplierPromoTariffDao,
                crossRegionalDeliveryHelper,
                oldFirstPartySuppliersIds);
    }

    @Bean
    public DbSupplierCategoryFeeService testDbSupplierCategoryFeeDao() {
        return Mockito.mock(DbSupplierCategoryFeeService.class);
    }

    @Bean
    public RegionTreeBuilder<Region> regionTreeBuilder() {
        return mock(ExtendedRegionTreePlainTextBuilder.class);
    }

    @Bean
    public PartnerNotificationClient partnerNotificationClientMock() {
        return mock(PartnerNotificationClient.class);
    }
}
