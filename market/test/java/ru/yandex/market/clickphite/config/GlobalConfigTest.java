package ru.yandex.market.clickphite.config;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.clickphite.config.validation.context.ConfigValidator;
import ru.yandex.market.clickphite.utils.ResourceUtils;
import ru.yandex.market.health.configs.clickphite.config.metric.GraphiteMetricConfig;
import ru.yandex.market.health.configs.clickphite.config.metric.SolomonSensorConfig;
import ru.yandex.market.health.configs.clickphite.config.metric.StatfaceReportConfig;
import ru.yandex.market.health.configs.clickphite.metric.graphite.SplitNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 03.09.2018
 */
public class GlobalConfigTest {
    private final ConfigurationService configurationService = new ConfigurationService();

    @Before
    public void setUp() {
        configurationService.setConfigValidator(new ConfigValidator());
    }

    @Test
    public void test() throws IOException, SplitNotFoundException {
        ConfigFile configFile = new ConfigFile(
            ResourceUtils.getResourceFile("global_config_test/filter.json")
        );
        configurationService.parseAndCheck(configFile);

        assertThat(configFile.getGraphiteMetricConfigs())
            .extracting(GraphiteMetricConfig::getFilter)
            .containsExactly("defaultFilter");

        assertThat(configFile.getStatfaceReportConfigs())
            .extracting(StatfaceReportConfig::getFilter)
            .containsExactly("defaultFilter");

        assertThat(configFile.getSolomonSensorConfigs())
            .extracting(SolomonSensorConfig::getFilter)
            .containsExactly("defaultFilter");
    }
}
