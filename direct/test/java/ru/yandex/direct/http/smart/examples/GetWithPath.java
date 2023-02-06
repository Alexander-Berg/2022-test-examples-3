package ru.yandex.direct.http.smart.examples;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.GET;
import ru.yandex.direct.http.smart.http.Path;

public class GetWithPath extends MockServerBase {

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("GET");
                return new MockResponse().setBody(request.getPath());
            }
        };
    }

    interface Api {
        @GET("/{id1}/item/{id2}")
        Call<String> get(@Path("id1") String id1, @Path("id2") String id2);

        @GET("/{id1}/item/{id2}/")
        Call<String> get2(@Path("id1") String id1, @Path("id2") String id2);
    }

    @Test
    public void getRequest() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get("111", "222").execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("/111/item/222");
    }

    @Test
    public void getRequestWithSlashAtTheEnd() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get2("111", "222").execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("/111/item/222/");
    }
}
