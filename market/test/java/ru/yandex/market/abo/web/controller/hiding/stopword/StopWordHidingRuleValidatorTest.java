package ru.yandex.market.abo.web.controller.hiding.stopword;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.category.CategoryManager;
import ru.yandex.market.abo.core.export.hidden.StopWordsHidingRulesProcessorTest;
import ru.yandex.market.abo.core.hiding.rules.stopword.model.StopWordHidingRule;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * @author artemmz
 * @date 15.03.18.
 */
class StopWordHidingRuleValidatorTest {

    @InjectMocks
    private StopWordHidingRuleValidator ruleValidator;
    @Mock
    private CategoryManager categoryManager;

    private StopWordHidingRule rule;

    @BeforeEach
    @SuppressWarnings("all")
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        rule = StopWordsHidingRulesProcessorTest.initStopWordRule();
        doNothing().when(categoryManager).validateMarketCategories(any());
    }

    @Test
    public void allCorrect() {
        ruleValidator.validate(rule);
    }

    @Test
    public void validateMorphology() {
        rule.setMorphology(null);
        assertThrows(IllegalArgumentException.class, () ->
                ruleValidator.validate(rule));
    }

    @Test
    public void validateForbidden() {
        rule.setForbiddenOnMarket(null);
        assertThrows(IllegalArgumentException.class, () ->
                ruleValidator.validate(rule));
    }

    @Test
    public void validateDigits() {
        rule.setCategoryWhitelistCsv("digit?...no)");
        assertThrows(IllegalArgumentException.class, () ->
                ruleValidator.validate(rule));
    }

    @Test
    public void validateCategoryList() {
        rule.setCategoryBlacklistCsv("404");
        rule.setCategoryWhitelistCsv(null);
        assertThrows(IllegalArgumentException.class, () ->
                ruleValidator.validate(rule));
        rule.setCategoryWhitelistCsv("1, 2, 3");
        ruleValidator.validate(rule);
    }
}
