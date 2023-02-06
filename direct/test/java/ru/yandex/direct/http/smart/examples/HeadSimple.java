package ru.yandex.direct.http.smart.examples;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.asynchttp.Result;
import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.HEAD;

public class HeadSimple extends MockServerBase {

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("HEAD");
                softAssertions.assertThat(request.getPath()).isEqualTo("/");
                return new MockResponse().setBody("ok");
            }
        };
    }

    interface Api {
        @HEAD("/")
        Call<Void> get();
    }

    @Test
    public void getRequest() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        Result<Void> result = api.get().execute();
        softAssertions.assertThat(result.getErrors()).isNull();
    }
}
