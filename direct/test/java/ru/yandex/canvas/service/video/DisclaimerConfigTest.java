package ru.yandex.canvas.service.video;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.configs.DisclaimerConfig;
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
public class DisclaimerConfigTest {
    /*
    if disclaimer_present:
194         config['disclaimer'] = {
195             'title': lazy_gettext('disclaimer-group-title'),
196             'options': [{
197                 'title': lazy_gettext('content-title'),
198                 'name': 'text',
199                 'type': 'text',
200                 'editable': False,
201                 'visible': True,
202                 'defaultValue': '',
203                 'allowedValues': []
204             }, {
205                 'title': lazy_gettext('background-color-title'),
206                 'name': 'background_color',
207                 'type': 'colorpicker',
208                 'editable': False,
209                 'visible': False,
210                 'allowedValues': [],
211                 'defaultValue': '#000000'
212             }, {
213                 'title': lazy_gettext('color-title'),
214                 'name': 'text_color',
215                 'type': 'colorpicker',
216                 'editable': False,
217                 'visible': False,
218                 'allowedValues': [],
219                 'defaultValue': '#FFFFFF'
220             }]
221         }
     */

    @Test
    public void disclaimerCreationTest() {
        PresetDescription description = new PresetDescription();

        DisclaimerConfig disclaimerConfig = new DisclaimerConfig(description);

        assertEquals("Title", disclaimerConfig.getTitle(), "disclaimer-group-title");
        assertEquals("canBeHidden", disclaimerConfig.getIsHidden(), null);

        assertEquals("canBeHidden", disclaimerConfig.getOptionConfigs().size(), 3);

        assertThat("", disclaimerConfig.getOptionConfigs(), Matchers.contains(
                allOf(instanceOf(TextOptionConfig.class),
                        hasProperty("title", equalTo("content-title")),
                        hasProperty("name", equalTo("text")),
                        hasProperty("editable", equalTo(false)),
                        hasProperty("visible", equalTo(true)),
                        hasProperty("limit", equalTo(0)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo(""))
                ),
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
                        hasProperty("defaultValue", equalTo("#FFFFFF")))
                )
        );

    }


}
