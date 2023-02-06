package ru.yandex.market.clickphite.config;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.clickphite.config.metric.GraphiteMetricConfig;
import ru.yandex.market.clickphite.config.metric.SolomonSensorConfig;
import ru.yandex.market.clickphite.config.metric.StatfaceReportConfig;
import ru.yandex.market.clickphite.config.validation.context.ConfigValidator;

import java.io.File;
import java.io.IOException;

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
    public void test() throws IOException {
        ConfigFile configFile = new ConfigFile(new File(
            "src/test/resources/global_config_test/filter.json"
        ));
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
