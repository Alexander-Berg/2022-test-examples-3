package ru.yandex.direct.http.smart.examples;

import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.GET;
import ru.yandex.direct.http.smart.http.Query;

import static java.util.Arrays.asList;

public class GetWithQueryList extends MockServerBase {

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setBody(request.getPath());
            }
        };
    }

    interface Api {
        @GET("/")
        Call<String> getList(@Query("key") List<String> val);

        @GET("/")
        Call<String> getArray(@Query("key") String... val);
    }

    @Test
    public void getList() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.getList(asList("val1", "val2")).execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("/?key=val1&key=val2");
    }

    @Test
    public void getArray() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.getArray("val1", "val2").execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("/?key=val1&key=val2");
    }
}
