package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.CostLimitInformation;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.credit.CreditInformation;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus;
import ru.yandex.market.loyalty.api.model.discount.PriceLeftForFreeDeliveryResponseV3;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.actualization.multicart.processor.JewelryValidatorAndPaymentOptionsUpdater.JEWELRY_COST_LIMIT_200K;
import static ru.yandex.market.checkout.checkouter.actualization.multicart.processor.JewelryValidatorAndPaymentOptionsUpdater.JEWELRY_PASSPORT_REQUIRED;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.IGNORE_DISCOUNT_ON_BUNDLES_WHEN_CALCULATING_CART_MIN_COST;
import static ru.yandex.market.checkout.checkouter.feature.type.common.MapFeatureType.MULTI_CART_MIN_COSTS_BY_REGION;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.ALICE_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.COIN_THRESHOLD_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.ALREADY_FREE;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS;

public class MultiCartActualizeV2Test extends AbstractWebTestBase {

    @Autowired
    protected CheckouterProperties checkouterProperties;

    public static final long MOSCOW_REGION = 213L;
    public static final long RUSSIA_REGION = 225L;
    private static final int JEWELRY_CATEGORY = 91275;

    @Test
    public void testDeliveryDiscountMapForwarding() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(COIN_THRESHOLD_FREE_DELIVERY,
                        PriceLeftForFreeDeliveryResponseV3
                                .builder()
                                .setPriceLeftForFreeDelivery(BigDecimal.TEN)
                                .setStatus(WILL_BE_FREE_WITH_MORE_ITEMS)
                                .setThreshold(BigDecimal.ONE)
                                .build());
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(FreeDeliveryReason.ALICE_FREE_DELIVERY,
                        PriceLeftForFreeDeliveryResponseV3
                                .builder()
                                .setPriceLeftForFreeDelivery(BigDecimal.ZERO)
                                .setStatus(FreeDeliveryStatus.ALREADY_FREE)
                                .setThreshold(BigDecimal.TEN)
                                .build());

        var cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(parameters);

        var deliveryDiscountMap = cart.getDeliveryDiscountMap();
        assertThat(deliveryDiscountMap, is(notNullValue()));
        assertThat(deliveryDiscountMap.size(), is(2));

        var first = deliveryDiscountMap.get(COIN_THRESHOLD_FREE_DELIVERY);
        assertThat(first.getStatus(), is(WILL_BE_FREE_WITH_MORE_ITEMS));
        assertThat(first.getPriceLeftForFreeDelivery(), is(BigDecimal.TEN));
        assertThat(first.getThreshold(), is(BigDecimal.ONE));

        var second = deliveryDiscountMap.get(ALICE_FREE_DELIVERY);
        assertThat(second.getStatus(), is(ALREADY_FREE));
        assertThat(second.getPriceLeftForFreeDelivery(), is(BigDecimal.ZERO));
        assertThat(second.getThreshold(), is(BigDecimal.TEN));
    }

    @Test
    public void testCreditInformation() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(parameters);
        CreditInformation creditInformation = cart.getCreditInformation();
        assertNotNull(creditInformation);
        assertEquals(BigDecimal.valueOf(3500), creditInformation.getPriceForCreditAllowed());
        assertEquals(BigDecimal.valueOf(605), creditInformation.getCreditMonthlyPayment());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldReturnTooCheapMultiCartWhenOldPriceExceedsThreshold(boolean ignoreDiscountOnBundles) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        checkouterFeatureWriter.writeValue(IGNORE_DISCOUNT_ON_BUNDLES_WHEN_CALCULATING_CART_MIN_COST,
                ignoreDiscountOnBundles);

        OrderItem firstItem = parameters.getOrder().getItems().iterator().next();
        FeedOfferId feedOfferId = firstItem.getFeedOfferId();
        ItemInfo itemInfo = parameters.getReportParameters().overrideItemInfo(feedOfferId);
        itemInfo.getPrices().discountOldMin = firstItem.getBuyerPrice().add(BigDecimal.valueOf(5001));
        itemInfo.setSupplierType(SupplierType.THIRD_PARTY);

        BigDecimal minCostInMoscow = BigDecimal.valueOf(5000);

        checkouterFeatureWriter.writeValue(MULTI_CART_MIN_COSTS_BY_REGION, Map.of(
                RUSSIA_REGION, BigDecimal.ZERO,
                MOSCOW_REGION, minCostInMoscow
        ));

        MultiCart multiCart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(parameters);
        Set<ItemPromo> orderPromos = multiCart.getCarts().get(0).getItems().iterator().next().getPromos();
        assertThat(orderPromos, hasSize(1));
        assertThat(orderPromos.iterator().next().getPromoDefinition().getType(), equalTo(PromoType.MARKET_BLUE));
        assertTooCheapToCheckout(multiCart, minCostInMoscow);
    }

    private void assertTooCheapToCheckout(MultiCart multiCart, BigDecimal minCost) {
        assertTooCheapToCheckout(multiCart, minCost, minCost.subtract(BigDecimal.valueOf(250)));
    }

    private void assertTooCheapToCheckout(@Nonnull MultiCart multiCart,
                                          @Nonnull BigDecimal minCost,
                                          @Nonnull BigDecimal remainingBeforeCheckout) {
        CostLimitInformation costLimitInfo = multiCart.getCostLimitInformation();
        assertNotNull(costLimitInfo);
        assertThat(costLimitInfo.getMinCost(), equalTo(minCost));
        assertThat(costLimitInfo.getRemainingBeforeCheckout(), equalTo(remainingBeforeCheckout));
        assertThat(costLimitInfo.getErrors(), contains(CostLimitInformation.Code.TOO_CHEAP_MULTI_CART));
        assertTrue(multiCart.isValid());
    }

    @Test
    public void createDbsJewelryPassportRequiredTest() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        OrderItem item = parameters.getOrder().getItems().iterator().next();
        item.setCount(200); //200 * 250 RUB
        item.setCategoryId(91280);
        parameters.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(item)
                .categoryId(JEWELRY_CATEGORY)
                .deliveryPartnerType("SHOP")
                .build()));
        parameters.getReportParameters().setDeliveryPartnerTypes(List.of("SHOP"));
        MultiCart actualCart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(parameters);
        assertTrue(actualCart.getCarts().get(0).getValidationWarnings().stream().anyMatch(
                w -> w.getCode().equals(JEWELRY_PASSPORT_REQUIRED)));
    }

    @Test
    public void createFbsJewelryCostLimitErrorTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        OrderItem item = parameters.getOrder().getItems().iterator().next();
        item.setCount(1000); // 1000 * 250
        item.setCategoryId(91280);

        parameters.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(item)
                .deliveryPartnerType("YANDEX_MARKET")
                .categoryId(JEWELRY_CATEGORY)
                .build()));
        parameters.getReportParameters().setDeliveryPartnerTypes(List.of("YANDEX_MARKET"));

        MultiCart actualCart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(parameters);
        assertTrue(actualCart.getCarts().get(0).getValidationErrors().stream().anyMatch(
                w -> w.getCode().equals(JEWELRY_COST_LIMIT_200K)));
    }
}
