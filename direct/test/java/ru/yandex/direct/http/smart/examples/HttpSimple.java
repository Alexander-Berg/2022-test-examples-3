package ru.yandex.direct.http.smart.examples;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.HTTP;

public class HttpSimple extends MockServerBase {

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("MYMETHOD");
                softAssertions.assertThat(request.getPath()).isEqualTo("/");
                return new MockResponse().setBody("ok");
            }
        };
    }

    interface Api {
        @HTTP(method = "MYMETHOD", path = "/")
        Call<String> get();
    }

    @Test
    public void getRequest() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get().execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("ok");
    }
}
