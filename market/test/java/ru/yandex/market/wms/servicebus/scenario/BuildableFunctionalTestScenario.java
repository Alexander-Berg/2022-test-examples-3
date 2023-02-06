package ru.yandex.market.wms.servicebus.scenario;

import java.io.IOException;
import java.io.StringReader;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.BaseMatcher;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.web.servlet.MockMvc;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import ru.yandex.market.logistic.api.model.common.response.AbstractResponse;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.wms.servicebus.scenario.builder.FulfillmentInteraction;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;


public abstract class BuildableFunctionalTestScenario<T extends AbstractResponse> {

    private final MockMvc mockMvc;
    private final Class<T> valueType;
    private final Runnable runnable;
    private final ObjectMapper objectMapper;

    private final String wrapRequestUrl;
    private final String wrapRequestPath;
    private final HttpMethod wrapRequestMethod;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public BuildableFunctionalTestScenario(MockMvc mockMvc,
                                           Class<T> valueType,
                                           Runnable runnable,
                                           ObjectMapper objectMapper,
                                           String wrapRequestUrl,
                                           String wrapRequestPath,
                                           HttpMethod wrapRequestMethod) {
        this.mockMvc = mockMvc;
        this.valueType = valueType;
        this.runnable = runnable;
        this.objectMapper = objectMapper;
        this.wrapRequestUrl = wrapRequestUrl;
        this.wrapRequestPath = wrapRequestPath;
        this.wrapRequestMethod = wrapRequestMethod;

    }

    public final void start() throws Exception {

        if (runnable != null) {
            runnable.run();
        } else if (wrapRequestPath != null) {
            String response = executeRequest(
                    mockMvc,
                    wrapRequestUrl,
                    wrapRequestPath,
                    wrapRequestMethod
            );

            doAssertion(response);
        } else {
            throw new IllegalStateException("Either runnable of wrapRequest must be provided");
        }

        verifyMocks();
    }

    protected abstract void configureMocks(MockRestServiceServer mockServer) throws Exception;

    protected abstract void doAssertion(String response) throws IOException;

    protected void doCustomAssertions(
            String actualResponseContent,
            BiConsumer<ResponseWrapper<T>,
                    SoftAssertions> asserts
    ) throws IOException {
        JavaType type = objectMapper.getTypeFactory().constructParametricType(ResponseWrapper.class, valueType);

        ResponseWrapper<T> actualValue = objectMapper.readValue(actualResponseContent, type);
        SoftAssertions softAssertions = new SoftAssertions();

        asserts.accept(actualValue, softAssertions);

        softAssertions.assertAll();
    }

    protected void assertResponseMatchExpected(
            String actualResponseContent,
            String expectedResponsePath
    ) throws IOException {
        String expectedValueString = extractFileContent(expectedResponsePath);
        assertXmlValuesAreEqual(actualResponseContent, expectedValueString);

        assertSerializationIsCorrect(actualResponseContent, expectedValueString);

    }

    private void assertXmlValuesAreEqual(String actualResponseContent, String expectedValueString) {
        assertThatResponseIsXml(actualResponseContent);

        Diff diff = DiffBuilder.compare(expectedValueString)
                .withTest(actualResponseContent)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
                .withComparisonListeners()
                .ignoreWhitespace()
                .ignoreComments()
                .checkForSimilar()
                .build();

        assertThat(diff.hasDifferences())
                .as(diff.toString())
                .isFalse();
    }

    private void assertThatResponseIsXml(String xml) {
        DocumentBuilder builder = createBuilder();

        try {
            builder.parse(new InputSource(new StringReader(xml)));
        } catch (SAXException | IOException e) {
            throw new RuntimeException("Failed to parse actual response xml [" + xml + "]");
        }
    }

    private DocumentBuilder createBuilder() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        try {
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertSerializationIsCorrect(
            String actualResponseContent,
            String expectedValueString
    ) throws IOException {
        T actualValue = objectMapper.readValue(actualResponseContent, valueType);
        T expectedValue = objectMapper.readValue(expectedValueString, valueType);

        assertThat(actualValue)
                .as("Comparing actual value with expected response")
                .isEqualToComparingFieldByFieldRecursively(expectedValue);
    }

    protected void mockFulfillmentCall(
            String url,
            FulfillmentInteraction interaction,
            MockRestServiceServer mockServer,
            MediaType contentType,
            Function<String, BaseMatcher<? super String>> matcherFunction
    ) throws IOException {
        ResponseActions actions = mockServer
                .expect(times(interaction.getInvocationCount()), requestTo(url))
                .andExpect(method(interaction.getFulfillmentUrl().getHttpMethod()));

        if (interaction.getExpectedRequestPath() != null) {
            String expectedContent = extractFileContent(interaction.getExpectedRequestPath());
            actions.andExpect(content().string(matcherFunction.apply(expectedContent)));
        }

        actions.andRespond(MockRestResponseCreators.withStatus(interaction.getResponseStatus())
                .contentType(contentType)
                .body(extractFileContent(interaction.getResponsePath())));

    }

    private String executeRequest(
            MockMvc mockMvc,
            String url,
            String requestPath,
            HttpMethod httpMethod
    ) throws Exception {
        return mockMvc.perform(request(httpMethod, url)
                .contentType("text/xml;charset=UTF-8")
                .content(extractFileContent(requestPath)))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String extractFileContent(String relativePath) throws IOException {
        return IOUtils.toString(
                getSystemResourceAsStream(relativePath),
                "UTF-8"
        );
    }

    private void verifyMocks() {
    }
}
