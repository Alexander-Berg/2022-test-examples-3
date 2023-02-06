package ru.yandex.market.ff.base;

import com.github.springtestdbunit.TransactionDbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.abo.api.client.AboAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.ff.configuration.FeatureToggleTestConfiguration;
import ru.yandex.market.ff.configuration.IntegrationTestConfiguration;
import ru.yandex.market.ff.controller.health.RequestProblemController;
import ru.yandex.market.ff.dbqueue.producer.LesReturnBoxEventQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.PutFFInboundRegistryQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.SendMbiNotificationQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.service.PublishCalendarShopRequestChangeProducerService;
import ru.yandex.market.ff.listener.RequestStatusChangeListener;
import ru.yandex.market.ff.repository.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.ff.service.EnvironmentParamService;
import ru.yandex.market.ff.service.FulfillmentInfoService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.RequestTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.ShopRequestModificationService;
import ru.yandex.market.ff.service.ShopRequestStatusService;
import ru.yandex.market.ff.service.SupplierRatingIntervalService;
import ru.yandex.market.ff.service.implementation.PublishToLogbrokerCalendarShopRequestChangeService;
import ru.yandex.market.ff.service.implementation.lgw.FfApiRequestOrdersWithdrawService;
import ru.yandex.market.ff.service.implementation.tanker.TankerCacheManager;
import ru.yandex.market.ff.service.registry.RegistryUnitService;
import ru.yandex.market.ff.service.util.hibernate.ResettableSequenceStyleGenerator;
import ru.yandex.market.ff.util.HibernateQueriesExecutionListener;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderClient;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageOutboundClient;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchClient;
import ru.yandex.market.id.MarketIdServiceGrpc;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ContextConfiguration(
        loader = AnnotationConfigWebContextLoader.class,
        classes = IntegrationTestConfiguration.class)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        TransactionDbUnitTestExecutionListener.class,
        HibernateQueriesExecutionListener.class,
})
@WebAppConfiguration("classpath:resources")
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:servant-integration-test.properties")
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader.class)
public abstract class IntegrationTest {

    public static final String TEST_BUCKET = "test";
    public static final String MBI_TEST_BUCKET = "market-mbi-test";

    @Autowired
    protected RequestProblemController requestProblemController;

    @Autowired
    protected MdsS3Client mdsS3Client;

    @Autowired
    protected MdsS3Client mdsFfwfS3Client;

    @Autowired
    protected MbiApiClient mbiApiClient;

    @Autowired
    protected CheckouterAPI checkouterAPI;

    @Autowired
    protected LomClient lomClient;

    @Autowired
    protected ReturnsApi returnsApi;

    @Autowired
    protected FfApiRequestOrdersWithdrawService ffApiWithdrawService;

    @Autowired
    protected TankerCacheManager tankerCacheManager;

    @Autowired
    @Qualifier("stockStorageOutboundMock")
    protected StockStorageOutboundClient stockStorageOutboundClient;

    @Autowired
    protected StockStorageOrderClient stockStorageOrderClient;

    @Autowired
    protected StockStorageSearchClient stockStorageSearchClient;

    @Autowired
    protected FulfillmentClient fulfillmentClient;

    @Deprecated(forRemoval = true)
    @Autowired
    protected DeliveryClient deliveryClient;

    @Autowired
    protected EnvironmentParamService environmentParamService;

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected MarketIdServiceGrpc.MarketIdServiceBlockingStub marketIdServiceBlockingStub;

    @Autowired
    protected Terminal terminal;

    protected SoftAssertions assertions;

    @Autowired
    protected MeasurementApiClient measurementApiClient;

    @Autowired
    protected AboAPI aboApi;

    @Autowired
    protected RequestStatusChangeListener requestStatusChangeListenerMock;

    @Autowired
    protected RegistryUnitService registryUnitService;

    @Autowired
    protected ShopRequestFetchingService shopRequestFetchingService;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @Autowired
    private ResourceLocationFactory ffResourceLocationFactory;

    @Autowired
    protected SupplierRatingIntervalService supplierRatingIntervalService;

    @Autowired
    protected TrackerApiClient trackerApiClient;

    @Autowired
    protected DeliveryParams deliveryParams;

    @Autowired
    protected TrustworthyInfoClient trustworthyInfoClient;

    @Autowired
    protected SendMbiNotificationQueueProducer sendMbiNotificationQueueProducer;
    @Autowired
    private FulfillmentInfoService fulfillmentInfoService;

    @Autowired
    protected RequestTypeService requestTypeService;

    @Autowired
    protected CalendaringServiceClientApi csClient;

    @Autowired
    private FeatureToggleTestConfiguration.FTConfig ftConfig;

    @Autowired
    protected ShopRequestModificationService shopRequestModificationService;

    @Autowired
    protected PublishToLogbrokerCalendarShopRequestChangeService publishToLogbrokerCalendarShopRequestChangeService;

    @Autowired
    protected PublishCalendarShopRequestChangeProducerService publishCalendarShopRequestChangeProducerService;

    @Autowired
    private RequestSubTypeService requestSubTypeService;

    @Autowired
    private ShopRequestStatusService shopRequestStatusService;

    @Autowired
    protected PutFFInboundRegistryQueueProducer putFFInboundRegistryQueueProducer;

    @Autowired
    protected LesProducer lesProducer;

    @Autowired
    protected LesReturnBoxEventQueueProducer lesReturnBoxEventQueueProducer;

    @BeforeEach
    public void createAssertions() {
        ResettableSequenceStyleGenerator.resetAllInstances();
        assertions = new SoftAssertions();
        when(resourceLocationFactory.createLocation(anyString()))
                .thenAnswer((Answer<ResourceLocation>) invocation -> {
                    final String fileName = invocation.getArgument(0);
                    return ResourceLocation.create(MBI_TEST_BUCKET, fileName);
                });
        when(ffResourceLocationFactory.createLocation(anyString()))
                .thenAnswer((Answer<ResourceLocation>) invocation -> {
                    final String fileName = invocation.getArgument(0);
                    return ResourceLocation.create(TEST_BUCKET, fileName);
                });
    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    @AfterEach
    public void resetMocks() {
        Mockito.reset(
                mdsS3Client,
                mdsFfwfS3Client,
                resourceLocationFactory,
                ffResourceLocationFactory,
                mbiApiClient,
                stockStorageOutboundClient,
                stockStorageOrderClient,
                stockStorageSearchClient,
                fulfillmentClient,
                lmsClient,
                marketIdServiceBlockingStub,
                checkouterAPI,
                lomClient,
                returnsApi,
                terminal,
                measurementApiClient,
                deliveryClient,
                aboApi,
                publishToLogbrokerCalendarShopRequestChangeService,
                requestStatusChangeListenerMock,
                registryUnitService,
                shopRequestFetchingService,
                shopRequestModificationService,
                trackerApiClient,
                deliveryParams,
                trustworthyInfoClient,
                sendMbiNotificationQueueProducer,
                ffApiWithdrawService,
                csClient,
                publishCalendarShopRequestChangeProducerService,
                shopRequestStatusService,
                putFFInboundRegistryQueueProducer,
                lesProducer,
                lesReturnBoxEventQueueProducer
        );
        environmentParamService.clearCache();
        supplierRatingIntervalService.invalidateCache();
        tankerCacheManager.getTankerCache().clear();
        fulfillmentInfoService.invalidateCache();
        requestTypeService.invalidateCache();
        requestSubTypeService.invalidateCache();
        ftConfig.cleanUp();
    }
}
