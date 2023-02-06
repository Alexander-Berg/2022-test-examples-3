package ru.yandex.market.pers.area.service;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.area.exception.TemplateNotFoundException;
import ru.yandex.market.pers.area.model.Template;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 09.11.17
 */
public class TemplateServiceTest extends MarketUtilsMockedDbTest {
    @Autowired
    private TemplateService templateService;

    @Test
    public void loadTemplatesReturnsEmptyMapIfNoTemplatesExist() throws Exception {
        Map<String, Template> templateMap = templateService.loadAllTemplates();
        assertEquals(Collections.emptyMap(), templateMap);
    }

    @Test
    public void loadTemplatesReturnsExistingTemplates() throws Exception {
        Template expected1 = template();
        expected1.setTitle("1");
        templateService.addTemplate(expected1);
        Template expected2 = template();
        expected2.setTitle("2");
        templateService.addTemplate(expected2);

        Map<String, Template> templateMap = templateService.loadAllTemplates();
        assertEquals(new HashMap<String, Template>() {{
            put(expected1.getTitle(), expected1);
            put(expected2.getTitle(), expected2);
        }}, templateMap);
    }

    @Test
    public void applyTemplateSimpleTemplate() throws Exception {
        Template template = template();
        template.setTitle("my_template");
        template.setBody("${message}");

        templateService.addTemplate(template);
        JSONObject model = new JSONObject();
        String expectedText = "Hello, FTL!";
        model.put("message", expectedText);
        templateService.invalidateCache();
        String actualText = templateService.applyTemplate(template.getTitle(), model);
        assertEquals(expectedText, actualText);
    }

    @Test
    public void loadTemplateLoadsExistingTemplate() throws Exception {
        Template expected = template();
        templateService.addTemplate(expected);
        assertEquals(expected, templateService.loadTemplate(expected.getTitle()));
    }

    @Test
    public void loadTemplateThrowsExceptionIfTemplateNotFound() throws Exception {
        assertThrows(TemplateNotFoundException.class, () -> {
            templateService.loadTemplate("not_existing");
        });
    }

    @Test
    public void applyTemplateThrowsTemplateNotFound() throws Exception {
        assertThrows(TemplateNotFoundException.class, () -> {
            templateService.applyTemplate("not_existing", new JSONObject());
        });
    }

    private Template template() {
        return new Template(
            "template-title",
            "template body",
            Template.Language.FTL
        );
    }
}
