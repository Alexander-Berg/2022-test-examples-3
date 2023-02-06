package ru.yandex.market.loyalty.core.utils;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.stereotype.Component;

import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.accounting.AccountMatter;
import ru.yandex.market.loyalty.core.model.action.PromoActionContainer;
import ru.yandex.market.loyalty.core.model.action.PromoActionParameterName;
import ru.yandex.market.loyalty.core.model.action.PromoActionType;
import ru.yandex.market.loyalty.core.model.action.PromoActionsMap;
import ru.yandex.market.loyalty.core.model.cashback.BillingSchema;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinDescription;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.order.OrderStage;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.CashbackPromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.CouponPromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.ExcludedOffersType;
import ru.yandex.market.loyalty.core.model.promo.ExcludedOffersType;
import ru.yandex.market.loyalty.core.model.promo.InfiniteUseCouponPromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;
import ru.yandex.market.loyalty.core.model.promo.PromocodePromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.promo.SingleUseCouponPromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.TicketDescription;
import ru.yandex.market.loyalty.core.model.promo.cashback.CashbackSource;
import ru.yandex.market.loyalty.core.model.promo.cashback.ExternalCashbackPromoBuilder;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.rule.RulesContainer;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.avatar.AvatarImageId;
import ru.yandex.market.loyalty.lightweight.DateUtils;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.apache.commons.lang.time.DateUtils.addDays;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;

/**
 * @author ukchuvrus
 */
@Component
public class PromoUtils {

    public static final String DEFAULT_TICKET_NUMBER = "MARTCHECKOUT-213";
    private final PromoService promoService;
    private final PromoManager promoManager;

    // these constants are part of test api too, so they are public
    public static final String DEFAULT_COUPON_CODE = "couponCode";
    public static final String ANOTHER_COUPON_CODE = "anotherCouponCode";
    public static final Date DEFAULT_START_DATE = new GregorianCalendar(2017, Calendar.FEBRUARY, 1).getTime();
    public static final Date DEFAULT_END_DATE = new GregorianCalendar(2030, Calendar.FEBRUARY, 1).getTime();
    public static final Date DEFAULT_EMISSION_END_DATE = new GregorianCalendar(2029, Calendar.FEBRUARY, 1).getTime();
    public static final String DEFAULT_DESCRIPTION = "promo_desc";
    public static final String DEFAULT_NAME = "promo";
    public static final BigDecimal DEFAULT_BUDGET = BigDecimal.valueOf(1000);
    public static final BigDecimal DEFAULT_EMISSION_BUDGET = BigDecimal.valueOf(2000);
    public static final BigDecimal DEFAULT_COUPON_VALUE = BigDecimal.valueOf(300);
    public static final BigDecimal DEFAULT_COUPON_VALUE_PERCENT = BigDecimal.valueOf(10);
    public static final BigDecimal DEFAULT_MAX_ORDER_TOTAL = BigDecimal.valueOf(50000);
    public static final BigDecimal DEFAULT_MIN_ORDER_TOTAL = BigDecimal.valueOf(1000);
    public static final BigDecimal DEFAULT_MAX_PURCHASE_VALUE = BigDecimal.valueOf(5000);
    public static final int DEFAULT_EXPIRATION_DAYS = 90;
    public static final CoreMarketPlatform DEFAULT_PLATFORM = CoreMarketPlatform.BLUE;
    public static final BigDecimal DEFAULT_CONVERSION = BigDecimal.valueOf(100);
    public static final String DEFAULT_TITLE = "Title";
    public static final String SHOP_PROMO_ID = "123523_CODE";
    public static final String SHOP_PROMO_ID_1 = "123524_CODE";

    public PromoUtils(
            PromoService promoService, PromoManager promoManager
    ) {
        this.promoService = promoService;
        this.promoManager = promoManager;
    }

    public static TicketDescription defaultTicketDescription() {
        return new TicketDescription("Особая акция одобрено Гришаковым", DEFAULT_TICKET_NUMBER, false);
    }

    public static AccrualPromoBuilder accrualPromoBuilder() {
        return new AccrualPromoBuilder();
    }

