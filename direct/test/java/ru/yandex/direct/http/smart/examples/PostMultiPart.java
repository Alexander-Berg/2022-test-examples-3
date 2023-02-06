package ru.yandex.direct.http.smart.examples;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.asynchttpclient.request.body.multipart.ByteArrayPart;
import org.asynchttpclient.request.body.multipart.FilePart;
import org.asynchttpclient.request.body.multipart.PartBase;
import org.asynchttpclient.request.body.multipart.StringPart;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.Multipart;
import ru.yandex.direct.http.smart.http.POST;
import ru.yandex.direct.http.smart.http.Part;
import ru.yandex.direct.http.smart.http.PartMap;

import static java.util.Arrays.asList;

public class PostMultiPart extends MockServerBase {

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("POST");
                return new MockResponse().setBody(request.getBody().readString(Charset.defaultCharset()));
            }
        };
    }

    interface Api {
        @POST("/")
        @Multipart
        Call<String> get(@Part("part") String part);

        @POST("/")
        @Multipart
        Call<String> getList(@Part("part") List<String> parts);

        @POST("/")
        @Multipart
        Call<String> getListPart(@Part List<PartBase> parts);

        @POST("/")
        @Multipart
        Call<String> getArray(@Part("part") String... parts);

        @POST("/")
        @Multipart
        Call<String> getArrayPart(@Part PartBase... parts);

        @POST("/")
        @Multipart
        Call<String> get(@Part PartBase part);

        @POST("/")
        @Multipart
        Call<String> get(@PartMap(encoding = "myencoding") Map<String, Object> parts);
    }

    @Test
    public void stringAsPart() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get("myPart").execute().getSuccess();
        softAssertions.assertThat(resp).contains("Content-Disposition: form-data; name=\"part\"");
        softAssertions.assertThat(resp).contains("Content-Type: text/plain; charset=UTF-8");
        softAssertions.assertThat(resp).contains("Content-Transfer-Encoding: binary");
        softAssertions.assertThat(resp).contains("myPart");
    }

    @Test
    public void stringPart() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get(new StringPart("part", "myPart")).execute().getSuccess();
        softAssertions.assertThat(resp).contains("Content-Disposition: form-data; name=\"part\"");
        softAssertions.assertThat(resp).contains("myPart");
    }

    @Test
    public void filePart() throws URISyntaxException {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get(new FilePart("filePart",
                getResourceAsFile(PostBody.PostBodyVariants.FILE_NAME))).execute().getSuccess();
        softAssertions.assertThat(resp)
                .contains("Content-Disposition: form-data; name=\"filePart\"; filename=\"string_body.txt\"");
        softAssertions.assertThat(resp).contains("Content-Type: text/plain");
        softAssertions.assertThat(resp).contains(PostBody.PostBodyVariants.CONTENT);
    }

    @Test
    public void byteArrayPart() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get(new ByteArrayPart("part", "myPart".getBytes())).execute().getSuccess();
        softAssertions.assertThat(resp).contains("Content-Disposition: form-data; name=\"part\"");
        softAssertions.assertThat(resp).contains("myPart");
    }

    @Test
    public void partMap() throws URISyntaxException {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.get(
                ImmutableMap.of("part1", "val1",
                        "part2", new PartBody("val2"),
                        "part3", getResourceAsFile(PostBody.PostBodyVariants.FILE_NAME),
                        "part4", "val4".getBytes()
                )).execute().getSuccess();
        softAssertions.assertThat(resp).contains("Content-Transfer-Encoding: myencoding");
        softAssertions.assertThat(resp).contains("name=\"part1\"");
        softAssertions.assertThat(resp).contains("val1");
        softAssertions.assertThat(resp).contains("name=\"part2\"");
        softAssertions.assertThat(resp).contains("val2");
        softAssertions.assertThat(resp)
                .contains("name=\"part3\"; filename=\"string_body.txt\"");
        softAssertions.assertThat(resp).contains("name=\"part4\"");
        softAssertions.assertThat(resp).contains("val4");
        softAssertions.assertThat(resp).contains(PostBody.PostBodyVariants.CONTENT);
    }

    @Test
    public void getList() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.getList(asList("val1", "val2")).execute().getSuccess();
        softAssertions.assertThat(resp).contains("Content-Disposition: form-data; name=\"part\"");
        softAssertions.assertThat(resp).contains("val1");
        softAssertions.assertThat(resp).contains("val2");
    }

    @Test
    public void getListPart() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp =
                api.getListPart(asList(new StringPart("part1", "val1"), new StringPart("part2", "val2"))).execute()
                        .getSuccess();
        softAssertions.assertThat(resp).contains("Content-Disposition: form-data; name=\"part1\"");
        softAssertions.assertThat(resp).contains("Content-Disposition: form-data; name=\"part2\"");
        softAssertions.assertThat(resp).contains("val1");
        softAssertions.assertThat(resp).contains("val2");
    }

    @Test
    public void getArrayPart() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp =
                api.getArrayPart(new StringPart("part1", "val1"), new StringPart("part2", "val2")).execute()
                        .getSuccess();
        softAssertions.assertThat(resp).contains("Content-Disposition: form-data; name=\"part1\"");
        softAssertions.assertThat(resp).contains("Content-Disposition: form-data; name=\"part2\"");
        softAssertions.assertThat(resp).contains("val1");
        softAssertions.assertThat(resp).contains("val2");
    }

    @Test
    public void getArray() {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);
        String resp = api.getArray("val1", "val2").execute().getSuccess();
        softAssertions.assertThat(resp).contains("Content-Disposition: form-data; name=\"part\"");
        softAssertions.assertThat(resp).contains("val1");
        softAssertions.assertThat(resp).contains("val2");
    }

    private static File getResourceAsFile(String resourceName) throws URISyntaxException {
        return new File(ClassLoader.getSystemResource(resourceName).toURI());
    }

    public static class PartBody {
        private String val;

        public PartBody(String val) {
            this.val = val;
        }

        @Override
        public String toString() {
            return val;
        }
    }
}
