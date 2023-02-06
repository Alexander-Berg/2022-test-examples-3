package ru.yandex.market.tpl.core.config;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.core.JmsTemplate;

import yandex.market.combinator.v0.CombinatorGrpc;
import ru.yandex.common.util.geocoder.GeoClient;
import ru.yandex.common.util.region.CustomRegionAttribute;
import ru.yandex.common.util.region.ExtendedRegionTreePlainTextBuilder;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.geobase.HttpGeobase;
import ru.yandex.geobase.beans.GeobaseRegionData;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.yard.client.YardClientApi;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.sc.internal.client.ScLogisticsClient;
import ru.yandex.market.tpl.common.covid.VaccinationValidator;
import ru.yandex.market.tpl.common.covid.external.TplCovidExternalService;
import ru.yandex.market.tpl.common.ds.client.DeliveryClient;
import ru.yandex.market.tpl.common.ds.client.TplLgwClient;
import ru.yandex.market.tpl.common.dsm.client.api.CourierApi;
import ru.yandex.market.tpl.common.dsm.client.api.EmployerApi;
import ru.yandex.market.tpl.common.logbroker.producer.LogbrokerProducerFactory;
import ru.yandex.market.tpl.common.lrm.client.api.ReturnsApi;
import ru.yandex.market.tpl.common.passport.client.PassportApiClient;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalFindApi;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalRetrieveApi;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalStoreApi;
import ru.yandex.market.tpl.common.sms.YaSmsClient;
import ru.yandex.market.tpl.common.taxi.driver.trackstory.client.api.DefaultTaxiDriverTrackStoryPositionApi;
import ru.yandex.market.tpl.common.taxi.driver.trackstory.client.api.DefaultTaxiDriverTrackStoryTrackApi;
import ru.yandex.market.tpl.common.taxi.vgw.talks.client.api.DefaultTaxiVGWTalksApi;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.api.DefaultTaxiVGWForwardingsApi;
import ru.yandex.market.tpl.common.transferact.client.api.DocumentApi;
import ru.yandex.market.tpl.common.transferact.client.api.SignatureApi;
import ru.yandex.market.tpl.common.transferact.client.api.TransferApi;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.common.web.tvm.DummyTvmClient;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.adapter.GlobalSettingsProviderAdapterImpl;
import ru.yandex.market.tpl.core.domain.order.address.AddressQueryService;
import ru.yandex.market.tpl.core.domain.order.validator.TplOrderRescheduleValidator;
import ru.yandex.market.tpl.core.domain.pickup.yt.repository.CheckTableYtRepository;
import ru.yandex.market.tpl.core.domain.pickup.yt.repository.YtPickupPointRepository;
import ru.yandex.market.tpl.core.domain.region.TplRegion;
import ru.yandex.market.tpl.core.domain.region.TplRegionService;
import ru.yandex.market.tpl.core.domain.region.actualization.TplRegionBorderGisDao;
import ru.yandex.market.tpl.core.domain.routing.schedule.RoutingRequestWaveService;
import ru.yandex.market.tpl.core.domain.tracker.TrackerService;
import ru.yandex.market.tpl.core.domain.usershift.location.DetailingEnum;
import ru.yandex.market.tpl.core.domain.usershift.location.precise.PreciseGeoPointService;
import ru.yandex.market.tpl.core.external.boxbot.LockerApi;
import ru.yandex.market.tpl.core.external.cms.CmsTemplatorClient;
import ru.yandex.market.tpl.core.external.crm.CrmClient;
import ru.yandex.market.tpl.core.external.delivery.sc.ScClient;
import ru.yandex.market.tpl.core.external.delivery.sc.ScLgwClient;
import ru.yandex.market.tpl.core.external.delivery.sc.SortCenterDirectClient;
import ru.yandex.market.tpl.core.external.juggler.JugglerPushClient;
import ru.yandex.market.tpl.core.external.lifepay.LifePayClient;
import ru.yandex.market.tpl.core.external.locker.LockerClient;
import ru.yandex.market.tpl.core.external.routing.delivery.client.RoutingClient;
import ru.yandex.market.tpl.core.external.routing.vrp.client.VrpClient;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettingsProvider;
import ru.yandex.market.tpl.core.external.rover.RoverClient;
import ru.yandex.market.tpl.core.external.rover.RoverPosition;
import ru.yandex.market.tpl.core.external.xiva.PushSendService;
import ru.yandex.market.tpl.core.external.xiva.XivaClient;
import ru.yandex.market.tpl.core.external.xiva.XivaSendClient;
import ru.yandex.market.tpl.core.external.xiva.XivaTvmClient;
import ru.yandex.market.tpl.core.service.monitoring.TplLogMonitoringService;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.startrek.client.Events;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author kukabara
 */
