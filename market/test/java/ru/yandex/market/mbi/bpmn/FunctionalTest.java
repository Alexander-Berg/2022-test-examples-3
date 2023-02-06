package ru.yandex.market.mbi.bpmn;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.collections.MapUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.stockstorage.client.StockStorageWarehouseGroupClient;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.bpmn.client.Ff4shopsClient;
import ru.yandex.market.mbi.bpmn.config.SpringApplicationConfig;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;

import static org.mockito.Mockito.reset;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {SpringApplicationConfig.class}
)
@ActiveProfiles(profiles = {"functionalTest", "development"})
@TestPropertySource("classpath:functional-test.properties")
@Execution(ExecutionMode.SAME_THREAD)
public abstract class FunctionalTest {

    private static final String BASE_URL = "http://localhost:";

    public static final String TEST_BUSINESS_KEY = "TBK";

    @LocalServerPort
    private int port;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    protected ProcessEngine processEngine;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected Ff4shopsClient ff4shopsClient;

    @Autowired
    public MbiApiClient mbiApiClient;

    @Autowired
    public MbiOpenApiClient mbiOpenApiClient;

    @Autowired
    public DataCampClient dataCampShopClient;

    @Autowired
    public StockStorageWarehouseGroupClient stockStorageWarehouseGroupClient;

    @BeforeEach
    void setUp() {
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @AfterEach
    void tearDown() {
        reset(mbiApiClient);
        reset(mbiOpenApiClient);
        reset(stockStorageWarehouseGroupClient);
        reset(dataCampShopClient);
    }

    protected String baseUrl() {
        return BASE_URL + port;
    }

    protected URI getUri(String path, Map<String, String> params) {
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl());
            uriBuilder.setPath(path);
            if (MapUtils.isNotEmpty(params)) {
                params.forEach(uriBuilder::setParameter);
            }
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T sendHttpRequest(HttpUriRequest request, Class<T> tClass) {
        HttpResponse response = null;
        try {
            response = HttpClientBuilder.create().build().execute(request);
            return objectMapper.readValue(response.getEntity().getContent(), tClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T doPost(String path, Object request, Class<T> tClass) {
        HttpPost httpPost = doHttpPost(path, request);
        return sendHttpRequest(httpPost, tClass);
    }

    protected HttpPost doHttpPost(String path, Object request) {
        try {
            HttpPost httpRequest = new HttpPost(getUri(path, null));
            httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpRequest.setEntity(
                    new StringEntity(new ObjectMapper().writeValueAsString(request),
                            ContentType.APPLICATION_JSON
                    )
            );
            return httpRequest;
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
    }

}
