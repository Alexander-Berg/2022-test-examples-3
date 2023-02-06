package ru.yandex.market.logistic.gateway;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.apache.http.HttpHeaders;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.delivery.gruzin.client.GruzinClient;
import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.logistic.api.model.common.PartnerMethod;
import ru.yandex.market.logistic.api.utils.UniqService;
import ru.yandex.market.logistic.gateway.config.ExecutorTestConfiguration;
import ru.yandex.market.logistic.gateway.service.converter.HtmlToPdfConverter;
import ru.yandex.market.logistic.gateway.utils.FileReadingUtils;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.personal.PersonalClient;
import ru.yandex.passport.tvmauth.TvmClient;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistic.gateway.utils.MockServerUtils.createMockRestServiceServer;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ExecutorTestConfiguration.class)
@MockBean({
    JmsTemplate.class,
    AmazonS3.class,
    HtmlToPdfConverter.class,
    TvmClient.class,
    TvmClientApi.class,
    MdbClient.class,
    TransportManagerClient.class,
    GruzinClient.class,
    TrackerApiClient.class,
    LMSClient.class,
    PersonalClient.class,
})
@MockBean(name = "wwClientJson", classes = WwClient.class)
@MockBean(name = "wwClientHtml", classes = WwClient.class)
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    ResetDatabaseTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class
})
@AutoConfigureMockMvc
@CleanDatabase
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
@ActiveProfiles(profiles = "integration-test")
@TestPropertySource({"classpath:integration-test/application-integration-test.properties"})
public abstract class AbstractIntegrationTest {

    private final static String UNIQ = "awBaUQaDl97JrlH6lPn7k4yy4RD0Y4FO";

    private static final String EMPTY_JSON = "{}";

    private static final String EMPTY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root></root>";

    protected static final String GATEWAY_URL = "https://localhost/query-gateway";

    protected static final String TEST_PROCESS_ID_STRING = "123";

    protected static final long TEST_PROCESS_ID = 123;

    static {
        Locale.setDefault(Locale.ENGLISH);
    }

    protected ObjectMapper jsonMapper;

    protected SoftAssertions softAssert;

    @MockBean
    protected UniqService uniqService;

    @Autowired
    protected MockMvc mockMvc;

    @Value("${fulfillment.stockstorage.api.host}")
    protected String stockStorageHost;

    @Value("${iris.url}")
    protected String irisHost;

    @Value("${ff.partner.service.yado.servicePropertiesUrl}")
    protected String servicePropertiesUrl;

    @Value("${ff.partner.service.yado.authenticationListUrl}")
    protected String authenticationListUrl;

    @Value("${ff.partner.service.yado.ffActiveMarketShopsUrl}")
    protected String ffActiveMarketShopsUrl;

    @Value("${ff.partner.service.yado.scActiveMarketShopsUrl}")
    protected String scActiveMarketShopsUrl;

    @Value("${ff.partner.service.yado.dsActiveMarketShopsUrl}")
    protected String dsActiveMarketShopsUrl;

    @Autowired
    @Qualifier("lgwRestTemplatesMapByMethods")
    protected Map<PartnerMethod, RestTemplate> lgwRestTemplatesMapByMethods;

    @Autowired
    protected TestableClock clock;

    @Autowired
    private CacheManager cacheManager;

    @Before
    public void setupAbstractContextualTest() {
        when(uniqService.generate()).thenReturn(UNIQ);
        softAssert = new SoftAssertions();
        createJsonMapper();
        clock.clearFixed();
    }

    @After
    public void tearDownAbstractContextualTest() {
        softAssert.assertAll();

        cacheManager.getCacheNames().stream()
            .map(cacheManager::getCache)
            .filter(Objects::nonNull)
            .forEach(Cache::clear);
    }

    protected final String getFileContent(String filename) {
        return FileReadingUtils.getFileContent(filename);
    }

    protected void prepareMockServerJsonScenarioWithStringBody(MockRestServiceServer mock,
                                                               ExpectedCount expectedCount,
                                                               String requestUrl,
                                                               String requestBody,
                                                               String responseBody) {
        ResponseActions responseActions = mock.expect(expectedCount, requestTo(requestUrl));

        if (requestBody != null) {
            responseActions
                .andExpect(content().string(JsonMatcher.getMatcherFunction().apply(requestBody)));
        }

        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON_UTF8)
            .body(responseBody != null ? responseBody : EMPTY_JSON);

        responseActions.andRespond(taskResponseCreator);
    }

    protected void prepareMockServerJsonScenario(MockRestServiceServer mock,
                                                 ExpectedCount expectedCount,
                                                 String requestUrl,
                                                 String responsePath) {
        prepareMockServerJsonScenario(mock, expectedCount, requestUrl, null, responsePath);
    }

    protected void prepareMockServerJsonScenario(MockRestServiceServer mock,
                                                 String requestUrl,
                                                 String requestPath,
                                                 String responsePath) {
        prepareMockServerJsonScenario(mock, ExpectedCount.once(), requestUrl, requestPath, responsePath);
    }

    protected void prepareMockServerJsonScenario(MockRestServiceServer mock,
                                                 ExpectedCount expectedCount,
                                                 String requestUrl,
                                                 String requestPath,
                                                 String responsePath) {
        String requestBody = Optional.ofNullable(requestPath).map(this::getFileContent).orElse(null);
        String responseBody = Optional.ofNullable(responsePath).map(this::getFileContent).orElse(EMPTY_JSON);
        prepareMockServerJsonScenarioWithStringBody(mock, expectedCount, requestUrl, requestBody, responseBody);
    }

    protected void prepareMockServerXmlScenario(MockRestServiceServer mock,
                                                String requestUrl,
                                                String requestPath,
                                                String responsePath) {
        ResponseActions responseActions = mock.expect(requestTo(requestUrl))
            .andExpect(header(HttpHeaders.AUTHORIZATION, containsString("AccessToken")));

        if (requestPath != null) {
            responseActions.andExpect(content().string(CompareMatcher.isSimilarTo(getFileContent(requestPath))
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))));
        }

        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_XML)
            .body(responsePath != null ? getFileContent(responsePath) : EMPTY_XML);

        responseActions.andRespond(taskResponseCreator);
    }

    protected MockRestServiceServer createMockServerByRequest(PartnerMethod partnerMethod) {
        return createMockRestServiceServer(lgwRestTemplatesMapByMethods.get(partnerMethod));
    }

    private void createJsonMapper() {
        jsonMapper = new ObjectMapper();

        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jsonMapper.setSerializationInclusion(NON_ABSENT);
    }

    protected final String extractFileContent(String relativePath) {
        return FileReadingUtils.getFileContent(relativePath);
    }

    protected void assertJsonBodyMatches(String pathToFileWithExpectedJson, String actualJson) {
        softAssert.assertThat(new JsonMatcher(getFileContent(pathToFileWithExpectedJson))
            .matches(actualJson))
            .as("Asserting that JSON response is correct")
            .isTrue();
    }
}
