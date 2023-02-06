package ru.yandex.market.tsum.clients.ferryman;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.tsum.clients.ferryman.model.BatchIdResponse;
import ru.yandex.market.tsum.clients.ferryman.model.BatchStatus;
import ru.yandex.market.tsum.clients.ferryman.model.BatchStatusResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class FerrymanClientMockedTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    private FerrymanClient ferrymanClient;

    @Before
    public void setUp() {
        ferrymanClient = new FerrymanClient("http://localhost:" + wireMockRule.port());
    }

    private void addJsonStub(String url, String jsonFile) {
        try {
            wireMockRule.stubFor(get(urlEqualTo(url))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(getTestResourceAsString(jsonFile))));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource", e);
        }
    }

    @Test
    public void addTableTest() throws ExecutionException, InterruptedException {
        addJsonStub("/add-table?namespace=namespace&path=path&timestamp=42&delta=false&cluster=cluster",
            "clients/ferryman/batch_id_response.json");
        BatchIdResponse response = ferrymanClient
            .addTable("namespace", "path", 42L, false, "cluster")
            .get();
        Assert.assertEquals("12345", response.getBatchId());
    }

    @Test
    public void getBatchStatusTest() throws ExecutionException, InterruptedException {
        addJsonStub("/get-batch-status?batch=12345",
            "clients/ferryman/batch_status_final_response.json");
        BatchStatusResponse response = ferrymanClient.getBatchStatus("12345").get();
        Assert.assertEquals(BatchStatus.FINAL, response.getBatchStatus());
    }

    private String getTestResourceAsString(String resourceName) throws IOException {
        return Resources.toString(Resources.getResource(resourceName), Charsets.UTF_8);
    }
}
