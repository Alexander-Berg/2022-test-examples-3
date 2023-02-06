package ru.yandex.market.logistics.cs.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.logistics.cs.config.dbqueue.CapacityCounterQueueConfig;
import ru.yandex.market.logistics.cs.config.dbqueue.CounterNotificationsQueueConfiguration;
import ru.yandex.market.logistics.cs.config.dbqueue.DayOffByCapacityQueueConfiguration;
import ru.yandex.market.logistics.cs.config.dbqueue.DayOffByServiceQueueConfiguration;
import ru.yandex.market.logistics.cs.config.dbqueue.DayOffNotificationsQueueConfiguration;
import ru.yandex.market.logistics.cs.config.dbqueue.LomOrderEventQueueConfiguration;
import ru.yandex.market.logistics.cs.config.dbqueue.ServiceCounterBatchQueueConfig;
import ru.yandex.market.logistics.cs.config.dbqueue.TelegramNotificationsQueueConfiguration;
import ru.yandex.market.logistics.cs.dbqueue.dayoff.bycapacity.ServiceToDayByCapacityProducer;
import ru.yandex.market.logistics.cs.dbqueue.dayoff.byservice.DayOffByServiceProducer;
import ru.yandex.market.logistics.cs.dbqueue.logbroker.checkouter.LogbrokerCheckouterConsumptionProducer;
import ru.yandex.market.logistics.cs.dbqueue.notifications.telegram.TelegramNotificationsProducer;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceCounterBatchProducer;
import ru.yandex.market.logistics.cs.dbqueue.valuecounter.CapacityCounterRecalculationProducer;
import ru.yandex.market.logistics.cs.logbroker.checkouter.OrderEventConsumer;
import ru.yandex.market.logistics.cs.logbroker.lom.LomEventConsumer;
import ru.yandex.market.logistics.cs.monitoring.SolomonClient;
import ru.yandex.market.logistics.cs.repository.custom.LmsServiceForcedDayOffCustomRepositoryImpl;
import ru.yandex.market.logistics.cs.service.CapacityValueCounterService;
import ru.yandex.market.logistics.cs.service.EventService;
import ru.yandex.market.logistics.cs.service.ServiceDayOffService;
import ru.yandex.market.logistics.cs.service.TelegramNotificationService;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.base.CompoundDatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;
import ru.yandex.market.logistics.util.client.tvm.TvmSecurityConfiguration;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

@EnableAutoConfiguration
@Configuration
@EnableZonkyEmbeddedPostgres
@Import({
    JpaConfig.class,
    CompoundDatabaseCleanerConfig.class,
    LiquibaseConfig.class,
    DBQueueConfig.class,
    DatabaseConfig.class,
    CapacityCounterQueueConfig.class,
    ClockConfiguration.class,
    DayOffByServiceQueueConfiguration.class,
    DayOffByCapacityQueueConfiguration.class,
    TelegramNotificationsQueueConfiguration.class,
    DayOffNotificationsQueueConfiguration.class,
    CounterNotificationsQueueConfiguration.class,
    AccountingTaskLifecycleListenerConfig.class,
    ServiceCounterBatchQueueConfig.class,
    CacheConfiguration.class,
    FeaturePropertiesConfiguration.class,
    LomClientConfig.class,
    CheckouterClientMockConfig.class,
    DayOffNotificationsQueueConfiguration.class,
    CounterNotificationsQueueConfiguration.class,
    TvmSecurityConfiguration.class,
    LomOrderEventQueueConfiguration.class,
    LogbrokerMockConfig.class,
    LomEventConsumer.class,
})
@ComponentScan(basePackages = {
    "ru.yandex.market.logistics.cs.dbqueue",
    "ru.yandex.market.logistics.cs.domain.jdbc.mapper",
    "ru.yandex.market.logistics.cs.facade",
    "ru.yandex.market.logistics.cs.converter",
    "ru.yandex.market.logistics.cs.notifications",
    "ru.yandex.market.logistics.cs.service",
    "ru.yandex.market.logistics.cs.controller",
})
@SpyBean(classes = {
    CapacityCounterRecalculationProducer.class,
    ServiceToDayByCapacityProducer.class,
    DayOffByServiceProducer.class,
    ServiceCounterBatchProducer.class,
    ServiceDayOffService.class,
    CapacityValueCounterService.class,
    TelegramNotificationsProducer.class,
    LmsServiceForcedDayOffCustomRepositoryImpl.class,
    LogbrokerCheckouterConsumptionProducer.class,
    OrderEventConsumer.class,
    EventService.class,
})
@MockBean({
    TelegramNotificationService.class,
    HttpGeobase.class,
    LMSClient.class,
    TvmClientApi.class,
    SolomonClient.class,
})
@EnableWebMvc
public class IntegrationTestConfig {
}