    public void buildPromocodePromo(PromocodePromoBuilder builder) {
        promoManager.createPromocodePromo(builder);
    }

    public Promo buildWalletAccrualPromo(AccrualPromoBuilder builder) {
        return builder.build(promoManager);
    }

    public static class Coupon {
        public static SingleUseCouponPromoBuilder defaultSingleUse() {
            SingleUseCouponPromoBuilder promoBuilder = CouponPromoBuilder.singleUse();
            applyCommon(promoBuilder);
            return promoBuilder
                    .setCouponValue(DEFAULT_COUPON_VALUE, CoreCouponValueType.FIXED)
                    .setEmissionBudget(DEFAULT_EMISSION_BUDGET)
                    .setExpiration(ExpirationPolicy.expireByDays(DEFAULT_EXPIRATION_DAYS))
                    .setStartEmissionDate(DEFAULT_START_DATE)
                    .setEndEmissionDate(DEFAULT_EMISSION_END_DATE)
                    .setConversion(DEFAULT_CONVERSION);
        }

        public static SingleUseCouponPromoBuilder defaultSingleUseWithPercent() {
            SingleUseCouponPromoBuilder promoBuilder = CouponPromoBuilder.singleUse();
            applyCommon(promoBuilder);
            return promoBuilder
                    .setCouponValue(DEFAULT_COUPON_VALUE_PERCENT, CoreCouponValueType.PERCENT)
                    .setEmissionBudget(DEFAULT_EMISSION_BUDGET)
                    .setExpiration(ExpirationPolicy.expireByDays(DEFAULT_EXPIRATION_DAYS))
                    .setStartEmissionDate(DEFAULT_START_DATE)
                    .setEndEmissionDate(DEFAULT_EMISSION_END_DATE)
                    .setConversion(DEFAULT_CONVERSION)
                    .addPromoRule(
                            RuleType.UPPER_BOUND_DISCOUNT_BASE_RULE,
                            RuleParameterName.MAX_ORDER_TOTAL,
                            Set.of(DEFAULT_MAX_ORDER_TOTAL)
                    );
        }

        public static InfiniteUseCouponPromoBuilder defaultInfiniteUse() {
            return defaultInfiniteUse(PromoSubType.COUPON);
        }

        public static InfiniteUseCouponPromoBuilder defaultInfiniteUse(PromoSubType promoSubType) {
            InfiniteUseCouponPromoBuilder promoBuilder = CouponPromoBuilder.infiniteUse(promoSubType);
            applyCommon(promoBuilder);
            return promoBuilder
                    .setCouponValue(DEFAULT_COUPON_VALUE, CoreCouponValueType.FIXED)
                    .setCouponCode(DEFAULT_COUPON_CODE)
                    .addPromoRule(
                            RuleType.EXCLUDED_OFFERS_FILTER_RULE,
                            RuleParameterName.EXCLUDED_OFFERS_TYPE,
                            Set.of(ExcludedOffersType.PRICE_DISCOUNT)
                    );
        }

        private static void applyCommon(CouponPromoBuilder<?> promoBuilder) {
            promoBuilder
                    .setName(DEFAULT_NAME)
                    .setDescription(DEFAULT_DESCRIPTION)
                    .setPlatform(DEFAULT_PLATFORM)
                    .setStatus(PromoStatus.ACTIVE)
                    .setBudget(DEFAULT_BUDGET)
                    .setStartDate(DEFAULT_START_DATE)
                    .setEndDate(DEFAULT_END_DATE)
                    .setTicketDescription(defaultTicketDescription())
                    .setBudgetThreshold(DEFAULT_BUDGET.multiply(new BigDecimal("0.1")))
                    .setPromoSource(LOYALTY_VALUE);
        }
    }

    public abstract static class PromoBuilder<T extends PromoBuilder<T>> {
        String name = DEFAULT_NAME;
        CoreMarketPlatform platform = DEFAULT_PLATFORM;
        Date startDate = DEFAULT_START_DATE;
        Date endDate = DEFAULT_END_DATE;
        String description = DEFAULT_DESCRIPTION;
        PromoStatus status = PromoStatus.ACTIVE;
        AccountMatter emissionMatter;
        Integer promoSource = LOYALTY_VALUE;

