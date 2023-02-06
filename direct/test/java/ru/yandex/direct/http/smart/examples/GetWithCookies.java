package ru.yandex.direct.http.smart.examples;

import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.Cookie;
import ru.yandex.direct.http.smart.http.GET;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.catchThrowable;

public class GetWithCookies extends MockServerBase {

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setBody(request.getHeaders().get("Cookie"));
            }
        };
    }

    interface Api {
        @GET("/")
        Call<String> get(@Cookie(value = "cookie1") String cookie);

        @GET("/")
        Call<String> getList(@Cookie(value = "cookie1") List<String> cookies);

        @GET("/")
        Call<String> getArray(@Cookie(value = "cookie1") String... cookies);
    }

    @Test
    public void cookie() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get("value1").execute().getSuccess();
        softAssertions.assertThat(resp).contains("cookie1=value1");
    }

    @Test
    public void cookieList() {
        Throwable thrown = catchThrowable(() -> {
                    Smart smart = builder().build();
                    Api api = smart.create(Api.class);
                    api.getList(asList("value1", "value2")).execute();
                }
        );

        softAssertions.assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("@Cookie parameter must be a scalar (parameter #1)\n" +
                        "    for method Api.getList");

    }

    @Test
    public void cookieArray() {
        Throwable thrown = catchThrowable(() -> {
                    Smart smart = builder().build();
                    Api api = smart.create(Api.class);
                    api.getArray("value1", "value2").execute();
                }
        );

        softAssertions.assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("@Cookie parameter must be a scalar (parameter #1)\n" +
                        "    for method Api.getArray");
    }
}
