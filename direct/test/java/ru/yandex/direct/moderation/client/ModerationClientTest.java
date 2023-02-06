package ru.yandex.direct.moderation.client;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.direct.asynchttp.ParallelFetcher;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.asynchttp.ParsableStringRequest;
import ru.yandex.direct.asynchttp.Result;
import ru.yandex.direct.moderation.client.model.CommonModerationRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.utils.JsonUtils.fromJson;

public class ModerationClientTest {

    public static class TestRequestParams extends CommonModerationRequest {
        private Integer param1;

        public TestRequestParams(Integer param1) {
            this.param1 = param1;
        }

        public Integer getParam1() {
            return param1;
        }
    }

    public static class TestResult {
        private Integer answer1;

        @JsonCreator
        public TestResult(@JsonProperty("answer1") Integer answer1) {
            this.answer1 = answer1;
        }

        public Integer getAnswer1() {
            return answer1;
        }

        public void setAnswer1(Integer answer1) {
            this.answer1 = answer1;
        }
    }

    private static String testJsonRpcMethod = "testMethod";

    private static String testUrl = "http://direct-mod.yandex.ru/jsonrpc";
    private ModerationClient moderationClient;

    private static TestRequestParams testRequestParams = new TestRequestParams(1);
    private static TestResult testResult = new TestResult(1);


    private static String validRequest = "{\"id\":1,\"method\":\"testMethod\",\"params\":{\"param1\":1},\"jsonrpc\":\"2.0\"}";
    private static String validResponse = "{\"id\":1,\"result\":{\"answer1\":1},\"jsonrpc\":\"2.0\"}";
    private static String invalidResponse = "{\"id\":1,\"result\":[1],\"jsonrpc\":\"2.0\"}}";
    private static String invalidResponseBadJson = "{\"id\":1,\"result\":{answer1:1},\"jsonrpc\":\"2.0\"}";
    private static String invalidResponseNoResultNoError = "{\"id\":1,\"jsonrpc\":\"2.0\"}";
    private static String validResponseWithError = "{\"id\":1,\"jsonrpc\":\"2.0\",\"error\":{\"code\":32500,\"message\":\"Parse error\"}}";

    @Mock
    private ParallelFetcher<String> fetcher;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        ParallelFetcherFactory parallelFetcherFactory = mock(ParallelFetcherFactory.class);

        when(parallelFetcherFactory.getParallelFetcher()).thenAnswer(fs -> fetcher);

        ModerationClientConfiguration moderationClientConfiguration =
                new ModerationClientConfiguration(testUrl);
        moderationClient = new ModerationClient(moderationClientConfiguration, parallelFetcherFactory);


    }

    @Test
    public void testRequestFormat() throws TimeoutException, InterruptedException {
        doAnswer(new CheckedAnswer(testUrl, validResponse, validRequest)).when(fetcher)
                .execute(isA(ParsableStringRequest.class));
        moderationClient.get(testJsonRpcMethod, testRequestParams, TestResult.class);
    }

    @Test
    public void testParseCorrectAnswer() throws TimeoutException, InterruptedException {
        doAnswer(new CheckedAnswer(testUrl, validResponse)).when(fetcher)
                .execute(isA(ParsableStringRequest.class));
        TestResult result = moderationClient.get(testJsonRpcMethod, testRequestParams, TestResult.class);

        assertThat("корректный ответ парсится в ожидаемый объект", result, beanDiffer(testResult));
    }

    @Test
    public void testParseIncorrectAnswer() throws TimeoutException, InterruptedException {
        doAnswer(new CheckedAnswer(testUrl, invalidResponse)).when(fetcher)
                .execute(isA(ParsableStringRequest.class));

        thrown.expect(ModerationClientException.class);
        thrown.expectMessage("Got error while parsing response");

        moderationClient.get(testJsonRpcMethod, testRequestParams, TestResult.class);
    }

    @Test
    public void testParseIncorrectJsonAnswer() throws TimeoutException, InterruptedException {
        doAnswer(new CheckedAnswer(testUrl, invalidResponseBadJson)).when(fetcher)
                .execute(isA(ParsableStringRequest.class));

        thrown.expect(ModerationClientException.class);
        thrown.expectMessage("Got error while parsing response");

        moderationClient.get(testJsonRpcMethod, testRequestParams, TestResult.class);

    }

    @Test
    public void testResponseWithoutResult() throws TimeoutException, InterruptedException {
        doAnswer(new CheckedAnswer(testUrl, invalidResponseNoResultNoError)).when(fetcher)
                .execute(isA(ParsableStringRequest.class));

        thrown.expect(ModerationClientException.class);
        thrown.expectMessage("Got \"null\" as a result for JSON-RPC request");

        moderationClient.get(testJsonRpcMethod, testRequestParams, TestResult.class);
    }

    @Test
    public void testFailedResponseWithError() throws TimeoutException, InterruptedException {
        doAnswer(new CheckedAnswer(testUrl, validResponseWithError)).when(fetcher)
                .execute(isA(ParsableStringRequest.class));

        thrown.expect(ModerationClientException.class);
        thrown.expectMessage("Got error on response for JSON-RPC request");

        moderationClient.get(testJsonRpcMethod, testRequestParams, TestResult.class);
    }

    @Test
    public void testExceptionAfterFetcherError() throws TimeoutException, InterruptedException {
        doAnswer(new CheckedAnswer(testUrl, validResponse, new RuntimeException("Custom internal server error"))).when(fetcher)
                .execute(isA(ParsableStringRequest.class));

        thrown.expect(ModerationClientException.class);
        thrown.expectMessage("Got error on response for JSON-RPC request");

        moderationClient.get(testJsonRpcMethod, testRequestParams, TestResult.class);
    }

    private class CheckedAnswer implements Answer<Result<String>> {
        private String expectedUrl;
        private String expectedRequestBody;
        private String successValue;
        private Throwable error;

        private CheckedAnswer(String expectedUrl, String successValue) {
            this.expectedUrl = expectedUrl;
            this.successValue = successValue;
        }

        private CheckedAnswer(String expectedUrl, String successValue, String expectedRequestBody) {
            this.expectedUrl = expectedUrl;
            this.successValue = successValue;
            this.expectedRequestBody = expectedRequestBody;
        }

        private CheckedAnswer(String expectedUrl, String successValue, Throwable error) {
            this.expectedUrl = expectedUrl;
            this.successValue = successValue;
            this.error = error;
        }

        @Override
        public Result<String> answer(InvocationOnMock invocation) throws Throwable {
            ParsableStringRequest request = (ParsableStringRequest) invocation.getArguments()[0];
            assertThat("адрес запроса верный", request.getAHCRequest().getUrl(), equalTo(expectedUrl));

            if (expectedRequestBody != null) {
                assertThat("тело запроса соответствует ожидаемому",
                        fromJson(request.getAHCRequest().getStringData(), Map.class),
                        beanDiffer(fromJson(expectedRequestBody, Map.class)));
            }

            Result<String> result = new Result<>(0);
            result.setSuccess(successValue);

            if (error != null) {
                result.addError(error);
            }
            return result;
        }
    }
}