        protected abstract T that();

        public T setDescription(String description) {
            this.description = description;
            return that();
        }

        public T setStatus(PromoStatus status) {
            this.status = status;
            return that();
        }

        public T setName(String name) {
            this.name = name;
            return that();
        }

        public T setPlatform(CoreMarketPlatform platform) {
            this.platform = platform;
            return that();
        }

        public T setStartDate(Date startDate) {
            this.startDate = DateUtils.copy(startDate);
            return that();
        }

        public T setEndDate(Date endDate) {
            this.endDate = DateUtils.copy(endDate);
            return that();
        }

        public T setEmissionMatter(AccountMatter emissionMatter) {
            this.emissionMatter = emissionMatter;
            return that();
        }

        public String getName() {
            return name;
        }

        public CoreMarketPlatform getPlatform() {
            return platform;
        }

        public Date getStartDate() {
            return new Date(startDate.getTime());
        }

        public Date getEndDate() {
            return new Date(endDate.getTime());
        }

        public String getDescription() {
            return description;
        }

        public PromoStatus getStatus() {
            return status;
        }

        public AccountMatter getEmissionMatter() {
            return emissionMatter;
        }
    }

    public static class SmartShopping {
        public static final BigDecimal DEFAULT_COIN_FIXED_NOMINAL = BigDecimal.valueOf(300);
        public static final BigDecimal DEFAULT_COIN_PERCENT_NOMINAL = BigDecimal.TEN;
        public static final String DEFAULT_COIN_DESCRIPTION = "ОЧЕНЬ выгодная монетка";
        public static final AvatarImageId DEFAULT_COIN_IMAGE = new AvatarImageId(123, "asdasda");
        public static final String DEFAULT_COIN_BACKGROUND = "#aaaaaa";
        public static final BigDecimal DEFAULT_EMISSION_BUDGET_IN_COINS = BigDecimal.valueOf(20);
        public static final BigDecimal DEFAULT_CONVERSION = BigDecimal.valueOf(100);
        public static final BigDecimal DEFAULT_AVERAGE_BILL = BigDecimal.valueOf(6000);
        public static final String DEFAULT_OUTGOING_LINK = "https://m.beru.ru/";

        public static SmartShoppingPromoBuilder<?> defaultFixed() {
            return defaultFixed(DEFAULT_COIN_FIXED_NOMINAL);
        }

        public static SmartShoppingPromoBuilder<?> defaultPercent() {
            return defaultPercent(DEFAULT_COIN_PERCENT_NOMINAL);
        }

        public static SmartShoppingPromoBuilder<?> defaultFixed(BigDecimal nominal) {
            return defaultFixed(nominal, defaultCoinDescriptionBuilder());
        }

        public static SmartShoppingPromoBuilder<?> defaultPercent(BigDecimal nominal) {
            return defaultPercent(nominal, defaultCoinDescriptionBuilder());
        }

        public static SmartShoppingPromoBuilder<?> defaultFreeMsku() {
            return withDefaults(SmartShoppingPromoBuilder.freeMsku())
                    .setExpiration(ExpirationPolicy.expireByDays(DEFAULT_EXPIRATION_DAYS))
                    .setCoinDescription(CoinDescription.builder()
                            .setBackgroundColor("#fff")
                            .setRestrictionDescription("на все заказы")
                            .setDescription("")
                            .setAvatarImageId(new AvatarImageId(0, "image"))
                            .setTitle("title")
                            .setOutgoingLink(null)
                    )
                    .setEmissionBudget(DEFAULT_EMISSION_BUDGET_IN_COINS);
        }

