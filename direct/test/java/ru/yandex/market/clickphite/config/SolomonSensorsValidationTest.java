package ru.yandex.market.clickphite.config;

import org.junit.Test;
import ru.yandex.market.clickphite.config.validation.context.ConfigValidationException;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 25.07.2018
 */
public class SolomonSensorsValidationTest {
    private final ConfigurationService configurationService = new ConfigurationService();

    @Test
    public void noLabelsOrLabelsArray() {
        shouldFail("noLabelsOrLabelsArray");
    }

    @Test
    public void noRequiredLabels() {
        shouldFail("noLabelProject");
        shouldFail("noLabelCluster");
        shouldFail("noLabelService");
        shouldFail("noLabelSensor");
    }

    @Test
    public void noSplitExists() {
        shouldFail("noSplitExists");
        shouldFail("noSplitExistsArray");
    }

    private void shouldFail(String configName) {
        assertThatThrownBy(() -> validate(configName)).isInstanceOf(ConfigValidationException.class);
    }

    private void validate(String configName) {
        try {
            configurationService.parseAndCheck(new ConfigFile(new File(
                "src/test/resources/solomon_sensors_validation_test/" + configName + ".json"
            )));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
