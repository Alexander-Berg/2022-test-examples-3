package ru.yandex.market.abo.core.export.hidden;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.offer.hidden.blue.BlueOfferHidingReason;
import ru.yandex.market.abo.core.hiding.rules.blue.BlueOfferHidingRule;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 18.09.18.
 */
class BlueOfferHidingRulesProcessorTest {
    @Mock
    private BlueOfferHidingRule rule;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(rule.getHidingReason()).thenReturn(BlueOfferHidingReason.MANUALLY_HIDDEN);
    }

    @Test
    void noReason() {
        when(rule.getHidingReason()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> BlueOfferHidingRulesProcessor.validate(rule));
    }

    @Test
    void allNull() {
        when(rule.getMarketSku()).thenReturn(null);
        when(rule.getShopSku()).thenReturn(null);
        when(rule.getSupplierId()).thenReturn(null);
        when(rule.getModelId()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> BlueOfferHidingRulesProcessor.validate(rule));
    }

    @Test
    void marketSkuWithShopSkuIllegalCombo() {
        when(rule.getMarketSku()).thenReturn(0L);
        when(rule.getShopSku()).thenReturn("1");
        assertThrows(IllegalArgumentException.class, () -> BlueOfferHidingRulesProcessor.validate(rule));
    }

    @Test
    void shopSkuCannotBeAlone() {
        when(rule.getMarketSku()).thenReturn(null);
        when(rule.getSupplierId()).thenReturn(null);
        when(rule.getShopSku()).thenReturn("1");
        assertThrows(IllegalArgumentException.class, () -> BlueOfferHidingRulesProcessor.validate(rule));
    }

    @Test
    void marketSku() {
        when(rule.getMarketSku()).thenReturn(0L);
        when(rule.getModelId()).thenReturn(null);
        BlueOfferHidingRulesProcessor.validate(rule);
    }

    @Test
    void marketSkuAndSupplierId() {
        when(rule.getMarketSku()).thenReturn(0L);
        when(rule.getSupplierId()).thenReturn(1L);
        when(rule.getModelId()).thenReturn(null);
        BlueOfferHidingRulesProcessor.validate(rule);
    }

    @Test
    void shopSkuAndSupplierId() {
        when(rule.getMarketSku()).thenReturn(null);
        when(rule.getShopSku()).thenReturn("1");
        when(rule.getSupplierId()).thenReturn(1L);
        when(rule.getModelId()).thenReturn(null);
        BlueOfferHidingRulesProcessor.validate(rule);
    }

    @Test
    void modelId() {
        when(rule.getMarketSku()).thenReturn(null);
        when(rule.getShopSku()).thenReturn(null);
        when(rule.getSupplierId()).thenReturn(null);
        when(rule.getModelId()).thenReturn(1L);
        BlueOfferHidingRulesProcessor.validate(rule);
    }

    @Test
    void intSupplierId() {
        when(rule.getMarketSku()).thenReturn(3123123L);
        when(rule.getSupplierId()).thenReturn((long) Integer.MAX_VALUE + 1);
        assertThrows(IllegalArgumentException.class, () -> BlueOfferHidingRulesProcessor.validate(rule));
    }

    @Test
    void fromCartDiff() {
        BlueOfferHidingRule cartDiffHiding = BlueOfferHidingRule.fromCartDiff(1, "some_shop_sku", 21314L, BlueOfferHidingReason.CARTDIFF);
        BlueOfferHidingRulesProcessor.validate(cartDiffHiding);
    }

    @Test
    void fromOrderItem() {
        BlueOfferHidingRulesProcessor.validate(
                BlueOfferHidingRule.fromOrderItem(4234234L, 42342L, "s-k-u", BlueOfferHidingReason.CANCELLED_ORDER)
        );
    }
}
