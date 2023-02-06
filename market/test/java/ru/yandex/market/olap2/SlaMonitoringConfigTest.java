package ru.yandex.market.olap2;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.olap2.config.SlaMonitoringConfig;
import ru.yandex.market.olap2.sla.SlaCube;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SlaMonitoringConfigTest {

    private static String TEST_CONF_PATH = "/configs/sla_cubes.yaml";

    @Test
    public void testReadConf() throws IOException {
        SlaMonitoringConfig conf = new SlaMonitoringConfig();
        List<SlaCube> result = conf.getCubes(TEST_CONF_PATH);

        assertThat("Wrong parse result!", result.size(), is(4));
        SlaCube noDefaults = getCube(result, "fact_new_order_dict");
        assertThat(noDefaults.getExpectedHour(), is(8));
        assertThat(noDefaults.getOnlyWarn(), is(true));

        SlaCube defaultWarn = getCube(result, "fact_new_order_item_dict");
        assertThat(defaultWarn.getExpectedHour(), is(9));
        assertThat(defaultWarn.getOnlyWarn(), is(false));

        SlaCube defaultHour = getCube(result, "fact_order_dict");
        assertThat(defaultHour.getExpectedHour(), is(11));
        assertThat(defaultHour.getOnlyWarn(), is(true));

        SlaCube allDafaults = getCube(result, "fact_cpa_order_oos");
        assertThat(allDafaults.getExpectedHour(), is(11));
        assertThat(allDafaults.getOnlyWarn(), is(false));
    }

    private SlaCube getCube(List<SlaCube> result, String name) {
        return result.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }
}
