package ru.yandex.market.tsum.clients.nanny;

import java.io.IOException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.tsum.clients.nanny.its.ItsValue;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

public class NannyItsClientTest {
    private static class TestData {
        private final String key;

        TestData(String key) {
            this.key = key;
        }

        String getKey() {
            return key;
        }
    }

    private static class TestDataAdapter extends TypeAdapter<TestData> {
        @Override
        public void write(JsonWriter out, TestData value) throws IOException {
            out.value(value.getKey());
        }

        @Override
        public TestData read(JsonReader in) throws IOException {
            return new TestData(in.nextString());
        }
    }

    private NannyItsClient itsClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        String url = String.format("http://localhost:%d", wireMockRule.port());
        itsClient = new NannyItsClient(url, "FAKE-TOKEN", new NettyHttpClientContext(new HttpClientConfig()));
    }

    @Test
    public void testReadValue() {
        // arrange
        stubFor(get(urlEqualTo("/v1/values/my/location/ruchka/"))
            .withHeader("Authorization", equalTo("OAuth FAKE-TOKEN"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"user_value\": \"{\\\"key\\\": \\\"value\\\"}\"}")
                .withHeader("ETag", "etag12")
            )
        );

        // act
        ItsValue<TestData> itsValue = itsClient.readValue("my/location", "ruchka", TestData.class);

        // assert
        assertEquals("value", itsValue.getUserValue().getKey());
        assertEquals("etag12", itsValue.getETag());
    }

    @Test
    public void testReadValueWithError() {
        // arrange
        stubFor(get(urlEqualTo("/v1/values/my/location/ruchka/"))
            .withHeader("Authorization", equalTo("OAuth FAKE-TOKEN"))
            .willReturn(aResponse()
                .withStatus(404)
                .withBody("Not found")
            )
        );

        // assert
        exception.expect(RuntimeException.class);

        // act
        itsClient.readValue("my/location", "ruchka", TestData.class);
    }

    @Test
    public void testWriteValue() {
        // arrange
        stubFor(post(urlEqualTo("/v1/values/my/location/ruchka/"))
            .withHeader("Authorization", equalTo("OAuth FAKE-TOKEN"))
            .withHeader("If-Match", equalTo("etag13"))
            .withRequestBody(equalToJson("{\"value\": \"{\\n  \\\"key\\\": \\\"value\\\"\\n}\"}"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("ETag", "etag14")
            )
        );

        // act
        ItsValue<TestData> itsValue = new ItsValue(new TestData("value"), "etag13");
        itsClient.writeValue("my/location", "ruchka", itsValue);

        // assert
        assertEquals("value", itsValue.getUserValue().getKey());
        assertEquals("etag14", itsValue.getETag());
    }

    @Test
    public void testWriteValueWithError() {
        // arrange
        stubFor(post(urlEqualTo("/v1/values/my/location/ruchka/"))
            .withHeader("Authorization", equalTo("OAuth FAKE-TOKEN"))
            .withHeader("If-Match", equalTo("etag13"))
            .withRequestBody(equalToJson("{\"value\": \"{\\n  \\\"key\\\": \\\"value\\\"\\n}\"}"))
            .willReturn(aResponse()
                .withStatus(500)
            )
        );

        // act
        exception.expect(RuntimeException.class);

        ItsValue<TestData> itsValue = new ItsValue(new TestData("value"), "etag13");
        itsClient.writeValue("my/location", "ruchka", itsValue);

        // assert
        assertEquals("value", itsValue.getUserValue().getKey());
        assertEquals("etag14", itsValue.getETag());
    }

    @Test
    public void testReadValueWithCustomGSON() {
        // arrange
        stubFor(get(urlEqualTo("/v1/values/my/location/ruchka/"))
            .withHeader("Authorization", equalTo("OAuth FAKE-TOKEN"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"user_value\": \"\\\"value\\\"\"}")
                .withHeader("ETag", "etag12")
            )
        );

        // act
        Gson gson = new GsonBuilder().registerTypeAdapter(TestData.class, new TestDataAdapter()).create();
        ItsValue<TestData> itsValue = itsClient.readValue("my/location", "ruchka", TestData.class, gson);

        // assert
        assertEquals("value", itsValue.getUserValue().getKey());
        assertEquals("etag12", itsValue.getETag());
    }

    @Test
    public void testWriteValueWithCustomGSON() {
        // arrange
        stubFor(post(urlEqualTo("/v1/values/my/location/ruchka/"))
            .withHeader("Authorization", equalTo("OAuth FAKE-TOKEN"))
            .withHeader("If-Match", equalTo("etag13"))
            .withRequestBody(equalToJson("{\"value\": \"\\\"value\\\"\"}"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("ETag", "etag14")
            )
        );

        // act
        Gson gson = new GsonBuilder().registerTypeAdapter(TestData.class, new TestDataAdapter()).create();
        ItsValue<TestData> itsValue = new ItsValue(new TestData("value"), "etag13");
        itsClient.writeValue("my/location", "ruchka", itsValue, gson);

        // assert
        assertEquals("value", itsValue.getUserValue().getKey());
        assertEquals("etag14", itsValue.getETag());
    }

}
