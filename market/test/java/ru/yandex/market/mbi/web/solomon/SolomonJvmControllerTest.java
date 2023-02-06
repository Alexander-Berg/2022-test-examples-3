package ru.yandex.market.mbi.web.solomon;

import java.util.concurrent.CompletableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.ArraySizeComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.mbi.web.solomon.config.BaseFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolomonJvmControllerTest extends BaseFunctionalTest {
    @Autowired
    private TestRestTemplate template;

    private static final String URL = "/solomon-jvm";

    @Test
    void getJvmSensorsTest() throws JSONException {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        CompletableFuture<ResponseEntity<String>> futureResponse = CompletableFuture
                .supplyAsync(() -> template.getForEntity(URL, String.class))
                .exceptionally(e -> null);
        CompletableFuture<ResponseEntity<String>> anotherFutureResponse = CompletableFuture
                .supplyAsync(() -> template.getForEntity(URL, String.class))
                .exceptionally(e -> null);
        ResponseEntity<String> response = futureResponse.join();
        ResponseEntity<String> anotherResponse = anotherFutureResponse.join();

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(anotherResponse.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertNotNull(anotherResponse.getBody());

        ResponseEntity<String> emptyResponse = response.getBody().equals("{}") ? response : anotherResponse;
        ResponseEntity<String> notEmptyResponse = emptyResponse == response ? anotherResponse : response;
        assertEquals("{}", emptyResponse.getBody());

        String responseBody = notEmptyResponse.getBody();
        // Каждую секунду считывается 66 сенсоров, следовательно за 2 секунды должно быть
        // 132 сенсора либо больше
        JSONAssert.assertEquals("{\"sensors\":[132,10000]}", responseBody,
                new ArraySizeComparator(JSONCompareMode.LENIENT));
        JSONArray sensors = new JSONObject(responseBody).getJSONArray("sensors");
        long firstTs = sensors.getJSONObject(0).getLong("ts");
        long lastTs = sensors.getJSONObject(sensors.length() - 1).getLong("ts");
        assertTrue(firstTs < lastTs);
    }
}
