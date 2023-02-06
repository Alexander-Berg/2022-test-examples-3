package ru.yandex.canvas.service.video;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.config.VideoAdditionsTestConfiguration;
import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.configs.TitleConfig;
import ru.yandex.canvas.service.video.presets.configs.options.ColorPickerOptionConfig;
import ru.yandex.canvas.service.video.presets.configs.options.TextOptionConfig;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ControllerTestConfiguration.class, VideoAdditionsTestConfiguration.class})
public class TitleConfigTest {

    /*
    if title_present:
 99         config['title'] = {
100             'title': lazy_gettext('title-group-title'),
101             'options': [{
102                 'title': lazy_gettext('background-color-title'),
103                 'name': 'background_color',
104                 'type': 'colorpicker',
105                 'editable': title_bg_editable,
106                 'visible': title_bg_editable,
107                 'allowedValues': [],
108                 'dependsOnVideoColor': title_bg_depends_on_video,
109                 'defaultValue': 'Title'
110             }, {
111                 'title': lazy_gettext('color-title'),
112                 'name': 'text_color',
113                 'type': 'colorpicker',
114                 'editable': False,
115                 'visible': False,
116                 'allowedValues': [],
117                 'defaultValue': '#FFFFFF'
118             }, {
119                 'title': lazy_gettext('content-title') if title_editable else '',
120                 'name': 'text',
121                 'type': 'text',
122                 'editable': title_editable,
123                 'limit': 35,
124                 'visible': True,
125                 'allowedValues': [],
126                 'defaultValue': ''
127             }]
128         }

     */

    @Test
    public void titleCreationTest() {
        PresetDescription description = new PresetDescription();

        description.setTitlePresent(true);
        description.setTitleEditable(true);

        TitleConfig titleConfig = new TitleConfig(description);

        assertEquals("Title", titleConfig.getTitle(), "title-group-title");
        assertEquals("canBeHidden", titleConfig.getIsHidden(), null);

        assertEquals("configuration size", titleConfig.getOptionConfigs().size(), 4);

        assertThat("", titleConfig.getOptionConfigs(), Matchers.contains(
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("background-color-title")),
                        hasProperty("name", equalTo("background_color")),
                        hasProperty("editable", equalTo(false)),
                        hasProperty("visible", equalTo(false)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo("Title")) //Title ???
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
                        hasProperty("limit", equalTo(35)),
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
                        hasProperty("defaultValue", equalTo(""))
                )
                )
        );

    }


    @Test
    public void uneditableTitleCreationTest() {
        PresetDescription description = new PresetDescription();

        description.setTitlePresent(true);
        description.setTitleEditable(false);

        TitleConfig titleConfig = new TitleConfig(description);

        assertEquals("Title", titleConfig.getTitle(), "title-group-title");
        assertEquals("canBeHidden", titleConfig.getIsHidden(), null);

        assertEquals("configuration size", titleConfig.getOptionConfigs().size(), 3);

        assertThat("", titleConfig.getOptionConfigs(), Matchers.contains(
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("background-color-title")),
                        hasProperty("name", equalTo("background_color")),
                        hasProperty("editable", equalTo(false)),
                        hasProperty("visible", equalTo(false)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo("Title")) //Title ???
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
                        hasProperty("title", equalTo("")),
                        hasProperty("name", equalTo("text")),
                        hasProperty("editable", equalTo(false)),
                        hasProperty("visible", equalTo(true)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo(""))
                )
                )
        );

    }

    @Test
    public void titleWithBackgroundfromVideo() {
        PresetDescription description = new PresetDescription();
        description.setTitleEditable(true);
        description.setTitleBackgroundFromVideo(true);

        TitleConfig titleConfig = new TitleConfig(description);

        assertEquals("Title", titleConfig.getTitle(), "title-group-title");
        assertEquals("canBeHidden", titleConfig.getIsHidden(), null);

        assertEquals("configuration size", titleConfig.getOptionConfigs().size(), 4);

        assertThat("", titleConfig.getOptionConfigs(), Matchers.contains(
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("background-color-title")),
                        hasProperty("name", equalTo("background_color")),
                        hasProperty("editable", equalTo(true)),
                        hasProperty("visible", equalTo(true)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo("Title"))
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
                        hasProperty("limit", equalTo(35)),
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
                        hasProperty("defaultValue", equalTo(""))
                )
                )
        );
    }
}
