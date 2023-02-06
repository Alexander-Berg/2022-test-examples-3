package ru.yandex.market.checkout.checkouter.promo.promocode;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;

/**
 * Проверяет, что мы корректно считаем субсидию, если несколько раз нам передают сниженную buyerPrice(после
 * применения промокода)
 *
 * @author Nikolai Iusiumbeli
 * date: 01/11/2017
 */
public class NominalBuyerPriceTest extends AbstractWebTestBase {

    private static final String PROMO_CODE = "PROMO_CODE";
    private static final BigDecimal BUYER_PRICE_NOMINAL = BigDecimal.valueOf(1000);
    private static final BigDecimal SHOP_PRICE_CHANGED = BigDecimal.valueOf(900);
    private Parameters parameters;
    private OrderItem orderItem;


    @BeforeEach
    public void setUp() throws Exception {
        orderItem = OrderItemProvider.getOrderItem();
        orderItem.setMsku(123L);
        orderItem.setShopSku("some offer");
        orderItem.setBuyerPrice(BUYER_PRICE_NOMINAL);
        orderItem.setPrice(BUYER_PRICE_NOMINAL); //for pushapi mock
        orderItem.getPrices().setBuyerPriceNominal(null);

        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().setItems(Collections.singleton(orderItem));
    }

    @Test
    public void testSetNominalBuyerPrice() throws Exception {
        MultiCart cart = orderCreateHelper.cart(parameters);
        OrderItem cartItem = cart.getCarts().get(0).getItem(orderItem.getFeedOfferId());
        assertThat(cartItem.getBuyerPrice(), numberEqualsTo(BUYER_PRICE_NOMINAL));
        assertThat(cartItem.getPrices().getBuyerPriceNominal(), numberEqualsTo(BUYER_PRICE_NOMINAL));
    }

    @Test
    public void testChangeNominalBuyerPrice() {
        parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId()).getPrices().value =
                SHOP_PRICE_CHANGED;
        parameters.turnOffErrorChecks();

        OrderItem cartItem = orderCreateHelper.cart(parameters).getCarts().get(0).getItem(orderItem.getFeedOfferId());
        assertThat(cartItem.getChanges(), hasItem(ItemChange.PRICE));
        assertThat(cartItem.getBuyerPrice(), numberEqualsTo(SHOP_PRICE_CHANGED));
        assertThat(cartItem.getPrices().getBuyerPriceNominal(), numberEqualsTo(SHOP_PRICE_CHANGED));
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void testPromoCodeApply() throws Exception {
        parameters.setupPromo(PROMO_CODE);
        parameters.setMockLoyalty(true);
        parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId()).getPrices().value =
                BUYER_PRICE_NOMINAL;

        orderItem.setBuyerPrice(BigDecimal.valueOf(200));
        orderItem.getPrices().setBuyerPriceNominal(BUYER_PRICE_NOMINAL);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getPromoCode(), notNullValue());

        Order order = multiCart.getCarts().get(0);
        OrderItem cartItem = order.firstItemFor(orderItem.getFeedOfferId());
        assertThat(cartItem.getBuyerPrice(),
                numberEqualsTo(BUYER_PRICE_NOMINAL.subtract(LoyaltyDiscount.TEST_ITEM_SUBSIDY_VALUE)));
        assertThat(cartItem.getPrices().getBuyerPriceNominal(), numberEqualsTo(BUYER_PRICE_NOMINAL));

        //повторная отправка этой же корзины в карт
        long buyerRegionId = 213L;
        order.setDelivery(new Delivery(buyerRegionId));
        multiCart.setBuyerRegionId(buyerRegionId);
        multiCart.setPaymentMethod(PaymentMethod.YANDEX);
        multiCart.setPaymentType(PaymentType.PREPAID);

        Parameters parameters = new Parameters(multiCart);
        parameters.setMockLoyalty(true);
        parameters.getReportParameters().setShopSupportsSubsidies(true);
        parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId()).getPrices().value =
                BUYER_PRICE_NOMINAL;

        MultiCart cart2 = orderCreateHelper.cart(parameters);
        OrderItem cartItem2 = cart2.getCarts().get(0).firstItemFor(orderItem.getFeedOfferId());
        assertThat(cartItem2.getBuyerPrice(),
                numberEqualsTo(BUYER_PRICE_NOMINAL.subtract(LoyaltyDiscount.TEST_ITEM_SUBSIDY_VALUE)));
        assertThat(cartItem2.getPrices().getBuyerPriceNominal(), numberEqualsTo(BUYER_PRICE_NOMINAL));

    }

    @Test
    public void testApplyPromoCodeWhenShopPriceChanged() {
        parameters.setupPromo(PROMO_CODE);
        parameters.setMockLoyalty(true);
        parameters.turnOffErrorChecks();

        orderItem.setBuyerPrice(BigDecimal.TEN);
        parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId()).getPrices().value =
                SHOP_PRICE_CHANGED;
        orderItem.getPrices().setBuyerPriceNominal(BUYER_PRICE_NOMINAL);

        OrderItem cartItem = orderCreateHelper.cart(parameters).getCarts().get(0).getItem(orderItem.getFeedOfferId());
        assertThat(cartItem.getChanges(), hasItem(ItemChange.PRICE));
        assertThat(cartItem.getBuyerPrice(),
                numberEqualsTo(SHOP_PRICE_CHANGED.subtract(LoyaltyDiscount.TEST_ITEM_SUBSIDY_VALUE)));
        assertThat(cartItem.getPrices().getBuyerPriceNominal(), numberEqualsTo(SHOP_PRICE_CHANGED));
        assertThat(cartItem.getPromos(), notNullValue());
    }
}
