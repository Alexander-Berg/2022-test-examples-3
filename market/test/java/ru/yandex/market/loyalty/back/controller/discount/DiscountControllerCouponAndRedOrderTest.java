package ru.yandex.market.loyalty.back.controller.discount;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.CouponDto;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.OrderItemResponse;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesRequest;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesResponse;
import ru.yandex.market.loyalty.api.model.red.RedOrder;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.core.dao.accounting.OperationContextDao;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredMetaTransactionService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.service.mail.AlertNotificationService;
import ru.yandex.market.loyalty.core.service.mail.YabacksMailer;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.BUDGET_EXCEEDED;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COUPON_ALREADY_SPENT;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COUPON_EXPIRED;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COUPON_NOT_APPLICABLE;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COUPON_NOT_EXISTS;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.INSUFFICIENT_TOTAL;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.OVERDRAFT_EXCEEDED;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.PROMO_NOT_ACTIVE;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.REVERT_TOKEN_NOT_FOUND;
import static ru.yandex.market.loyalty.core.service.coupon.CouponCode.of;
import static ru.yandex.market.loyalty.core.service.discount.ItemPromoCalculation.calculateTotalDiscount;
import static ru.yandex.market.loyalty.core.service.mail.AlertNotificationService.DEFAULT_ALERT_PROMO_EMISSION_BUDGET;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.STICK_CATEGORY;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.SUPPLIER_EXCLUSION_ID;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.generateWith;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.same;
import static ru.yandex.market.loyalty.core.utils.DiscountResponseUtil.hasCouponError;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_MSKU;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_QUANTITY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.OrderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.keyOf;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.msku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderItemBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.supplier;
import static ru.yandex.market.loyalty.core.utils.SequenceCustomizer.compose;
import static ru.yandex.market.loyalty.lightweight.CommonUtils.getNonNullValue;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

/**
 * @author maratik
 */
@TestFor(DiscountController.class)
public class DiscountControllerCouponAndRedOrderTest extends DiscountControllerGenerationSupportedTest {

    private static final double MAXIMUM_ALLOWED_DISCOUNT_OVERDRAFT_FOR_PENNIES = 0.01;
    private static final double MAXIMUM_ALLOWED_DISCOUNT_OVERDRAFT_FOR_RUBLES = 1;
    private static final String COUPON_NOT_APPLICABLE_USER_MESSAGE = "Купон не к чему применить";
    private static final BigDecimal INITIAL_CURRENT_BUDGET = BigDecimal.valueOf(700);
    private static final String COUPON_FOR_PROMO_2 = "coupon_promo_2";
    private static final String COUPON_FOR_PROMO_3 = "coupon_promo_3";
    private static final String COUPON_FOR_PROMO_4 = "coupon_promo_4";
    private static final String BLUE_PROMO_CODE = "BLUEMOB";
    private static final BigDecimal BLUE_PROMO_COUPON_VALUE = BigDecimal.valueOf(500);
    private static final BigDecimal BLUE_PROMO_CURRENT_BUDGET = BigDecimal.valueOf(1_000_000);

    private long promo1Id;
    private long redPromoId;

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoService promoService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private OperationContextDao operationContextDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private YabacksMailer yabacksMailer;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private AlertNotificationService alertNotificationService;
    @Autowired
    private DiscountUtils discountUtils;
    @Autowired
    private DeferredMetaTransactionService deferredMetaTransactionService;

