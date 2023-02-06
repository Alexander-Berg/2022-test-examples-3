package ru.yandex.direct.http.smart.examples;

import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.GET;
import ru.yandex.direct.http.smart.http.QueryName;

import static java.util.Arrays.asList;

public class GetWithQueryName extends MockServerBase {
    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getPath()).isEqualTo("/?key1&key2");
                return new MockResponse().setBody("ok");
            }
        };
    }

    interface Api {
        @GET("/")
        Call<String> getTwo(@QueryName String key1, @QueryName String key2);

        @GET("/")
        Call<String> getList(@QueryName List<String> lst);

        @GET("/")
        Call<String> getArray(@QueryName String... arr);
    }

    @Test
    public void getTwo() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.getTwo("key1", "key2").execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("ok");
    }

    @Test
    public void getList() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.getList(asList("key1", "key2")).execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("ok");
    }

    @Test
    public void getArray() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.getArray("key1", "key2").execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("ok");
    }
}
