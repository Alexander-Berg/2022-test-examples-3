package ru.yandex.market.mbo.gurulight.template.conditional;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleType;
import ru.yandex.market.mbo.gwt.models.rules.gl.templates.ConditionalRulesTemplate;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author commince
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ConditionalRuleGeneratorTest {

    @Test
    public void testRulesGeneration() throws Exception {
        List<ConditionalRulesTemplate> templates = GLRuleTestHelper.generateConditionalTestTemplates();

        List<GLRule> rules = new ConditionalRuleGenerator(templates).generateAllRules();

        List<GLRule> etalon = GLRuleTestHelper.generateConditionalTestRules();

        assertEquals(etalon.size(), rules.size());

        //Вместо containsAll с кастомным equals
        assertEquals(etalon.stream().filter(o ->
            o.getType() == GLRuleType.CONDITIONAL_DEFINITIONS).collect(
                Collectors.toList()).size(), etalon.stream().filter(o ->
                    rules.stream().filter(oo ->
                        oo.getType() == GLRuleType.CONDITIONAL_DEFINITIONS)
                        .collect(Collectors.toList())
                        .stream()
                        .filter(oo ->
                          GLRuleTestHelper.rulesEqualForTest(o, oo))
                    .count() > 0).count());

    }
}