@Configuration
public class MockIntegrationTestsConfig {

    @Configuration
    public static class TvmTestConfiguration {

        @Bean
        public TvmClient tvmClient() {
            return new DummyTvmClient();
        }

    }

    @Bean
    public RoverClient roverClient() {
        RoverClient roverClient = mock(RoverClient.class);
        when(roverClient.getLocation()).thenReturn(new RoverPosition(BigDecimal.valueOf(55.734016),
                BigDecimal.valueOf(37.587607), BigDecimal.valueOf(90)));
        return roverClient;
    }

    @Bean
    public YaSmsClient yaSmsClient() {
        return mock(YaSmsClient.class);
    }

    @Bean
    public XivaClient xivaClient() {
        XivaClient mock = mock(XivaClient.class);
        init(mock);
        return mock;
    }

    @Bean
    public XivaTvmClient partnerCarrierXivaClient() {
        XivaTvmClient mock = mock(XivaTvmClient.class);
        when(mock.subscribe(any())).thenReturn(true);
        when(mock.unsubscribe(any())).thenReturn(true);
        when(mock.send(any())).thenReturn("");
        return mock;
    }

    @Bean
    public XivaSendClient xivaSendClient() {
        return mock(XivaSendClient.class);
    }

    public static void init(XivaClient mock) {
        when(mock.subscribe(any())).thenReturn(true);
        when(mock.unsubscribe(any())).thenReturn(true);
        when(mock.send(any())).thenReturn("");
    }

    @Bean
    public TrackerApiClient trackerApiClient() {
        return mock(TrackerApiClient.class);
    }

    @Bean
    public ScLgwClient scLgwClient() {
        return mock(ScLgwClient.class);
    }

    @Bean
    public ru.yandex.market.logistic.gateway.client.DeliveryClient lgwDeliveryClient() {
        return mock(ru.yandex.market.logistic.gateway.client.DeliveryClient.class);
    }

    @Bean
    public SortCenterDirectClient sortCenterDirectClient() {
        return mock(SortCenterDirectClient.class);
    }

    @Bean
    public TplLgwClient tplLgwClient() {
        return mock(TplLgwClient.class);
    }

    @Bean
    public DeliveryClient dsLgwClient() {
        return mock(DeliveryClient.class);
    }

    @Bean
    public BlackboxClient blackboxClient() {
        return mock(BlackboxClient.class);
    }

    @Bean
    public JugglerPushClient jugglerPushClient() {
        return mock(JugglerPushClient.class);
    }

