package ru.yandex.canvas.service.video;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.configs.SubtitlesConfig;
import ru.yandex.canvas.service.video.presets.configs.options.ColorPickerOptionConfig;
import ru.yandex.canvas.service.video.presets.configs.options.TextOptionConfig;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
public class SubtitlesConfigTest {
    @Test
    public void subtitlesCreationTest() {
        PresetDescription description = new PresetDescription();
        description.setSubtitlesPresent(true);

        SubtitlesConfig subtitlesConfig = new SubtitlesConfig(description);

        assertEquals("Title", subtitlesConfig.getTitle(), "subtitles-group-title");
        assertEquals("Tooltip", subtitlesConfig.getTooltip(), "subtitles-group-tooltip");
        assertEquals("canBeHidden", subtitlesConfig.getIsHidden(), true);

        assertEquals("canBeHidden", subtitlesConfig.getOptionConfigs().size(), 4);

        assertThat("", subtitlesConfig.getOptionConfigs(), Matchers.contains(
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
                allOf(instanceOf(TextOptionConfig.class),
                        hasProperty("title", equalTo("content-title")),
                        hasProperty("name", equalTo("text")),
                        hasProperty("editable", equalTo(true)),
                        hasProperty("visible", equalTo(true)),
                        hasProperty("limit", equalTo(10000)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo(""))
                ),
                allOf(instanceOf(TextOptionConfig.class),
                        hasProperty("title", equalTo("")),
                        hasProperty("name", equalTo("placeholder")),
                        hasProperty("editable", equalTo(false)),
                        hasProperty("visible", equalTo(false)),
                        hasProperty("limit", equalTo(0)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo("")))
                )
        );
    }
}
