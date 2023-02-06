package ru.yandex.market.tsum.clients.platform.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Sergey Filippov <rolenof@yandex-team.ru>
 */
public class EnvironmentRequestTest {

    @Test
    public void getObjectId() throws IOException {
        final EnvironmentRequest environmentRequest = readJson();
        assertEquals("tools.startrek-api-stand.dev-rolenof", environmentRequest.getObjectId());
    }

    @Test
    public void setComment() throws IOException {
        final EnvironmentRequest environmentRequest = readJson();
        environmentRequest.setComment("my-comment");

        assertEquals("my-comment", new JsonParser().parse(environmentRequest.toString()).getAsJsonObject().get(
            "comment").getAsString());
    }

    @Test
    public void setComponentRepository() throws IOException {
        final EnvironmentRequest environmentRequest = readJson();
        environmentRequest.setComponentRepository("ctash-st", "repo-222", "hash:18");

        final JsonObject result = new JsonParser().parse(environmentRequest.toString()).getAsJsonObject();
        final JsonObject componentProperties =
            result.getAsJsonArray("components").get(0).getAsJsonObject().get("properties").getAsJsonObject();
        assertEquals("repo-222", componentProperties.get("repository").getAsString());
        assertEquals("hash:18", componentProperties.get("hash").getAsString());
    }

    @Test(expected = NoSuchElementException.class)
    public void throwNoSuchElementWhenNoSuchComponent() throws IOException {
        final EnvironmentRequest environmentRequest = readJson();
        environmentRequest.setComponentRepository("ctash-st-123", "repo-222", "hash:18");
    }

    private EnvironmentRequest readJson() throws IOException {
        final EnvironmentRequest environmentRequest;
        try (InputStream inputStream = EnvironmentRequestTest.class.getClassLoader().getResourceAsStream("clients" +
            "/platform/env.json")) {
            final JsonObject jObject = new JsonParser().parse(new InputStreamReader(inputStream)).getAsJsonObject();
            environmentRequest = new EnvironmentRequest(jObject);
        }
        return environmentRequest;
    }
}
