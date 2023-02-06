package ru.yandex.market.pers.area.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.market.pers.area.db.PersAreaEmbeddedDbUtil;
import ru.yandex.market.pers.area.model.Template;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 07.11.17
 */
public class TemplateDaoTest {
    private PersAreaEmbeddedDbUtil dbUtil = PersAreaEmbeddedDbUtil.INSTANCE;

    @BeforeEach
    public void truncateTables() throws Exception {
        dbUtil.truncatePersAreaTables();
    }

    @Test
    public void addTemplateReturnsAddedTemplate() throws Exception {
        TemplateDao templateDao = new TemplateDao(dbUtil.getPersAreaJdbcTemplate());
        Template expected = template();
        assertEquals(expected, templateDao.addTemplate(expected));
    }

    @Test
    public void getTemplateNotExisting() throws Exception {
        TemplateDao templateDao = new TemplateDao(dbUtil.getPersAreaJdbcTemplate());
        assertNull(templateDao.getTemplate("not_existing"));
    }

    @Test
    public void getTemplateReturnsExistingTemplate() throws Exception {
        TemplateDao templateDao = new TemplateDao(dbUtil.getPersAreaJdbcTemplate());
        Template expected = template();
        templateDao.addTemplate(expected);
        Template actual = templateDao.getTemplate(expected.getTitle());
        assertEquals(expected, actual);
    }

    @Test
    public void getAllTemplatesReturnsAllTemplates() throws Exception {
        TemplateDao templateDao = new TemplateDao(dbUtil.getPersAreaJdbcTemplate());
        Template expected1 = template();
        expected1.setTitle("1");
        templateDao.addTemplate(expected1);
        Template expected2 = template();
        expected2.setTitle("2");
        templateDao.addTemplate(expected2);
        List<Template> actualTemplates = templateDao.getAllTemplates();
        assertEquals(Arrays.asList(expected1, expected2), actualTemplates);
    }

    @Test
    public void getAllTemplatesReturnsEmptyListIfNoTemplatesPresent() throws Exception {
        TemplateDao templateDao = new TemplateDao(dbUtil.getPersAreaJdbcTemplate());
        assertEquals(Collections.emptyList(), templateDao.getAllTemplates());
    }

    private Template template() {
        return new Template(
            "title",
            "body",
            Template.Language.FTL
        );
    }
}