    @SuppressFBWarnings("URF_UNREAD_FIELD")
    @Before
    public void init() {
        Promo promoWithUserMessage = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setPlatform(CoreMarketPlatform.BLUE)
                .setBudget(INITIAL_CURRENT_BUDGET)
                .setCouponNotApplicableMessage(COUPON_NOT_APPLICABLE_USER_MESSAGE)
        );
        promo1Id = promoWithUserMessage.getId();

        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(COUPON_FOR_PROMO_2)
                        .setBudget(INITIAL_CURRENT_BUDGET)
        );

        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(COUPON_FOR_PROMO_3)
                        .setCouponValue(BigDecimal.valueOf(2.01), CoreCouponValueType.FIXED)
                        .setBudget(INITIAL_CURRENT_BUDGET)
        );

        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(BLUE_PROMO_CODE)
                        .setCouponValue(BLUE_PROMO_COUPON_VALUE, CoreCouponValueType.FIXED)
                        .setBudget(BLUE_PROMO_CURRENT_BUDGET)
        );

        redPromoId = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse(PromoSubType.RED_ORDER)
                        .setBudget(INITIAL_CURRENT_BUDGET)
                        .setPlatform(CoreMarketPlatform.RED)
        ).getId();

        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .setCouponValue(BigDecimal.ZERO, CoreCouponValueType.FIXED)
                        .setBudget(INITIAL_CURRENT_BUDGET)
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponValue(BLUE_PROMO_COUPON_VALUE, CoreCouponValueType.FIXED)
        );

        deferredMetaTransactionService.consumeBatchOfTransactions(10);

    }

    @Test
    public void testInactiveCalc() {
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.valueOf(1_000_000)),
                        price(BigDecimal.valueOf(1_000_000))
                )
                .build();

        Promo promo =
                promoService.getPromo(couponService.getCouponByCode(of(COUPON_FOR_PROMO_2)).orElseThrow(() -> new AssertionError("not found")).getPromoId());
        promoService.updateStatus(promo, PromoStatus.INACTIVE);

        MultiCartDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).withCoupon(COUPON_FOR_PROMO_2).build());

        assertThat(discountResponse, hasCouponError(PROMO_NOT_ACTIVE));
    }

    @Test
    public void shouldNotFailOnOrderWithoutApplicablePromos() {
        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();
        MultiCartDiscountResponse orderResponse = marketLoyaltyClient
                .calculateDiscount(
                        DiscountRequestBuilder.builder(order).build()
                );
        assertThat(calculateTotalDiscount(orderResponse.getOrders().get(0)), comparesEqualTo(BigDecimal.ZERO));
        long promosCount = orderResponse.getOrders().get(0).getItems().stream()
                .map(OrderItemResponse::getPromos)
                .mapToLong(Collection::size)
                .sum();
        assertEquals(0L, promosCount);
    }

    @Test
    public void shouldUseOnlyFilteredItemsToOrderTotalRule() {
        int couponCategory = 12312131;

        BigDecimal minOrderTotal = BigDecimal.valueOf(2500);
        BigDecimal filteredItemPrice = BigDecimal.valueOf(1250);
        BigDecimal secondItemPrice = BigDecimal.valueOf(2890);


        String couponCode = "someCouponCode";
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponCode(couponCode)
                        .addPromoRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                Collections.singleton(minOrderTotal))
                        .addPromoRule(RuleType.CATEGORY_FILTER_RULE, RuleParameterName.CATEGORY_ID,
                                Collections.singleton(couponCategory))
        );

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(filteredItemPrice),
                        categoryId(couponCategory)
                ).withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        price(secondItemPrice)
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient
                .calculateDiscount(
                        DiscountRequestBuilder.builder(order).withCoupon(couponCode).build()
                );

        assertThat(discountResponse, hasCouponError(INSUFFICIENT_TOTAL));
    }

    @Test
    public void shouldUseMaxOrderTotalRuleForPercentCoupon() {

        BigDecimal maxOrderTotal = BigDecimal.valueOf(5000);
        BigDecimal filteredItemPrice = BigDecimal.valueOf(3000);
        BigDecimal secondItemPrice = BigDecimal.valueOf(3000);


        String couponCode = "someCouponCode";
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponCode(couponCode)
                        .setCouponValue(BigDecimal.TEN, CoreCouponValueType.PERCENT)
                        .addPromoRule(RuleType.UPPER_BOUND_DISCOUNT_BASE_RULE, RuleParameterName.MAX_ORDER_TOTAL,
                                Collections.singleton(maxOrderTotal))
        );

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(filteredItemPrice)
                )
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        price(secondItemPrice)
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient
                .calculateDiscount(
                        DiscountRequestBuilder.builder(order).withCoupon(couponCode).build()
                );

        assertThat(
                discountResponse,
                allOf(
                        hasProperty(
                                "coinErrors",
                                is(empty())
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        hasProperty(
                                                "items",
                                                containsInAnyOrder(
                                                        hasProperty(
                                                                "promos",
                                                                contains(
                                                                        hasProperty(
                                                                                "discount",
                                                                                comparesEqualTo(BigDecimal.valueOf(250))
                                                                        )
                                                                )
                                                        ),
                                                        hasProperty(
                                                                "promos",
                                                                contains(
                                                                        hasProperty(
                                                                                "discount",
                                                                                comparesEqualTo(BigDecimal.valueOf(250))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void shouldUseMaxOrderTotalRuleForFixedCoupon() {

        BigDecimal maxOrderTotal = BigDecimal.valueOf(5000);
        BigDecimal filteredItemPrice = BigDecimal.valueOf(3000);
        BigDecimal secondItemPrice = BigDecimal.valueOf(3000);


        String couponCode = "someCouponCode";
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponCode(couponCode)
                        .setCouponValue(BigDecimal.valueOf(600), CoreCouponValueType.FIXED)
                        .addPromoRule(RuleType.UPPER_BOUND_DISCOUNT_BASE_RULE, RuleParameterName.MAX_ORDER_TOTAL,
                                Collections.singleton(maxOrderTotal))
        );

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(filteredItemPrice)
                )
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        price(secondItemPrice)
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient
                .calculateDiscount(
                        DiscountRequestBuilder.builder(order).withCoupon(couponCode).build()
                );

        assertThat(
                discountResponse,
                allOf(
                        hasProperty(
                                "coinErrors",
                                is(empty())
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        hasProperty(
                                                "items",
                                                containsInAnyOrder(
                                                        hasProperty(
                                                                "promos",
                                                                contains(
                                                                        hasProperty(
                                                                                "discount",
                                                                                comparesEqualTo(BigDecimal.valueOf(300))
                                                                        )
                                                                )
                                                        ),
                                                        hasProperty(
                                                                "promos",
                                                                contains(
                                                                        hasProperty(
                                                                                "discount",
                                                                                comparesEqualTo(BigDecimal.valueOf(300))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }


    @Test
    public void test7Calc() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        calcOk(coupon, 7, 897);
    }

    @Test
    public void test1Calc() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        calcOk(coupon, 1, 100500);
    }

    @Test
    public void test5Calc() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        calcOk(coupon, 5, 300 / 5 * 4);
    }

    @Test
    public void test17Calc() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        calcOk(coupon, 17, 300 / 5 * 4);
    }

    @Test
    public void test17CalcRubles() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        calcOkRubles(coupon, 17, 300 / 5 * 4);
    }

    @Test
    public void test7CalcRubles() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        calcOkRubles(coupon, 7, 897);
    }

    @Test
    public void test1CalcRubles() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        calcOkRubles(coupon, 1, 100500);
    }

    @Test
    public void test5CalcRubles() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        calcOkRubles(coupon, 5, 300 / 5 * 4);
    }

    @Test
    public void testCalcRubles() {
        BigDecimal itemPrice = BigDecimal.valueOf(1000);
        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(itemPrice.intValue()))
        )), 2, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithDeliveriesRequest order = orderRequestBuilder.build();

        MultiCartDiscountResponse retOrder = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order).withCoupon(COUPON_FOR_PROMO_3).build()
        );

        List<BigDecimal> actual = retOrder.getOrders().get(0).getItems().stream()
                .map(OrderRequestUtils::sumPriceWithDiscount)
                .collect(Collectors.toList());

        List<BigDecimal> expected = Arrays.asList(
                itemPrice.subtract(BigDecimal.valueOf(2.00)),
                itemPrice.subtract(BigDecimal.valueOf(1.00))
        );

        assertThat(actual, containsInAnyOrder(expected.stream()
                .map(Matchers::comparesEqualTo)
                .collect(Collectors.toList())
        ));
    }

    @Test
    public void testRealOrder() {
        OrderWithDeliveriesRequest request = orderRequestBuilder()
                .withOrderId("3832198")
                .withOrderItem(
                        itemKey(ItemKey.ofFeedOffer(475690L, "493303.000011.4600697130903")),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(60)),
                        msku(DEFAULT_MSKU),
                        categoryId(1)
                )
                .withOrderItem(
                        itemKey(ItemKey.ofFeedOffer(475690L, "495143.4600697122090")),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(75)),
                        msku(DEFAULT_MSKU),
                        categoryId(1)
                )
                .withOrderItem(
                        itemKey(ItemKey.ofFeedOffer(475690L, "518681.4627148993337")),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(1990)),
                        msku(DEFAULT_MSKU),
                        categoryId(1)
                )
                .withOrderItem(
                        itemKey(ItemKey.ofFeedOffer(475690L, "493303.000002.4607163090792")),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(9)),
                        msku(DEFAULT_MSKU),
                        categoryId(1)
                )
                .withOrderItem(
                        itemKey(ItemKey.ofFeedOffer(475690L, "497799.4606711700916")),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(55)),
                        msku(DEFAULT_MSKU),
                        categoryId(1)
                )
                .withOrderItem(
                        itemKey(ItemKey.ofFeedOffer(475690L, "493303.000042.4015400265207")),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(1319)),
                        msku(DEFAULT_MSKU),
                        categoryId(1)
                )
                .build();

        MultiCartDiscountResponse result = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(request)
                        .withCoupon(BLUE_PROMO_CODE)
                        .withPlatform(MarketPlatform.BLUE)
                        .build()
        );

        assertThat(calculateTotalDiscount(result.getOrders().get(0)), comparesEqualTo(BLUE_PROMO_COUPON_VALUE));
        assertThat(result.getOrders().get(0).getItems(), containsInAnyOrder(
                allOf(
                        hasProperty("offerId", equalTo("493303.000011.4600697130903")),
                        hasProperty("promos", contains(hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(8)))))
                ),
                allOf(
                        hasProperty("offerId", equalTo("495143.4600697122090")),
                        hasProperty("promos", contains(hasProperty("discount", comparesEqualTo(BigDecimal.TEN))))
                ),
                allOf(
                        hasProperty("offerId", equalTo("518681.4627148993337")),
                        hasProperty("promos", contains(
                                anyOf(hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(286))),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(285))))))
                ),
                allOf(
                        hasProperty("offerId", equalTo("493303.000002.4607163090792")),
                        hasProperty("promos", contains(hasProperty("discount", comparesEqualTo(BigDecimal.ONE))))
                ),
                allOf(
                        hasProperty("offerId", equalTo("497799.4606711700916")),
                        hasProperty("promos", contains(hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(7)))))
                ),
                allOf(
                        hasProperty("offerId", equalTo("493303.000042.4015400265207")),
                        hasProperty("promos", contains(
                                anyOf(hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(188))),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(189))))))
                )
        ));
    }

    @Test
    public void shouldFailToUseHidFromExclusionList() {
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(categoryId(STICK_CATEGORY))
                .build();

        MultiCartDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).withCoupon(COUPON_FOR_PROMO_2).build());

        assertThat(discountResponse, hasCouponError(COUPON_NOT_APPLICABLE));
    }

    @Test
    public void shouldFailToUseSupplierIdFromExclusionList() {
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(supplier(SUPPLIER_EXCLUSION_ID))
                .build();

        MultiCartDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).withCoupon(COUPON_FOR_PROMO_2).build());

        assertThat(discountResponse, hasCouponError(COUPON_NOT_APPLICABLE));
    }

    @Test
    public void shouldFailToUseRedOrderCouponAsNormalCoupon() {
        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(1000))
        )), 2, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithDeliveriesRequest order = orderRequestBuilder.build();

        MarketLoyaltyError error = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).withCoupon(COUPON_FOR_PROMO_4).withPlatform(MarketPlatform.RED).build())
        ).getModel();

        assertEquals(COUPON_NOT_EXISTS.name(), error.getCode());
    }

    @Test
    public void shouldRevertRedOrder() {
        BigDecimal fullDiscount = BigDecimal.valueOf(300);

        RedOrder redOrder = new RedOrder(redPromoId, fullDiscount);
        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(1000))
        )), 1, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        MultiCartDiscountResponse spendResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestBuilder.builder(orderRequestBuilder.build())
                        .withPlatform(MarketPlatform.RED)
                        .withRedOrder(redOrder)
                        .build()
        );

        assertThat(
                promoService.getPromo(redPromoId).getCurrentBudget(),
                comparesEqualTo(INITIAL_CURRENT_BUDGET.subtract(fullDiscount))
        );

        marketLoyaltyClient.revertDiscount(getDiscountTokens(spendResponse.getOrders().get(0)));

        assertThat(
                promoService.getPromo(redPromoId).getCurrentBudget(),
                comparesEqualTo(INITIAL_CURRENT_BUDGET)
        );
    }

    @Test
    public void shouldApplyFullDiscountFromRedOrder() {
        BigDecimal itemPrice = BigDecimal.valueOf(1000);
        BigDecimal itemsCount = BigDecimal.valueOf(2);
        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(itemPrice.intValue()))
        )), itemsCount.intValue(), compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithDeliveriesRequest order = orderRequestBuilder.build();

        BigDecimal fullDiscount = BigDecimal.valueOf(300);
        RedOrder redOrder = new RedOrder(redPromoId, fullDiscount);
        MultiCartDiscountResponse retOrder = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order)
                        .withPlatform(MarketPlatform.RED)
                        .withRedOrder(redOrder)
                        .build()
        );

        List<BigDecimal> actualPrices = retOrder.getOrders().get(0).getItems().stream()
                .map(OrderRequestUtils::totalDiscount)
                .collect(Collectors.toList());

        assertThat(actualPrices, containsInAnyOrder(
                comparesEqualTo(fullDiscount.divide(itemsCount, 0, RoundingMode.HALF_DOWN)),
                comparesEqualTo(fullDiscount.divide(itemsCount, 0, RoundingMode.HALF_DOWN))
        ));
    }

    @Test
    public void shouldFailIfRedOrderIsInvalid() {
        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(1000))
        )), 2, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithDeliveriesRequest order = orderRequestBuilder.build();

        assertThrows(Exception.class, () -> marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order)
                        .withPlatform(MarketPlatform.RED)
                        .withRedOrder(new RedOrder(redPromoId, (BigDecimal) null))
                        .build()
        ));
    }

    @Test
    public void testCalcCategoryPromo() {
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.valueOf(5)),
                        price(BigDecimal.valueOf(1000))
                )
                .build();

        marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order).withCoupon(COUPON_FOR_PROMO_2).build()
        );
    }

    @Test
    public void testCategoryOverdraftExceeded() {
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.valueOf(1_000_000)),
                        price(BigDecimal.valueOf(1_000_000))
                )
                .build();

        MultiCartDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).withCoupon(COUPON_FOR_PROMO_2).build());

        assertThat(discountResponse, hasCouponError(OVERDRAFT_EXCEEDED));
    }

    @Test
    public void testCalcOkOverdraft() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        OrderRequestBuilder requestBuilder = orderRequestBuilder();

        IntStream.of(7, 17)
                .forEach(i -> requestBuilder.withOrderItem(
                        itemKey(ItemKey.ofFeedOffer(i + 1L, String.valueOf(i + 1))),
                        quantity(BigDecimal.valueOf(i)),
                        price(BigDecimal.valueOf(1_000))
                        )
                );

        MultiCartDiscountResponse orderResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(requestBuilder.build()).withCoupon(coupon.getCode()).build()
        );
        checkOverdraft(orderResponse.getOrders().get(0), MAXIMUM_ALLOWED_DISCOUNT_OVERDRAFT_FOR_PENNIES);
    }

    @Test
    public void testInflatedCalcOkOverdraft() {
        CouponDto coupon = createActivatedCoupon(promo1Id);
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.valueOf(1_000_000)),
                        price(BigDecimal.valueOf(7))
                )
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        quantity(BigDecimal.valueOf(7)),
                        price(BigDecimal.valueOf(1_000_000))
                )
                .build();

        MultiCartDiscountResponse orderResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order).withCoupon(coupon.getCode()).build()
        );

        checkOverdraft(orderResponse.getOrders().get(0), MAXIMUM_ALLOWED_DISCOUNT_OVERDRAFT_FOR_PENNIES);
    }

    @Test
    public void testCalcOkOverdraftRublesOk() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        MultiCartDiscountResponse orderResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(orderRequestBuilder()
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(DISCOUNT_AMOUNT.getAmount()
                                        .multiply(BigDecimal.valueOf(MAXIMUM_ALLOWED_DISCOUNT_OVERDRAFT_FOR_RUBLES))
                                        .setScale(0, BigDecimal.ROUND_UP)),
                                price(BigDecimal.valueOf(1000))
                        )
                        .build()).withCoupon(coupon.getCode()).build()
        );
        checkOverdraft(orderResponse.getOrders().get(0), MAXIMUM_ALLOWED_DISCOUNT_OVERDRAFT_FOR_RUBLES);
    }

    @Test
    public void testCalcOkOverdraftRublesNotOk() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.valueOf(2).multiply(DISCOUNT_AMOUNT.getAmount())
                                .multiply(BigDecimal.valueOf(MAXIMUM_ALLOWED_DISCOUNT_OVERDRAFT_FOR_RUBLES))
                                .setScale(0, BigDecimal.ROUND_UP)
                                .add(BigDecimal.ONE)),
                        price(BigDecimal.valueOf(1000))
                )
                .build();

        MultiCartDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).withCoupon(coupon.getCode()).build());

        assertThat(discountResponse, hasCouponError(OVERDRAFT_EXCEEDED));
    }

    @Test
    public void testInflatedCalcNotOkOverdraft() {
        CouponDto coupon = createActivatedCoupon(promo1Id);
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.valueOf(1_000_000)),
                        price(BigDecimal.valueOf(7))
                )
                .build();

        MultiCartDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).withCoupon(coupon.getCode()).build());

        assertThat(discountResponse, hasCouponError(OVERDRAFT_EXCEEDED));
    }

    @Test
    public void testCalcBudgetExceeded() {
        CouponDto coupon1 = createActivatedCoupon(promo1Id);
        CouponDto coupon2 = createActivatedCoupon(promo1Id);
        CouponDto coupon3 = createActivatedCoupon(promo1Id);

        spendOk(coupon1, 7, 1000);
        spendOk(coupon2, 7, 1000);

        Promo promo = promoService.getPromo(promo1Id);

        assertThat(promo.getCurrentBudget(), lessThan(BigDecimal.valueOf(300)));

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(itemKey(DEFAULT_ITEM_KEY))
                .build();

        MultiCartDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).withCoupon(coupon3.getCode()).build());

        assertThat(discountResponse, hasCouponError(BUDGET_EXCEEDED));

        Promo promoStored = promoService.getPromo(promo.getId());
        assertEquals(100, promoStored.getCurrentBudget().longValueExact());
        assertEquals(600, promoStored.getSpentBudget().longValueExact());
    }

    @Test
    public void shouldFailToSpendWhenBudgetExceeded() {
        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .setBudget(BigDecimal.valueOf(200))
                        .setCouponValue(BigDecimal.valueOf(500), CoreCouponValueType.FIXED)
        );
        Coupon coupon =
                couponService.createOrGetCoupon(CouponCreationRequest.builder("someKey", couponPromo.getId()).forceActivation(true).build(),
                discountUtils.getRulesPayload());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .build();

        MultiCartDiscountResponse discountResponse =
                marketLoyaltyClient.spendDiscount(DiscountRequestBuilder.builder(order).withCoupon(coupon.getCode()).build());

        assertThat(discountResponse, hasCouponError(BUDGET_EXCEEDED));
    }

    @Test
    public void testCalcExpired() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        Coupon couponStored =
                couponService.getCouponByCode(of(coupon.getCode())).orElseThrow(() -> new AssertionError("not found"));
        jdbcTemplate.update("UPDATE coupon SET activation_time = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(91)), couponStored.getId());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(itemKey(DEFAULT_ITEM_KEY))
                .build();

        MultiCartDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).withCoupon(coupon.getCode()).build());

        assertThat(discountResponse, hasCouponError(COUPON_EXPIRED));
    }

    @Test
    public void testCalcExpiredInfinityCoupon() {
        Coupon couponStored =
                couponService.getCouponByCode(of(COUPON_FOR_PROMO_2)).orElseThrow(() -> new AssertionError("not " +
                        "found"));
        jdbcTemplate.update("UPDATE coupon SET activation_time = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(91)), couponStored.getId());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(itemKey(DEFAULT_ITEM_KEY))
                .build();

        marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order).withCoupon(COUPON_FOR_PROMO_2).build()
        );
    }

    @Test
    public void testSpend() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        OrderWithDeliveriesResponse order = spendOk(coupon, 7, 1000);

        getDiscountTokens(order);
    }

    @Test
    public void testAlert() {
        long promoId = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setBudget(INITIAL_CURRENT_BUDGET)
                .setBudgetThreshold(INITIAL_CURRENT_BUDGET.subtract(BigDecimal.valueOf(50)))
        ).getId();

        CouponDto coupon = createActivatedCoupon(promoId);

        spendOk(coupon, 3, 1000);

        processEmailQueue();
        verify(yabacksMailer).sendMail(anyString(), isNull(), anyString(), anyString());

        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(1000))
        )), 3, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(
                orderRequestBuilder
                        .build()
        ).withCoupon(coupon.getCode()).build());

        processEmailQueue();
        verifyZeroInteractions(yabacksMailer);
    }

    private void processEmailQueue() {
        alertNotificationService.processEmailQueue(100);
    }

    @Test
    public void testEmissionAlert() {
        Promo promoWithUserMessage = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setPlatform(CoreMarketPlatform.BLUE)
                .setBudget(INITIAL_CURRENT_BUDGET)
                .setEmissionBudget(DEFAULT_ALERT_PROMO_EMISSION_BUDGET.add(BigDecimal.ONE))
                .setCouponNotApplicableMessage(COUPON_NOT_APPLICABLE_USER_MESSAGE)
        );
        promo1Id = promoWithUserMessage.getId();

        createActivatedCoupon(promo1Id);

        processEmailQueue();
        verify(yabacksMailer).sendMail(anyString(), isNull(), anyString(), anyString());

        createActivatedCoupon(promo1Id);

        processEmailQueue();
        verifyZeroInteractions(yabacksMailer);
    }

    @Test
    public void testInactiveSpend() {
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(itemKey(DEFAULT_ITEM_KEY))
                .build();

        Promo promo = promoService.getPromo(
                couponService.getCouponByCode(of(COUPON_FOR_PROMO_2)).orElseThrow(() -> new AssertionError("not " +
                        "found")).getPromoId());
        promoService.updateStatus(promo, PromoStatus.INACTIVE);

        MultiCartDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).withCoupon(COUPON_FOR_PROMO_2).build());
        assertThat(discountResponse, hasCouponError(PROMO_NOT_ACTIVE));
    }

    @Test
    public void testExpiredSpend() {
        CouponDto coupon = createActivatedCoupon(promo1Id);
        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem(itemKey(DEFAULT_ITEM_KEY))
                .build();

        Coupon couponStored =
                couponService.getCouponByCode(of(coupon.getCode())).orElseThrow(() -> new AssertionError("not found"));
        jdbcTemplate.update("UPDATE coupon SET activation_time = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(91)), couponStored.getId());

        MultiCartDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).withCoupon(coupon.getCode()).build());
        assertThat(discountResponse, hasCouponError(COUPON_EXPIRED));
    }

    @Test
    public void testNotExpiredSpendByInfinityCoupon() {
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(itemKey(DEFAULT_ITEM_KEY))
                .build();

        Coupon couponStored =
                couponService.getCouponByCode(of(COUPON_FOR_PROMO_2)).orElseThrow(() -> new AssertionError("not " +
                        "found"));
        jdbcTemplate.update("UPDATE coupon SET activation_time = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(91)), couponStored.getId());

        marketLoyaltyClient.spendDiscount(
                DiscountRequestBuilder.builder(order).withCoupon(COUPON_FOR_PROMO_2).build()
        );
    }

    @Test
    public void testSpendBudgetExceeded() {
        CouponDto coupon1 = createActivatedCoupon(promo1Id);
        CouponDto coupon2 = createActivatedCoupon(promo1Id);
        CouponDto coupon3 = createActivatedCoupon(promo1Id);

        Promo promo = promoService.getPromo(promo1Id);
        OrderWithDeliveriesResponse orderCalc1 = calcOk(coupon1, 7, 1000);
        OrderWithDeliveriesResponse orderCalc2 = calcOk(coupon2, 7, 1000);
        OrderWithDeliveriesResponse orderCalc3 = calcOk(coupon3, 7, 1000);

        // достаточно, чтобы потратить 2, но недостаточно, чтобы потратить 3
        assertThat(calculateTotalDiscount(orderCalc1).add(calculateTotalDiscount(orderCalc2)),
                lessThan(promo.getCurrentBudget()));
        assertThat(calculateTotalDiscount(orderCalc1).add(calculateTotalDiscount(orderCalc2)).add(calculateTotalDiscount(orderCalc3)), greaterThan(promo.getCurrentBudget()));

        spendOk(coupon1, 7, 1000);
        spendOk(coupon2, 7, 1000);

        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(1000))
        )), 7, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithDeliveriesRequest order = orderRequestBuilder.build();
        MultiCartDiscountResponse discountResponse =
                marketLoyaltyClient.spendDiscount(DiscountRequestBuilder.builder(order).withCoupon(coupon3.getCode()).build());
        assertThat(discountResponse, hasCouponError(BUDGET_EXCEEDED));
    }

    @Test
    public void testSpendBudgetExceededWithReserve() throws InterruptedException {
        supplementaryDataLoader.createReserveIfNotExists(BigDecimal.valueOf(1_000_000));

        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setPlatform(CoreMarketPlatform.BLUE)
                .setCouponValue(BLUE_PROMO_COUPON_VALUE, CoreCouponValueType.FIXED)
                .setBudget(BLUE_PROMO_COUPON_VALUE.multiply(BigDecimal.valueOf(2)).subtract(BigDecimal.valueOf(20)))
                .setBudgetThreshold(BigDecimal.valueOf(100_000))
                .setCanBeRestoredFromReserveBudget(true)
        );
        CouponDto coupon1 = createActivatedCoupon(promo.getId());
        CouponDto coupon2 = createActivatedCoupon(promo.getId());
        CouponDto coupon3 = createActivatedCoupon(promo.getId());

        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(1000))
        )), 7, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithDeliveriesRequest orderRequest = orderRequestBuilder
                .build();
        DiscountRequestBuilder discountRequest = DiscountRequestBuilder.builder(orderRequest);

        MultiCartDiscountResponse orderCalc1 =
                marketLoyaltyClient.calculateDiscount(discountRequest.withCoupon(coupon1.getCode()).build());
        MultiCartDiscountResponse orderCalc2 =
                marketLoyaltyClient.calculateDiscount(discountRequest.withCoupon(coupon2.getCode()).build());

        // достаточно, чтобы потратить 1, но недостаточно, чтобы потратить 2
        assertThat(calculateTotalDiscount(orderCalc1.getOrders().get(0)), lessThan(promo.getCurrentBudget()));
        assertThat(calculateTotalDiscount(orderCalc1.getOrders().get(0)).add(calculateTotalDiscount(orderCalc2.getOrders().get(0))), greaterThan(promo.getCurrentBudget()));

        marketLoyaltyClient.spendDiscount(discountRequest.withCoupon(coupon1.getCode()).build());
        budgetService.waitForReserveBudget();
        marketLoyaltyClient.spendDiscount(discountRequest.withCoupon(coupon2.getCode()).build());
        budgetService.waitForReserveBudget();
        marketLoyaltyClient.spendDiscount(discountRequest.withCoupon(coupon3.getCode()).build());
        budgetService.waitForReserveBudget();
        assertThat(promoService.getPromo(promo.getId()).getCurrentBudget(), greaterThan(BigDecimal.ZERO));
        processEmailQueue();
        verify(yabacksMailer).sendMail(anyString(), isNull(), anyString(), anyString());
    }

    @Test
    public void testSpendBudgetExceededWithInsufficientReserve() throws InterruptedException {
        supplementaryDataLoader.createReserveIfNotExists(BigDecimal.ONE);

        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setPlatform(CoreMarketPlatform.BLUE)
                .setCouponValue(BLUE_PROMO_COUPON_VALUE, CoreCouponValueType.FIXED)
                .setBudget(BLUE_PROMO_COUPON_VALUE.multiply(BigDecimal.valueOf(2)).subtract(BigDecimal.valueOf(20)))
                .setBudgetThreshold(BigDecimal.valueOf(100_000))
                .setCanBeRestoredFromReserveBudget(true)
        );
        CouponDto coupon1 = createActivatedCoupon(promo.getId());
        CouponDto coupon2 = createActivatedCoupon(promo.getId());
        CouponDto coupon3 = createActivatedCoupon(promo.getId());

        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(1000))
        )), 7, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithDeliveriesRequest orderRequest = orderRequestBuilder
                .build();
        DiscountRequestBuilder discountRequest = DiscountRequestBuilder.builder(orderRequest);

        MultiCartDiscountResponse orderCalc1 =
                marketLoyaltyClient.calculateDiscount(discountRequest.withCoupon(coupon1.getCode()).build());
        MultiCartDiscountResponse orderCalc2 =
                marketLoyaltyClient.calculateDiscount(discountRequest.withCoupon(coupon2.getCode()).build());

        // достаточно, чтобы потратить 1, но недостаточно, чтобы потратить 2
        assertThat(calculateTotalDiscount(orderCalc1.getOrders().get(0)), lessThan(promo.getCurrentBudget()));
        assertThat(calculateTotalDiscount(orderCalc1.getOrders().get(0)).add(calculateTotalDiscount(orderCalc2.getOrders().get(0))), greaterThan(promo.getCurrentBudget()));

        marketLoyaltyClient.spendDiscount(discountRequest.withCoupon(coupon1.getCode()).build());
        assertThat(promoService.getPromo(promo.getId()).getCurrentBudget(), greaterThan(BigDecimal.ZERO));
        marketLoyaltyClient.spendDiscount(discountRequest.withCoupon(coupon2.getCode()).build());
        assertThat(promoService.getPromo(promo.getId()).getCurrentBudget(), lessThan(BigDecimal.ZERO));

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(discountRequest.withCoupon(coupon3.getCode()).build());
        assertThat(discountResponse, hasCouponError(BUDGET_EXCEEDED));
        budgetService.waitForReserveBudget();
        processEmailQueue();
        verify(yabacksMailer).sendMail(anyString(), isNull(), anyString(), anyString());
    }

    @Test
    public void testAlreadySpend() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        spendOk(coupon, 7, 1000);
        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(1000))
        )), 7, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithDeliveriesRequest order = orderRequestBuilder.build();
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(order).withCoupon(coupon.getCode()).build();
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(request);

        assertThat(discountResponse, hasCouponError(COUPON_ALREADY_SPENT));
    }

    @Test
    public void testRevert() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        OrderWithDeliveriesResponse order = spendOk(coupon, 7, 1000);
        marketLoyaltyClient.revertDiscount(getDiscountTokens(order));
    }

    @Test
    public void testSpendRevertSpend() {
        CouponDto coupon = createActivatedCoupon(promo1Id);

        OrderWithDeliveriesResponse order = spendOk(coupon, 7, 1000);
        marketLoyaltyClient.revertDiscount(getDiscountTokens(order));
        spendOk(coupon, 7, 1000);
    }

    @Test
    public void testIncorrectDiscountToken() {
        MarketLoyaltyError result = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.revertDiscount(Collections.singleton("such invalid token"))
        ).getModel();

        assertEquals(REVERT_TOKEN_NOT_FOUND.name(), result.getCode());
    }

    @Test
    public void testSpendDiscountWithOperationContext() {
        CouponDto coupon = createActivatedCoupon(promo1Id);
        OperationContextDto operationContext = OperationContextFactory
                .withYandexUidBuilder("yandexUid")
                .buildOperationContextDto();

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();
        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder.builder(order)
                .withOperationContext(operationContext)
                .withCoupon(coupon.getCode())
                .build());

        assertThat(
                operationContextDao.get(
                        getNonNullValue(jdbcTemplate.queryForObject("SELECT MAX(id) FROM operation_context", Long.class))),
                samePropertyValuesAs(operationContext, "id", "creationTime", "couponCode", "orderTotal", "deviceInfoRequest", "boundedUid")
        );
    }
}
