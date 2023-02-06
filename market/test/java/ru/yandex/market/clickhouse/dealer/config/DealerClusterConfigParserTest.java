package ru.yandex.market.clickhouse.dealer.config;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Aleksei Malygin <a href="mailto:Malygin-Me@yandex-team.ru"></a>
 * Date: 2019-01-18
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class DealerClusterConfigParserTest {

    @Autowired
    PropertyResolver propertyResolver;

    @Test
    public void parseConfig() throws Exception {
        DealerClusterConfigParser parser = new DealerClusterConfigParser(propertyResolver::resolveRequiredPlaceholders);
        Collection<DealerClusterConfig> actualConfig = parser.parseConfig(readJsonObject("/config/clusters" +
            "/test_health_market_cluster.json"), null);

        Collection<DealerClusterConfig> expectedClustersConfig = Arrays.asList(
            getClusterConfig("test_market_health", "test_market_health_next"),
            getClusterConfig("stable_market_health", "stable_market_health_next")
        );

        Assert.assertEquals(expectedClustersConfig, actualConfig);
    }

    private DealerClusterConfig getClusterConfig(String clusterId, String clusterForDdlApply) {
        return DealerClusterConfig.newBuilder()
            .withClusterId(clusterId)
            .withClusterForDdlApply(clusterForDdlApply)
            .build();
    }

    private JsonObject readJsonObject(String path) throws Exception {
        try (Reader confReader = new InputStreamReader(getClass().getResourceAsStream(path))) {
            return new Gson().fromJson(confReader, JsonObject.class);
        }
    }
}
