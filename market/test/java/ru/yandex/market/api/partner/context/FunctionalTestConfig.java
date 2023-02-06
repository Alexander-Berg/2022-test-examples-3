package ru.yandex.market.api.partner.context;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.api.partner.MockClassBeanPostProcessor;
import ru.yandex.market.api.partner.client.orderservice.PapiOrderServiceClient;
import ru.yandex.market.api.partner.controllers.price.OfferPriceControllerTestConfig;
import ru.yandex.market.api.partner.controllers.stocks.OfferStockControllerTestConfig;
import ru.yandex.market.api.partner.sec.impl.PriceLabsFlags;
import ru.yandex.market.core.config.ShopsDataTestConfig;
import ru.yandex.market.core.config.TarifficatorClientFunctionalTestConfig;
import ru.yandex.market.core.database.EmbeddedPostgresConfig;
import ru.yandex.market.core.datacamp.DataCampCoreServicesConfig;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.delivery.AsyncTarifficatorService;
import ru.yandex.market.core.delivery.label.metrics.LabelGenerationProtoLBEvent;
import ru.yandex.market.core.environment.UnitedCatalogEnvironmentService;
import ru.yandex.market.core.fulfillment.tariff.TariffsService;
import ru.yandex.market.core.order.ServiceFeePartitionDao;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsYtStorage;
import ru.yandex.market.core.ping.Tvm2Checker;
import ru.yandex.market.core.replenishment.supplier.PilotSupplierYtDao;
import ru.yandex.market.core.solomon.SolomonTestJvmConfig;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeedMapper;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics4shops.client.api.ExcludeOrderFromShipmentApi;
import ru.yandex.market.logistics4shops.client.api.OrderBoxApi;
import ru.yandex.market.mbi.core.ff4shops.FF4ShopsOpenApiClient;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.SaasDatacampService;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.notification.telegram.bot.client.PartnerBotRestClient;
import ru.yandex.market.personal_market.PersonalMarketService;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

@ParametersAreNonnullByDefault
@Configuration
@Import({
        EmbeddedPostgresConfig.class,
        OfferPriceControllerTestConfig.class,
        SolomonTestJvmConfig.class,
        DataCampCoreServicesConfig.class,
        OfferStockControllerTestConfig.class,
        ShopsDataTestConfig.class,
        TarifficatorClientFunctionalTestConfig.class,
})
public class FunctionalTestConfig {
    private static final List<Class> classesToMock = List.of(Tvm2.class);
    private static final String ENV_SEND_BOXES_TO_L4S = "partner-api.send.boxes.to.l4s";

    @Bean
    public MockClassBeanPostProcessor mockClassBeanPostProcessor() {
        return new MockClassBeanPostProcessor(classesToMock);
    }

    @Bean
    @Primary
    public Tvm2 tvm2() {
        return mock(Tvm2.class);
    }

    @Bean
    public Tvm2 mbiTvm() {
        return mock(Tvm2.class);
    }

    @Bean
    public TariffsService clientTariffsService() {
        return mock(TariffsService.class);
    }

    @Bean
    public TariffsService impatientClientTariffsService() {
        return mock(TariffsService.class);
    }

    @Bean
    public Tvm2Checker tvm2Checker(List<Tvm2> tvms) {
        return new Tvm2Checker(tvms);
    }

    @Bean
    public String getGrpcServerName() {
        return InProcessServerBuilder.generateName();
    }

    @Bean
    public MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase() {
        return mock(MarketIdServiceGrpc.MarketIdServiceImplBase.class,
                delegatesTo(new MarketIdServiceGrpc.MarketIdServiceImplBase() {
                }));
    }

