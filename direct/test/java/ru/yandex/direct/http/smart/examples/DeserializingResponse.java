package ru.yandex.direct.http.smart.examples;

import java.lang.reflect.Type;
import java.nio.charset.Charset;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.asynchttpclient.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.direct.http.smart.annotations.Json;
import ru.yandex.direct.http.smart.annotations.ResponseHandler;
import ru.yandex.direct.http.smart.annotations.Xml;
import ru.yandex.direct.http.smart.converter.ResponseChecker;
import ru.yandex.direct.http.smart.converter.ResponseConverter;
import ru.yandex.direct.http.smart.converter.ResponseConverterFactory;
import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.Body;
import ru.yandex.direct.http.smart.http.POST;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DeserializingResponse extends MockServerBase {

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setBody(request.getBody().readString(Charset.defaultCharset()))
                        .setHeader("header1", "value1");
            }
        };
    }

    interface Api {
        @POST("/")
        @Xml
        Call<BodyContent> getXml(@Body String body);

        @POST("/")
        @Json
        Call<BodyContent> getJson(@Body String body);

        @POST("/")
        @ResponseHandler(parserClass = CustomParser.class, expectedCodes = 200)
        Call<CustomBody> getCustom(@Body String body);

        @POST("/")
        @ResponseHandler(checkerClass = CustomResponseChecker.class)
        Call<String> getCustomCheck(@Body String body);
    }

    @Test
    public void asJson() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        BodyContent resp = api.getJson("{\"key\":\"val\"}").execute().getSuccess();
        softAssertions.assertThat(resp.getKey()).isEqualTo("val");
    }

    @Test
    public void asXml() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        BodyContent resp = api.getXml("<BodyContent><key>val</key></BodyContent>").execute().getSuccess();
        softAssertions.assertThat(resp.getKey()).isEqualTo("val");
    }

    @Test
    public void custom() {
        Smart smart = builder()
                .withResponseConverterFactory(
                        ResponseConverterFactory.builder()
                                .addConverters(new CustomParser())
                                .build()
                )
                .build();
        Api api = smart.create(Api.class);
        CustomBody resp = api.getCustom("123456789").execute().getSuccess();
        softAssertions.assertThat(resp.length).isEqualTo(9);
    }

    @Test
    public void customResponseCheck() {
        CustomResponseChecker checker = Mockito.spy(new CustomResponseChecker());
        Smart smart = builder()
                .withResponseConverterFactory(ResponseConverterFactory.builder()
                        .addChecker(CustomResponseChecker.class, checker)
                        .build())
                .build();
        Api api = smart.create(Api.class);
        String resp = api.getCustomCheck("body").execute().getSuccess();
        verify(checker, times(1)).check(any());
    }

    public static class BodyContent {
        private String key;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    public static class CustomBody {
        private int length;
    }

    public static class CustomParser implements ResponseConverter<CustomBody> {
        @Override
        public CustomBody convert(Response response, Type responseType) {
            CustomBody res = new CustomBody();
            res.length = response.getResponseBody().length();
            return res;
        }
    }

    public static class CustomResponseChecker implements ResponseChecker {
        @Override
        public boolean check(Response response) {
            return response.hasResponseHeaders();
        }
    }
}
