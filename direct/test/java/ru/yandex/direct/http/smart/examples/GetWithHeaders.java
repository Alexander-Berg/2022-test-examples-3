package ru.yandex.direct.http.smart.examples;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.GET;
import ru.yandex.direct.http.smart.http.Header;
import ru.yandex.direct.http.smart.http.HeaderMap;
import ru.yandex.direct.http.smart.http.Headers;

import static java.util.Arrays.asList;

public class GetWithHeaders extends MockServerBase {

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setBody(request.getHeaders().toString());
            }
        };
    }

    interface Api {
        @GET("/")
        Call<String> get(@Header(value = "header1") String header);

        @GET("/")
        Call<String> getList(@Header(value = "header1") List<String> headers);

        @GET("/")
        Call<String> getArray(@Header(value = "header1") String... headers);

        @GET("/")
        @Headers({"header1: value1", "header2: value2"})
        Call<String> get();

        @GET("/")
        Call<String> get(@HeaderMap Map<String, String> headers);

        @GET("/")
        @Headers({"Content-Type: text/plain"})
        Call<String> withContentType();
    }

    @Test
    public void header() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get("value1").execute().getSuccess();
        softAssertions.assertThat(resp).contains("header1: value1");
    }

    @Test
    public void headerList() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.getList(asList("value1", "value2")).execute().getSuccess();
        softAssertions.assertThat(resp).contains("header1: value1");
        softAssertions.assertThat(resp).contains("header1: value2");
    }

    @Test
    public void headerArray() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.getArray("value1", "value2").execute().getSuccess();
        softAssertions.assertThat(resp).contains("header1: value1");
        softAssertions.assertThat(resp).contains("header1: value2");
    }

    @Test
    public void headers() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get().execute().getSuccess();
        softAssertions.assertThat(resp).contains("header1: value1");
        softAssertions.assertThat(resp).contains("header2: value2");
    }

    @Test
    public void headersMap() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get(ImmutableMap.of("header1", "value1")).execute().getSuccess();
        softAssertions.assertThat(resp).contains("header1: value1");
    }

    @Test
    public void withContentType() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.withContentType().execute().getSuccess();
        softAssertions.assertThat(resp).containsIgnoringCase("Content-Type: text/plain");
    }

    @Test
    public void headerConfiguratorCalledAfterApplyingHeadersAnnotations() {
        Smart smart = builder()
                .addHeaderConfigurator(
                        headers -> softAssertions.assertThat(headers.get("Content-Type")).isEqualTo("text/plain"))
                .build();
        Api api = smart.create(Api.class);
        String resp = api.withContentType().execute().getSuccess();
        String resp2 = api.get(ImmutableMap.of("Content-Type", "text/plain")).execute().getSuccess();
        softAssertions.assertThat(resp).containsIgnoringCase("Content-Type: text/plain");
        softAssertions.assertThat(resp2).containsIgnoringCase("Content-Type: text/plain");
    }
}
