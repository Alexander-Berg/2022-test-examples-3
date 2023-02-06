package ru.yandex.market.psku.postprocessor.config;

import java.net.URI;
import java.util.Collections;

import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.market.mbo.tracker.TrackerService;
import ru.yandex.market.mbo.tracker.TrackerServiceImpl;
import ru.yandex.market.mbo.tracker.client.SummonMaillistClient;
import ru.yandex.market.mbo.tracker.client.TrackerServiceUnavailableRetryStrategy;
import ru.yandex.market.mbo.tracker.models.TrackerClientData;
import ru.yandex.market.mbo.tracker.utils.TrackerServiceHelper;
import ru.yandex.market.mbo.users.MboUsersService;
import ru.yandex.market.mbo.users.MboUsersServiceStub;
import ru.yandex.market.psku.postprocessor.common.config.TestDataBaseConfiguration;
import ru.yandex.market.psku.postprocessor.common.db.config.CommonDaoConfig;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.GenerationTaskType;
import ru.yandex.market.psku.postprocessor.service.tracker.PskuTrackerService;
import ru.yandex.market.psku.postprocessor.service.tracker.processing.ClassificationProcessingStrategy;
import ru.yandex.market.psku.postprocessor.service.tracker.processing.MskuFromPskuGenProcessingStrategy;
import ru.yandex.market.psku.postprocessor.service.tracker.processing.WaitContentProcessingStrategy;
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor;
import ru.yandex.market.request.httpclient.trace.TraceHttpResponseInterceptor;
import ru.yandex.market.request.trace.Module;
import ru.yandex.startrek.client.AuthenticatingStartrekClient;
import ru.yandex.startrek.client.StartrekClientBuilder;

@Configuration
@Import({
        ExternalServicesConfig.class,
        TestDataBaseConfiguration.class,
        CommonDaoConfig.class
})
public class TrackerServicesManualTestConfig {

    private static final String SYSTEM_TAG = "robot-test-ppp-tag";

    @Value("https://st.test.yandex-team.ru")
    private String trackerUrl;
    @Value("Enter your login here")
    private String author;
    @Value("Enter your tracker token here")
    private String trackerAuthToken;
    @Value("psku-post-processor")
    private String userAgent;
    @Value("PSKUSUPTEST")
    private String trackerQueue;
    @Value("psku")
    private String pskuTag;
    @Value("https://st-api.test.yandex-team.ru")
    private URI trackerApiUrl;
    @Value("120000")
    private int trackerSocketTimeout;
    @Value("10000")
    private int trackerConnectionTimeout;
    @Value("50")
    private int trackerMaxConnections;
    @Value("3")
    private int trackerRetryCount;
    @Value("1000")
    private int trackerRetryInterval;
    @Value("https://mbo-testing.market.yandex.ru/gwt/#modelEditor/entity-id=")
    private String mboEntityBaseUrl;
    @Value("https://cm-testing.market.yandex-team.ru/#/suppliers/")
    private String mbocSuppliersBaseUrl;
    @Value("44895")
    private long clusterizationComponentId;
    @Value("44996")
    private long pskuToModelComponentId;
    @Value("44898")
    private long moderationComponentId;
    @Value("44900")
    private long classificationComponentId;
    @Value("http://mbo-http-exporter.tst.vs.market.yandex.net:8084/mboUsers/")
    private String mboUsersHost;
    @Value("https://mbo-testing.market.yandex.ru/gwt/#tovarTree/hyperId=")
    private String mboCategoryBaseUrl;

    @Autowired
    private CommonDaoConfig commonDaoConfig;

    @Bean
    public PskuTrackerService pskuTrackerService() {
        return new PskuTrackerService(
                clusterProcessingStrategy(),
                null,
                classificationProcessingStrategy(),
                waitContentProcessingStrategy());
    }

    @Bean
    public MskuFromPskuGenProcessingStrategy clusterProcessingStrategy() {
        return new MskuFromPskuGenProcessingStrategy(
                trackerService(),
                commonDaoConfig.trackerTicketPskuStatusDao(),
                mboUsersService(),
                mboEntityBaseUrl,
                mbocSuppliersBaseUrl,
                clusterizationComponentId,
                GenerationTaskType.CLUSTER);
    }

