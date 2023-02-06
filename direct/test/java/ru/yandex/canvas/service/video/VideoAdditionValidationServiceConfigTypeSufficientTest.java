package ru.yandex.canvas.service.video;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.canvas.service.video.presets.configs.ConfigType;

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class VideoAdditionValidationServiceConfigTypeSufficientTest {
    @Parameterized.Parameter
    public ConfigType configType;

    @Parameterized.Parameters(name = "ConfigType {0}")
    public static Collection<ConfigType> types() {
        return Arrays.asList(ConfigType.values());
    }

    @Test
    public void configTypeToElementTypeMapIsComplete() {
        assertTrue("config type found in map", VideoAdditionValidationService.getAdditionElementTypeByConfigType(configType) != null);
    }
}
