package ru.yandex.market.checkout.checkouter.b2b;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cart.MultiCartTotals;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.common.pay.FinancialUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.util.loyalty.model.PromocodeDiscountEntry;

public class B2bVatCalculationTest extends AbstractWebTestBase {

    private static final String PROMO_CODE = "PROMO-CODE";
    private static final String PROMO_KEY = "PROMO-KEY";
    private static final double ASSERTION_DELTA = 0.001d;

    @Test
    @DisplayName("Суммарный НДС считается с учетом скидки по промокоду")
    void totalVatCalculationTakesIntoAccountItemDiscounts() {
        // Given
        double vatRate = 0.2;
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        OrderItem item = parameters.getItems().iterator().next();

        // set up price without VAT from Report
        BigDecimal priceWithoutVat =
                FinancialUtils.roundPrice(item.getBuyerPrice().multiply(BigDecimal.valueOf(1.0 - vatRate)));
        item.getPrices().setPriceWithoutVat(priceWithoutVat);

        // set up promocode from Loyalty
        BigDecimal promocodeDiscount = BigDecimal.valueOf(10);
        parameters.getLoyaltyParameters()
                .expectPromocode(PromocodeDiscountEntry.promocode(PROMO_CODE, PROMO_KEY)
                        .discount(Map.of(item.getOfferItemKey(), promocodeDiscount)));

        // When
        MultiCart cart = orderCreateHelper.cart(parameters);

        // Then
        MultiCartTotals totals = cart.getTotals();

        // price was reduced by the promocode amount
        BigDecimal priceWithDiscount = item.getBuyerPrice().subtract(promocodeDiscount);
        Assertions.assertEquals(priceWithDiscount.doubleValue(), totals.getBuyerItemsTotal().doubleValue(),
                ASSERTION_DELTA);

        // vat calculated with discount consideration
        BigDecimal expectedTotalVat = priceWithDiscount.multiply(BigDecimal.valueOf(vatRate));
        Assertions.assertEquals(expectedTotalVat.doubleValue(), totals.getVatTotal().doubleValue(), ASSERTION_DELTA);

        // promocode is listed among applied
        Assertions.assertFalse(cart.getCarts().get(0).getPromos().isEmpty());
        PromoDefinition promoCodeInfo = cart.getCarts().get(0).getPromos().get(0).getPromoDefinition();
        Assertions.assertEquals(PROMO_CODE, promoCodeInfo.getPromoCode());
        Assertions.assertEquals(PROMO_KEY, promoCodeInfo.getMarketPromoId());
        Assertions.assertEquals(
                promocodeDiscount.doubleValue(),
                cart.getCarts().get(0).getPromos().get(0).getBuyerItemsDiscount().doubleValue(),
                ASSERTION_DELTA);
    }

    @Test
    @DisplayName("По товару НДС считается с учетом скидки по промокоду")
    void orderItemVatCalculationTakesIntoAccountItemDiscounts() {
        // Given
        double vatRate = 0.2;
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        OrderItem item = parameters.getItems().iterator().next();

        // set up price without VAT from Report
        BigDecimal priceWithoutVat =
                FinancialUtils.roundPrice(item.getBuyerPrice().multiply(BigDecimal.valueOf(1.0 - vatRate)));
        item.getPrices().setPriceWithoutVat(priceWithoutVat);

        // set up promocode from Loyalty
        BigDecimal promocodeDiscount = BigDecimal.valueOf(10);
        parameters.getLoyaltyParameters()
                .expectPromocode(PromocodeDiscountEntry.promocode(PROMO_CODE, PROMO_KEY)
                        .discount(Map.of(item.getOfferItemKey(), promocodeDiscount)));

        // When
        MultiCart cart = orderCreateHelper.cart(parameters);

        // Then
        Order order = cart.getCarts().iterator().next();
        OrderItem orderItem = order.getItems().iterator().next();

        BigDecimal expectedPriceWithoutVat = item.getBuyerPrice().subtract(promocodeDiscount)
                .multiply(BigDecimal.valueOf(1 - vatRate));
        Assertions.assertEquals(expectedPriceWithoutVat.doubleValue(),
                orderItem.getPrices().getPriceWithoutVat().doubleValue(),
                ASSERTION_DELTA);
    }
}