        public static SmartShoppingPromoBuilder<?> defaultFixed(
                BigDecimal nominal, CoinDescription.Builder coinDescriptionBuilder
        ) {
            return withDefaults(SmartShoppingPromoBuilder.fixed(nominal))
                    .setExpiration(ExpirationPolicy.expireByDays(DEFAULT_EXPIRATION_DAYS))
                    .setConversion(DEFAULT_CONVERSION)
                    .setCoinDescription(coinDescriptionBuilder)
                    .addCoinRule(
                            RuleType.EXCLUDED_OFFERS_FILTER_RULE,
                            RuleParameterName.EXCLUDED_OFFERS_TYPE,
                            Set.of(ExcludedOffersType.PRICE_DISCOUNT)
                    );
        }

        public static SmartShoppingPromoBuilder<?> defaultPercent(
                BigDecimal nominal, CoinDescription.Builder coinDescriptionBuilder
        ) {
            return withDefaults(SmartShoppingPromoBuilder.percent(nominal))
                    .setExpiration(ExpirationPolicy.expireByDays(DEFAULT_EXPIRATION_DAYS))
                    .setConversion(DEFAULT_CONVERSION)
                    .setAverageBill(DEFAULT_AVERAGE_BILL)
                    .setCoinDescription(coinDescriptionBuilder)
                    .addCoinRule(
                            RuleType.EXCLUDED_OFFERS_FILTER_RULE,
                            RuleParameterName.EXCLUDED_OFFERS_TYPE,
                            Set.of(ExcludedOffersType.PRICE_DISCOUNT)
                    );
        }

        public static SmartShoppingPromoBuilder<?> defaultFreeDelivery() {
            return withDefaults(SmartShoppingPromoBuilder.freeDelivery())
                    .setExpiration(ExpirationPolicy.expireByDays(DEFAULT_EXPIRATION_DAYS))
                    .setCoinDescription(defaultCoinDescriptionBuilder())
                    .addCoinRule(
                            RuleType.EXCLUDED_OFFERS_FILTER_RULE,
                            RuleParameterName.EXCLUDED_OFFERS_TYPE,
                            Set.of(ExcludedOffersType.FREE_DELIVERY)
                    );
        }

        public static SmartShoppingPromoBuilder<?> defaultFreeDeliveryWithMinOrderTotalRule(BigDecimal minOrderTotal) {
            return defaultFreeDelivery()
                    .setRulesContainer(getRulesContainerWithMinOrderTotal(minOrderTotal))
                    .setPromoKey("testKey");
        }

        public static SmartShoppingPromoBuilder<?> defaultFreeDeliveryWithNoDbsRule() {
            return defaultFreeDelivery()
                    .setRulesContainer(getRulesContainerWithNoDbsRule())
                    .setPromoKey("testKey");
        }

        public static RulesContainer getRulesContainerWithMinOrderTotal(BigDecimal minOrderTotal) {
            var rulesContainer = new RulesContainer();
            var minOrderTotalCuttingRule = RuleContainer
                    .builder(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE)
                    .withParams(RuleParameterName.MIN_ORDER_TOTAL, Collections.singleton(minOrderTotal))
                    .build();
            rulesContainer.add(minOrderTotalCuttingRule);

            return rulesContainer;
        }

        public static RulesContainer getRulesContainerWithNoDbsRule() {
            var rulesContainer = new RulesContainer();
            var filterRule = RuleContainer
                    .builder(RuleType.DBS_SUPPLIER_FLAG_RESTRICTION_FILTER_RULE)
                    .withSingleParam(RuleParameterName.NOT_DBS_SUPPLIER_FLAG_RESTRICTION, true)
                    .build();
            rulesContainer.add(filterRule);

            return rulesContainer;
        }

        public static SmartShoppingPromoBuilder<?> defaultDynamic() {
            return withDefaults(SmartShoppingPromoBuilder.dynamic())
                    .setCoinCreationReason(CoreCoinCreationReason.ORDER_DYNAMIC);
        }

