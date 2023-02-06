package ru.yandex.market.fulfillment.wrap.core.scenario.builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.BaseMatcher;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.fulfillment.wrap.core.scenario.BuildableFunctionalTestScenario;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.common.response.AbstractResponse;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;

import static java.util.stream.Collectors.toMap;

public class FunctionalTestScenarioBuilder<T extends AbstractResponse> {

    private final Class<T> responseType;

    private String wrapUrl;
    private HttpMethod wrapHttpMethod;
    private String wrapRequestPath;
    private Runnable runnable;

    private String expectedResponsePath;
    private BiConsumer<ResponseWrapper<T>, SoftAssertions> expectedResponseRequirements = (response, assertions) -> {
    };

    private final List<FulfillmentInteraction> fulfillmentInteractions = new ArrayList<>();

    protected FunctionalTestScenarioBuilder(Class<T> responseType) {
        this.responseType = responseType;
    }

    public static <Response extends AbstractResponse> FunctionalTestScenarioBuilder<Response> start() {
        return start(null);
    }

    public static <Response extends AbstractResponse> FunctionalTestScenarioBuilder<Response> start(
        Class<Response> responseType
    ) {
        return new FunctionalTestScenarioBuilder<>(responseType);
    }

    public FunctionalTestScenarioBuilder<T> thenMockFulfillmentRequests(
        FulfillmentInteraction... fulfillmentInteractions
    ) {
        FulfillmentInteraction[] interactions = Optional.ofNullable(fulfillmentInteractions)
            .orElseGet(() -> new FulfillmentInteraction[0]);

        Stream.of(interactions).forEach(this::thenMockFulfillmentRequest);
        return this;
    }

    public FunctionalTestScenarioBuilder<T> thenMockFulfillmentRequest(FulfillmentInteraction fulfillmentInteraction) {
        fulfillmentInteractions.add(fulfillmentInteraction);
        return this;
    }

    public FunctionalTestScenarioBuilder<T> thenMockFulfillmentRequest(
        List<FulfillmentInteraction> fulfillmentInteractionList
    ) {
        fulfillmentInteractions.addAll(fulfillmentInteractionList);
        return this;
    }

    public FunctionalTestScenarioBuilder<T> sendRequestToWrapQueryGateway(String requestPath) {
        return sendRequestToWrap("/query-gateway", HttpMethod.POST, requestPath);
    }

    public FunctionalTestScenarioBuilder<T> run(Runnable runnable) {
        this.runnable = runnable;
        return this;
    }

    public FunctionalTestScenarioBuilder<T> sendRequestToWrap(String url,
                                                              HttpMethod httpMethod,
                                                              String requestPath) {
        this.wrapUrl = url;
        this.wrapHttpMethod = httpMethod;
        this.wrapRequestPath = requestPath;

        return this;
    }

    public FunctionalTestScenarioBuilder<T> andExpectWrapAnswerToContainErrors(
        Map<ErrorCode, Integer> expectedErrors
    ) {
        return andExpectWrapAnswerToMeetRequirements((response, assertions) -> {
                boolean hasErrors = response.getRequestState().hasErrors();

                assertions.assertThat(hasErrors)
                    .as("Assert that response hasErrors flag is true")
                    .isTrue();

                List<ErrorPair> errorPairs = response.getRequestState().getErrorCodes();
                Map<ErrorCode, Integer> actualErrors = errorPairs.stream()
                    .collect(toMap(
                        ErrorPair::getCode,
                        (errorPair) -> 1,
                        (value1, value2) -> value1 + value2
                    ));

                assertions.assertThat(actualErrors)
                    .as("Assert that errors container contains only expected errors")
                    .isEqualTo(expectedErrors);
            }
        );
    }

    public FunctionalTestScenarioBuilder<T> andExpectWrapAnswerToBeEqualTo(String expectedResponsePath) {
        this.expectedResponsePath = expectedResponsePath;

        return this;
    }


    public FunctionalTestScenarioBuilder<T> andExpectWrapAnswerToMeetRequirements(
        BiConsumer<ResponseWrapper<T>, SoftAssertions> requirements
    ) {
        this.expectedResponseRequirements = requirements;

        return this;
    }

    public BuildableFunctionalTestScenario<T> build(MockMvc mockMvc,
                                                    RestTemplate restTemplate,
                                                    ObjectMapper objectMapper,
                                                    String rootUrl,
                                                    String... pathSegments) {
        return new BuildableFunctionalTestScenario<T>(
            mockMvc,
            responseType,
            runnable,
            restTemplate,
            objectMapper,
            wrapUrl,
            wrapRequestPath,
            wrapHttpMethod
        ) {

            @Override
            protected void configureMocks(MockRestServiceServer mockServer) throws Exception {
                for (FulfillmentInteraction interaction : fulfillmentInteractions) {
                    String apiEndpointUrl = createFulfillmentApiUrl(
                        rootUrl,
                        pathSegments,
                        interaction.getFulfillmentUrl()
                    );

                    mockFulfillmentCall(
                        apiEndpointUrl,
                        interaction,
                        mockServer,
                        interaction.getResponseContentType(),
                        getMatcherFunction()
                    );
                }
            }

            @Override
            protected void doAssertion(String response) throws IOException {
                if (expectedResponsePath != null) {
                    assertResponseMatchExpected(response, expectedResponsePath);
                }

                if (expectedResponseRequirements != null) {
                    doCustomAssertions(response, expectedResponseRequirements);
                }
            }
        };
    }


    private String createFulfillmentApiUrl(String rootUrl,
                                           String[] urlSegments,
                                           FulfillmentUrl fulfillmentUrl) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(rootUrl);
        if (urlSegments != null && urlSegments.length > 0) {
            builder.pathSegment(urlSegments);
        }

        return builder
            .pathSegment(fulfillmentUrl.getUrlParts().toArray(new String[0]))
            .queryParams(fulfillmentUrl.getUrlArguments())
            .toUriString();
    }

    public Function<String, BaseMatcher<? super String>> getMatcherFunction() {
        return JsonMatcher::new;
    }
}
