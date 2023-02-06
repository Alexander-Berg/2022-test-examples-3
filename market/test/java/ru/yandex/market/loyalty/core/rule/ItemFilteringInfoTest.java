package ru.yandex.market.loyalty.core.rule;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.loyalty.core.rule.ItemFilteringInfo.FilterRuleResult;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ItemFilteringInfoTest {

    @Test
    public void trueAndTrueShouldNotNegativeResult() {
        FilterRuleResult ruleResult = new FilterRuleResult(true);
        assertFalse(ruleResult.and(new FilterRuleResult(true)).isNegative());
    }

    @Test
    public void trueAndFalseShouldNegativeResult() {
        FilterRuleResult ruleResult = new FilterRuleResult(true);
        assertTrue(ruleResult.and(new FilterRuleResult(false)).isNegative());
    }

    @Test
    public void nullAndTrueShouldNotNegativeResult() {
        FilterRuleResult ruleResult = new FilterRuleResult(Collections.EMPTY_SET, null, null);
        assertFalse(ruleResult.and(new FilterRuleResult(true)).isNegative());
    }

    @Test
    public void nullAndFalseShouldNegativeResult() {
        FilterRuleResult ruleResult = new FilterRuleResult(Collections.EMPTY_SET, null, null);
        assertTrue(ruleResult.and(new FilterRuleResult(false)).isNegative());
    }

    @Test
    public void exclusionsAndTrueShouldNegativeResult() {
        FilterRuleResult ruleResult = new FilterRuleResult(Set.of(FilterReason.SUPPLIER), null, null);
        assertTrue(ruleResult.and(new FilterRuleResult(true)).isNegative());
    }

    @Test
    public void exclusionsAndSameInclusionsShouldNegativeResult() {
        FilterRuleResult ruleResult = new FilterRuleResult(
                Set.of(FilterReason.SUPPLIER), Set.of(FilterReason.SUPPLIER),null, null);
        assertTrue(ruleResult.and(new FilterRuleResult(true)).isNegative());
    }

    @Test
    public void exclusionsAndSameInclusionsWithOptionalRulesShouldNotNegativeResult() {
        FilterRuleResult ruleResult = new FilterRuleResult(
                Set.of(FilterReason.SUPPLIER), Set.of(FilterReason.SUPPLIER),null, null);
        assertFalse(ruleResult.and(new FilterRuleResult(true)).isNegative(true));
    }

    @Test
    public void exclusionsAndNotSameInclusionsWithOptionalRulesShouldNegativeResult() {
        FilterRuleResult ruleResult = new FilterRuleResult(
                Set.of(FilterReason.SUPPLIER), Set.of(FilterReason.CATEGORY),null, null);
        assertTrue(ruleResult.and(new FilterRuleResult(true)).isNegative(true));
    }

    @Test
    public void shouldZeroDiscount() {
        FilterRuleResult ruleResult = new FilterRuleResult(
                Set.of(FilterReason.SUPPLIER), Set.of(FilterReason.SUPPLIER),null, null);
        assertTrue(ruleResult.checkZeroDiscount());
    }
}