        private static SmartShoppingPromoBuilder<?> withDefaults(SmartShoppingPromoBuilder<?> coinPromoBuilder) {
            return coinPromoBuilder
                    .setStartDate(DEFAULT_START_DATE)
                    .setEndDate(DEFAULT_END_DATE)
                    .setEmissionStartDate(DEFAULT_START_DATE)
                    .setEmissionEndDate(addDays(DEFAULT_END_DATE, -91))
                    .setName(DEFAULT_NAME)
                    .setBudget(DEFAULT_BUDGET)
                    .setEmissionBudget(DEFAULT_EMISSION_BUDGET_IN_COINS)
                    .setTicketDescription(defaultTicketDescription())
                    .setStatus(PromoStatus.ACTIVE)
                    .setCoinCreationReason(CoreCoinCreationReason.ORDER)
                    .setPromoSource(LOYALTY_VALUE);
        }

        public static CoinDescription.Builder defaultCoinDescriptionBuilder() {
            return CoinDescription.builder()
                    .setDescription(DEFAULT_COIN_DESCRIPTION)
                    .setAvatarImageId(DEFAULT_COIN_IMAGE)
                    .setOutgoingLink(DEFAULT_OUTGOING_LINK)
                    .setBackgroundColor(DEFAULT_COIN_BACKGROUND);
        }

        public static Coin buildCoin(CoinKey coinKey, BigDecimal nominal, CoreCoinType coinType) {
            return Coin.builder(coinKey)
                    .setType(coinType)
                    .setPromoSubType(PromoSubType.MARKET_BONUS)
                    .setNominal(nominal)
                    .setStartDate(new Date())
                    .setCreationDate(new Date())
                    .setEndDate(new Date())
                    .setBudgetAccountId(0L)
                    .setSpendingAccountId(0L)
                    .setBudgetMode(BudgetMode.SYNC)
                    .setPromoId(0L)
                    .setCoinPropsId(0L)
                    .setCoinDescriptionId(0L)
                    .setPlatform(CoreMarketPlatform.BLUE)
                    .setExpirationPolicy(ExpirationPolicy.expireByDays(DEFAULT_EXPIRATION_DAYS))
                    .build();
        }

        public static PromocodePromoBuilder defaultFixedPromocode() {
            return defaultFixedPromocode(DEFAULT_COIN_FIXED_NOMINAL);
        }

        public static PromocodePromoBuilder defaultFirstOrderPromocode() {
            return defaultFirstOrderPromocode(DEFAULT_COIN_FIXED_NOMINAL);
        }

        public static PromocodePromoBuilder defaultFixedPromocode(BigDecimal nominal) {
            return withDefaults(PromocodePromoBuilder.fixed(nominal))
                    .setExpiration(ExpirationPolicy.expireByDays(DEFAULT_EXPIRATION_DAYS))
                    .setConversion(DEFAULT_CONVERSION);
        }

        public static PromocodePromoBuilder defaultPercentPromocode(BigDecimal nominal) {
            return withDefaults(PromocodePromoBuilder.percent(nominal))
                    .setExpiration(ExpirationPolicy.expireByDays(DEFAULT_EXPIRATION_DAYS))
                    .setConversion(DEFAULT_CONVERSION);
        }

        private static PromocodePromoBuilder defaultFirstOrderPromocode(BigDecimal nominal) {
            RulesContainer rc = new RulesContainer();
            rc.add(RuleContainer.builder(RuleType.FIRST_ORDER_CUTTING_RULE).build());
            return withDefaults(PromocodePromoBuilder.fixed(nominal))
                    .setExpiration(ExpirationPolicy.expireByDays(DEFAULT_EXPIRATION_DAYS))
                    .setEmissionBudget(null)
                    .setRulesContainer(rc)
                    .setConversion(DEFAULT_CONVERSION);
        }

        private static PromocodePromoBuilder withDefaults(PromocodePromoBuilder promocodePromoBuilder) {
            return promocodePromoBuilder
                    .setStartDate(DEFAULT_START_DATE)
                    .setEndDate(DEFAULT_END_DATE)
                    .setEmissionStartDate(DEFAULT_START_DATE)
                    .setEmissionEndDate(addDays(DEFAULT_END_DATE, -91))
                    .setName(DEFAULT_NAME)
                    .setBudget(DEFAULT_BUDGET)
                    .setEmissionBudget(DEFAULT_EMISSION_BUDGET_IN_COINS)
                    .setTicketDescription(defaultTicketDescription())
                    .setStatus(PromoStatus.ACTIVE)
                    .setCoinCreationReason(CoreCoinCreationReason.ORDER)
                    .setPromoStorageId(SHOP_PROMO_ID)
                    .setShopPromoId(randomString())
                    .setPromoSource(LOYALTY_VALUE);
        }

