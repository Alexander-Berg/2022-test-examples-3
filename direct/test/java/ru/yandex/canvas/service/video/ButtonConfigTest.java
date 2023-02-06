package ru.yandex.canvas.service.video;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.PresetTheme;
import ru.yandex.canvas.service.video.presets.configs.ButtonConfig;
import ru.yandex.canvas.service.video.presets.configs.options.ColorPickerOptionConfig;
import ru.yandex.canvas.service.video.presets.configs.options.TextOptionConfig;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/*
    config['button'] = {
280         'title': lazy_gettext('button-group-title'),
281         'tooltip': lazy_gettext('button-group-tooltip'),
282         'options': [],
283     }
284     config['button']['options'].append({
285         'title': lazy_gettext('background-color-title'),
286         'tooltip': lazy_gettext('button-background-color-tooltip'),
287         'name': 'color',
288         'type': 'colorpicker',
289         'editable': True,
290         'visible': True,
291         'allowedValues': GOOD_COLORS if button_limited_color else [],
292         'defaultValue': '#000'
293     })
294     config['button']['options'].append({
295         'title': lazy_gettext('color-title'),
296         'name': 'text_color',
297         'type': 'colorpicker',
298         'editable': True,
299         'visible': True,
300         'allowedValues': ['#000000', '#ffffff'],  # only black and white
301         'defaultValue': '#000'
302     })
303     config['button']['options'].append({
304         'title': lazy_gettext('border-color-title'),
305         'name': 'border_color',
306         'type': 'colorpicker',
307         'editable': False,
308         'visible': False,
309         'allowedValues': [],
310         'defaultValue': '#000'
311     })
312     if button_text_present:
313         config['button']['options'].append({
314             'title': lazy_gettext('content-title'),
315             'name': 'text',
316             'type': 'text',
317             'limit': 17,
318             'editable': True,
319             'visible': True,
320             'allowedValues': [],
321             'defaultValue': ''
322         })
 */

@RunWith(SpringJUnit4ClassRunner.class)
public class ButtonConfigTest {
    @Test
    public void buttonCreationTest() {
        PresetDescription description = new PresetDescription();
        description.setAgePresent(true);

        ButtonConfig buttonConfig = new ButtonConfig(description);

        assertEquals("Title", buttonConfig.getTitle(), "button-group-title");
//XXX        assertEquals("Tooltip", button.getTooltip(), "button-group-title");


        assertEquals("canBeHidden", buttonConfig.getIsHidden(), null);

        assertEquals("canBeHidden", buttonConfig.getOptionConfigs().size(), 3);

        assertThat("", buttonConfig.getOptionConfigs(), Matchers.contains(
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("background-color-title")),
                        hasProperty("tooltip", equalTo("button-background-color-tooltip")),
                        hasProperty("name", equalTo("color")),
                        hasProperty("editable", equalTo(true)),
                        hasProperty("visible", equalTo(true)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo("#000000"))
                ),
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("color-title")),
                        hasProperty("name", equalTo("text_color")),
                        hasProperty("editable", equalTo(true)),
                        hasProperty("visible", equalTo(true)),
                        hasProperty("allowedValues", Matchers.contains("#000000", "#ffffff")),
                        hasProperty("defaultValue", equalTo("#000000"))
                ),
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("border-color-title")),
                        hasProperty("name", equalTo("border_color")),
                        hasProperty("editable", equalTo(false)),
                        hasProperty("visible", equalTo(false)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo("#000000"))
                )

                )
        );

    }

    @Test
    public void buttonEditableWithLimitedColorsCreationTest() {
        PresetDescription description = new PresetDescription();
        description.setAgePresent(true);
        description.setButtonLimitedColor(true);
        description.setButtonTextPresent(true);
        description.setPresetTheme(PresetTheme.LAKE);

        ButtonConfig buttonConfig = new ButtonConfig(description);

        assertEquals("Title", buttonConfig.getTitle(), "button-group-title");
//XXX        assertEquals("Tooltip", button.getTooltip(), "button-group-title");


        assertEquals("canBeHidden", buttonConfig.getIsHidden(), null);

        assertEquals("canBeHidden", buttonConfig.getOptionConfigs().size(), 5);

        assertThat("", buttonConfig.getOptionConfigs(), Matchers.contains(
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("background-color-title")),
                        hasProperty("tooltip", equalTo("button-background-color-tooltip")),
                        hasProperty("name", equalTo("color")),
                        hasProperty("editable", equalTo(true)),
                        hasProperty("visible", equalTo(true)),
                        hasProperty("allowedValues",
                                Matchers.contains("#ffdc00", "#ff0000", "#ffffff", "#fb3e00", "#008bff")),
                        hasProperty("defaultValue", equalTo("#000000"))
                ),
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("color-title")),
                        hasProperty("name", equalTo("text_color")),
                        hasProperty("editable", equalTo(true)),
                        hasProperty("visible", equalTo(true)),
                        hasProperty("allowedValues", Matchers.contains("#000000", "#ffffff")),
                        hasProperty("defaultValue", equalTo("#000000"))
                ),
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("border-color-title")),
                        hasProperty("name", equalTo("border_color")),
                        hasProperty("editable", equalTo(false)),
                        hasProperty("visible", equalTo(false)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo("#000000"))
                ),
                allOf(instanceOf(TextOptionConfig.class),
                        hasProperty("title", equalTo("content-title")),
                        hasProperty("name", equalTo("text")),
                        hasProperty("limit", equalTo(17)),
                        hasProperty("editable", equalTo(true)),
                        hasProperty("visible", equalTo(true)),
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
