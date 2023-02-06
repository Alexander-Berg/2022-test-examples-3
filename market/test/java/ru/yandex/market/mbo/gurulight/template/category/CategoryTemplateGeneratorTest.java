package ru.yandex.market.mbo.gurulight.template.category;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gurulight.template.conditional.GLRuleTestHelper;
import ru.yandex.market.mbo.gwt.models.rules.gl.templates.ConditionalRulesTemplate;
import ru.yandex.market.mbo.gwt.models.rules.gl.templates.CategoryRulesTemplate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author commince
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryTemplateGeneratorTest {

    @Test
    public void testTemplateGeneration() throws Exception {
        List<GLRule> rules = GLRuleTestHelper.generateCategoryTestRules();

        List<CategoryRulesTemplate> templates = new CategoryTemplateGenerator(rules).generateTemplates();

        Comparator<CategoryRulesTemplate> comparator = Comparator.comparing(CategoryRulesTemplate::getMainParamId);

        templates = templates.stream().sorted(comparator).collect(Collectors.toList());

        assertEquals(3, templates.size());
        templates.forEach(this::checkParamIds);

        assertEquals(Long.valueOf(21L), templates.get(0).getMainParamOptionId());
        assertEquals(1, templates.get(0).getMainParamAliasOptionIds().size());
        assertTrue(templates.get(0).getMainParamAliasOptionIds().containsAll(Arrays.asList(223L)));
        assertEquals(2, templates.get(0).getDefinitionParamOptionIds().size());
        assertTrue(templates.get(0).getDefinitionParamOptionIds().containsAll(Arrays.asList(31L, 32L)));

        assertEquals(Long.valueOf(22L), templates.get(1).getMainParamOptionId());
        assertEquals(2, templates.get(1).getMainParamAliasOptionIds().size());
        assertTrue(templates.get(1).getMainParamAliasOptionIds().containsAll(Arrays.asList(224L, 225L)));
        assertEquals(2, templates.get(1).getDefinitionParamOptionIds().size());
        assertTrue(templates.get(1).getDefinitionParamOptionIds().containsAll(Arrays.asList(33L, 34L)));

        assertEquals(Long.valueOf(23L), templates.get(2).getMainParamOptionId());
        assertEquals(1, templates.get(2).getMainParamAliasOptionIds().size());
        assertTrue(templates.get(2).getMainParamAliasOptionIds().containsAll(Arrays.asList(226L)));
        assertEquals(3, templates.get(2).getDefinitionParamOptionIds().size());
        assertTrue(templates.get(2).getDefinitionParamOptionIds().containsAll(Arrays.asList(34L, 35L, 36L)));
    }

    private void checkParamIds(ConditionalRulesTemplate template) {
        assertEquals(Long.valueOf(1L), template.getConditionalParamId());
        assertEquals(Long.valueOf(2L), template.getMainParamId());
        assertEquals(Long.valueOf(3L), template.getDefinitionParamId());
    }

    private void checkParamIds(CategoryRulesTemplate template) {
        assertEquals(Long.valueOf(2L), template.getMainParamId());
        assertEquals(Long.valueOf(3L), template.getDefinitionParamId());
    }
}