    @Bean
    public ManagedChannel managedChannel() {
        return InProcessChannelBuilder
                .forName(getGrpcServerName())
                .directExecutor()
                .build();
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public io.grpc.Server marketIdServer() {
        return InProcessServerBuilder
                .forName(getGrpcServerName())
                .directExecutor()
                .addService(marketIdServiceImplBase())
                .build();
    }

    @Bean
    public PilotSupplierYtDao pilotSupplierYtDao() {
        return mock(PilotSupplierYtDao.class);
    }

    @Bean
    public SalesDynamicsYtStorage salesDynamicsYtStorage() {
        return mock(SalesDynamicsYtStorage.class);
    }

    @Bean
    public WwClient wwClient() {
        return mock(WwClient.class);
    }

    @Bean
    public LogbrokerService samovarLogbrokerService() {
        return mock(LogbrokerService.class);
    }

    @Bean
    public LogbrokerService partnerApiLogsLogbrokerService() {
        return mock(LogbrokerService.class);
    }

    @Bean
    public ThreadPoolExecutor partnerApiLogsLogbrokerExecutor() {
        return mock(ThreadPoolExecutor.class);
    }

    @Bean
    public Supplier<Boolean> enabledHiddenOffersViaDatacampSupplier(
            UnitedCatalogEnvironmentService unitedCatalogEnvironmentService
    ) {
        return unitedCatalogEnvironmentService::isEnabledHiddenOffersViaDatacamp;
    }

    @Bean
    public PartnerBotRestClient partnerBotRestClient() {
        return mock(PartnerBotRestClient.class);
    }

    @Bean
    public DataCampClient dataCampShopClient() {
        return mock(DataCampClient.class);
    }

    @Bean
    public SaasService saasService() {
        return mock(SaasDatacampService.class);
    }

    @Bean
    public NesuClient nesuClient() {
        return mock(NesuClient.class);
    }

    @Bean
    public OrderBoxApi orderBoxApi() {
        return mock(OrderBoxApi.class);
    }

    @Bean
    public ExcludeOrderFromShipmentApi excludeOrderFromShipmentApi() {
        return mock(ExcludeOrderFromShipmentApi.class);
    }

    @Bean
    public Supplier<Integer> samovarInactivePeriodSupplier(EnvironmentService environmentService) {
        return () -> environmentService.getIntValue(SamovarFeedMapper.SAMOVAR_INACTIVE_FEED_PERIOD_ENV_KEY,
                SamovarFeedMapper.INACTIVE_DEFAULT_PERIOD_MINUTES);
    }

    @Bean
    public Supplier<Boolean> isCiForMappingEnabled(UnitedCatalogEnvironmentService unitedCatalogEnvironmentService) {
        return unitedCatalogEnvironmentService::isCiForMappingEnabled;
    }

    @Bean
    public PriceLabsFlags priceLabsFlags(EnvironmentService environmentService) {
        return new PriceLabsFlags(environmentService);
    }

    @Bean
    public AsyncTarifficatorService asyncTarifficatorService() {
        return mock(AsyncTarifficatorService.class);
    }

    @Bean
    public LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public Supplier<Integer> positionOfferRequestThresholdSupplier(
            UnitedCatalogEnvironmentService unitedCatalogEnvironmentService
    ) {
        return unitedCatalogEnvironmentService::getPositionOfferRequestThreshold;
    }

    @Bean
    public Supplier<Boolean> needPutBoxesToL4s(EnvironmentService environmentService) {
        return () -> environmentService.getBooleanValue(ENV_SEND_BOXES_TO_L4S, false);
    }

    @Bean
    public LogbrokerEventPublisher<LabelGenerationProtoLBEvent> logbrokerLabelGenerateEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public PapiOrderServiceClient papiOrderServiceClient() {
        return mock(PapiOrderServiceClient.class);
    }

    @Bean
    public FF4ShopsOpenApiClient ff4ShopsOpenApiClient() {
        return mock(FF4ShopsOpenApiClient.class);
    }

    @Bean
    public PersonalMarketService personalMarketService() {
        return mock(PersonalMarketService.class);
    }

    @Bean
    public ServiceFeePartitionDao serviceFeePartitionDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new ServiceFeePartitionDao(namedParameterJdbcTemplate);
    }
}
