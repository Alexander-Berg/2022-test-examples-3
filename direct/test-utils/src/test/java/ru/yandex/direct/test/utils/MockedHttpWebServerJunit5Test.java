package ru.yandex.direct.test.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MockedHttpWebServerJunit5Test {
    @RegisterExtension
    public static MockedHttpWebServerExtention server = new MockedHttpWebServerExtention(ContentType.APPLICATION_XML);

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
            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertEquals(answer, response.body());
        } catch (IOException | InterruptedException e) {
            Assertions.fail(e);
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
            Assertions.assertEquals(404, response.statusCode());
        } catch (IOException | InterruptedException e) {
            Assertions.fail(e);
        }
    }
}
