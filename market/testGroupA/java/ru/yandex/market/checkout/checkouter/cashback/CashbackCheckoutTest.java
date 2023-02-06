package ru.yandex.market.checkout.checkouter.cashback;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.shop.MarketplaceFeature;
import ru.yandex.market.checkout.checkouter.storage.promo.OrderItemPromoDao;
import ru.yandex.market.checkout.checkouter.storage.promo.OrderPromoDao;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkouter.entities.OrderItemPromoEntity;
import ru.yandex.market.checkouter.entities.OrderPromoEntity;
import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.CashbackRestrictionReason;
import ru.yandex.market.loyalty.api.model.CashbackType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.singleItemCashbackResponseWithThresholds;

public class CashbackCheckoutTest extends CashbackTestBase {

    @Autowired
    private OrderItemPromoDao itemPromoDao;
    @Autowired
    private OrderPromoDao orderPromoDao;

    @Test
    void shouldCheckoutWithEmitCashback() throws Exception {
        MultiCart cart = orderCreateHelper.cart(singleItemWithCashbackParams);
        singleItemWithCashbackParams.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        MultiOrder checkout = orderCreateHelper.checkout(cart, singleItemWithCashbackParams);
        Order order = checkout.getCarts().get(0);
        List<OrderPromoEntity> promosForOrders =
                orderPromoDao.getPromosForOrders(Collections.singleton(order.getId()));

        List<OrderPromoEntity> cashbackOrderPromo = promosForOrders.stream()
                .filter(promo -> promo.getType().equals(PromoType.CASHBACK.getCode()))
                .collect(Collectors.toList());
        assertThat(cashbackOrderPromo, is(not(empty())));

        List<OrderItemPromoEntity> itemPromos = itemPromoDao.getItemPromos(order.getItems()
                .stream()
                .map(OrderItem::getId)
                .collect(Collectors.toList()));

        assertThat(itemPromos, anyOf(
                cashbackOrderPromo.stream().map(promo -> hasItem(allOf(
                        hasProperty("orderPromoId", equalTo(promo.getId())),
                        hasProperty("cashbackAccrualAmount", comparesEqualTo(new BigDecimal("100.00"))),
                        hasProperty("cashbackSpendLimit", nullValue())
                ))).toArray(Matcher[]::new)
        ));
    }

    @Test
    void shouldCheckoutWithSpendCashback() throws Exception {
        MultiCart cart = orderCreateHelper.cart(singleItemWithCashbackParams);
        singleItemWithCashbackParams.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        MultiOrder checkout = orderCreateHelper.checkout(cart, singleItemWithCashbackParams);
        Order order = checkout.getCarts().get(0);

        List<OrderPromoEntity> promosForOrders =
                orderPromoDao.getPromosForOrders(Collections.singleton(order.getId()));

        List<OrderPromoEntity> cashbackOrderPromo = promosForOrders.stream()
                .filter(promo -> promo.getType().equals(PromoType.CASHBACK.getCode()))
                .collect(Collectors.toList());
        assertThat(cashbackOrderPromo, is(not(empty())));

        List<OrderItemPromoEntity> itemPromos =
                itemPromoDao.getItemPromos(order.getItems().stream().map(OrderItem::getId)
                        .collect(Collectors.toList()));

        assertThat(itemPromos, anyOf(
                cashbackOrderPromo.stream().map(promo -> hasItem(allOf(
                        hasProperty("orderPromoId", is(promo.getId())),
                        hasProperty("cashbackAccrualAmount", nullValue()),
                        hasProperty("cashbackSpendLimit", equalTo(new BigDecimal("30.00")))
                ))).toArray(Matcher[]::new)
        ));
    }

