package ru.yandex.market.checkout.checkouter.service.business;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.checkouter.promo.bundles.utils.LoyaltyTestUtils;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;
import static ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder.getExperiments;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemBuilder;
import static ru.yandex.market.checkout.test.providers.OrderProvider.orderBuilder;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.ALREADY_ZERO_PRICE;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.COIN_THRESHOLD_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.LARGE_SIZED_CART;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.REGION_WITHOUT_THRESHOLD;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.YA_PLUS_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.ALREADY_FREE;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.NO_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS;

public class LoyaltyDeliveryDiscountChangeTest extends AbstractWebTestBase {

    private static final String COIN_NOT_APPLICABLE_WARNING_CODE = "FREE_DELIVERY_COIN_NOT_APPLICABLE";
    private static final String YA_PLUS_NOT_APPLICABLE_WARNING_CODE = "FREE_DELIVERY_YA_PLUS_NOT_APPLICABLE";
    private static final String COIN_IS_NOW_APPLICABLE_WARNING_CODE = "FREE_DELIVERY_COIN_IS_NOW_APPLICABLE";
    private static final String YA_PLUS_IS_NOW_APPLICABLE_WARNING_CODE = "FREE_DELIVERY_YA_PLUS_IS_NOW_APPLICABLE";

    @Autowired
    private LoyaltyService loyaltyService;

    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                new Object[]{ALREADY_FREE, YA_PLUS_FREE_DELIVERY,
                        NO_FREE_DELIVERY, LARGE_SIZED_CART, YA_PLUS_NOT_APPLICABLE_WARNING_CODE},
                new Object[]{NO_FREE_DELIVERY, REGION_WITHOUT_THRESHOLD,
                        ALREADY_FREE, YA_PLUS_FREE_DELIVERY, YA_PLUS_IS_NOW_APPLICABLE_WARNING_CODE},
                new Object[]{NO_FREE_DELIVERY, REGION_WITHOUT_THRESHOLD,
                        ALREADY_FREE, COIN_THRESHOLD_FREE_DELIVERY, COIN_IS_NOW_APPLICABLE_WARNING_CODE},
                new Object[]{WILL_BE_FREE_WITH_MORE_ITEMS, COIN_THRESHOLD_FREE_DELIVERY,
                        ALREADY_FREE, COIN_THRESHOLD_FREE_DELIVERY, COIN_IS_NOW_APPLICABLE_WARNING_CODE},
                new Object[]{ALREADY_FREE, COIN_THRESHOLD_FREE_DELIVERY,
                        WILL_BE_FREE_WITH_MORE_ITEMS, COIN_THRESHOLD_FREE_DELIVERY, COIN_NOT_APPLICABLE_WARNING_CODE},
                new Object[]{ALREADY_FREE, COIN_THRESHOLD_FREE_DELIVERY,
                        NO_FREE_DELIVERY, REGION_WITHOUT_THRESHOLD, COIN_NOT_APPLICABLE_WARNING_CODE}
        ).map(Arguments::of);
    }

    @ParameterizedTest(name = "statusBefore={0}, reasonBefore={1}, statusAfter={2}, reasonAfter={3}, " +
            "expectedWarningCode={4}")
    @MethodSource("parameterizedTestData")
    public void shouldCreateDeliveryDiscountChangeWarnings(FreeDeliveryStatus statusBefore,
                                                           FreeDeliveryReason reasonBefore,
                                                           FreeDeliveryStatus statusAfter,
                                                           FreeDeliveryReason reasonAfter,
                                                           String expectedWarningCode) {
        var orderItemBuilder = orderItemBuilder()
                .offer(PRIMARY_OFFER)
                .price(1337);
        var cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(orderItemBuilder));
        cart.setFreeDeliveryStatus(statusBefore);
        cart.setFreeDeliveryReason(reasonBefore);

        var requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters().setFreeDeliveryStatus(statusAfter);
        requestParameters.getLoyaltyParameters().setFreeDeliveryReason(reasonAfter);

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContext(orderItemBuilder, requestParameters.getBuiltMultiCart()));

        assertThat(cart.getValidationWarnings(), hasSize(1));
        var warning = Iterables.getOnlyElement(cart.getValidationWarnings());
        assertThat(warning.getCode(), is(expectedWarningCode));
        if (warning.getMessage() != null) {
            assertThat(warning.getMessage(), allOf(
                    containsString(statusAfter.getCode()),
                    containsString(statusAfter.getRusDescription())
            ));
        }
    }

    @Test
    public void shouldNotCreateDeliveryDiscountChangeWarningsWhenDeliveryHasNotBeenChanged() {
        var orderItemBuilder = orderItemBuilder()
                .offer(PRIMARY_OFFER)
                .price(1337);
        var cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(orderItemBuilder));
        cart.setFreeDeliveryStatus(ALREADY_FREE);
        cart.setFreeDeliveryReason(YA_PLUS_FREE_DELIVERY);

        var requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters().setFreeDeliveryStatus(ALREADY_FREE);
        requestParameters.getLoyaltyParameters().setFreeDeliveryReason(YA_PLUS_FREE_DELIVERY);

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContext(orderItemBuilder, requestParameters.getBuiltMultiCart()));

        assertThat(cart.getValidationWarnings(), is(nullValue()));
    }

    @Test
    public void shouldNotCreateDeliveryDiscountChangeWarningsWhenFreeDeliveryHasSomeOtherReason() {
        var orderItemBuilder = orderItemBuilder()
                .offer(PRIMARY_OFFER)
                .price(1337);
        var cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(orderItemBuilder));
        cart.setFreeDeliveryStatus(ALREADY_FREE);
        cart.setFreeDeliveryReason(ALREADY_ZERO_PRICE);

        var requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters().setFreeDeliveryStatus(WILL_BE_FREE_WITH_MORE_ITEMS);
        requestParameters.getLoyaltyParameters().setFreeDeliveryReason(COIN_THRESHOLD_FREE_DELIVERY);

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContext(orderItemBuilder, requestParameters.getBuiltMultiCart()));

        assertThat(cart.getValidationWarnings(), is(nullValue()));
    }

    @Test
    public void shouldApplySupplierMultiCartPromoWhenSupplierDiscountExist() {
        var orderItemBuilder = orderItemBuilder()
                .offer(PRIMARY_OFFER)
                .price(1337);
        Order order1 = createOrder(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        Order order2 = createOrder(BigDecimal.valueOf(49), BigDecimal.ZERO, BigDecimal.valueOf(49));
        Order order3 = createOrder(BigDecimal.valueOf(99), BigDecimal.valueOf(50), BigDecimal.valueOf(99));
        var cart = MultiCartProvider.buildMultiCart(List.of(order1, order2, order3));
        var requestParameters = new Parameters();
        enableUnifiedTariffs(requestParameters);
        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContext(orderItemBuilder, requestParameters.getBuiltMultiCart()));

        assertThat(cart.getCarts().get(0).getDelivery().getPrice(), equalTo(BigDecimal.ZERO));
        assertThat(cart.getCarts().get(1).getDelivery().getPrice(), equalTo(BigDecimal.valueOf(49)));
        assertThat(cart.getCarts().get(2).getDelivery().getPrice(), equalTo(BigDecimal.valueOf(49)));
    }

    private Order createOrder(BigDecimal supplierPrice, BigDecimal supplierDiscount, BigDecimal price) {
        var orderItemBuilder = orderItemBuilder()
                .offer(PRIMARY_OFFER)
                .price(1337);
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildPickupDeliveryResponse();
        deliveryResponse.setSupplierPrice(supplierPrice);
        deliveryResponse.setSupplierDiscount(supplierDiscount);
        deliveryResponse.setPrice(price);
        return orderBuilder()
                .color(Color.WHITE)
                .delivery(deliveryResponse)
                .someLabel()
                .stubApi()
                .itemBuilder(orderItemBuilder)
                .build();
    }

    private LoyaltyContext createTestContext(OrderItemProvider.OrderItemBuilder orderItemBuilder, MultiCart multiCart) {
        var foundOffers = Collections.singletonList(FoundOfferBuilder.createFrom(orderItemBuilder.build())
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.GENERIC_BUNDLE.getCode())
                .build());
        return LoyaltyTestUtils.createTestContext(multiCart, foundOffers);
    }

    private void enableUnifiedTariffs(Parameters parameters) {
        checkouterProperties.setEnableUnifiedTariffs(true);
        String experiment = MARKET_UNIFIED_TARIFFS + "=" + MARKET_UNIFIED_TARIFFS_VALUE;
        CheckoutContextHolder.setExperiments(getExperiments().with(Set.of(experiment)));
        parameters.setExperiments(getExperiments().with(Set.of(experiment)));
    }
}
