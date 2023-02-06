package ru.yandex.canvas.service.video;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.configs.DomainConfig;
import ru.yandex.canvas.service.video.presets.configs.options.ColorPickerOptionConfig;
import ru.yandex.canvas.service.video.presets.configs.options.TextOptionConfig;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class DomainConfigTest {
    /*
     if domain_present:
255         config['domain'] = {
256             'title': lazy_gettext('domain-group-title'),
257             'options': [{
258                 'title': lazy_gettext('color-title'),
259                 'name': 'text_color',
260                 'type': 'colorpicker',
261                 'editable': domain_color_present,
262                 'visible': domain_color_present,
263                 'allowedValues': [],
264                 'defaultValue': '#857b8c'
265             }],
266         }
267         if domain_editable:
268             config['domain']['options'].append({
269                 'title': lazy_gettext('content-title'),
270                 'name': 'text',
271                 'type': 'text',
272                 'editable': domain_editable,
273                 'limit': 50,
274                 'visible': domain_editable,
275                 'allowedValues': [],
276                 'defaultValue': ''
277             })
278

    */

    @Test
    public void domainCreationWithColorTest() {
        PresetDescription description = new PresetDescription();
        description.setDomainColor("#cafeba");

        DomainConfig domainConfig = new DomainConfig(description);

        assertEquals("Title", domainConfig.getTitle(), "domain-group-title");
        assertEquals("canBeHidden", domainConfig.getIsHidden(), null);

        assertEquals("canBeHidden", domainConfig.getOptionConfigs().size(), 1);

        assertThat("", domainConfig.getOptionConfigs(), Matchers.contains(
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("color-title")),
                        hasProperty("name", equalTo("text_color")),
                        hasProperty("editable", equalTo(true)),
                        hasProperty("visible", equalTo(true)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo("#857b8c")))
                )
        );

    }

    @Test
    public void domainEditableCreationWithColorTest() {
        PresetDescription description = new PresetDescription();
        description.setDomainColor("#cafeba");
        description.setDomainEditable(true);

        DomainConfig domainConfig = new DomainConfig(description);

        assertEquals("Title", domainConfig.getTitle(), "domain-group-title");
        assertEquals("canBeHidden", domainConfig.getIsHidden(), null);

        assertEquals("domain config size", domainConfig.getOptionConfigs().size(), 2);

        assertThat("", domainConfig.getOptionConfigs(), Matchers.contains(
                allOf(instanceOf(ColorPickerOptionConfig.class),
                        hasProperty("title", equalTo("color-title")),
                        hasProperty("name", equalTo("text_color")),
                        hasProperty("editable", equalTo(true)),
                        hasProperty("visible", equalTo(true)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo("#857b8c"))
                ),
                allOf(instanceOf(TextOptionConfig.class),
                        hasProperty("title", equalTo("content-title")),
                        hasProperty("name", equalTo("text")),
                        hasProperty("editable", equalTo(true)),
                        hasProperty("visible", equalTo(true)),
                        hasProperty("limit", equalTo(50)),
                        hasProperty("allowedValues", empty()),
                        hasProperty("defaultValue", equalTo(""))
                )
        ));

    }


}