    @Bean
    public MskuFromPskuGenProcessingStrategy pskuToModelProcessingStrategy() {
        return new MskuFromPskuGenProcessingStrategy(
            trackerService(),
            commonDaoConfig.trackerTicketPskuStatusDao(),
            mboUsersService(),
            mboEntityBaseUrl,
            mbocSuppliersBaseUrl,
            pskuToModelComponentId,
            GenerationTaskType.TO_MODEL);
    }


    @Bean
    public WaitContentProcessingStrategy waitContentProcessingStrategy() {
        return new WaitContentProcessingStrategy(
                trackerService(),
                commonDaoConfig.trackerTicketPskuStatusDao(),
                mboEntityBaseUrl,
                mboCategoryBaseUrl,
                trackerQueue);

    }


    @Bean
    public ClassificationProcessingStrategy classificationProcessingStrategy() {
        return new ClassificationProcessingStrategy(
                trackerService(),
                commonDaoConfig.trackerTicketPskuStatusDao(),
                mboUsersService(),
                mboEntityBaseUrl,
                mbocSuppliersBaseUrl,
                classificationComponentId);
    }

    @Bean
    public TrackerService trackerService() {
        return new TrackerServiceImpl(clientData(), trackerClient(), trackerServiceHelper(),
                summonMaillistClient(), null);
    }

    @Bean
    public AuthenticatingStartrekClient trackerClient() {
        return (AuthenticatingStartrekClient) StartrekClientBuilder.newBuilder()
                .uri(trackerApiUrl)
                .httpClient(trackerHttpClient())
                .customFields(Cf.toHashMap(TrackerServicesConfig.CUSTOM_FIELDS))
                .build(trackerAuthToken);
    }

    @Bean
    public SummonMaillistClient summonMaillistClient() {
        return new SummonMaillistClient(trackerHttpClient(), trackerApiUrl, trackerAuthToken);
    }

    @Bean
    public CloseableHttpClient trackerHttpClient() {
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();

        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(registry);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(trackerConnectionTimeout)
                .setConnectTimeout(trackerConnectionTimeout)
                .setSocketTimeout(trackerSocketTimeout)
                .setCookieSpec(CookieSpecs.DEFAULT)
                .setStaleConnectionCheckEnabled(true)
                .build();

        ServiceUnavailableRetryStrategy retryStrategy = retryStrategy();

        return HttpClientBuilder.create()
                .setServiceUnavailableRetryStrategy(retryStrategy)
                .setUserAgent(userAgent)
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(trackerRetryCount)
                .setMaxConnTotal(trackerMaxConnections)
                .setConnectionManager(connManager)
                .addInterceptorFirst(new TraceHttpRequestInterceptor(Module.STARTREK))
                .addInterceptorLast(new TraceHttpResponseInterceptor())
                .build();
    }

    @Bean
    public TrackerServiceHelper trackerServiceHelper() {
        return new TrackerServiceHelper(null, author, null,
                null, null, null);
    }

    @Bean
    public MboUsersService mboUsersService() {
        MboUsersServiceStub service = new MboUsersServiceStub();
        service.setHost(mboUsersHost);
        service.setHttpRequestInterceptor(new TraceHttpRequestInterceptor(Module.MBO_HTTP_EXPORTER));
        service.setHttpResponseInterceptor(new TraceHttpResponseInterceptor());
        service.setUserAgent(userAgent);
        return service;
    }

    private TrackerClientData clientData() {
        TrackerClientData clientData = new TrackerClientData(trackerUrl, author, trackerQueue, SYSTEM_TAG);
        clientData.setTags(Collections.singletonList(pskuTag));
        return clientData;
    }

    private ServiceUnavailableRetryStrategy retryStrategy() {
        return new TrackerServiceUnavailableRetryStrategy(trackerRetryCount, trackerRetryInterval);
    }
}
