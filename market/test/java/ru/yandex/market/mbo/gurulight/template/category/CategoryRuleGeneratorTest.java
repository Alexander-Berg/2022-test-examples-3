package ru.yandex.market.mbo.gurulight.template.category;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gurulight.template.conditional.GLRuleTestHelper;
import ru.yandex.market.mbo.gwt.models.rules.gl.templates.CategoryRulesTemplate;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author commince
 */
public class CategoryRuleGeneratorTest {

    @Test
    public void testRulesGeneration() throws Exception {
        List<CategoryRulesTemplate> templates = GLRuleTestHelper.generateCategoryTestTemplates();

        List<GLRule> rules = new CategoryRuleGenerator(templates).generateAllRules();

        List<GLRule> etalon = GLRuleTestHelper.generateCategoryTestRules();

        assertEquals(etalon.size(), rules.size());

        //Вместо containsAll с кастомным equals
        assertEquals(etalon.size(), etalon.stream().filter(o ->
            rules.stream().filter(
                oo -> GLRuleTestHelper.rulesEqualForTest(o, oo)
            ).count() > 0).count());

    }
}
