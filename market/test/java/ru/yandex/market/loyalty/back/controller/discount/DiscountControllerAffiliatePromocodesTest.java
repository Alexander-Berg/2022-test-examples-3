package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.CoinProps;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.ExcludedOffersType;
import ru.yandex.market.loyalty.core.model.promo.MarketDepartment;
import ru.yandex.market.loyalty.core.model.promo.NominalStrategy;
import ru.yandex.market.loyalty.core.model.promo.PromocodePromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.rule.RulesContainer;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.service.promocode.StorePromocodeService;
import ru.yandex.market.loyalty.core.test.SupplementaryDataLoader;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.core.AllOf.allOf;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContextDto;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.msku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.supplier;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.vendor;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.defaultTicketDescription;

@TestFor(DiscountController.class)
public class DiscountControllerAffiliatePromocodesTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    protected ClockForTests clock;
    @Autowired
    protected CoinService coinService;
    @Autowired
    protected StorePromocodeService storePromocodeService;

    /**
     * Must be one of values hardcoded in
     * {@link SupplementaryDataLoader#populateCategoryTree}
     */
    private static final int CATEGORY_ID = 123141412;
    private static final int NOT_LOGIC_CATEGORY_ID = 456;
    private static final long VENDOR_ID = 4444L;

    private static final long USER_ID = 123L;
    private static final long FEED_ID = 123;
    private static final String FIRST_SSKU = "first offer";

    @Test
    public void testFixedMinOrderLimitMatch() {
        int budget = 6000;
        int nominal = 300;
        int minOrderLimit = 2500;
        String promoKey = prepareFixedMinOrderLimit(budget, nominal, minOrderLimit);

        var discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promoKey),
                        categoryId(CATEGORY_ID),
                        price(10000)
                )
                .build());

        assertThat(discountResponse.getCoins(), emptyIterable());
        assertThat(discountResponse.getUnusedPromocodes(), emptyIterable());
        assertThat(discountResponse.getPromocodeErrors(), emptyIterable());
        assertThat(discountResponse.getCoinErrors(), emptyIterable());

        var discountItems = discountResponse.getOrders().get(0).getItems();
        assertThat(discountItems, iterableWithSize(1));

        //noinspection unchecked
        assertThat(discountItems.get(0),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(300)))
                ))));

        var promo = promoService.getPromoByPromoKey(promoKey);
        assertThat(promo.getCurrentBudget().intValue(), equalTo(budget - nominal));
    }

    @Test
    public void testFixedMinOrderLimitLowBucketPrice() {

        String promoKey = prepareFixedMinOrderLimit(6000, 300, 2500);

        var discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promoKey),
                        categoryId(CATEGORY_ID),
                        price(500)
                )
                .build());

        assertThat(discountResponse.getCoins(), emptyIterable());
        assertThat(discountResponse.getUnusedPromocodes(), emptyIterable());
        assertThat(discountResponse.getPromocodeErrors(), iterableWithSize(1));
        assertThat(discountResponse.getPromocodeErrors().iterator().next().getError().getError().getUserMessage(),
                is("Добавьте товаров по акции до 2 500 ₽"));
    }

    @Test
    public void testFixedMinOrderLimitWrongCategory() {
        String promoKey = prepareFixedMinOrderLimit(6000, 300, 2500);

        var discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promoKey),
                        categoryId(CATEGORY_ID + 1),
                        price(10000)
                )
                .build());

        assertThat(discountResponse.getCoins(), emptyIterable());
        assertThat(discountResponse.getUnusedPromocodes(), emptyIterable());
        assertThat(discountResponse.getPromocodeErrors(), iterableWithSize(1));
        assertThat(discountResponse.getPromocodeErrors().iterator().next().getError().getError().getCode(),
                is(MarketLoyaltyErrorCode.NOT_SUITABLE_FILTER_RULES.name()));
    }

    @Test
    public void testFixedMinOrderLimitLowBudget() {
        String promoKey = prepareFixedMinOrderLimit(200, 300, 2500);

        var discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promoKey),
                        categoryId(CATEGORY_ID),
                        price(10000)
                )
                .build());

        assertThat(discountResponse.getPromocodeErrors(), iterableWithSize(1));
        assertThat(discountResponse.getPromocodeErrors().iterator().next().getError().getError().getCode(),
                is(MarketLoyaltyErrorCode.BUDGET_EXCEEDED.name()));
    }

    @Test
    public void testPercentFirstOrderCutting() {
        RulesContainer rc = createRulesContainer();
        rc.add(RuleContainer.builder(RuleType.CATEGORY_FILTER_RULE)
                .withSingleParam(RuleParameterName.CATEGORY_ID, CATEGORY_ID)
                .withSingleParam(RuleParameterName.NOT_LOGIC, false)
                .build());
        rc.add(RuleContainer.builder(RuleType.FIRST_ORDER_CUTTING_RULE).build());

        var promo = promoManager.createPromocodePromo(createPromo(createCoins(CoreCoinType.PERCENT, 5), 1000, 2)
                .setRulesContainer(rc));
        activatePromocodes("PROMO2-AF");
        var order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promo.getPromoKey()),
                        categoryId(CATEGORY_ID),
                        price(10000)
                )
                .build();
        var discountResponse = spend(order);
        assertThat(discountResponse.getCoins(), emptyIterable());
        assertThat(discountResponse.getUnusedPromocodes(), emptyIterable());
        assertThat(discountResponse.getPromocodeErrors(), emptyIterable());
        assertThat(discountResponse.getCoinErrors(), emptyIterable());

        var discountItems = discountResponse.getOrders().get(0).getItems();
        assertThat(discountItems, iterableWithSize(1));

        //noinspection unchecked
        assertThat(discountItems.get(0),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(500)))
                ))));

        // попытка повторного применения
        discountResponse = spend(order);
        assertThat(discountResponse.getPromocodeErrors().iterator().next().getError().getError().getCode(),
                is(MarketLoyaltyErrorCode.COUPON_NOT_EXISTS.name()));
    }

    @Test
    public void testBindOnlyOnce() {
        RulesContainer rc = createRulesContainer();
        rc.add(RuleContainer.builder(RuleType.CATEGORY_FILTER_RULE)
                .withSingleParam(RuleParameterName.CATEGORY_ID, CATEGORY_ID)
                .withSingleParam(RuleParameterName.NOT_LOGIC, false)
                .build());

        var promo = promoManager.createPromocodePromo(createPromo(createCoins(CoreCoinType.PERCENT, 5), 1000, 2)
                .setRulesContainer(rc)
                .setBindOnlyOnce(true)
        );
        activatePromocodes("PROMO2-AF");
        var order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promo.getPromoKey()),
                        categoryId(CATEGORY_ID),
                        price(10000)
                )
                .build();
        var discountResponse = spend(order);
        assertThat(discountResponse.getPromocodeErrors(), emptyIterable());

        order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promo.getPromoKey()),
                        categoryId(CATEGORY_ID),
                        price(10000)
                )
                .build();
        discountResponse = spend(order);
        assertThat(discountResponse.getPromocodeErrors().iterator().next().getError().getError().getCode(),
                is(MarketLoyaltyErrorCode.COUPON_NOT_EXISTS.name()));
    }

    @Test
    public void testBrandMatch() {
        RulesContainer rc = createRulesContainer();
        rc.add(RuleContainer.builder(RuleType.VENDOR_FILTER_RULE)
                .withSingleParam(RuleParameterName.VENDOR_ID, VENDOR_ID)
                .withSingleParam(RuleParameterName.NOT_LOGIC, false)
                .build());

        int budget = 6000;
        int nominal = 300;
        var promocode = promoManager.createPromocodePromo(createPromo(createCoins(CoreCoinType.FIXED, nominal),
                budget, 1)
                .setRulesContainer(rc));
        activatePromocodes("PROMO1-AF");

        var discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promocode.getPromoKey()),
                        vendor(VENDOR_ID),
                        price(10000)
                )
                .build());

        assertThat(discountResponse.getCoins(), emptyIterable());
        assertThat(discountResponse.getUnusedPromocodes(), emptyIterable());
        assertThat(discountResponse.getPromocodeErrors(), emptyIterable());
        assertThat(discountResponse.getCoinErrors(), emptyIterable());

        var discountItems = discountResponse.getOrders().get(0).getItems();
        assertThat(discountItems, iterableWithSize(1));

        //noinspection unchecked
        assertThat(discountItems.get(0),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(300)))
                ))));

        var promo = promoService.getPromoByPromoKey(promocode.getPromoKey());
        assertThat(promo.getCurrentBudget().intValue(), equalTo(budget - nominal));
    }

    @Test
    public void testCategoryAndBrandMatch() {
        RulesContainer rc = createRulesContainer();
        rc.add(RuleContainer.builder(RuleType.VENDOR_FILTER_RULE)
                .withSingleParam(RuleParameterName.VENDOR_ID, VENDOR_ID)
                .withSingleParam(RuleParameterName.NOT_LOGIC, false)
                .build());
        rc.add(RuleContainer.builder(RuleType.CATEGORY_FILTER_RULE)
                .withSingleParam(RuleParameterName.CATEGORY_ID, CATEGORY_ID)
                .withSingleParam(RuleParameterName.NOT_LOGIC, false)
                .build()
        );

        int budget = 6000;
        int nominal = 300;
        var promocode = promoManager.createPromocodePromo(createPromo(createCoins(CoreCoinType.FIXED, nominal),
                budget, 1)
                .setRulesContainer(rc));
        activatePromocodes("PROMO1-AF");

        //1) Only brand - no match
        var discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promocode.getPromoKey()),
                        vendor(VENDOR_ID),
                        price(10000)
                )
                .build());
        assertThat(discountResponse.getPromocodeErrors().iterator().next().getError().getError().getCode(),
                is(MarketLoyaltyErrorCode.NOT_SUITABLE_FILTER_RULES.name()));

        //2) Only category - no match
        discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promocode.getPromoKey()),
                        categoryId(CATEGORY_ID),
                        price(10000)
                )
                .build());
        assertThat(discountResponse.getPromocodeErrors().iterator().next().getError().getError().getCode(),
                is(MarketLoyaltyErrorCode.NOT_SUITABLE_FILTER_RULES.name()));

        //3) Brand and category match
        discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promocode.getPromoKey()),
                        categoryId(CATEGORY_ID),
                        vendor(VENDOR_ID),
                        price(10000)
                )
                .build());
        var discountItems = discountResponse.getOrders().get(0).getItems();
        assertThat(discountItems, iterableWithSize(1));

        //noinspection unchecked
        assertThat(discountItems.get(0),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(300)))
                ))));

        var promo = promoService.getPromoByPromoKey(promocode.getPromoKey());
        assertThat(promo.getCurrentBudget().intValue(), equalTo(budget - nominal));
    }

    @Test
    public void testMsku() {
        RulesContainer rc = createRulesContainer();
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withParams(RuleParameterName.MSKU_ID, Set.of("22", "23"))
                .withSingleParam(RuleParameterName.NOT_LOGIC, false)
                .build());

        int budget = 6000;
        int nominal = 300;
        var promocode = promoManager.createPromocodePromo(createPromo(createCoins(CoreCoinType.FIXED, nominal),
                budget, 1)
                .setRulesContainer(rc));
        activatePromocodes("PROMO1-AF");

        var discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promocode.getPromoKey()),
                        price(10000)
                )
                .build());

        assertThat(discountResponse.getPromocodeErrors().iterator().next().getError().getError().getCode(),
                is(MarketLoyaltyErrorCode.NOT_SUITABLE_FILTER_RULES.name()));


        discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("23"),
                        promoKeys(promocode.getPromoKey()),
                        vendor(VENDOR_ID),
                        price(10000)
                )
                .build());
        var discountItems = discountResponse.getOrders().get(0).getItems();
        assertThat(discountItems, iterableWithSize(1));

        //noinspection unchecked
        assertThat(discountItems.get(0),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(300)))
                ))));

        var promo = promoService.getPromoByPromoKey(promocode.getPromoKey());
        assertThat(promo.getCurrentBudget().intValue(), equalTo(budget - nominal));
    }

    @Test
    public void testAllWithExceptionsMatch() {
        RulesContainer rc = createRulesContainer();
        rc.add(RuleContainer.builder(RuleType.CATEGORY_FILTER_RULE)
                .withSingleParam(RuleParameterName.CATEGORY_ID, NOT_LOGIC_CATEGORY_ID)
                .withSingleParam(RuleParameterName.NOT_LOGIC, true)
                .build());
        var promo = promoManager.createPromocodePromo(createPromo(createCoins(CoreCoinType.FIXED, 200), 1000, 3)
                .setRulesContainer(rc));
        activatePromocodes("PROMO3-AF");
        var order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promo.getPromoKey()),
                        categoryId(CATEGORY_ID),
                        price(10000)
                )
                .build();
        var discountResponse = spend(order);
        assertThat(discountResponse.getCoins(), emptyIterable());
        assertThat(discountResponse.getUnusedPromocodes(), emptyIterable());
        assertThat(discountResponse.getPromocodeErrors(), emptyIterable());
        assertThat(discountResponse.getCoinErrors(), emptyIterable());

        var discountItems = discountResponse.getOrders().get(0).getItems();
        assertThat(discountItems, iterableWithSize(1));

        //noinspection unchecked
        assertThat(discountItems.get(0),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(200)))
                ))));
    }

    @Test
    public void testAllWithExceptionsNotMatch() {
        RulesContainer rc = createRulesContainer();
        rc.add(RuleContainer.builder(RuleType.CATEGORY_FILTER_RULE)
                .withSingleParam(RuleParameterName.CATEGORY_ID, NOT_LOGIC_CATEGORY_ID)
                .withSingleParam(RuleParameterName.NOT_LOGIC, true)
                .build());
        var promo = promoManager.createPromocodePromo(createPromo(createCoins(CoreCoinType.FIXED, 200), 1000, 3)
                .setRulesContainer(rc));
        activatePromocodes("PROMO3-AF");
        var order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promo.getPromoKey()),
                        categoryId(NOT_LOGIC_CATEGORY_ID),
                        price(10000)
                )
                .build();
        var discountResponse = spend(order);
        assertThat(discountResponse.getPromocodeErrors(), iterableWithSize(1));
        assertThat(discountResponse.getPromocodeErrors().iterator().next().getError().getError().getUserMessage(),
                is("Промокод неприменим к такому составу заказа"));

    }

    @Test
    public void testUpperBoundOrderLimit() {
        RulesContainer rc = createRulesContainer();
        rc.add(RuleContainer.builder(RuleType.UPPER_BOUND_DISCOUNT_BASE_RULE)
                .withSingleParam(RuleParameterName.MAX_ORDER_TOTAL, BigDecimal.valueOf(10000)).build());

        var promo = promoManager.createPromocodePromo(createPromo(createCoins(CoreCoinType.PERCENT, 10), 100000, 4)
                .setRulesContainer(rc));
        activatePromocodes("PROMO4-AF");
        var order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promo.getPromoKey()),
                        price(15000)
                )
                .build();
        var discountResponse = spend(order);
        assertThat(discountResponse.getPromocodeErrors(), iterableWithSize(0));

        var discountItems = discountResponse.getOrders().get(0).getItems();
        assertThat(discountItems, iterableWithSize(1));
        //noinspection unchecked
        assertThat(discountItems.get(0),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(1000)))
                ))));
    }

    @Test
    public void testShopInclude() {
        RulesContainer rc = createRulesContainer();
        rc.add(RuleContainer.builder(RuleType.SUPPLIER_FILTER_RULE)
                .withSingleParam(RuleParameterName.SUPPLIER_ID, 4444L)
                .withSingleParam(RuleParameterName.NOT_LOGIC, false)
                .build());
        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withSingleParam(RuleParameterName.MSKU_ID, "3232")
                .withSingleParam(RuleParameterName.NOT_LOGIC, true)
                .build()
        );

        int budget = 6000;
        int nominal = 300;
        var promocode = promoManager.createPromocodePromo(createPromo(createCoins(CoreCoinType.FIXED, nominal),
                budget, 1)
                .setRulesContainer(rc));
        activatePromocodes("PROMO1-AF");

        //1) MSKU exclude - no match
        var discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("3232"),
                        promoKeys(promocode.getPromoKey()),
                        supplier(4444),
                        price(10000)
                )
                .build());
        assertThat(discountResponse.getPromocodeErrors().iterator().next().getError().getError().getCode(),
                is(MarketLoyaltyErrorCode.NOT_SUITABLE_FILTER_RULES.name()));

        //2) Match
        discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promocode.getPromoKey()),
                        supplier(4444),
                        price(10000)
                )
                .build());

        var discountItems = discountResponse.getOrders().get(0).getItems();
        assertThat(discountItems, iterableWithSize(1));

        //noinspection unchecked
        assertThat(discountItems.get(0),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(300)))
                ))));

        var promo = promoService.getPromoByPromoKey(promocode.getPromoKey());
        assertThat(promo.getCurrentBudget().intValue(), equalTo(budget - nominal));
    }

    @Test
    public void testShopExclude() {
        RulesContainer rc = createRulesContainer();
        rc.add(RuleContainer.builder(RuleType.VENDOR_FILTER_RULE)
                .withSingleParam(RuleParameterName.VENDOR_ID, VENDOR_ID)
                .withSingleParam(RuleParameterName.NOT_LOGIC, false)
                .build());
        rc.add(RuleContainer.builder(RuleType.SUPPLIER_FILTER_RULE)
                .withSingleParam(RuleParameterName.SUPPLIER_ID, 5555L)
                .withSingleParam(RuleParameterName.NOT_LOGIC, true)
                .build()
        );

        int budget = 6000;
        int nominal = 300;
        var promocode = promoManager.createPromocodePromo(createPromo(createCoins(CoreCoinType.FIXED, nominal),
                budget, 1)
                .setRulesContainer(rc));
        activatePromocodes("PROMO1-AF");

        //1) supplier exclude - no match
        var discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        promoKeys(promocode.getPromoKey()),
                        vendor(VENDOR_ID),
                        supplier(5555),
                        price(10000)
                )
                .build());
        assertThat(discountResponse.getPromocodeErrors().iterator().next().getError().getError().getCode(),
                is(MarketLoyaltyErrorCode.NOT_SUITABLE_FILTER_RULES.name()));

