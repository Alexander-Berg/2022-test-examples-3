package ru.yandex.direct.http.smart.examples;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.asynchttp.Result;
import ru.yandex.direct.http.smart.annotations.Id;
import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.GET;
import ru.yandex.direct.http.smart.http.Url;

public class GetWithRequestId extends MockServerBase {

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setBody("ok");
            }
        };
    }

    interface Api {
        @GET
        Call<String> get(@Url String url, @Id Long id);
    }

    @Test
    public void getRequest() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        Result<String> resp = api.get("path", 555L).execute();
        softAssertions.assertThat(resp.getId()).isEqualTo(555L);
    }
}
