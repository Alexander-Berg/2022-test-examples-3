package ru.yandex.direct.http.smart.examples;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.Field;
import ru.yandex.direct.http.smart.http.FieldMap;
import ru.yandex.direct.http.smart.http.FormUrlEncoded;
import ru.yandex.direct.http.smart.http.POST;

import static java.util.Arrays.asList;

public class PostForm extends MockServerBase {

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("POST");
                softAssertions.assertThat(request.getHeader("content-length")).isNotEmpty();
                softAssertions.assertThat(request.getHeader("content-type"))
                        .isEqualTo("application/x-www-form-urlencoded");
                softAssertions.assertThat(request.getPath()).isEqualTo("/");
                return new MockResponse().setBody(request.getBody().readString(Charset.defaultCharset()));
            }
        };
    }

    interface Api {
        @POST("/")
        @FormUrlEncoded
        Call<String> get(@Field("field") String val);

        @POST("/")
        @FormUrlEncoded
        Call<String> getList(@Field("field") List<String> val);

        @POST("/")
        @FormUrlEncoded
        Call<String> getArray(@Field("field") String... val);

        @POST("/")
        @FormUrlEncoded
        Call<String> get(@FieldMap Map<String, String> fields);
    }

    @Test
    public void field() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get("value").execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("field=value");
    }

    @Test
    public void fieldList() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.getList(asList("value1", "value2")).execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("field=value1&field=value2");
    }

    @Test
    public void fieldArray() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.getArray("value1", "value2").execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("field=value1&field=value2");
    }


    @Test
    public void fieldMap() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get(ImmutableMap.of("field1", "val1", "field2", "val2")).execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo("field1=val1&field2=val2");
    }
}