//2) Match
        discountResponse = spend(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        msku("1"),
                        promoKeys(promocode.getPromoKey()),
                        vendor(VENDOR_ID),
                        supplier(5556),
                        price(10000)
                )
                .build());
        var discountItems = discountResponse.getOrders().get(0).getItems();
        assertThat(discountItems, iterableWithSize(1));

        //noinspection unchecked
        assertThat(discountItems.get(0),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(300)))
                ))));

        var promo = promoService.getPromoByPromoKey(promocode.getPromoKey());
        assertThat(promo.getCurrentBudget().intValue(), equalTo(budget - nominal));
    }

    private String prepareFixedMinOrderLimit(int budget, int nominal, int minOrderLimit) {
        RulesContainer rc = createRulesContainer();
        rc.add(RuleContainer.builder(RuleType.CATEGORY_FILTER_RULE)
                .withSingleParam(RuleParameterName.CATEGORY_ID, CATEGORY_ID)
                .withSingleParam(RuleParameterName.NOT_LOGIC, false)
                .build());
        rc.add(RuleContainer.builder(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE)
                .withSingleParam(RuleParameterName.MIN_ORDER_TOTAL, BigDecimal.valueOf(minOrderLimit)).build());

        var fixedMinOrderLimitPromocode =
                promoManager.createPromocodePromo(createPromo(createCoins(CoreCoinType.FIXED, nominal), budget, 1)
                .setRulesContainer(rc));
        activatePromocodes("PROMO1-AF");
        return fixedMinOrderLimitPromocode.getPromoKey();
    }

    private void activatePromocodes(String promocodeValue) {
        promoService.reloadActiveSmartShoppingPromoIdsCache();
        storePromocodeService.savePromocodes(
                Identity.Type.UID.buildIdentity(String.valueOf(USER_ID)),
                Set.of(promocodeValue)
        );

        var operationContext = new OperationContextDto();
        operationContext.setUid(USER_ID);

        promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .operationContext(operationContext)
                        .userId(USER_ID)
                        .useSaved(true)
                        .build());
    }

    private MultiCartWithBundlesDiscountResponse spend(OrderWithBundlesRequest order) {
        HttpHeaders experiments = new HttpHeaders();
        experiments.add("X-Market-Rearrfactors", "loyalty-promo-report");

        return marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .useInternalPromocode(true)
                        .build(), experiments);
    }

    private CoinProps.Builder createCoins(CoreCoinType type, long nominal) {
        return CoinProps.builder()
                .setType(type)
                .setNominal(BigDecimal.valueOf(nominal))
                .setExpirationPolicy(ExpirationPolicy.expireByDays(90));
    }

    private PromocodePromoBuilder createPromo(
            CoinProps.Builder coinProps, long budget, int num) {
        return PromocodePromoBuilder.withCustomBuilder(coinProps)
                .setName("Child of aff_parent_0000 (some promocode)")
                .setCode("PROMO" + num + "-AF")
                .setPromoStorageId("PROMO" + num + "-AF")
                .setActionCode("PROMO" + num + "-AF")
                .setDontUploadToIdx(true)
                .setStatus(PromoStatus.ACTIVE)
                .setEmissionBudget(BigDecimal.valueOf(429496730))
                .setGeneratedPromocode(false)
                .setVersion(-1)
                .setPlatform(CoreMarketPlatform.BLUE)
                .setEmissionStartDate(Date.from(clock.instant().minus(5, ChronoUnit.DAYS)))
                .setEmissionEndDate(Date.from(clock.instant().plus(5, ChronoUnit.DAYS)))
                .setStartDate(Date.from(clock.instant().minus(5, ChronoUnit.DAYS)))
                .setEndDate(Date.from(clock.instant().plus(5, ChronoUnit.DAYS)))
                .setCoinCreationReason(CoreCoinCreationReason.ORDER)
                .setOnlyForPlus(false)
                .setNominalStrategy(new NominalStrategy.DefaultStrategy())
                .setBudgetProlongationDisabled(false)
                .setLandingUrl("")
                .setPromoSource(NMarket.Common.Promo.Promo.ESourceType.AFFILIATE.getNumber())
                .setMarketDepartment(MarketDepartment.OTHER)
                .setBudget(BigDecimal.valueOf(budget))
                .setExternalBudget(BigDecimal.valueOf(budget))
                .setEmissionFolding(false)
                .setBusinessId(3L)
                .setBindOnlyOnce(true)
                .setUpdatedAt(Date.from(clock.instant()))
                .setClid(3L)
                .setBudgetMode(BudgetMode.SYNC)
                .setAdditionalConditionsText("при стоимости корзины от 500 до 5000 р")
                .setPromoOfferAndAcceptance("")
                .setNoLandingUrl(false)
                .setConversion(BigDecimal.valueOf(100))
                .setTicketDescription(defaultTicketDescription());
    }

    private static RulesContainer createRulesContainer() {
        RulesContainer rcCategories = new RulesContainer();
        rcCategories.add(RuleContainer.builder(RuleType.HIDDEN_DATA_UNTIL_BIND_RESTRICTION_RULE)
                .withParams(RuleParameterName.HIDDEN_DATA_UNTIL_BIND, Set.of(false))
                .build());
        rcCategories.add(RuleContainer.builder(RuleType.EXCLUDED_OFFERS_FILTER_RULE)
                .withParams(RuleParameterName.EXCLUDED_OFFERS_TYPE, Set.of(ExcludedOffersType.PRICE_DISCOUNT))
                .build());
        return rcCategories;
    }
}
