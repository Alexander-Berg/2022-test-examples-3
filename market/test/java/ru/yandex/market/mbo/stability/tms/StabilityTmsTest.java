package ru.yandex.market.mbo.stability.tms;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.mbo.stability.tms.solomon.config.Config;
import ru.yandex.market.mbo.stability.tms.solomon.config.ConfigReader;
import ru.yandex.market.solomon.TimeSeries;

public class StabilityTmsTest {

    String testConfig = "name: MBOC stability\n" +
            "id: market.mbo.mboc.stability\n" +
            "version: 5\n" +
            "solomonProject: market-mbo\n" +
            "solomonCluster: autoPush\n" +
            "solomonService: mbo-stability\n" +
            "weight: 100\n" +
            "sub:\n" +
            "  - name: MBO Category UI\n" +
            "    id: ui\n" +
            "    weight: 200\n" +
            "    expressionSensor: project='market-mbo', cluster='production', service='nginx-rtc', " +
            "sensor='mbo.mbo-category-ui.nginx.errors.3xx.total'\n" +
            "    expressionThreshold: 0.01\n" +
            "    expressionOverThresholdIs1: false\n" +
            "  - name: MBO Category TMS\n" +
            "    id: tms\n" +
            "    weight: 100\n" +
            "    expressionSensor: project='market-mbo', cluster='mbo_category_tms_micrometer_production', " +
            "service='mbo_category_tms_micrometer', sensor='hikaricp_connections', host='-', pool='MBOC Postgres'\n" +
            "    expressionThreshold: 60\n" +
            "    expressionOverThresholdIs1: true\n" +
            "  - name: MBO Category - Juggler Checks\n" +
            "    id: juggler\n" +
            "    weight: 150\n" +
            "    expressionSolomonProject: juggler\n" +
            "    expression: ({project='juggler', cluster='checks', service='push', market_branch='mbo', " +
            "market_mbo_environment='production', juggler_host='mbo_category', market_mbo_service='mbo-category', " +
            "status='ok', juggler_service='production_market_mbo_category'})\n";

    private static Logger log = LoggerFactory.getLogger(StabilityTmsTest.class);

    @Test
    public void testBuildSolomonExpression() throws Exception {
        InputStream configIs = new ByteArrayInputStream(testConfig.getBytes());
        Config config = ConfigReader.readConfiguration(configIs);

        String expression = config.getRootNode().buildSolomonExpression(config.getRootNode());

        Assert.assertEquals("((200.0*({project='market-mbo',cluster='autoPush',service='mbo-stability',sensor='market" +
                ".mbo.mboc.stability.ui',version='v5',average='none'}))+(100.0*({project='market-mbo'," +
                "cluster='autoPush',service='mbo-stability',sensor='market.mbo.mboc.stability.tms',version='v5'," +
                "average='none'}))+(150.0*({project='market-mbo',cluster='autoPush',service='mbo-stability'," +
                "sensor='market.mbo.mboc.stability.juggler',version='v5',average='none'})))/450.0", expression);
    }

    @Test
    public void testLinearInterpolation() {
        TimeSeries orig = new TimeSeries();
        orig.put(Instant.ofEpochMilli(1000), 1.0);
        orig.put(Instant.ofEpochMilli(5000), 0.0);

        TimeSeries interpolated = orig.linearInterpolate(
                Instant.ofEpochMilli(0L),
                Instant.ofEpochMilli(6000),
                1
        );

        Assert.assertTrue("interpolated.size() != 7: " + interpolated.size(),
                interpolated.size() == 7);
        Assert.assertTrue("interpolated.get(0L) != 1.0: " + interpolated.get(Instant.ofEpochMilli(0L)),
                interpolated.get(Instant.ofEpochMilli(0L)) == 1.0);
        Assert.assertTrue("interpolated.get(1000LL) != 1.0: " + interpolated.get(Instant.ofEpochMilli(1000L)),
                interpolated.get(Instant.ofEpochMilli(1000L)) == 1.0);
        Assert.assertTrue("interpolated.get(2000L) != 0.75: " + interpolated.get(Instant.ofEpochMilli(2000L)),
                interpolated.get(Instant.ofEpochMilli(2000L)) == 0.75);
        Assert.assertTrue("interpolated.get(3000L) != 0.5: " + interpolated.get(Instant.ofEpochMilli(3000L)),
                interpolated.get(Instant.ofEpochMilli(3000L)) == 0.5);
        Assert.assertTrue("interpolated.get(4000L) != 0.25: " + interpolated.get(Instant.ofEpochMilli(4000L)),
                interpolated.get(Instant.ofEpochMilli(4000L)) == 0.25);
        Assert.assertTrue("interpolated.get(5000L) != 0.0: " + interpolated.get(Instant.ofEpochMilli(5000L)),
                interpolated.get(Instant.ofEpochMilli(5000L)) == 0.0);
        Assert.assertTrue("interpolated.get(6000L) != 0.0: " + interpolated.get(Instant.ofEpochMilli(6000L)),
                interpolated.get(Instant.ofEpochMilli(6000L)) == 0.0);
    }

    @Ignore
    @Test
    public void testReadConfigs() {
        List<Config> configs = new ArrayList<>();
        try {
            configs = ConfigReader.readConfigs();
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            Assert.fail(sw.toString());
        }

        Assert.assertEquals(1, configs.size());
        Assert.assertEquals("market.mbo.stability", configs.get(0).getRootNode().getId());
    }
}
