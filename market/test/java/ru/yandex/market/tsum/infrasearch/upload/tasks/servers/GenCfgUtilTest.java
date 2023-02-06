package ru.yandex.market.tsum.infrasearch.upload.tasks.servers;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.market.tsum.clients.gencfg.GenCfgClient;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupInfo;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static ru.yandex.market.tsum.core.TestResourceLoader.getTestResourceAsString;

public class GenCfgUtilTest {

    private final static String RESOURCE_DIRECTORY_PATH = "infrasearch/upload/tasks/servers/util/";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    private GenCfgClient genCfgClient;

    @Before
    public void setUp() throws Exception {
        genCfgClient = new GenCfgClient("http://localhost:" + wireMockRule.port() + "/");
    }

    @Before
    public void prepareWireMock() {
        addJsonStub("/trunk/groups", "gencfg_all_groups_response.json");

        addJsonStub("/trunk/groups/SAS_MARKET_MASTER", "gencfg_master_group_response.json");
        addJsonStub("/trunk/searcherlookup/groups/SAS_MARKET_MASTER/instances", "gencfg_group_master_instances_response.json");

        addJsonStub("/trunk/groups/SAS_MARKET_SLAVE", "gencfg_slave_group_response.json");
        addJsonStub("/trunk/searcherlookup/groups/SAS_MARKET_SLAVE/instances", "gencfg_group_master_instances_response.json");

        addJsonStub("/trunk/groups/SAS_MARKET", "gencfg_market_group_response.json");
        addJsonStub("/trunk/searcherlookup/groups/SAS_MARKET/instances", "gencfg_group_market_instances_response.json");
    }

    private void addJsonStub(String url, String jsonFile) {
        try {
            wireMockRule.stubFor(get(urlEqualTo(url))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(getTestResourceAsString(RESOURCE_DIRECTORY_PATH + jsonFile))));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource", e);
        }
    }

    @Test
    public void getMarketGroupInfoTest() throws Exception {
        List<GenCfgGroupInfo> genCfgGroupInfos = GenCfgUtil.getMarketGenCfgInfos(genCfgClient);
        Assert.assertEquals(2, genCfgGroupInfos.size());
        List<String> genCfgGroupNames =  genCfgGroupInfos.stream().map(GenCfgGroupInfo::getName).collect(Collectors.toList());
        Assert.assertTrue(genCfgGroupNames.contains("SAS_MARKET"));
        Assert.assertTrue(genCfgGroupNames.contains("SAS_MARKET_MASTER"));
    }
}
