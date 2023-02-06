package ru.yandex.market.abo.web.controller.hiding.universal;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.category.CategoryManager;
import ru.yandex.market.abo.core.hiding.rules.stopword.model.OfferTag;
import ru.yandex.market.abo.core.hiding.rules.universal.HidingRule;
import ru.yandex.market.abo.core.region.country.CountryService;
import ru.yandex.market.checkout.checkouter.order.Color;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 22/04/19.
 */
class HidingRuleValidatorTest {
    @InjectMocks
    HidingRuleValidator ruleValidator;
    @Mock
    CategoryManager categoryManager;
    @Mock
    CountryService countryService;
    @Mock
    private HidingRule rule;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(rule.getRgb()).thenReturn(Set.of(Color.RED));
        when(rule.getCountry()).thenReturn(null);
        doNothing().when(categoryManager).validateMarketCategories(any());
    }

    @Test
    void stopWord() {
        ruleValidator.validate(rule);
        when(rule.getStopWordTags()).thenReturn(List.of(OfferTag.description));
        assertThrows(IllegalArgumentException.class, () -> ruleValidator.validate(rule));
    }

    @Test
    void country() {
        ruleValidator.validate(rule);
        int countryId = -1;
        when(rule.getCountry()).thenReturn((long) countryId);
        when(countryService.loadById(countryId)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> ruleValidator.validate(rule));
    }

    @Test
    void allNull() {
        HidingRule empty = new HidingRule();
        empty.setRgb(Set.of(Color.RED));
        assertThrows(IllegalArgumentException.class, () -> ruleValidator.validate(empty));
    }
}
