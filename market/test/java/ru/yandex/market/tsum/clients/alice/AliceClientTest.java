package ru.yandex.market.tsum.clients.alice;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

public class AliceClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private AliceClient aliceClient;

    @Before
    public void setUp() {
        stubFor(get(urlEqualTo("/api/drills/iva?format=text&level=network"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("test")));

        stubFor(get(urlEqualTo("/cs-admin/yandex-alice-sources" +
            "/master/alice-sources/data/texts/maintenance_work.txt"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("instructions\nyet another instructions")));

        String url = String.format("http://localhost:%d", wireMockRule.port());
        aliceClient = new AliceClient(url, url);
    }

    @Test
    public void getTrainingPlan() {
        String trainingPlan = aliceClient.getTrainingPlan("iva", AliceClient.Level.NETWORK);
        assertEquals(trainingPlan, "test");
        verify(getRequestedFor(urlMatching("/api/drills/.*")));
    }

    @Test
    public void resolveMaintancePlan() {
        String maintenancePlan = aliceClient.resolveMaintenancePlan();
        assertEquals(maintenancePlan, "%%(markdown)\ninstructions\nyet another instructions\n%%");
        verify(getRequestedFor(urlMatching(".*?maintenance_work.txt$")));
    }
}
