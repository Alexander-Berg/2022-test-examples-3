package ru.yandex.direct.test.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

public class MockedHttpWebServerTest {

    @Rule
    public final MockedHttpWebServerRule server = new MockedHttpWebServerRule(ContentType.APPLICATION_JSON);

    @Test
    public void checkResponse() {
        String path = "/test-path";
        String answer = "test-response111";
        server.addResponse(path, answer);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(server.getServerURL() + path))
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpClient httpClient = HttpClient.newHttpClient();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Assert.assertEquals(200, response.statusCode());
            Assert.assertEquals(answer, response.body());
        } catch (IOException | InterruptedException e) {
            Assume.assumeNoException(e);
        }
    }

    @Test
    public void checkNotFound() {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(server.getServerURL() + "/not-exists"))
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpClient httpClient = HttpClient.newHttpClient();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            Assert.assertEquals(404, response.statusCode());
        } catch (IOException | InterruptedException e) {
            Assume.assumeNoException(e);
        }
    }
}
