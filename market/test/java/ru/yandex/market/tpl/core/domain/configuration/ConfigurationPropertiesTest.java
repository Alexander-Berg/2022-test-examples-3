package ru.yandex.market.tpl.core.domain.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettings;

public class ConfigurationPropertiesTest {

    @Test
    public void syncingGlobalSettingsWithConfigurationProperties() {
        List<String> globalSettings = Arrays.stream(GlobalSettings.values()).map(Enum::toString)
                .collect(Collectors.toList());
        List<String> configurationProperties = Arrays.stream(ConfigurationProperties.values()).map(Enum::toString)
                .collect(Collectors.toList());
        Assertions.assertThat(configurationProperties).containsAll(globalSettings);
    }

}
