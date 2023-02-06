package ru.yandex.direct.http.smart.examples;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.http.smart.annotations.Proto;
import ru.yandex.direct.http.smart.annotations.ProtoAsJson;
import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.Body;
import ru.yandex.direct.http.smart.http.POST;
import ru.yandex.direct.utils.JsonUtils;

public class PostBody {

    interface Api {
        @POST("/")
        Call<String> post(@Body String body);

        @POST("/")
        Call<String> post(@Body File body);

        @POST("/")
        Call<String> post(@Body InputStream body);

        @POST("/")
        Call<String> post(@Body byte[] body);
    }

    @Nested
    class PostBodyVariants extends MockServerBase {

        public static final String CONTENT = "requestContent";
        public static final String FILE_NAME = "ru/yandex/direct/http/smart/examples/string_body.txt";

        @Override
        public Dispatcher dispatcher() {
            return new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    softAssertions.assertThat(request.getPath()).isEqualTo("/");
                    softAssertions.assertThat(request.getMethod()).isEqualTo("POST");
                    return new MockResponse().setBody(request.getBody().readString(Charset.defaultCharset()));
                }
            };
        }


        @Test
        public void postString() {
            Smart smart = builder().build();
            Api api = smart.create(Api.class);
            String resp = api.post(CONTENT).execute().getSuccess();
            softAssertions.assertThat(resp).isEqualTo(CONTENT);
        }

        @Test
        public void postFile() throws URISyntaxException {
            Smart smart = builder().build();
            Api api = smart.create(Api.class);
            String resp = api.post(getResourceAsFile(FILE_NAME)).execute().getSuccess();
            softAssertions.assertThat(resp).isEqualTo(CONTENT);
        }

        @Test
        public void postStream() {
            Smart smart = builder().build();
            Api api = smart.create(Api.class);
            String resp = api.post(new ByteArrayInputStream(CONTENT.getBytes())).execute().getSuccess();
            softAssertions.assertThat(resp).isEqualTo(CONTENT);
        }

        @Test
        public void postBytes() {
            Smart smart = builder().build();
            Api api = smart.create(Api.class);
            String resp = api.post(CONTENT.getBytes()).execute().getSuccess();
            softAssertions.assertThat(resp).isEqualTo(CONTENT);
        }

        private File getResourceAsFile(String resourceName) throws URISyntaxException {
            return new File(ClassLoader.getSystemResource(resourceName).toURI());
        }
    }

    interface ProtoJsonApi {
        @POST("/")
        @ProtoAsJson
        Call<Sample.SampleProto> postProtoJson(@ProtoAsJson @Body Sample.SampleProto message);

        @POST("/")
        @ProtoAsJson
        Call<List<Sample.SampleProto>> postProtoCollectionJson(@ProtoAsJson @Body List<Sample.SampleProto> messageList);
    }

    @Nested
    class PostBodyProtoAsJson extends MockServerBase {

        private String actualRequestBody;

        @Override
        public Dispatcher dispatcher() {
            return new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    try (var body = request.getBody().clone()) {
                        actualRequestBody = body.readString(StandardCharsets.UTF_8);
                    }
                    return new MockResponse().setBody(request.getBody());
                }
            };
        }


        Sample.SampleProto message = Sample.SampleProto.newBuilder()
                .setBooleanVal(true)
                .setStringVal("str_val")
                .setInt32Val(500).build();

        @Test
        public void postProtoJson() {
            Smart smart = builder().build();
            ProtoJsonApi api = smart.create(ProtoJsonApi.class);
            var expectedRequestBody = JsonUtils.toJson(message);
            Sample.SampleProto resp = api.postProtoJson(message).execute().getSuccess();
            softAssertions.assertThat(actualRequestBody).isEqualTo(expectedRequestBody);
            softAssertions.assertThat(resp).isEqualTo(message);
        }

        @Test
        public void postProtoCollectionJson() {
            var message1 = Sample.SampleProto.newBuilder().mergeFrom(message).setInt32Val(1).build();
            var message2 = Sample.SampleProto.newBuilder().mergeFrom(message).setInt32Val(2).build();
            List<Sample.SampleProto> messageList = List.of(message1, message2);
            var stringMessage1 = JsonUtils.toJson(message1);
            var stringMessage2 = JsonUtils.toJson(message2);

            Smart smart = builder().build();
            ProtoJsonApi api = smart.create(ProtoJsonApi.class);
            List<Sample.SampleProto> resp = api.postProtoCollectionJson(messageList).execute().getSuccess();
            softAssertions.assertThat(actualRequestBody).contains(stringMessage1);
            softAssertions.assertThat(actualRequestBody).contains(stringMessage2);
            softAssertions.assertThat(resp).isEqualTo(messageList);
        }
    }

    public interface ProtoApi {
        @POST("/")
        @Proto
        Call<Sample.SampleProto> postProto(@Proto @Body Sample.SampleProto message);

        @POST("/")
        @Proto
        Call<List<Sample.SampleProto>> postProtoCollection(@Proto @Body List<Sample.SampleProto> messageList);
    }

    @Nested
    class PostBodyProto extends MockServerBase {
        @Override
        public Dispatcher dispatcher() {
            return new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    return new MockResponse().setBody(request.getBody());
                }
            };
        }

        Sample.SampleProto message = Sample.SampleProto.newBuilder()
                .setBooleanVal(true)
                .setStringVal("str_val")
                .setInt32Val(500).build();

        @Test
        public void postProto() {
            Smart smart = builder().build();
            ProtoApi api = smart.create(ProtoApi.class);
            Sample.SampleProto resp = api.postProto(message).execute().getSuccess();
            softAssertions.assertThat(resp).isEqualTo(message);
        }

        @Test
        public void postProtoCollection() {
            List<Sample.SampleProto> messageList = List.of(
                    Sample.SampleProto.newBuilder().mergeFrom(message).setInt32Val(1).build(),
                    Sample.SampleProto.newBuilder().mergeFrom(message).setInt32Val(2).build());
            Smart smart = builder().build();
            ProtoApi api = smart.create(ProtoApi.class);
            List<Sample.SampleProto> resp = api.postProtoCollection(messageList).execute().getSuccess();
            softAssertions.assertThat(resp).isEqualTo(messageList);
        }
    }
}
