package ru.yandex.market.delivery.mdbapp.configuration;

import java.util.Objects;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterDeliveryAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnApi;
import ru.yandex.market.checkout.checkouter.config.CheckouterAnnotationJsonConfig;
import ru.yandex.market.checkout.checkouter.config.CheckouterSerializationJsonConfig;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.jackson.CheckouterModule;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.delivery.export.client.DeliveryExportClient;
import ru.yandex.market.delivery.mdbapp.components.email.sender.MailSender;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.logbroker.MarketDataBus;
import ru.yandex.market.delivery.mdbapp.components.service.marketid.LegalInfoReceiver;
import ru.yandex.market.delivery.mdbapp.components.service.translate.TranslateService;
import ru.yandex.market.delivery.mdbapp.components.service.translate.TranslateServiceImpl;
import ru.yandex.market.delivery.mdbapp.components.service.translate.client.TranslateAPI;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.TranslateCacheRepository;
import ru.yandex.market.delivery.mdbapp.integration.gateway.LgwGateway;
import ru.yandex.market.delivery.mdbapp.integration.gateway.OrderEventsGateway;
import ru.yandex.market.delivery.mdbapp.integration.service.PersonalDataService;
import ru.yandex.market.delivery.mdbapp.json.DeliveryDatesMixIn;
import ru.yandex.market.delivery.mdbapp.json.DeliveryMixIn;
import ru.yandex.market.delivery.mdbapp.json.OfferItemMixIn;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.mdb.lrm.client.api.ReturnsApi;
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.repository.EnableRepositoryUtils;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.pvz.client.logistics.PvzLogisticsClient;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.sc.internal.client.ScIntClient;
import ru.yandex.market.tpl.internal.client.TplInternalClient;

@Configuration
@EnableZonkyEmbeddedPostgres
@EnableRepositoryUtils
@Import({
    CacheConfiguration.class,
    CacheProperties.class,
    CancellationResultProcessorConfiguration.class,
    CapacityConfig.class,
    DbUnitTestConfiguration.class,
    DeletedEntitiesProperties.class,
    FailoverConfiguration.class,
    FeatureProperties.class,
    LiquibaseConfiguration.class,
    LockerCodeProperties.class,
    RepositoryConfiguration.class,
    SenderConfiguration.class,
    StartrekConfiguration.class,
    YtProperties.class,
    CheckouterAnnotationJsonConfig.class,
    CheckouterSerializationJsonConfig.class,
})
@MockBean({
    HealthManager.class,
    CheckouterAPI.class,
    CheckouterDeliveryAPI.class,
    CheckouterReturnApi.class,
    DeliveryClient.class,
    DeliveryExportClient.class,
    FulfillmentClient.class,
    LMSClient.class,
    LegalInfoReceiver.class,
    LgwGateway.class,
    LomClient.class,
    ReturnsApi.class,
    MailSender.class,
    MarketDataBus.class,
    MbiApiClient.class,
    MqmClient.class,
    PechkinHttpClient.class,
    PvzLogisticsClient.class,
    RegionService.class,
    ScIntClient.class,
    TarifficatorClient.class,
    TplInternalClient.class,
    TranslateAPI.class,
    TvmClientApi.class,
    Yt.class,
    OrderEventsGateway.class,
    PersonalDataService.class,
})
@SpyBean({
    TestableClock.class,
})
@Profile("medium-integration-test")
public class IntegrationTestConfiguration {

    @Autowired
    private TestableClock clock;

    @Autowired
    private CacheManager cacheManager;

    @Bean
    @Primary
    public Yt primaryYt(Yt yt) {
        return yt;
    }

    @Bean
    @Qualifier("backup")
    public Yt backupYt(Yt yt) {
        return yt;
    }

    @Bean
    public TranslateService translateService(
        TranslateAPI translateAPI,
        TranslateCacheRepository translateRepository
    ) {
        return new TranslateServiceImpl(translateAPI, translateRepository);
    }

    @Bean
    @Primary
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    protected ObjectMapper commonJsonMapper() {
        return Jackson2ObjectMapperBuilder.json()
            .modulesToInstall(new CheckouterModule(), new JavaTimeModule())
            .simpleDateFormat("dd-MM-yyyy HH:mm:ss")
            .timeZone(TimeZone.getDefault())
            .mixIn(OfferItem.class, OfferItemMixIn.class)
            .mixIn(Delivery.class, DeliveryMixIn.class)
            .mixIn(DeliveryDates.class, DeliveryDatesMixIn.class)
            .featuresToDisable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_INVALID_SUBTYPE
            )
            .featuresToEnable(
                DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
                DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE
            )
            .filters(new SimpleFilterProvider().setFailOnUnknownId(false))
            .build()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
            .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
    }

    @AfterEach
    void tearDown() {
        cacheManager.getCacheNames().stream()
            .map(cacheManager::getCache)
            .filter(Objects::nonNull)
            .forEach(Cache::clear);
        RequestContextHolder.clearContext();
        clock.clearFixed();
    }
}
