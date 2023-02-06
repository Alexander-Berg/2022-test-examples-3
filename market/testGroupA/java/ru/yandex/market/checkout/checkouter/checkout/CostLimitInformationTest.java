package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.CostLimitInformation;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.checkout.util.loyalty.LoyaltyParameters;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.common.util.ObjectUtils.avoidNull;
import static ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams.X_EXPERIMENTS;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_HIT_RATE_GROUP;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_MARKET_REQUEST_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_ORDER_META;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.IGNORE_DISCOUNT_ON_BUNDLES_WHEN_CALCULATING_CART_MIN_COST;
import static ru.yandex.market.checkout.checkouter.feature.type.common.MapFeatureType.MULTI_CART_MIN_COSTS_BY_REGION;
import static ru.yandex.market.checkout.checkouter.feature.type.common.MapFeatureType.MULTI_CART_MIN_COSTS_BY_REGION_EXPERIMENT;

public class CostLimitInformationTest extends AbstractWebTestBase {

    public static final long MOSCOW_REGION = 213L;
    public static final long RUSSIA_REGION = 225L;
    private Parameters parameters;

    @BeforeEach
    void setUp() {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Delivery delivery = parameters.getOrder().getDelivery();
        assertThat(delivery, is(notNullValue()));
        assertThat(delivery.getDeliveryPartnerType(), is(not(DeliveryPartnerType.SHOP)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldReturnTooCheapMultiCart(boolean ignoreDiscountOnBundles) throws Exception {
        BigDecimal minCostInMoscow = BigDecimal.valueOf(5000);
        checkouterFeatureWriter.writeValue(IGNORE_DISCOUNT_ON_BUNDLES_WHEN_CALCULATING_CART_MIN_COST,
                ignoreDiscountOnBundles);
        checkouterFeatureWriter.writeValue(MULTI_CART_MIN_COSTS_BY_REGION, Map.of(
                RUSSIA_REGION, BigDecimal.ZERO,
                MOSCOW_REGION, minCostInMoscow
        ));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertTooCheapToCheckout(multiCart, minCostInMoscow);

        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertTooCheapToCheckout(multiOrder, minCostInMoscow);
    }

    @DisplayName("Тест проверяет минимальную стоимость корзины с учётом скидок по комплектам")
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldReturnTooCheapMultiCartWithBundlePromo(boolean ignoreDiscountOnBundles) throws Exception {
        BigDecimal minCostInMoscow = BigDecimal.valueOf(200);
        checkouterFeatureWriter.writeValue(IGNORE_DISCOUNT_ON_BUNDLES_WHEN_CALCULATING_CART_MIN_COST,
                ignoreDiscountOnBundles);
        checkouterFeatureWriter.writeValue(MULTI_CART_MIN_COSTS_BY_REGION, Map.of(
                RUSSIA_REGION, BigDecimal.ZERO,
                MOSCOW_REGION, minCostInMoscow
        ));
        parameters.setMockLoyalty(true);
        final LoyaltyParameters loyaltyParameters = parameters.getLoyaltyParameters();
        loyaltyParameters.clearDiscounts();
        parameters.getOrder().getItems().forEach(
                item -> loyaltyParameters.addLoyaltyDiscount(item,
                        new LoyaltyDiscount(BigDecimal.valueOf(100L), PromoType.BLUE_FLASH))
        );

        if (ignoreDiscountOnBundles) {
            checkSuccessOrderCreation(minCostInMoscow, parameters);
        } else {
            MultiCart multiCart = orderCreateHelper.cart(parameters);
            assertTooCheapToCheckout(multiCart, minCostInMoscow, BigDecimal.valueOf(50L));

            MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
            assertTooCheapToCheckout(multiOrder, minCostInMoscow, BigDecimal.valueOf(50L));
        }
    }

    @DisplayName("Тест проверяет минимальную стоимость корзины с учётом скидок по купону")
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldReturnEmptyErrorListWithCouponPromo(boolean ignoreDiscountOnBundles) throws Exception {
        BigDecimal minCostInMoscow = BigDecimal.valueOf(200);
        checkouterFeatureWriter.writeValue(IGNORE_DISCOUNT_ON_BUNDLES_WHEN_CALCULATING_CART_MIN_COST,
                ignoreDiscountOnBundles);
        checkouterFeatureWriter.writeValue(MULTI_CART_MIN_COSTS_BY_REGION, Map.of(
                RUSSIA_REGION, BigDecimal.ZERO,
                MOSCOW_REGION, minCostInMoscow
        ));
        parameters.setMockLoyalty(true);
        parameters.getBuiltMultiCart().setPromoCode("SOME_PROMO");
        final LoyaltyParameters loyaltyParameters = parameters.getLoyaltyParameters();
        loyaltyParameters.clearDiscounts();
        parameters.getOrder().getItems().forEach(
                item -> loyaltyParameters.addLoyaltyDiscount(item,
                        new LoyaltyDiscount(BigDecimal.valueOf(100L), PromoType.MARKET_COUPON))
        );

        checkSuccessOrderCreation(minCostInMoscow, parameters);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldReturnEmptyErrorListWhenMultiCartCostExceedsThreshold(boolean ignoreDiscountOnBundles)
            throws Exception {
        BigDecimal minCostInMoscow = BigDecimal.valueOf(50);
        checkouterFeatureWriter.writeValue(IGNORE_DISCOUNT_ON_BUNDLES_WHEN_CALCULATING_CART_MIN_COST,
                ignoreDiscountOnBundles);
        checkouterFeatureWriter.writeValue(MULTI_CART_MIN_COSTS_BY_REGION, Map.of(
                RUSSIA_REGION, BigDecimal.ZERO,
                MOSCOW_REGION, minCostInMoscow
        ));

        checkSuccessOrderCreation(minCostInMoscow, parameters);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldReturnTooCheapMultiCartForRussiaThreshold(boolean ignoreDiscountOnBundles) throws Exception {
        BigDecimal minCostInRussia = BigDecimal.valueOf(5000);
        checkouterFeatureWriter.writeValue(IGNORE_DISCOUNT_ON_BUNDLES_WHEN_CALCULATING_CART_MIN_COST,
                ignoreDiscountOnBundles);
        checkouterFeatureWriter.writeValue(MULTI_CART_MIN_COSTS_BY_REGION, Map.of(
                RUSSIA_REGION, minCostInRussia
        ));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertTooCheapToCheckout(multiCart, minCostInRussia);

        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertTooCheapToCheckout(multiOrder, minCostInRussia);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldReturnTooCheapMultiCartForRussiaExperimentThresholdTest(boolean ignoreDiscountOnBundles)
            throws Exception {
        BigDecimal minCostInRussiaTest = BigDecimal.valueOf(5000);
        MultiOrder multiOrder = commonMultiCartExperimentThresholdTest(minCostInRussiaTest, ignoreDiscountOnBundles);
        assertTooCheapToCheckout(multiOrder, minCostInRussiaTest);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void successMultiCartForRussiaExperimentThresholdTest(boolean ignoreDiscountOnBundles) throws Exception {
        BigDecimal minCostInRussiaTest = BigDecimal.valueOf(250);
        MultiOrder multiOrder = commonMultiCartExperimentThresholdTest(minCostInRussiaTest, ignoreDiscountOnBundles);
        assertNoErrors(multiOrder, minCostInRussiaTest);
    }

    @SuppressWarnings("checkstyle:HiddenField")
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

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        List<OrderPromo> orderPromos = multiCart.getCarts().get(0).getPromos();
        assertThat(orderPromos, hasSize(1));
        assertThat(orderPromos.get(0).getPromoDefinition().getType(), equalTo(PromoType.MARKET_BLUE));
        assertTooCheapToCheckout(multiCart, minCostInMoscow);
    }

    private MultiOrder commonMultiCartExperimentThresholdTest(
            BigDecimal minCostInRussiaTest,
            boolean ignoreDiscountOnBundles
    ) throws Exception {
        checkouterFeatureWriter.writeValue(IGNORE_DISCOUNT_ON_BUNDLES_WHEN_CALCULATING_CART_MIN_COST,
                ignoreDiscountOnBundles);
        checkouterFeatureWriter.writeValue(MULTI_CART_MIN_COSTS_BY_REGION, Map.of(
                RUSSIA_REGION, BigDecimal.ZERO
        ));

        checkouterFeatureWriter.writeValue(MULTI_CART_MIN_COSTS_BY_REGION_EXPERIMENT, Map.of(
                RUSSIA_REGION, minCostInRussiaTest
        ));

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        HttpHeaders headers = new HttpHeaders();
        headers.set(
                X_HIT_RATE_GROUP,
                avoidNull(
                        parameters.configuration().cart().request().getHitRateGroup(),
                        HitRateGroup.UNLIMIT
                ).name()
        );
        if (parameters.configuration().cart().request().getMetaInfo() != null) {
            headers.set(X_ORDER_META, parameters.configuration().cart().request().getMetaInfo());
        }
        if (parameters.configuration().cart().request().getMarketRequestId() != null) {
            headers.set(X_MARKET_REQUEST_ID, parameters.configuration().cart().request().getMarketRequestId());
        }
        headers.set(X_EXPERIMENTS, "market_checkouter_cart_threshold=1");

        return orderCreateHelper.checkout(multiCart, parameters, headers);
    }

    private void checkSuccessOrderCreation(BigDecimal minCost, Parameters parameters) throws Exception {
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertNoErrors(multiCart, minCost);

        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertNoErrors(multiOrder, minCost);
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

    private void assertNoErrors(MultiCart multiCart, BigDecimal minCost) {
        CostLimitInformation costLimitInfo = multiCart.getCostLimitInformation();
        assertNotNull(costLimitInfo);
        assertThat(costLimitInfo.getMinCost(), equalTo(minCost));
        assertThat(costLimitInfo.getRemainingBeforeCheckout(), equalTo(BigDecimal.ZERO));
        assertThat(costLimitInfo.getErrors(), empty());
        assertTrue(multiCart.isValid());
    }
}
