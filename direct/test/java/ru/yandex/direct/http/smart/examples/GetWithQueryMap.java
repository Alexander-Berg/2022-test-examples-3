package ru.yandex.direct.http.smart.examples;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.GET;
import ru.yandex.direct.http.smart.http.QueryMap;

public class GetWithQueryMap extends MockServerBase {

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getPath()).isEqualTo("/?key1=val1&key2=val2");
                return new MockResponse().setBody("ok");
            }
        };
    }

    interface Api {
        @GET("/")
        Call<String> get(@QueryMap Map<String, String> val);
    }

    @Test
    public void getRequest() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get(ImmutableMap.of("key1", "val1", "key2", "val2")).execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("ok");
    }
}