    @Test
    void shouldCheckoutWithCashbackThresholds() throws Exception {
        var singleItemWithCashbackThresholdsParams = BlueParametersProvider.defaultBlueOrderParameters();
        trustMockConfigurer.mockListWalletBalanceResponse();
        singleItemWithCashbackThresholdsParams.setCheckCartErrors(false);
        singleItemWithCashbackThresholdsParams.setupPromo("PROMO");
        singleItemWithCashbackThresholdsParams.getLoyaltyParameters()
                .setExpectedCashbackOptionsResponse(singleItemCashbackResponseWithThresholds());
        singleItemWithCashbackThresholdsParams.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        MultiCart cart = orderCreateHelper.cart(singleItemWithCashbackThresholdsParams);
        MultiOrder checkout = orderCreateHelper.checkout(cart, singleItemWithCashbackThresholdsParams);
        Order order = checkout.getCarts().get(0);
        List<OrderPromoEntity> promosForOrders =
                orderPromoDao.getPromosForOrders(Collections.singleton(order.getId()));

        List<OrderPromoEntity> cashbackOrderPromo = promosForOrders.stream()
                .filter(promo -> promo.getType().equals(PromoType.CASHBACK.getCode()))
                .collect(Collectors.toList());
        assertThat(cashbackOrderPromo, is(not(empty())));

        List<OrderItemPromoEntity> itemPromos = itemPromoDao.getItemPromos(order.getItems()
                .stream()
                .map(OrderItem::getId)
                .collect(Collectors.toList()));

        assertThat(itemPromos, anyOf(
                cashbackOrderPromo.stream().map(promo -> hasItem(allOf(
                        hasProperty("orderPromoId", equalTo(promo.getId())),
                        hasProperty("cashbackAccrualAmount", comparesEqualTo(new BigDecimal("100.00"))),
                        hasProperty("cashbackSpendLimit", nullValue())
                ))).toArray(Matcher[]::new)
        ));
    }

    //https://st.yandex-team.ru/MARKETCHECKOUT-16133
    @Test
    void shouldNotAddErrorIfSelectedOptionUnavailable() throws Exception {
        CashbackResponse response = new CashbackResponse(
                CashbackOptions.restricted(CashbackRestrictionReason.NOT_SUITABLE_CATEGORY),
                CashbackOptions.allowed(BigDecimal.valueOf(100)),
                CashbackType.EMIT
        );
        singleItemWithCashbackParams.getLoyaltyParameters().setCalcsExpectedCashbackResponse(response);
        singleItemWithCashbackParams.setCheckOrderCreateErrors(true);

        MultiCart cart = orderCreateHelper.cart(singleItemWithCashbackParams);
        MultiOrder checkout = orderCreateHelper.checkout(cart, singleItemWithCashbackParams);

//        assertThat(checkout.getValidationErrors(), hasItem(allOf(
//                hasProperty("code", is(LoyaltyErrorCodes.CASHBACK_UNAVAILABLE)),
//                hasProperty("message", is("Cashback unavailable"))
//        )));
        assertThat(checkout.getValidationErrors(), nullValue());
    }

    @Test
    @DisplayName("Проверяем, что в результате конвертации amount не выпишем возможный spend больше разрешенного " +
            "loyalty на позицию ")
    void shouldNotOverflowSpendAmount() throws Exception {
        singleItemWithCashbackParams.getItems().forEach(item -> item.setCount(4));
        singleItemWithCashbackParams.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        MultiCart cart = orderCreateHelper.cart(singleItemWithCashbackParams);
        MultiOrder checkout = orderCreateHelper.checkout(cart, singleItemWithCashbackParams);
        Order order = checkout.getCarts().get(0);

        List<OrderPromoEntity> promosForOrders =
                orderPromoDao.getPromosForOrders(Collections.singleton(order.getId()));

        List<OrderPromoEntity> cashbackOrderPromo = promosForOrders.stream()
                .filter(promo -> promo.getType().equals(PromoType.CASHBACK.getCode()))
                .collect(Collectors.toList());
        assertThat(cashbackOrderPromo, is(not(empty())));

        List<OrderItemPromoEntity> itemPromos =
                itemPromoDao.getItemPromos(order.getItems().stream().map(OrderItem::getId)
                        .collect(Collectors.toList()));

        assertThat(itemPromos, anyOf(
                cashbackOrderPromo.stream().map(promo -> hasItem(allOf(
                        hasProperty("orderPromoId", is(promo.getId())),
                        hasProperty("cashbackAccrualAmount", nullValue()),
                        hasProperty("cashbackSpendLimit", equalTo(new BigDecimal("7.00")))
                ))).toArray(Matcher[]::new)
        ));
    }


    @Test
    public void checkSpasiboDisabled() throws Exception {
        singleItemWithCashbackParams.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                CashbackOptions.restricted(CashbackRestrictionReason.CASHBACK_DISABLED),
                CashbackOptions.allowed(BigDecimal.valueOf(300)),
                CashbackType.SPEND
        ));
        MultiCart cart = orderCreateHelper.cart(singleItemWithCashbackParams);
        MultiOrder checkout = orderCreateHelper.checkout(cart, singleItemWithCashbackParams);
        Order order = checkout.getCarts().get(0);
        Set<MarketplaceFeature> validFeatures = order.getValidFeatures();
        assertFalse(validFeatures.contains(MarketplaceFeature.SPASIBO_PAY));
        assertTrue(validFeatures.contains(MarketplaceFeature.PLAINCPA));
    }
}
