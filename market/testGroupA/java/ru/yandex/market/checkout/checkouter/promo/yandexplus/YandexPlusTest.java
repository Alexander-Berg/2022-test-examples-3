package ru.yandex.market.checkout.checkouter.promo.yandexplus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Iterables;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.promo.AbstractPromoTestBase;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.checkout.util.matching.NumberMatcher;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.YANDEX_PLUS;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.DELIVERY_PRICE;

public class YandexPlusTest extends AbstractPromoTestBase {

    private Parameters parameters;

    @BeforeEach
    public void init() {
        parameters = createParameters();
    }

    @Test
    @DisplayName("Проверем, что Яндекс.Плюс заказ создается и стоимость доставки обнуляется")
    public void testYandexPlusCheckout() {
        parameters.setYandexPlus(true);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(100501L)
                        .addPickup(123L)
                        .build()
        );
        parameters.setYandexPlus(true);
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(DeliveryType.COURIER, new LoyaltyDiscount(BigDecimal.valueOf(200), YANDEX_PLUS));

        parameters.checkoutResultActions()
                .andExpect(jsonPath("$.orders[*].delivery.buyerPrice", "DELIVERY")
                        .value(hasItem(0)))
                .andExpect(jsonPath("$.orders[*].delivery.buyerDiscount", "DELIVERY")
                        .value(hasItem(DELIVERY_PRICE.intValue())))
                .andExpect(jsonPath("$.orders[*].delivery.promos[*].type", "DELIVERY")
                        .value(hasItem(YANDEX_PLUS.getCode())))
                .andExpect(jsonPath("$.orders[*].delivery.promos[*].buyerDiscount", "DELIVERY")
                        .value(hasItem(DELIVERY_PRICE.intValue())));

        parameters.configureMultiCart(multiCart -> multiCart.getCarts()
                .forEach(cart -> cart.setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress())));

        Long orderId = orderCreateHelper.createOrder(parameters).getId();

        Order order = orderService.getOrder(orderId);
        Set<? extends ItemPromo> promos = order.getDelivery().getPromos();

        ItemPromo deliveryItemPromo = Iterables.getFirst(promos, null);
        assertThat(deliveryItemPromo.getType(), is(YANDEX_PLUS));
        assertThat(deliveryItemPromo.getBuyerDiscount(), NumberMatcher.numberEqualsTo(DELIVERY_PRICE));

        OrderPromo orderPromo = order.getPromos()
                .stream()
                .filter(p -> p.getType() == YANDEX_PLUS)
                .findAny()
                .orElseThrow(() -> new RuntimeException("No promo YANDEX_PLUS"));
        assertThat(orderPromo.getDeliveryDiscount(), NumberMatcher.numberEqualsTo(DELIVERY_PRICE));
        // Я+ в варианте loyalty работает только при передаче региона в operationContext.
        loyaltyConfigurer.servedEvents().forEach(
                se -> {
                    LoggedRequest request = se.getRequest();
                    if (request.getUrl().contains("discount")) {
                        JsonTest.checkJsonMatcher(request.getBodyAsString(),
                                "$.operationContext.regionId",
                                equalTo(parameters.getBuiltMultiCart().getBuyerRegionId().intValue())
                        );
                    }
                }
        );
    }

    @Test
    public void testYandexPlusCart() {
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(123L)
                        .addPickup(123L)
                        .build()
        );
        parameters.setYandexPlus(true);
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(DeliveryType.COURIER, new LoyaltyDiscount(BigDecimal.valueOf(200), YANDEX_PLUS));

        parameters.cartResultActions()
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].buyerPrice", "DELIVERY")
                        .value(hasItem(0)))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].buyerPriceBeforeDiscount", "DELIVERY")
                        .value(hasItem(DELIVERY_PRICE.intValue())))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].type", "DELIVERY")
                        .value(hasItem(YANDEX_PLUS.getCode())))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].buyerDiscount", "DELIVERY")
                        .value(hasItem(DELIVERY_PRICE.intValue())));

        parameters.configureMultiCart(multiCart -> multiCart.getCarts()
                .forEach(cart -> cart.setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress())));

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        List<? extends Delivery> filteredOptions = multiCart.getCarts().get(0).getDeliveryOptions().stream()
                .filter(o -> CollectionUtils.isNotEmpty(o.getPromos()))
                .collect(Collectors.toList());

        assertThat(filteredOptions, hasSize(greaterThanOrEqualTo(1)));

        Optional<? extends ItemPromo> optionalItemPromo = filteredOptions.stream()
                .filter(Delivery::isFree)
                .flatMap(d -> d.getPromos().stream())
                .filter(promo -> promo.getType() == YANDEX_PLUS)
                .findFirst();

        assertThat(optionalItemPromo.isPresent(), is(true));
    }
}
