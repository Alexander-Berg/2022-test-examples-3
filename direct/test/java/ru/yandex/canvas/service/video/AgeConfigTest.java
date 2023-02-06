package ru.yandex.canvas.service.video;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.configs.AgeConfig;
import ru.yandex.canvas.service.video.presets.configs.options.AgeOptionConfig;
import ru.yandex.canvas.service.video.presets.configs.options.ColorPickerOptionConfig;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
public class AgeConfigTest {
    @Test
    public void ageCreationTest() {
        PresetDescription description = new PresetDescription();
        description.setAgePresent(true);

        AgeConfig ageConfig = new AgeConfig(description);

        assertEquals("Title", ageConfig.getTitle(), "age-group-title");
        assertEquals("canBeHidden", ageConfig.getIsHidden(), Boolean.TRUE);

        assertEquals("canBeHidden", ageConfig.getOptionConfigs().size(), 3);

        assertThat("", ageConfig.getOptionConfigs(), Matchers.contains(
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("background-color-title")),
                        hasProperty("name", equalTo("background_color")),
                        hasProperty("editable", equalTo(false)),
                        hasProperty("visible", equalTo(false)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo("#000000"))
                ),
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("color-title")),
                        hasProperty("name", equalTo("text_color")),
                        hasProperty("editable", equalTo(false)),
                        hasProperty("visible", equalTo(false)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo("#FFFFFF"))
                ),
                allOf(instanceOf(AgeOptionConfig.class),
                        hasProperty("title", equalTo("content-title")),
                        hasProperty("name", equalTo("text")),
                        hasProperty("editable", equalTo(false)),
                        hasProperty("visible", equalTo(true)),
                        hasProperty("allowedValues", Matchers.contains("0", "6", "12", "16", "18")),
                        hasProperty("defaultValue", equalTo("18"))
                )
                )
        );

    }
}
