package ru.yandex.market.tsum.clients.hq;

import java.io.IOException;
import java.util.List;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.api.client.util.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.tsum.clients.hq.model.RevisionStats;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

public class HqServiceTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private HqService service;

    @Before
    public void setUp() throws IOException {
        wireMockRule.stubFor(post(urlEqualTo("/rpc/federated/FindClusters/"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(
                        Resources.toString(Resources.getResource("clients/hq/FindClusters.json"), Charsets.UTF_8)
                            .replace("{PORT}", Integer.toString(wireMockRule.port())))
            )
        );
        wireMockRule.stubFor(post(urlEqualTo("/rpc/instances/FindInstances/"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(Resources.toString(Resources.getResource("clients/hq/FindInstanceResponse.json"),
                        Charsets.UTF_8)))
        );
        HqApiClient client = new HqApiClient(
            new NettyHttpClientContext(new HttpClientConfig()),
            "http://localhost:" + wireMockRule.port() + "/"
        );
        service = new HqService(client);
    }


    @Test
    public void testGetRestarts() {
        List<RevisionStats> actual = service.getRestarts("testing_market_mbi_shop_tms_sas");
        List<RevisionStats> expected = ImmutableList.of(
            new RevisionStats(
                "sas4-6779.search.yandex.net:23239@testing_market_mbi_shop_tms_sas",
                ImmutableMap.of("nginx", 1, "mbi-shop-tms", 5)
            ),
            new RevisionStats(
                "second_instance",
                ImmutableMap.of("nginx", 1, "mbi-shop-tms", 10)
            )
        );
        assertEquals(expected, actual);
    }

}
