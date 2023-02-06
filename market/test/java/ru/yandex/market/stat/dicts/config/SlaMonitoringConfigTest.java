package ru.yandex.market.stat.dicts.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import ru.yandex.market.stat.dicts.common.ShameDict;
import ru.yandex.market.stat.dicts.common.SlaDict;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SlaMonitoringConfigTest {
    private static String TEST_CONF_PATH = "/configs/sla-monitorings-mstat.yaml";
    private static String TEST_CONF_SHAME_PATH = "/configs/size-monitoring-shame-list.yaml";

    @Test
    public void testReadConf() throws IOException {
        SlaMonitoringConfig conf = new SlaMonitoringConfig();
        List<SlaDict> result = conf.getDicts(TEST_CONF_PATH);

        assertThat("Wrong parse result!", result.size(), is(4));
        SlaDict noDefaults = getCube(result, "dict1");
        assertThat(noDefaults.getSlaHour(), is(8));
        assertThat(noDefaults.getOnlyWarn(), is(true));

        SlaDict defaultWarn = getCube(result, "dict2");
        assertThat(defaultWarn.getSlaHour(), is(9));
        assertThat(defaultWarn.getOnlyWarn(), is(false));

        SlaDict defaultHour = getCube(result, "dict3");
        assertThat(defaultHour.getSlaHour(), is(11));
        assertThat(defaultHour.getOnlyWarn(), is(true));

        SlaDict allDafaults = getCube(result, "dict4");
        assertThat(allDafaults.getSlaHour(), is(11));
        assertThat(allDafaults.getOnlyWarn(), is(false));
    }

    @Test
    public void testReadShameConf() throws IOException {
        SlaMonitoringConfig conf = new SlaMonitoringConfig();
        ShameConfiguration result = conf.readShameMonitoringConfigs(TEST_CONF_SHAME_PATH);

        assertThat("Wrong parse result!", result.getBodypositiveAndGrowing().size(), is(2));
        List<ShameDict> sizeDict = result.getBodypositiveDicts();
        assertThat(sizeDict.size(), is(2));
        assertThat(sizeDict.stream().map(ShameDict::getName).collect(toList()), containsInAnyOrder("dict_1_big", "dict_2_big"));

        List<ShameDict> growDict = result.getSuperGrowingDicts();
        assertThat(growDict.size(), is(2));
        assertThat(growDict.stream().map(ShameDict::getName).collect(toList()), containsInAnyOrder("dict_3_grow", "dict_4_grow"));

        List<ShameDict> allDict = result.getBodypositiveAndGrowing();
        assertThat(allDict.size(), is(2));
        assertThat(allDict.stream().map(ShameDict::getName).collect(toList()), containsInAnyOrder("dict_5_all_inclusive", "dict_6_all_inclusive"));

        List<ShameDict> reloadDict = result.getBigReloads();
        System.out.println(reloadDict);
        assertThat(reloadDict.size(), is(1));
        assertThat(reloadDict.get(0).getName(), is("dict_7_reload"));

    }

    private SlaDict getCube(List<SlaDict> result, String name) {
        return result.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }
}
