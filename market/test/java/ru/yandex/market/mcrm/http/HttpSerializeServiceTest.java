package ru.yandex.market.mcrm.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.mcrm.utils.SerializationUtils;
import ru.yandex.market.mcrm.utils.serialize.ObjectSerializeService;

public class HttpSerializeServiceTest {
    ObjectSerializeService service;

    @BeforeEach
    public void setUp() {
        service = SerializationUtils.defaultObjectSerializeService();
    }

    @Test
    public void httpPath() {
        String path = Randoms.string();
        Http request = Http.get().path(path);

        Http deserialized = serializeAndDeserialize(request, Http.class);

        Assertions.assertEquals(path, deserialized.getPath());
    }

    @Test
    public void httpAttribute() {
        String key = Randoms.string();
        String value = Randoms.string();
        Http request = Http.get().attribute(key, value);

        Http deserialized = serializeAndDeserialize(request, Http.class);

        Assertions.assertEquals(value, deserialized.getAttribute(key));
    }

    @Test
    public void httpQueryParameter() {
        String key = Randoms.string();
        String value = Randoms.string();
        Http request = Http.get().queryParameter(key, value);

        Http deserialized = serializeAndDeserialize(request, Http.class);

        Http.NamedValue actual = deserialized.getQueryParameters().get(0);
        Assertions.assertEquals(key, actual.getName());
        Assertions.assertEquals(value, actual.getValue());
        Assertions.assertFalse(actual.isSecured());
    }

    @Test
    public void httpSecuredQueryParameter() {
        String key = Randoms.string();
        String value = Randoms.string();
        Http request = Http.get().securedQueryParameter(key, value);

        Http deserialized = serializeAndDeserialize(request, Http.class);

        Http.NamedValue actual = deserialized.getQueryParameters().get(0);
        Assertions.assertEquals(key, actual.getName());
        Assertions.assertEquals(value, actual.getValue());
        Assertions.assertTrue(actual.isSecured());
    }

    @Test
    public void httpHeader() {
        String key = Randoms.string();
        String value = Randoms.string();
        Http request = Http.get().header(key, value);

        Http deserialized = serializeAndDeserialize(request, Http.class);

        Http.NamedValue actual = deserialized.getHeaders().get(0);
        Assertions.assertEquals(key, actual.getName());
        Assertions.assertEquals(value, actual.getValue());
        Assertions.assertFalse(actual.isSecured());
    }

    @Test
    public void httpSecuredHeader() {
        String key = Randoms.string();
        String value = Randoms.string();
        Http request = Http.get().securedHeader(key, value);

        Http deserialized = serializeAndDeserialize(request, Http.class);

        Http.NamedValue actual = deserialized.getHeaders().get(0);
        Assertions.assertEquals(key, actual.getName());
        Assertions.assertEquals(value, actual.getValue());
        Assertions.assertTrue(actual.isSecured());
    }

    @Test
    public void httpBody() {
        byte[] value = CrmStrings.getBytes(Randoms.string());
        Http request = Http.get().body(value);

        Http deserialized = serializeAndDeserialize(request, Http.class);

        Assertions.assertArrayEquals(value, deserialized.getBody());
    }

    @Test
    public void httpMethod() {
        Http request = Http.get();

        Http deserialized = serializeAndDeserialize(request, Http.class);

        Assertions.assertEquals(request.getHttpMethod(), deserialized.getHttpMethod());
    }

    @Test
    public void parseUrl() {
        String urlWithoutParameters = "https://storage-ru1-radosgw2.voximplant.com/" +
                "vox-records/2019/04/12/ZWYxMDc1MTMwYWQ3M2VhNTViNzk4MjJiNDFjMmM1YTkvaHR0cDovL3d3dy1ydS" +
                "0yNi0yMC52b3hpbXBsYW50LmNvbS9yZWNvcmRzLzIwMTkvMDQvMTIvMmUzM2Y1MzZhZTgzYmZmNC4xNTU1MDU1" +
                "ODM3LjE3OTY5MDAubXAz";
        String fullUrl = urlWithoutParameters + "?record_id=123916861";

        String ignoredGivenBaseUrl = "http://example.com";

        Http request = Http.get().parseUrl(fullUrl);

        Assertions.assertEquals(fullUrl, request.toString(ignoredGivenBaseUrl));
        Assertions.assertEquals(urlWithoutParameters, request.getUrl(ignoredGivenBaseUrl));
    }

    @Test
    public void parseUrlWithEncodedPath() {
        String urlWithoutParameters = "http://80.67.252.78:8088/naumb/%7B40eb4f80-8703-46b8-ac38-4e948673e1e2%7D.jpeg";
        String url = urlWithoutParameters + "?catRange=global&filename=screenshot0.jpeg&anonymous";

        String ignoredGivenBaseUrl = "http://example.com";

        Http request = Http.get().parseUrl(url);

        Assertions.assertEquals(url, request.toString(ignoredGivenBaseUrl));
        Assertions.assertEquals(urlWithoutParameters, request.getUrl(ignoredGivenBaseUrl));
    }

    private <T> T serializeAndDeserialize(T value, Class<T> type) {
        byte[] serialized = service.serialize(value);
        System.out.println(CrmStrings.valueOf(serialized));
        return service.deserialize(serialized, type);
    }
}