        public static PromocodePromoBuilder pickupPromoBrandedPickupStickyFilterRule() {
            var rulesContainer = new RulesContainer();
            rulesContainer.add(RuleContainer.builder(RuleType.MARKET_BRANDED_PICKUP_RULE)
                    .withParams(RuleParameterName.MARKET_BRANDED_PICKUP_STICKY, Collections.singleton(true)).build());

            return withDefaults(PromocodePromoBuilder.fixed(BigDecimal.valueOf(1)))
                    .setExpiration(ExpirationPolicy.expireByDays(DEFAULT_EXPIRATION_DAYS))
                    .setEmissionBudget(null)
                    .setRulesContainer(rulesContainer)
                    .setConversion(DEFAULT_CONVERSION);
        }

        public static PromocodePromoBuilder pickupPromoBrandedPickupAndInsufficientTotalRule() {
            var rulesContainer = new RulesContainer();
            rulesContainer.add(RuleContainer.builder(RuleType.MARKET_BRANDED_PICKUP_RULE)
                    .withParams(RuleParameterName.MARKET_BRANDED_PICKUP_STICKY, Collections.singleton(true)).build());
            rulesContainer.add(RuleContainer.builder(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE)
                    .withParams(RuleParameterName.MIN_ORDER_TOTAL, Collections.singleton(BigDecimal.valueOf(500))).build());

            return withDefaults(PromocodePromoBuilder.fixed(BigDecimal.valueOf(1)))
                    .setExpiration(ExpirationPolicy.expireByDays(DEFAULT_EXPIRATION_DAYS))
                    .setEmissionBudget(null)
                    .setRulesContainer(rulesContainer)
                    .setConversion(DEFAULT_CONVERSION);
        }
    }

    public static class Cashback {
        public static CashbackPromoBuilder defaultPercent(BigDecimal nominal) {
            return defaultPercent(nominal, CashbackLevelType.ITEM);
        }

        public static CashbackPromoBuilder defaultPercent(int nominal) {
            return defaultPercent(BigDecimal.valueOf(nominal), CashbackLevelType.ITEM);
        }

        public static CashbackPromoBuilder defaultPercent(BigDecimal nominal, CashbackLevelType level) {
            return CashbackPromoBuilder.percent(nominal, level)
                    .setStartDate(DEFAULT_START_DATE)
                    .setEndDate(DEFAULT_END_DATE)
                    .setName(DEFAULT_NAME)
                    .setTicketDescription(defaultTicketDescription())
                    .setStatus(PromoStatus.ACTIVE)
                    .setBillingSchema(BillingSchema.SOLID)
                    .setPromoSource(LOYALTY_VALUE);
        }

        public static CashbackPromoBuilder defaultFixed(BigDecimal nominal) {
            return defaultFixed(nominal, CashbackLevelType.MULTI_ORDER);
        }

        public static CashbackPromoBuilder defaultFixed(BigDecimal nominal, CashbackLevelType level) {
            return CashbackPromoBuilder.fixed(nominal, level)
                    .setStartDate(DEFAULT_START_DATE)
                    .setEndDate(DEFAULT_END_DATE)
                    .setName(DEFAULT_NAME)
                    .setTicketDescription(defaultTicketDescription())
                    .setStatus(PromoStatus.ACTIVE)
                    .setBillingSchema(BillingSchema.SOLID)
                    .setPromoSource(LOYALTY_VALUE);
        }

        public static CashbackPromoBuilder defaultFixedWithMinOrder(BigDecimal nominal, BigDecimal minOrderTotal) {
            return defaultFixed(nominal)
                    .addCashbackRule(
                            RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                            RuleParameterName.MIN_ORDER_TOTAL,
                            minOrderTotal
                    );
        }

