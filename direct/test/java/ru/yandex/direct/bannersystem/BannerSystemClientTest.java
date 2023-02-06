package ru.yandex.direct.bannersystem;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import io.netty.handler.codec.http.EmptyHttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcher;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.asynchttp.ParsableStringRequest;
import ru.yandex.direct.bannersystem.exception.BsClientException;
import ru.yandex.direct.bannersystem.handle.BsHandleSpec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BannerSystemClientTest {
    private static class TestRequest {
        private final long numField;
        private final String textField;

        private TestRequest(long numField, String textField) {
            this.numField = numField;
            this.textField = textField;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestRequest request = (TestRequest) o;
            return numField == request.numField &&
                    Objects.equals(textField, request.textField);
        }

        @Override
        public int hashCode() {
            return Objects.hash(numField, textField);
        }
    }

    private static class TestResponse {
        private final LocalDateTime localDateTime;

        private TestResponse(LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
        }

        private LocalDateTime getLocalDateTime() {
            return localDateTime;
        }
    }

    private static final String TEST_URL = "http://yandex.ru/test.cgi";
    private static final String SIMPLE_TEXT_VALUE = "Simple text value!\nNext string\n   \n";
    private static final String SIMPLE_TEXT_BODY = "Next string\nHTTP BODY";
    private static final String VALID_JSON_TEXT_VALUE = "{\"numField\":123,\"textField\":\"someText\"}";
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(60);

    private BannerSystemClient bsClient;
    @Mock
    private ParallelFetcher<String> fetcher;
    @Mock
    private BsHandleSpec<TestRequest, TestResponse> spec;

    @Before
    public void before() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);

        ParallelFetcherFactory parallelFetcherFactory = mock(ParallelFetcherFactory.class);
        when(parallelFetcherFactory.defaultSettingsCopy())
                .thenReturn(new FetcherSettings());
        when(parallelFetcherFactory.getParallelFetcher(any(FetcherSettings.class)))
                .thenAnswer(fs -> fetcher);

        BsUriFactory bsUriFactory = mock(BsUriFactory.class);
        URI uri = new URI(TEST_URL);
        when(bsUriFactory.getProdUri(any())).thenReturn(uri);

        bsClient = new BannerSystemClient(bsUriFactory, parallelFetcherFactory);

        doReturn(ContentType.APPLICATION_OCTET_STREAM).when(spec).getRequestContentType();
        doReturn(EmptyHttpHeaders.INSTANCE).when(spec).getAdditionalHeaders();
    }

    @Test
    public void bsClientReturnsValidStringOnGet() throws InterruptedException {
        doAnswer(new BannerSystemCheckedAnswer(TEST_URL, SIMPLE_TEXT_VALUE).withCheckUuid(false))
                .when(fetcher).execute(isA(ParsableStringRequest.class));

        String data = bsClient.doRawRequest(TEST_URL, null, EmptyHttpHeaders.INSTANCE, TEST_TIMEOUT, "label");

        assertThat("простой GET-запрос отдает строку целиком", data, equalTo(SIMPLE_TEXT_VALUE));
    }

    @Test
    public void bsClientReturnsValidStringOnPost() throws InterruptedException {
        doAnswer(new BannerSystemCheckedAnswer(TEST_URL, SIMPLE_TEXT_BODY, SIMPLE_TEXT_VALUE).withCheckUuid(false))
                .when(fetcher).execute(isA(ParsableStringRequest.class));

        String data = bsClient.doRawRequest(TEST_URL, SIMPLE_TEXT_BODY, EmptyHttpHeaders.INSTANCE, TEST_TIMEOUT, "label");

        assertThat("простой POST-запрос отдает строку целиком", data, equalTo(SIMPLE_TEXT_VALUE));
    }

    @Test
    public void bsClientRequestWithHandle() throws InterruptedException {
        doAnswer(new BannerSystemCheckedAnswer(TEST_URL, SIMPLE_TEXT_VALUE))
                .when(fetcher).execute(isA(ParsableStringRequest.class));

        LocalDateTime now = LocalDateTime.now();
        doReturn(new TestResponse(now))
                .when(spec).deserializeResponseBody(eq(SIMPLE_TEXT_VALUE));

        TestResponse response = bsClient.doRequest(spec, Collections.emptyList(), TEST_TIMEOUT);

        assertThat("Получили корректный ответ", response.getLocalDateTime(), equalTo(now));
    }

    @Test
    public void bsClientRequestWithHandleAndBody() throws InterruptedException {
        doAnswer(new BannerSystemCheckedAnswer(TEST_URL, VALID_JSON_TEXT_VALUE, SIMPLE_TEXT_VALUE))
                .when(fetcher).execute(isA(ParsableStringRequest.class));

        TestRequest request = new TestRequest(1, "text");
        doReturn(VALID_JSON_TEXT_VALUE)
                .when(spec).serializeRequestBody(eq(request));

        LocalDateTime now = LocalDateTime.now();
        doReturn(new TestResponse(now))
                .when(spec).deserializeResponseBody(eq(SIMPLE_TEXT_VALUE));

        TestResponse response = bsClient.doRequest(spec, request, UUID.randomUUID(), TEST_TIMEOUT);

        assertThat("Получили корректный ответ", response.getLocalDateTime(), equalTo(now));
    }

    @Test
    public void bsClientProducesValidQuery() throws InterruptedException {
        doAnswer(new BannerSystemCheckedAnswer(TEST_URL + "?data=sometext", SIMPLE_TEXT_VALUE))
                .when(fetcher).execute(isA(ParsableStringRequest.class));

        LocalDateTime now = LocalDateTime.now();
        doReturn(new TestResponse(now))
                .when(spec).deserializeResponseBody(eq(SIMPLE_TEXT_VALUE));

        TestResponse response = bsClient.doRequest(
                spec, Collections.singletonList(new BasicNameValuePair("data", "sometext")),
                TEST_TIMEOUT);

        assertThat("Получили корректный ответ", response.getLocalDateTime(), equalTo(now));
    }

    @Test(expected = BsClientException.class)
    public void bsClientThrowsExceptionAfterFetcherError() throws InterruptedException {
        doAnswer(new BannerSystemCheckedAnswer(TEST_URL, new RuntimeException("Error"))).when(fetcher)
                .execute(isA(ParsableStringRequest.class));

        bsClient.doRequest(BsUriFactory.EXPORT_TABLE, Collections.emptyList(), TEST_TIMEOUT);
    }

    @Test(expected = BsClientException.class)
    public void bsClientThrowsExceptionOnResponseDeserealisation() throws InterruptedException {
        doAnswer(new BannerSystemCheckedAnswer(TEST_URL, SIMPLE_TEXT_VALUE))
                .when(fetcher).execute(isA(ParsableStringRequest.class));

        doThrow(RuntimeException.class)
                .when(spec).deserializeResponseBody(eq(SIMPLE_TEXT_VALUE));

        bsClient.doRequest(spec, Collections.emptyList(), TEST_TIMEOUT);
    }
}
