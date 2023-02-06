package ru.yandex.market.ff.configuration;

import java.time.ZoneId;

import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.services.sqs.AmazonSQS;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.deepmind.openapi.client.api.AvailabilitiesApi;
import ru.yandex.market.ff.config.properties.StartrekProperties;
import ru.yandex.market.ff.dbqueue.producer.LesReturnBoxEventQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.PublishCalendarShopRequestChangeQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.PutFFInboundRegistryQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.SendMbiNotificationQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.service.PublishCalendarShopRequestChangeProducerService;
import ru.yandex.market.ff.health.solomon.SolomonPushClient;
import ru.yandex.market.ff.listener.RequestStatusChangeListener;
import ru.yandex.market.ff.model.AppInfo;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.repository.ShopRequestDocumentRepository;
import ru.yandex.market.ff.service.BookingMetaService;
import ru.yandex.market.ff.service.CalendaringClientCachingService;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.DbQueueLogService;
import ru.yandex.market.ff.service.LimitService;
import ru.yandex.market.ff.service.LogisticManagementService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.RequestTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.ShopRequestModificationService;
import ru.yandex.market.ff.service.ShopRequestStatusService;
import ru.yandex.market.ff.service.StartrekIssueService;
import ru.yandex.market.ff.service.TimeSlotsService;
import ru.yandex.market.ff.service.implementation.LogbrokerProducerProvider;
import ru.yandex.market.ff.service.implementation.PublishToLogbrokerCalendarShopRequestChangeService;
import ru.yandex.market.ff.service.implementation.lgw.FfApiRequestOrdersWithdrawService;
import ru.yandex.market.ff.service.registry.RegistryUnitService;
import ru.yandex.market.ff.service.timeslot.ChangeActiveTimeSlotService;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageClientApi;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderClient;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageOutboundClient;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchClient;
import ru.yandex.market.health.jobs.service.TmsJobExecutionFacade;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logbroker.consumer.LogbrokerReader;
import ru.yandex.market.logbroker.producer.SimpleAsyncProducer;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.iris.client.api.MeasurementApiClient;
import ru.yandex.market.logistics.iris.client.api.TrustworthyInfoClient;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mboc.http.DeliveryParams;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.startrek.client.Session;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author kotovdv 06/08/2017.
 */
@Configuration
public class MockConfiguration {
    @Bean
    public MdsS3Client mdsS3Client() {
        return mock(MdsS3Client.class);
    }

    @Bean
    public MdsS3Client mdsFfwfS3Client() {
        return mock(MdsS3Client.class);
    }

    @Bean
    public ResourceLocationFactory resourceLocationFactory() {
        return mock(ResourceLocationFactory.class);
    }

    @Bean
    public ResourceLocationFactory ffResourceLocationFactory() {
        return mock(ResourceLocationFactory.class);
    }

    @Bean(name = "testFulfillmentClient")
    public FulfillmentClient fulfillmentClient() {
        return Mockito.mock(FulfillmentClient.class);
    }

    @Deprecated(forRemoval = true)
    @Bean(name = "testDeliveryClient")
    public DeliveryClient deliveryClient() {
        return Mockito.mock(DeliveryClient.class);
    }

    @Bean
    public LMSClient lmsClient() {
        return Mockito.mock(LMSClient.class);
    }

    @Bean
    public CalendaringServiceClientApi calendaringServiceClient() {
        return Mockito.mock(CalendaringServiceClientApi.class);
    }

    @Bean
    public CalendaringClientCachingService calendaringClientCachingService() {
        CalendaringClientCachingService calendaringClientCachingService = mock(CalendaringClientCachingService.class);
        when(calendaringClientCachingService.getZoneId(anyLong())).thenAnswer(
                (Answer<ZoneId>) invocation -> {
                    Long id = invocation.getArgument(0);
                    if (id == 300L) {
                        return ZoneId.of("Asia/Yekaterinburg");
                    }
                    return ZoneId.of("Europe/Moscow");
                });
        return calendaringClientCachingService;
    }

    @Bean
    public StockStorageClientApi stockStorageClientApi() {
        return Mockito.mock(StockStorageClientApi.class);
    }

    @Bean
    @Qualifier("stockStorageOutboundMock")
    public StockStorageOutboundClient stockStorageOutboundClient() {
        return Mockito.mock(StockStorageOutboundClient.class);
    }

