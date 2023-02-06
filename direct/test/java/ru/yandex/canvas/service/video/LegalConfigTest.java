package ru.yandex.canvas.service.video;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.configs.LegalConfig;
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
public class LegalConfigTest {
    /*
    if legal_present:
162         config['legal'] = {
163             'title': lazy_gettext('legal-group-title'),
164             'canBeHidden': True,
165             'options': [{
166                 'title': lazy_gettext('background-color-title'),
167                 'name': 'background_color',
168                 'type': 'colorpicker',
169                 'editable': False,
170                 'visible': False,
171                 'allowedValues': [],
172                 'defaultValue': '#000000'
173             }, {
174                 'title': lazy_gettext('content-title'),
175                 'name': 'text',
176                 'type': 'text',
177                 'editable': True,
178                 'visible': True,
179                 'limit': 700,
180                 'allowedValues': [],
181                 'defaultValue': ''
182             }, {
183                 'title': lazy_gettext('color-title'),
184                 'name': 'text_color',
185                 'type': 'colorpicker',
186                 'editable': False,
187                 'visible': False,
188                 'allowedValues': [],
189                 'defaultValue': '#FFFFFF'
190             }]
191         }
     */

    @Test
    public void legalCreationTest() {
        PresetDescription description = new PresetDescription();
        description.setAgePresent(true);

        LegalConfig legalConfig = new LegalConfig(description);

        assertEquals("Title", legalConfig.getTitle(), "legal-group-title");
        assertEquals("canBeHidden", legalConfig.getIsHidden(), Boolean.TRUE);

        assertEquals("canBeHidden", legalConfig.getOptionConfigs().size(), 3);

        assertThat("", legalConfig.getOptionConfigs(), Matchers.contains(
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("background-color-title")),
                        hasProperty("name", equalTo("background_color")),
                        hasProperty("editable", equalTo(false)),
                        hasProperty("visible", equalTo(false)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo("#000000"))
                ),
                allOf(instanceOf(TextOptionConfig.class),
                        hasProperty("title", equalTo("content-title")),
                        hasProperty("name", equalTo("text")),
                        hasProperty("editable", equalTo(true)),
                        hasProperty("visible", equalTo(true)),
                        hasProperty("limit", equalTo(700)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo(""))
                ),
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("color-title")),
                        hasProperty("name", equalTo("text_color")),
                        hasProperty("editable", equalTo(false)),
                        hasProperty("visible", equalTo(false)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo("#FFFFFF")))
                )
        );

    }


}
