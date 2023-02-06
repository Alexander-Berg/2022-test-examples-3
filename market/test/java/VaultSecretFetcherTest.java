package ru.yandex.market.starter.tvm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.yandex.market.starter.tvm.secret.VaultSecretFetcher;

import java.io.IOException;
import java.io.InputStream;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest
@ContextConfiguration(classes = VaultSecretFetcher.class)
@AutoConfigureWebClient(registerRestTemplate=true)
public class VaultSecretFetcherTest {

    @Autowired
    private VaultSecretFetcher vaultSecretFetcher;

    @Autowired
    private MockRestServiceServer vaultMock;

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void fetchSecretTest() throws IOException {
        final String secretId = "sec-test111";
        final String secret = "testSecret";
        final String versionId = "ver-test111";
        final String vaultToken = "kmnxckvinklcxnvxc";

        final JsonNode secretJson;
        try (final InputStream is = getClass().getClassLoader().getResourceAsStream("secretResponse.json")) {
            secretJson = mapper.readTree(is);
        }

        final JsonNode versionJson;
        try (final InputStream is = getClass().getClassLoader().getResourceAsStream("versionResponse.json")) {
            versionJson = mapper.readTree(is);
        }

        vaultMock
            .expect(requestTo("/1/secrets/" + secretId))
            .andExpect(header("Authorization", "OAuth " + vaultToken))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(secretJson.toString(), MediaType.APPLICATION_JSON));

        vaultMock
            .expect(requestTo("/1/versions/" + versionId))
            .andExpect(header("Authorization", "OAuth " + vaultToken))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(versionJson.toString(), MediaType.APPLICATION_JSON));

        Assertions.assertEquals(secret, vaultSecretFetcher.fetchSecret(vaultToken, secretId));
    }
}
