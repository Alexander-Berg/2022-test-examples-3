package ru.yandex.market.delivery.mdbclient;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;

import ru.yandex.market.delivery.mdbclient.config.RestTemplateConfig;
import ru.yandex.market.delivery.mdbclient.utils.JsonMatcher;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    RestTemplateConfig.class
})
@TestPropertySource("classpath:application-integration-test.properties")
public abstract class AbstractRestTest {

    protected final ObjectMapper objectMapper = ClientUtilsFactory.getObjectMapper();

    protected final SoftAssertions assertions = new SoftAssertions();

    @Value("${mdb.app.url}")
    protected String uri;

    @Autowired
    protected MdbClient mdbClient;

    @Autowired
    protected HttpTemplate mdbHttpTemplate;

    protected MockRestServiceServer mock;

    @BeforeEach
    public void setUp() {
        mock = MockRestServiceServer.createServer(((HttpTemplateImpl) mdbHttpTemplate).getRestTemplate());
    }

    @AfterEach
    public void tearDown() {
        mock.verify();
        assertions.assertAll();
    }

    protected <ExpectedResponseType, ActualResultType> void executePositiveScenario(
        String method,
        String expectedRequestPath,
        String responsePath,
        Class<ExpectedResponseType> responseTypeClass,
        Supplier<ActualResultType> clientCall,
        BiConsumer<ExpectedResponseType, ActualResultType> responseMatchingFunction
    ) throws IOException {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body(extractFileContent(responsePath));

        buildMockRestServiceServer(method, expectedRequestPath, taskResponseCreator);

        ActualResultType actualResult = clientCall.get();
        ExpectedResponseType expectedResponse =
            objectMapper.readValue(extractFileContent(responsePath), responseTypeClass);

        responseMatchingFunction.accept(expectedResponse, actualResult);
    }

    protected void executePositiveVoidScenario(
        String method,
        String expectedRequestPath,
        Runnable clientCall
    ) {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON);

        mock.expect(requestTo(uri + "/" + method))
            .andExpect(content().string(JsonMatcher.getMatcherFunction()
                .apply(extractFileContent(expectedRequestPath))))
            .andRespond(taskResponseCreator);

        clientCall.run();
    }

    protected void executePositiveVoidBodilessScenario(
        String method,
        Runnable clientCall
    ) {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON);

        mock.expect(requestTo(uri + "/" + method))
            .andRespond(taskResponseCreator);

        clientCall.run();
    }

    private void buildMockRestServiceServer(
        String method,
        String expectedRequestPath,
        ResponseCreator taskResponseCreator
    ) {
        if (expectedRequestPath == null) {
            mock.expect(requestTo(uri + "/" + method))
                .andRespond(taskResponseCreator);
            return;
        }
        mock.expect(requestTo(uri + "/" + method))
            .andExpect(content().string(JsonMatcher.getMatcherFunction()
                .apply(extractFileContent(expectedRequestPath))))
            .andRespond(taskResponseCreator);
    }
}
