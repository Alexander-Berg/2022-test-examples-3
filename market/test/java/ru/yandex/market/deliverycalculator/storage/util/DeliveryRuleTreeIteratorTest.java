package ru.yandex.market.deliverycalculator.storage.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.IteratorUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.deliverycalculator.model.DeliveryRule;
import ru.yandex.market.deliverycalculator.storage.model.LazyUnzippedDeliveryRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link DeliveryRuleTreeIterator}
 */
class DeliveryRuleTreeIteratorTest {

    @Test
    void testIterateOrder() {
        DeliveryRule root = new DeliveryRule();
        DeliveryRule locationRuleFrom = new DeliveryRule();
        root.setChildren(List.of(locationRuleFrom));

        DeliveryRule locationRuleTo1 = new DeliveryRule();
        locationRuleFrom.setChildren(new ArrayList<>());
        locationRuleFrom.getChildren().add(locationRuleTo1);

        DeliveryRule offerRule1 = new DeliveryRule();
        DeliveryRule offerRule2 = new DeliveryRule();
        locationRuleTo1.setChildren(new ArrayList<>());
        locationRuleTo1.getChildren().add(offerRule1);
        locationRuleTo1.getChildren().add(offerRule2);

        DeliveryRule locationRuleTo2 = new DeliveryRule();
        locationRuleFrom.getChildren().add(locationRuleTo2);

        DeliveryRule offerRule3 = new DeliveryRule();
        locationRuleTo2.setChildren(List.of(offerRule3));

        List<DeliveryRule> expectedOrder = List.of(offerRule1, offerRule2, locationRuleTo1,
                offerRule3, locationRuleTo2, locationRuleFrom, root);
        List<DeliveryRule> actualOrder = IteratorUtils.toList(new DeliveryRuleTreeIterator(root));

        assertThat(expectedOrder, is(actualOrder));
    }

    /**
     * Проверка вызова {@link DeliveryRule#getChildren()} только один раз при обходе
     */
    @Test
    void testLazyUnzippedRule() {
        DeliveryRule root = new DeliveryRule();
        DeliveryRule locationRuleFrom = new DeliveryRule();
        root.setChildren(List.of(locationRuleFrom));

        LazyUnzippedDeliveryRule lazyUnzippedDeliveryRule = mock(LazyUnzippedDeliveryRule.class);
        DeliveryRule offerRule3 = new DeliveryRule();
        when(lazyUnzippedDeliveryRule.getChildren()).thenReturn(List.of(offerRule3));

        locationRuleFrom.setChildren(List.of(lazyUnzippedDeliveryRule));

        List<DeliveryRule> actualOrder = IteratorUtils.toList(new DeliveryRuleTreeIterator(root));
        List<DeliveryRule> expectedOrder = List.of(offerRule3, lazyUnzippedDeliveryRule, locationRuleFrom, root);
        assertThat(expectedOrder, is(actualOrder));

        verify(lazyUnzippedDeliveryRule).getChildren();
    }

    @Test
    void testSingleRule() {
        DeliveryRule root = new DeliveryRule();

        DeliveryRuleTreeIterator deliveryRuleTreeIterator = new DeliveryRuleTreeIterator(root);
        assertThat(deliveryRuleTreeIterator.next(), is(root));
        assertThat(deliveryRuleTreeIterator.hasNext(), is(false));
    }
}