        public static CashbackPromoBuilder withNominalAndPriority(BigDecimal nominal, Integer priority) {
            CashbackPromoBuilder cashbackPromoBuilder = defaultPercent(nominal);
            cashbackPromoBuilder.setPriority(priority);
            return cashbackPromoBuilder;
        }

        public static PromoActionsMap getActionsMapWithStaticPerkAddition(String perkName, OrderStage stage) {
            var actions = new PromoActionsMap();
            var perkAdditionAction = PromoActionContainer
                    .builder(PromoActionType.STATIC_PERK_ADDITION_ACTION, stage)
                    .withParams(PromoActionParameterName.STATIC_PERK_NAME, Collections.singleton(perkName))
                    .build();
            actions.add(perkAdditionAction);

            return actions;
        }
    }

    public static class PersonalCashback {
        public static CashbackPromoBuilder defaultPerson() {
            return CashbackPromoBuilder.percent(BigDecimal.TEN, CashbackLevelType.ITEM)
                    .setStartDate(DEFAULT_START_DATE)
                    .setEndDate(DEFAULT_END_DATE)
                    .setName(DEFAULT_NAME)
                    .setTicketDescription(defaultTicketDescription())
                    .setStatus(PromoStatus.ACTIVE)
                    .setIsPersonalPromo(true)
                    .setPromoSource(LOYALTY_VALUE);
        }
    }

    public static class ExternalCashback {
        public static ExternalCashbackPromoBuilder defaultBank() {
            return ExternalCashbackPromoBuilder.create(CashbackSource.YANDEX_BANK)
                    .setStartDate(DEFAULT_START_DATE)
                    .setEndDate(DEFAULT_END_DATE)
                    .setName(DEFAULT_NAME)
                    .setTicketDescription(defaultTicketDescription())
                    .setStatus(PromoStatus.ACTIVE)
                    .setPromoSource(LOYALTY_VALUE);
        }
    }

    public static class WalletAccrual {
        public static AccrualPromoBuilder defaultAccrual() {
            return PromoUtils.accrualPromoBuilder()
                    .setName("promoName")
                    .setStartDate(DEFAULT_START_DATE)
                    .setEndDate(org.apache.commons.lang3.time.DateUtils.addDays(DEFAULT_START_DATE, 3));
        }

        public static ru.yandex.market.loyalty.core.model.promo.AccrualPromoBuilder defaultModelAccrual() {
            return new ru.yandex.market.loyalty.core.model.promo.AccrualPromoBuilder()
                    .setName("TestAccrualPromo")
                    .setPlatform(CoreMarketPlatform.BLUE)
                    .setStartDate(DEFAULT_START_DATE)
                    .setEndDate(DEFAULT_END_DATE)
                    .setDescription("Акция начисления кэшбэка для тестирования")
                    .setStatus(PromoStatus.ACTIVE)
                    .setEmissionBudget(BigDecimal.valueOf(100000))
                    .setTicketDescription(defaultTicketDescription())
                    .setPromoSource(LOYALTY_VALUE);
        }
    }

    public static class AccrualPromoBuilder extends PromoBuilder<AccrualPromoBuilder> {
        @Override
        protected AccrualPromoBuilder that() {
            return this;
        }

        Promo build(PromoManager promoManager) {
            ru.yandex.market.loyalty.core.model.promo.AccrualPromoBuilder builder =
                    new ru.yandex.market.loyalty.core.model.promo.AccrualPromoBuilder();
            builder = builder.setName(name)
                    .setStatus(status)
                    .setDescription(description)
                    .setPlatform(platform)
                    .setTicketDescription(defaultTicketDescription())
                    .setStartDate(startDate)
                    .setEndDate(endDate)
                    .setPromoSource(promoSource);

            return promoManager.createAccrualPromo(builder);
        }
    }

    @VisibleForTesting
    public void reloadFreeDeliveryPromosCache() {
        promoService.reloadFreeDeliveryPromoCache();
    }
}