    @Bean
    public StockStorageOrderClient stockStorageOrderClient() {
        return Mockito.mock(StockStorageOrderClient.class);
    }

    @Bean
    public DeliveryParams deliveryParamsService() {
        return Mockito.mock(DeliveryParams.class);
    }

    @Bean
    public MbiApiClient mbiApiClient() {
        return Mockito.mock(MbiApiClient.class);
    }

    @Bean
    public LomClient lomClient() {
        return Mockito.mock(LomClient.class);
    }

    @Bean
    @Primary
    public AvailabilitiesApi availabilitiesApiClient() {
        return Mockito.mock((AvailabilitiesApi.class));
    }

    @Bean
    public ReturnsApi returnsApi() {
        return Mockito.mock(ReturnsApi.class);
    }

    @Bean
    public TrackerApiClient trackerApiClient() {
        return Mockito.mock(TrackerApiClient.class);
    }

    @Bean
    public StockStorageSearchClient stockStorageSearchClient() {
        return Mockito.mock(StockStorageSearchClient.class);
    }

    @Bean
    public TrustworthyInfoClient trustworthyInfoClient() {
        return Mockito.mock(TrustworthyInfoClient.class);
    }

    @Bean
    @Primary
    public LogisticManagementService logisticManagementService() {
        return Mockito.mock(LogisticManagementService.class);
    }

    @Bean
    @Primary
    public TimeSlotsService timeSlotsService(TimeSlotsService timeSlotsService) {
        return Mockito.spy(timeSlotsService);
    }

    @Bean
    @Primary
    public ChangeActiveTimeSlotService changeActiveTimeSlotService(
            ChangeActiveTimeSlotService changeActiveTimeSlotService) {
        return Mockito.spy(changeActiveTimeSlotService);
    }

    @Bean
    @Primary
    public PublishToLogbrokerCalendarShopRequestChangeService publishToLogbrokerCalendarShopRequestChangeService(
            PublishCalendarShopRequestChangeProducerService publishCalendarShopRequestChangeProducerService,
            DateTimeService dateTimeService,
            RequestTypeService requestTypeService,
            ShopRequestDocumentRepository shopRequestDocumentRepository,
            BookingMetaService bookingMetaService,
            RequestItemRepository requestItemRepository,
            RequestSubTypeService requestSubTypeService
    ) {
        var publishToLogbrokerCalendarShopRequestChangeService = new PublishToLogbrokerCalendarShopRequestChangeService(
            publishCalendarShopRequestChangeProducerService,
            dateTimeService,
            requestTypeService,
            shopRequestDocumentRepository,
            bookingMetaService,
            requestItemRepository,
            requestSubTypeService
        );
        return Mockito.spy(publishToLogbrokerCalendarShopRequestChangeService);
    }

    @Bean
    public LogbrokerReader logbrokerReader() {
        return Mockito.mock(LogbrokerReader.class);
    }

    @Bean
    public TmsJobExecutionFacade tmsJobExecutionFacade() {
        return Mockito.mock(TmsJobExecutionFacade.class);
    }

    @Bean
    public CheckouterAPI checkouterAPI() {
        return Mockito.mock(CheckouterAPI.class);
    }

    @Bean
    public CheckouterOrderHistoryEventsApi checkouterOrderHistoryEventsApi() {
        return Mockito.mock(CheckouterOrderHistoryEventsApi.class);
    }

    @Bean
    public SolomonPushClient solomonPushClient() {
        return Mockito.mock(SolomonPushClient.class);
    }

    @Bean
    public MarketIdServiceGrpc.MarketIdServiceBlockingStub marketIdServiceBlockingStub() {
        return Mockito.mock(MarketIdServiceGrpc.MarketIdServiceBlockingStub.class);
    }

    @Bean
    @Primary
    public ShopRequestModificationService shopRequestModificationService(
            ShopRequestModificationService shopRequestModificationService) {
        return Mockito.spy(shopRequestModificationService);
    }

    @Bean
    @Primary
    public ShopRequestStatusService shopRequestStatusService(ShopRequestStatusService shopRequestStatusService) {
        return Mockito.spy(shopRequestStatusService);
    }