    @Bean
    public LifePayClient lifePayClient() {
        return mock(LifePayClient.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public VrpClient vrpClient() {
        return mock(VrpClient.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public RoutingClient routingClient() {
        return mock(RoutingClient.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpGeobase mockHttpGeobase() {
        HttpGeobase mock = mock(HttpGeobase.class);
        when(mock.getRegionId(anyLong(), anyLong()))
                .thenReturn(120547);
        GeobaseRegionData mockRegionData = new GeobaseRegionData();
        mockRegionData.setType(DetailingEnum.SECONDARY_DISTRICT_REGION_VILLAGE.getRegionTypes().get(0));
        when(mock.getRegion(anyInt()))
                .thenReturn(mockRegionData);
        return mock;
    }

    @Bean
    public GeoClient geoClient() {
        return mock(GeoClient.class);
    }

    @Bean("sendBeruXivaClient")
    public PushSendService sendBeruXivaClient() {
        return mock(PushSendService.class);
    }

    @Bean
    public LockerClient lockerClient() {
        return mock(LockerClient.class);
    }

    @Bean
    public LockerApi lockerApi() {
        return mock(LockerApi.class);
    }

    /**
     * Генерация geobase.csv :
     * curl -o regions.csv "http://geoexport.yandex.ru/?fields=Id,Runame,Type,Parent,Enname,TzOffset,
     * ChiefRegion,latitude,longitude&types=0,1,2,3,4,5,6,7,8,9,10,11,12,13"
     * grep "^10001\t\|^225\t\|^3\t\|^1\t\|^225\t\|^213\t\|^10000\t\|^959\t\|^977\t\|^26\t\|^39\t\|^121146\t\|^11029"
     * regions.csv
     */
    @Bean
    public static RegionService regionService(ExtendedRegionTreePlainTextBuilder extendedRegionTreePlainTextBuilder) {
        RegionService service = new RegionService();
        service.setRegionTreeBuilders(List.of(extendedRegionTreePlainTextBuilder));
        service.getRegionTree();
        return service;
    }

    @Bean
    public static ExtendedRegionTreePlainTextBuilder extendedRegionTreePlainTextBuilder() {
        ExtendedRegionTreePlainTextBuilder builderByUrl = new ExtendedRegionTreePlainTextBuilder();
        builderByUrl.setPlainTextURL(MockIntegrationTestsConfig.class.getResource("/geobase/geobase.csv"));
        builderByUrl.setSkipHeader(true);
        builderByUrl.setSkipUnRootRegions(true);
        builderByUrl.setAttributes(TplRegion.ATTRIBUTE_TO_COLUMN.keySet().toArray(new CustomRegionAttribute[0]));

        return builderByUrl;
    }

    @Bean
    public static CmsTemplatorClient cmsTemplatorClient() {
        return mock(CmsTemplatorClient.class);
    }

    @Bean
    public static CrmClient crmClient() {
        return mock(CrmClient.class);
    }

    @Bean
    public static LMSClient lmsClient() {
        return mock(LMSClient.class);
    }

    @Bean
    public static YtPickupPointRepository ytPickupPointRepository() {
        return mock(YtPickupPointRepository.class);
    }

    @Bean
    public static CheckTableYtRepository checkTableYtRepository() {
        return mock(CheckTableYtRepository.class);
    }

    @Bean
    public static MbiApiClient mbiApiClient() {
        return mock(MbiApiClient.class);
    }

    @Bean
    public static ScLogisticsClient scLogisticsClient() {
        return mock(ScLogisticsClient.class);
    }

    @Bean
    public static ScClient scClient() {
        return mock(ScClient.class);
    }

    @Primary
    @Bean
    public static LogbrokerProducerFactory mockedLogbrokerProducerFactory(AsyncProducer mockedAsyncProducer) {
        LogbrokerProducerFactory mockedLogbrokerProducerFactory = mock(LogbrokerProducerFactory.class);
        when(mockedLogbrokerProducerFactory.createProducer(any())).thenReturn(mockedAsyncProducer);
        when(mockedLogbrokerProducerFactory.createProducerSupportMultiThreadByHost(any())).thenReturn(mockedAsyncProducer);
        return mockedLogbrokerProducerFactory;
    }

    @Bean
    public static AsyncProducer mockedAsyncProducer() {
        AsyncProducer mock = mock(AsyncProducer.class);
        when(mock.write(any())).thenReturn(CompletableFuture.completedFuture(null));
        return mock;
    }

    @Primary
    @Bean
    public static TplOrderRescheduleValidator spiedRescheduleValidator(Clock clock,
                                                                       RoutingRequestWaveService waveService,
                                                                       ConfigurationProviderAdapter confProvider) {
        return spy(new TplOrderRescheduleValidator(waveService, clock, confProvider));
    }

    @Primary
    @Bean
    public static ConfigurationProviderAdapter spiedConfigurationProviderAdapter(ConfigurationProvider
                                                                                         configurationProvider) {
        return spy(new ConfigurationProviderAdapter(configurationProvider));
    }

    @Primary
    @Bean
    public static GlobalSettingsProvider spiedGlobalSettingsProvider(ConfigurationProvider
                                                                             configurationProvider) {
        return spy(new GlobalSettingsProviderAdapterImpl(configurationProvider));
    }

    @Primary
    @Bean
    public static TplRegionBorderGisDao mockedTplRegionBorderGisDao() {
        return mock(TplRegionBorderGisDao.class);
    }

    @Primary
    @Bean
    public static TplLogMonitoringService mockedTplLogMonitoringService() {
        return mock(TplLogMonitoringService.class);
    }

    @Primary
    @Bean
    public static AddressQueryService spiedAddressFactory(TplRegionService tplRegionService,
                                                          PreciseGeoPointService preciseGeoPointService) {
        return spy(new AddressQueryService(preciseGeoPointService, tplRegionService));
    }

    @ConditionalOnMissingBean
    @Bean
    public static VaccinationValidator vaccinationValidator() {
        return mock(VaccinationValidator.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public static TplCovidExternalService tplCovidExternalService() {
        return mock(TplCovidExternalService.class);
    }

    @Primary
    @Bean
    public static YardClientApi yardClientApi() {
        return mock(YardClientApi.class);
    }

    @Primary
    @Bean
    public static ReturnsApi returnsClientApi() {
        return mock(ReturnsApi.class);
    }

    @Primary
    @Bean
    public static TransferApi mockTransferApiClient() {
        return mock(TransferApi.class);
    }

    @Primary
    @Bean
    public static SignatureApi mockSignatureApiClient() {
        return mock(SignatureApi.class);
    }

    @Primary
    @Bean
    public static DocumentApi mockTransferActDocumentApiClient() {
        return mock(DocumentApi.class);
    }

    @Primary
    @Bean
    public static CourierApi mockDeliveryStaffManagerCourierApiClient() {
        return mock(CourierApi.class);
    }

    @Primary
    @Bean
    public static EmployerApi mockDeliveryStaffManagerEmployerApiClient() {
        return mock(EmployerApi.class);
    }

    @Primary
    @Bean
    public static DefaultTaxiVGWForwardingsApi taxiVoiceGatewayForwardingApi() {
        return mock(DefaultTaxiVGWForwardingsApi.class);
    }

    @Primary
    @Bean
    public static DefaultTaxiVGWTalksApi taxiVoiceGatewayTalksApi() {
        return mock(DefaultTaxiVGWTalksApi.class);
    }

    @Primary
    @Bean
    public static DefaultTaxiDriverTrackStoryTrackApi taxiDriverTrackStoryTrackApi() {
        return mock(DefaultTaxiDriverTrackStoryTrackApi.class);
    }

    @Primary
    @Bean
    public static DefaultTaxiDriverTrackStoryPositionApi taxiDriverTrackStoryPositionApi() {
        return mock(DefaultTaxiDriverTrackStoryPositionApi.class);
    }

    @Bean
    public static JmsTemplate createJmsTemplate() {
        return Mockito.mock(JmsTemplate.class);
    }

    @Primary
    @Bean
    public static Session mockTrackerSession() {
        return mock(Session.class);
    }

    @Primary
    @Bean
    public static TrackerService mockTrackerService() {
        return mock(TrackerService.class);
    }

    @Primary
    @Bean
    public static Issues mockTrackerIssues() {
        return mock(Issues.class);
    }

    @Primary
    @Bean
    public static Events mockTrackerEvents() {
        return mock(Events.class);
    }

    @Primary
    @Bean
    public static PassportApiClient mockPassportApi() {
        return mock(PassportApiClient.class);
    }

    @Primary
    @Bean
    public static CombinatorGrpc.CombinatorBlockingStub mockCombinatorBlockingStub() {
        return mock(CombinatorGrpc.CombinatorBlockingStub.class);
    }

    @Primary
    @Bean
    public static DefaultPersonalRetrieveApi personalRetrieveApi() {
        return mock(DefaultPersonalRetrieveApi.class);
    }

    @Primary
    @Bean
    public static DefaultPersonalStoreApi personalStoreApi() {
        return mock(DefaultPersonalStoreApi.class);
    }

    @Primary
    @Bean
    public static DefaultPersonalFindApi personalFindApi() {
        return mock(DefaultPersonalFindApi.class);
    }

}
