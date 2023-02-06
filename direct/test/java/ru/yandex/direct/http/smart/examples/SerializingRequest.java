package ru.yandex.direct.http.smart.examples;

import java.nio.charset.Charset;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.http.smart.annotations.Json;
import ru.yandex.direct.http.smart.annotations.Xml;
import ru.yandex.direct.http.smart.converter.JsonRequestConverter;
import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.Body;
import ru.yandex.direct.http.smart.http.POST;

public class SerializingRequest extends MockServerBase {

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setBody(request.getBody().readString(Charset.defaultCharset()));
            }
        };
    }

    interface Api {
        @POST("/")
        Call<String> postString(@Body BodyContent body);

        @POST("/")
        Call<String> postJson(@Json @Body BodyContent body);

        @POST("/")
            //можно указать конвертер прямо в Body
        Call<String> postBodyJson(@Body(converter = JsonRequestConverter.class) BodyContent body);

        @POST("/")
            //так делать не стоит, однако приоритет у явной аннотации про конвертер
        Call<String> postBodyXmlAndJson(@Xml @Body(converter = JsonRequestConverter.class) BodyContent body);

        @POST("/")
        Call<String> postXml(@Xml @Body BodyContent body);
    }

    @Test
    public void postString() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.postString(new BodyContent("val")).execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("val");
    }

    @Test
    public void postJson() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.postJson(new BodyContent("val")).execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("{\"key\":\"val\"}");
    }

    @Test
    public void postBodyJson() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.postBodyJson(new BodyContent("val")).execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("{\"key\":\"val\"}");
    }

    @Test
    public void postBodyXmlAndJson() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.postBodyXmlAndJson(new BodyContent("val")).execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("<BodyContent><key>val</key></BodyContent>");
    }

    @Test
    public void postXml() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.postXml(new BodyContent("val")).execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("<BodyContent><key>val</key></BodyContent>");
    }

    public static class BodyContent {
        private String key;

        public BodyContent(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }
}