    @Bean
    @Primary
    public PublishCalendarShopRequestChangeProducerService publishCalendarShopRequestChangeProducerService(
            AppInfo appInfo,
            PublishCalendarShopRequestChangeQueueProducer publishCalendarShopRequestChangeQueueProducer
    ) {
        return Mockito.spy(
                new PublishCalendarShopRequestChangeProducerService(
                        publishCalendarShopRequestChangeQueueProducer,
                        appInfo
                )
        );
    }

    @Bean
    public Terminal terminal() {
        return Mockito.mock(Terminal.class);
    }

    @Bean
    public MeasurementApiClient measurementApiClient() {
        return Mockito.mock(MeasurementApiClient.class);
    }

    @Bean
    public LogbrokerProducerProvider logbrokerProducerProvider(SimpleAsyncProducer mockedSimpleAsyncProducer)
            throws Exception {
        var mock = Mockito.mock(LogbrokerProducerProvider.class);
//        var providerProviderMock = mock(TopicSpecificLogbrokerProducerProvider.class);
//        when(providerProviderMock.asyncProducer()).thenReturn(mockedSimpleAsyncProducer);
//        when(mock.provide(any())).thenReturn(providerProviderMock);
        return mock;
    }

    @Bean
    public SimpleAsyncProducer mockedSympleAsyncProducer() {
        return Mockito.mock(SimpleAsyncProducer.class);
    }

    @Primary
    @Bean
    public RequestStatusChangeListener requestStatusChangeListenerMock() {
        return Mockito.mock(RequestStatusChangeListener.class);
    }

    @Bean
    @Primary
    RegistryUnitService registryUnitServiceSpy(RegistryUnitService registryUnitService) {
        return Mockito.spy(registryUnitService);
    }

    @Bean
    @Primary
    ShopRequestFetchingService shopRequestFetchingServiceSpy(ShopRequestFetchingService shopRequestFetchingService) {
        return Mockito.spy(shopRequestFetchingService);
    }

    @Bean
    SendMbiNotificationQueueProducer sendMbiNotificationQueueProducer(DbQueueLogService dbQueueLogService) {
        return Mockito.spy(new SendMbiNotificationQueueProducer(dbQueueLogService));
    }

    @Bean
    PutFFInboundRegistryQueueProducer putFFInboundRegistryQueueProducer(DbQueueLogService dbQueueLogService) {
        return Mockito.spy(new PutFFInboundRegistryQueueProducer(dbQueueLogService));
    }

    @Bean
    @Primary
    public LesProducer lesProducer() {
        return Mockito.mock(LesProducer.class);
    }

    @Bean
    @Primary
    public LesReturnBoxEventQueueProducer lesReturnBoxEventQueueProducer(DbQueueLogService queueLogService) {
        return Mockito.spy(new LesReturnBoxEventQueueProducer(queueLogService));
    }

    @Bean
    @Primary
    LimitService supplyLimitServiceSpy(LimitService limitService) {
        return Mockito.spy(limitService);
    }

    @Bean
    FfApiRequestOrdersWithdrawService ffApiWithdrawService() {
        return Mockito.mock(FfApiRequestOrdersWithdrawService.class);
    }

    @Bean
    public AppInfo appInfo() {
        return new AppInfo("mockHost", "FFWF-test");
    }

    @Bean
    @Primary
    public StartrekProperties startrekProperties() {
        StartrekProperties startrekPropertiesMock = mock(StartrekProperties.class);
        when(startrekPropertiesMock.getApiUri()).thenReturn("https://st-api.yandex-team.ru");
        when(startrekPropertiesMock.getUri()).thenReturn("https://st.yandex-team.ru");
        return startrekPropertiesMock;
    }

    @Bean
    @Primary
    public Session startrekSession() {
        return Mockito.mock(Session.class);
    }

    @Bean
    @Primary
    public StartrekIssueService startrekIssueService() {
        return Mockito.mock(StartrekIssueService.class);
    }

    @Bean
    @Primary
    public MboMappingsService mboMappingsService() {
        return Mockito.mock(MboMappingsService.class);
    }

    @Bean
    @Primary
    public AmazonSQS createAmazonSQSClient() {
        return Mockito.mock(AmazonSQS.class);
    }

    @Bean
    @Primary
    public AmazonHttpClient amazonHttpClient() {
        return Mockito.mock(AmazonHttpClient.class);
    }
}
