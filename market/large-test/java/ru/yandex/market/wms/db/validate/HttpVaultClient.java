package ru.yandex.market.wms.db.validate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

public class HttpVaultClient implements VaultClient {
    private final Logger log = org.slf4j.LoggerFactory.getLogger(ValidateMigrationsTest.class);

    private final String token;

    public HttpVaultClient(String token) {
        this.token = token;
    }

    @Override
    public Map<String, String> getSecretEntries(String secretId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .GET()
                    .headers("Authorization", "OAuth " + token)
                    .uri(URI.create(String.format("https://vault-api.passport.yandex.net/1/versions/%s/", secretId)))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new Exception("Response code: " + response.statusCode());
            }

            ObjectMapper om = new ObjectMapper();
            JsonNode rootNode = om.readTree(response.body());
            JsonNode valueNode = rootNode.get("version").get("value");

            Map<String, String> entries = new HashMap<>();
            valueNode.iterator().forEachRemaining(n -> {
                String key = n.get("key").textValue();
                String value = n.get("value").textValue();
                entries.put(key, value);
            });

            return entries;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
