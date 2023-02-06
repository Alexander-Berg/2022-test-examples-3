package ru.yandex.direct.http.smart.examples;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.GET;
import ru.yandex.direct.http.smart.http.Url;

public class GetWithUrl extends MockServerBase {

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("GET");
                softAssertions.assertThat(request.getPath()).isEqualTo("/path");
                return new MockResponse().setBody("ok");
            }
        };
    }

    interface Api {
        @GET
        Call<String> get(@Url String url);
    }

    @Test
    public void getRequest() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get("path").execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("ok");
    }
}
