package ru.yandex.market.mbo.db.params;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleImpl;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRulePredicate;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by ayratgdl on 05.10.15.
 */
@SuppressWarnings("checkstyle:magicNumber")
public class RulesServiceTest {

    @Test
    public void testContainsParamEmpty() throws Exception {
        GLRule rule = new GLRuleImpl();
        assertFalse(GLRulesServiceInterface.containsParam(rule, 0));
    }

    @Test
    public void testContainsParam() throws Exception {
        long paramId = 1;
        GLRule rule = new GLRuleImpl();
        rule.setIfs(Arrays.asList(new GLRulePredicate(paramId, 2, "type1")));
        rule.setThens(Arrays.asList(new GLRulePredicate(3, 4, "type1")));

        assertTrue(GLRulesServiceInterface.containsParam(rule, paramId));
        assertFalse(GLRulesServiceInterface.containsParam(rule, 2));
    }
}
