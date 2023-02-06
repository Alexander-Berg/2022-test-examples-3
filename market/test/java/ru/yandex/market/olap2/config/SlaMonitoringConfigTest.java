package ru.yandex.market.olap2.config;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import ru.yandex.market.olap2.model.SlaCube;
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
        assertThat(noDefaults.getSlaHour(), is(8));
        assertThat(noDefaults.getSlaHourInfra(), is(12));
        assertThat(noDefaults.getOnlyWarn(), is(true));

        SlaCube defaultWarn = getCube(result, "fact_new_order_item_dict");
        assertThat(defaultWarn.getSlaHour(), is(9));
        assertThat(defaultWarn.getSlaHourInfra(), is(11));
        assertThat(defaultWarn.getOnlyWarn(), is(false));

        SlaCube defaultHour = getCube(result, "fact_order_dict");
        assertThat(defaultHour.getSlaHour(), is(11));
        assertThat(defaultHour.getSlaHourInfra(), is(12));
        assertThat(defaultHour.getOnlyWarn(), is(true));

        SlaCube allDafaults = getCube(result, "fact_cpa_order_oos");
        assertThat(allDafaults.getSlaHour(), is(11));
        assertThat(allDafaults.getOnlyWarn(), is(false));
        assertThat(allDafaults.getSlaHourInfra(), is(12));
    }

    private SlaCube getCube(List<SlaCube> result, String name) {
        return result.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }
}
