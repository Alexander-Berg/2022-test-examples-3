package ru.yandex.travel.orders.services.promo;

import java.math.BigDecimal;
import java.util.List;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.hotels.common.orders.DolphinHotelItinerary;
import ru.yandex.travel.hotels.common.orders.OrderDetails;
import ru.yandex.travel.hotels.common.orders.TravellineHotelItinerary;
import ru.yandex.travel.orders.commons.proto.EPromoCodeNominalType;
import ru.yandex.travel.orders.commons.proto.EServiceType;
import ru.yandex.travel.orders.entities.DolphinOrderItem;
import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.orders.entities.TravellineOrderItem;
import ru.yandex.travel.orders.entities.promo.DiscountApplicationConfig;
import ru.yandex.travel.orders.entities.promo.HotelRestriction;
import ru.yandex.travel.orders.entities.promo.PromoAction;
import ru.yandex.travel.orders.entities.promo.PromoCode;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Enclosed.class)
public class DefaultPromoDiscountCalculatorTest {

    public static final long DEFAULT_PASSPORT_ID = 100L;
    public static final String TRAVELLINE_ORIGINAL_ID = "travelline_original_id";
    public static final String DOLPHIN_ORIGINAL_ID = "dolphin_original_id";
    public static final BigDecimal DEFAULT_NOMINAL = BigDecimal.valueOf(100L);
    public static final Money DEFAULT_NOMINAL_DISCOUNT = Money.of(DEFAULT_NOMINAL, ProtoCurrencyUnit.RUB);
    public static final Money DEFAULT_ORDER_MONEY = Money.of(1000L, ProtoCurrencyUnit.RUB);
    public static final Money LARGE_ORDER_MONEY = Money.of(100000L, ProtoCurrencyUnit.RUB);
    public static final Money ONE_RUB = Money.of(1, ProtoCurrencyUnit.RUB);

    public abstract static class TestBase {
        protected DefaultPromoDiscountCalculator calculator;
        protected PromoCode promo;
        protected DiscountApplicationConfig cfg;

        protected UserOrderCounterService userOrderCounterService;

        protected ServiceDescription serviceDescription;

        @Before
        public void setUp() {
            userOrderCounterService = Mockito.mock(UserOrderCounterService.class);
            calculator = new DefaultPromoDiscountCalculator(userOrderCounterService);

            cfg = createApplicationConfig();
            promo = createPromo(cfg);
            serviceDescription = ServiceDescription.builder()
                    .serviceType(EServiceType.PT_DOLPHIN_HOTEL)
                    .originalCost(DEFAULT_ORDER_MONEY)
                    .payload(new DolphinHotelItinerary() {{
                        setOrderDetails(OrderDetails.builder()
                                .originalId(DOLPHIN_ORIGINAL_ID)
                                .build());
                    }})
                    .build();
        }

        protected abstract DiscountApplicationConfig createApplicationConfig();

        protected PromoCode createPromo(DiscountApplicationConfig cfg) {
            return createNominalPromo(cfg);
        }

        protected PromoCodeDiscountCalculationCtx makeCalculationContext() {
            PromoCodeDiscountCalculationCtx ctx = new PromoCodeDiscountCalculationCtx();
            ctx.setPassportId(DEFAULT_PASSPORT_ID);
            ctx.setPromoCode(promo);
            ctx.setAllPromoCodes(List.of(promo));
            ctx.setUserStaff(false);
            ctx.setUserPlus(false);
            ctx.setServiceDescriptions(List.of(serviceDescription));
            return ctx;
        }

        protected PromoCodeDiscountApplicationCtx makeApplicationContext(Money amount) {
            var dolphinItinerary = new DolphinHotelItinerary();
            dolphinItinerary.setOrderDetails(OrderDetails.builder()
                    .originalId(DOLPHIN_ORIGINAL_ID)
                    .build());

            var orderItem = new DolphinOrderItem();
            orderItem.setItinerary(dolphinItinerary);

            return makeApplicationContext(amount, orderItem);
        }

        protected PromoCodeDiscountApplicationCtx makeApplicationContext(Money amount, OrderItem orderItem) {
            addFiscalItems(amount, orderItem);

            PromoCodeDiscountApplicationCtx ctx = new PromoCodeDiscountApplicationCtx();
            ctx.setPromoCode(promo);
            ctx.setAllPromoCodes(List.of(promo));
            ctx.setOrderItems(List.of(orderItem));
            ctx.setUserStaff(false);
            ctx.setUserPlus(false);
            return ctx;
        }

        protected void addFiscalItems(Money amount, OrderItem orderItem) {
            FiscalItem fiscalItem = new FiscalItem();
            fiscalItem.setMoneyAmount(amount);

            orderItem.addFiscalItem(fiscalItem);
        }


        protected PromoCode createNominalPromo(DiscountApplicationConfig cfg) {
            return createPromoCode(DEFAULT_NOMINAL, EPromoCodeNominalType.NT_VALUE,
                    createPromoAction(cfg));
        }

        protected PromoCode createPercentPromo(DiscountApplicationConfig cfg) {
            return createPromoCode(BigDecimal.valueOf(5L), EPromoCodeNominalType.NT_PERCENT,
                    createPromoAction(cfg));
        }


        protected PromoAction createPromoAction(DiscountApplicationConfig cfg) {
            PromoAction action = new PromoAction();
            action.setDiscountApplicationConfig(cfg);
            return action;
        }

        protected PromoCode createPromoCode(BigDecimal nominal, EPromoCodeNominalType type,
                                            PromoAction promoAction) {
            PromoCode result = new PromoCode();
            result.setCode("NOT_USED");
            result.setNominal(nominal);
            result.setNominalType(type);
            result.setPromoAction(promoAction);
            return result;
        }

    }

    public static class BasicNominalTest extends TestBase {
        @Override
        protected DiscountApplicationConfig createApplicationConfig() {
            return DiscountApplicationConfigTestData.empty().build();
        }

        @Test
        public void hugePromoCodeShouldNotMakeNegativePrice() {
            Money originalCost = DEFAULT_NOMINAL_DISCOUNT.subtract(
                    Money.of(10, ProtoCurrencyUnit.RUB)
            );
            this.serviceDescription.setOriginalCost(originalCost);
            PromoCodeDiscountCalculationCtx ctx = makeCalculationContext();

            CodeApplicationResult result = calculator.calculateDiscountForEstimation(ctx);
            Money expectedDiscount = serviceDescription.getOriginalCost().subtract(ONE_RUB);
            assertThat(result.getDiscountAmount()).isEqualTo(expectedDiscount);
        }

        @Test
        public void promoCodeWithDiscountEqualsToOriginalCost() {
            this.serviceDescription.setOriginalCost(DEFAULT_NOMINAL_DISCOUNT);
            PromoCodeDiscountCalculationCtx ctx = makeCalculationContext();

            CodeApplicationResult result = calculator.calculateDiscountForEstimation(ctx);
            Money expectedDiscount = serviceDescription.getOriginalCost().subtract(ONE_RUB);
            assertThat(result.getDiscountAmount()).isEqualTo(expectedDiscount);
        }

        @Test
        public void promoCodeWith100PercentDiscount() {
            promo = createPromoCode(BigDecimal.valueOf(100), EPromoCodeNominalType.NT_PERCENT, createPromoAction(cfg));
            PromoCodeDiscountCalculationCtx ctx = makeCalculationContext();

            CodeApplicationResult result = calculator.calculateDiscountForEstimation(ctx);
            Money expectedDiscount = serviceDescription.getOriginalCost().subtract(ONE_RUB);
            assertThat(result.getDiscountAmount()).isEqualTo(expectedDiscount);
        }
    }

    public static class MinTotalCostTest extends TestBase {
        @Override
        protected DiscountApplicationConfig createApplicationConfig() {
            return DiscountApplicationConfigTestData.empty()
                    .withMinTotalCost(DEFAULT_ORDER_MONEY).build();
        }

        @Override
        protected PromoCode createPromo(DiscountApplicationConfig cfg) {
            return createPercentPromo(cfg);
        }


        @Test
        public void testCalculateDiscountForApplicationMinTotalCost() {
            Money wrongAmount = DEFAULT_ORDER_MONEY.divide(2);

            PromoCodeDiscountApplicationCtx ctx = makeApplicationContext(wrongAmount);

            FiscalItemApplicationResult result = calculator.calculateDiscountForApplication(ctx);
            assertThat(result.getResultType()).isEqualTo(ApplicationResultType.NOT_APPLICABLE);

            ctx = makeApplicationContext(DEFAULT_ORDER_MONEY);

            result = calculator.calculateDiscountForApplication(ctx);
            assertThat(result.getResultType()).isEqualTo(ApplicationResultType.SUCCESS);
            assertThat(result.getDiscountMap().values().stream().findFirst().get()).isEqualTo(Money.of(50L,
                    ProtoCurrencyUnit.RUB));
        }

    }

    public static class StaffOnlyTest extends TestBase {
        @Override
        protected DiscountApplicationConfig createApplicationConfig() {
            return DiscountApplicationConfigTestData.empty()
                    .forStaffOnly().build();
        }

        @Test
        public void testCalculateDiscountForEstimationStaffOnly() {
            PromoCodeDiscountCalculationCtx ctx = makeCalculationContext();

            CodeApplicationResult result = calculator.calculateDiscountForEstimation(ctx);
            assertThat(result.getType()).isEqualTo(ApplicationResultType.NOT_APPLICABLE);

            ctx.setUserStaff(true);

            result = calculator.calculateDiscountForEstimation(ctx);
            assertThat(result.getType()).isEqualTo(ApplicationResultType.SUCCESS);
            assertThat(result.getDiscountAmount()).isEqualTo(DEFAULT_NOMINAL_DISCOUNT);
        }

        @Test
        public void testCalculateDiscountForApplicationStaffOnly() {
            promo = createPercentPromo(cfg);
            PromoCodeDiscountApplicationCtx ctx = makeApplicationContext(DEFAULT_ORDER_MONEY);

            FiscalItemApplicationResult result = calculator.calculateDiscountForApplication(ctx);
            assertThat(result.getResultType()).isEqualTo(ApplicationResultType.NOT_APPLICABLE);

            ctx.setUserStaff(true);

            result = calculator.calculateDiscountForApplication(ctx);
            assertThat(result.getResultType()).isEqualTo(ApplicationResultType.SUCCESS);
            assertThat(result.getDiscountMap().values().stream().findFirst().get()).isEqualTo(Money.of(50L,
                    ProtoCurrencyUnit.RUB));
        }


    }

    public static class PlusOnlyTest extends TestBase {
        @Override
        protected DiscountApplicationConfig createApplicationConfig() {
            return DiscountApplicationConfigTestData.empty()
                    .forPlusOnly().build();
        }

        @Test
        public void testCalculateDiscountForEstimationPlusOnly() {
            PromoCodeDiscountCalculationCtx ctx = makeCalculationContext();

            CodeApplicationResult result = calculator.calculateDiscountForEstimation(ctx);
            assertThat(result.getType()).isEqualTo(ApplicationResultType.NOT_APPLICABLE);

            ctx.setUserPlus(true);

            result = calculator.calculateDiscountForEstimation(ctx);
            assertThat(result.getType()).isEqualTo(ApplicationResultType.SUCCESS);
            assertThat(result.getDiscountAmount()).isEqualTo(DEFAULT_NOMINAL_DISCOUNT);
        }
    }

    public static class MaxPromoDiscountTest extends TestBase {

        public static final Integer LIMIT = 10;
        public static final Money LIMIT_MONEY = Money.of(LIMIT, ProtoCurrencyUnit.RUB);

        @Override
        protected DiscountApplicationConfig createApplicationConfig() {
            return DiscountApplicationConfigTestData.empty()
                    .withMaxPromo(LIMIT).build();
        }

        @Override
        protected PromoCode createPromo(DiscountApplicationConfig cfg) {
            return createPercentPromo(cfg);
        }

        @Test
        public void calculateDiscountNumberIsMaxPromo() {
            PromoCodeDiscountCalculationCtx ctx = makeCalculationContext();

            CodeApplicationResult result = calculator.calculateDiscountForEstimation(ctx);
            assertThat(result.getType()).isEqualTo(ApplicationResultType.SUCCESS);
            assertThat(result.getDiscountAmount()).isEqualTo(LIMIT_MONEY);
        }

        @Test
        public void maxPromoHasNoEffectWhenNominalPromoCodeIsUsed() {
            promo = createNominalPromo(cfg);

            PromoCodeDiscountCalculationCtx ctx = makeCalculationContext();

            CodeApplicationResult result = calculator.calculateDiscountForEstimation(ctx);
            assertThat(result.getType()).isEqualTo(ApplicationResultType.SUCCESS);
            assertThat(result.getDiscountAmount()).isEqualTo(DEFAULT_NOMINAL_DISCOUNT);
        }

        @Test
        public void whenDiscountForApplicationIsTooBigItGetsOverriden() {
            PromoCodeDiscountApplicationCtx ctx = makeApplicationContext(DEFAULT_ORDER_MONEY);

            FiscalItemApplicationResult result = calculator.calculateDiscountForApplication(ctx);
            assertThat(result.getResultType()).isEqualTo(ApplicationResultType.SUCCESS);
            assertThat(result.getDiscountMap().values().stream().findFirst().get()).isEqualTo(LIMIT_MONEY);
        }
    }

    public static class HotelRestrictionsForEstimationTest extends TestBase {
        @Override
        protected DiscountApplicationConfig createApplicationConfig() {
            var dolphinHotelRestriction = new HotelRestriction();
            dolphinHotelRestriction.setPartner(EServiceType.PT_DOLPHIN_HOTEL);
            dolphinHotelRestriction.setOriginalId(DOLPHIN_ORIGINAL_ID);

            return DiscountApplicationConfigTestData.empty()
                    .withHotelRestrictions(List.of(dolphinHotelRestriction))
                    .build();
        }

        @Test
        public void validHotelRestrictions() {
            PromoCodeDiscountCalculationCtx ctx = makeCalculationContext();

            CodeApplicationResult result = calculator.calculateDiscountForEstimation(ctx);
            assertThat(result.getType()).isEqualTo(ApplicationResultType.SUCCESS);
        }

        @Test
        public void invalidHotelRestrictions() {
            serviceDescription.setPayload(new TravellineHotelItinerary() {{
                setOrderDetails(OrderDetails.builder()
                        .originalId(TRAVELLINE_ORIGINAL_ID)
                        .build());
            }});
            PromoCodeDiscountCalculationCtx ctx = makeCalculationContext();

            CodeApplicationResult result = calculator.calculateDiscountForEstimation(ctx);
            assertThat(result.getType()).isEqualTo(ApplicationResultType.NOT_APPLICABLE);
        }

        @Test
        public void noHotelRestrictions() {
            cfg.setHotelRestrictions(List.of());
            PromoCodeDiscountCalculationCtx ctx = makeCalculationContext();

            CodeApplicationResult result = calculator.calculateDiscountForEstimation(ctx);
            assertThat(result.getType()).isEqualTo(ApplicationResultType.SUCCESS);
        }
    }

    public static class HotelRestrictionsForApplicationTest extends TestBase {
        @Override
        protected DiscountApplicationConfig createApplicationConfig() {
            var dolphinHotelRestriction = new HotelRestriction();
            dolphinHotelRestriction.setPartner(EServiceType.PT_DOLPHIN_HOTEL);
            dolphinHotelRestriction.setOriginalId(DOLPHIN_ORIGINAL_ID);

            return DiscountApplicationConfigTestData.empty()
                    .withHotelRestrictions(List.of(dolphinHotelRestriction))
                    .build();
        }

        @Test
        public void validHotelRestrictions() {
            PromoCodeDiscountApplicationCtx ctx = makeApplicationContext(DEFAULT_ORDER_MONEY);

            FiscalItemApplicationResult result = calculator.calculateDiscountForApplication(ctx);
            assertThat(result.getResultType()).isEqualTo(ApplicationResultType.SUCCESS);
        }

        @Test
        public void invalidHotelRestrictions() {
            var tlHotelItinerary = new TravellineHotelItinerary();
            tlHotelItinerary.setOrderDetails(OrderDetails.builder()
                    .originalId(TRAVELLINE_ORIGINAL_ID)
                    .build());
            var tlOrderItem = new TravellineOrderItem();
            tlOrderItem.setItinerary(tlHotelItinerary);

            PromoCodeDiscountApplicationCtx ctx = makeApplicationContext(DEFAULT_ORDER_MONEY, tlOrderItem);

            FiscalItemApplicationResult result = calculator.calculateDiscountForApplication(ctx);
            assertThat(result.getResultType()).isEqualTo(ApplicationResultType.NOT_APPLICABLE);
        }

        @Test
        public void noHotelRestrictions() {
            cfg.setHotelRestrictions(List.of());
            PromoCodeDiscountApplicationCtx ctx = makeApplicationContext(DEFAULT_ORDER_MONEY);

            FiscalItemApplicationResult result = calculator.calculateDiscountForApplication(ctx);
            assertThat(result.getResultType()).isEqualTo(ApplicationResultType.SUCCESS);
        }
    }

    public static class MultipleFiscalItemsTest extends TestBase {
        @Override
        protected DiscountApplicationConfig createApplicationConfig() {
            return DiscountApplicationConfigTestData.empty()
                    .build();
        }

        @Override
        protected void addFiscalItems(Money amount, OrderItem orderItem) {
            FiscalItem mainItem = new FiscalItem();
            mainItem.setMoneyAmount(amount.subtract(ONE_RUB));
            orderItem.addFiscalItem(mainItem);
            FiscalItem mealItem = new FiscalItem();
            mealItem.setMoneyAmount(ONE_RUB);
            orderItem.addFiscalItem(mealItem);
        }

        @Test
        public void testApplicableOnManyFiscalItems() {
            PromoCodeDiscountApplicationCtx ctx = makeApplicationContext(LARGE_ORDER_MONEY);
            FiscalItemApplicationResult result = calculator.calculateDiscountForApplication(ctx);
            assertThat(result.getResultType()).isEqualTo(ApplicationResultType.SUCCESS);
        }
    }

    public static class RoundingTest extends TestBase {
        @Override
        protected DiscountApplicationConfig createApplicationConfig() {
            return DiscountApplicationConfigTestData.empty()
                    .build();
        }

        @Override
        protected PromoCode createPromo(DiscountApplicationConfig cfg) {
            return createPromoCode(BigDecimal.valueOf(10L), EPromoCodeNominalType.NT_PERCENT,
                    createPromoAction(cfg));
        }

        @Test
        public void testNoRoundingError() {
            PromoCodeDiscountApplicationCtx ctx = makeApplicationContext(Money.of(5542.99, ProtoCurrencyUnit.RUB));
            FiscalItemApplicationResult result = calculator.calculateDiscountForApplication(ctx);
            assertThat(result.getResultType()).isEqualTo(ApplicationResultType.SUCCESS);
            assertThat(result.getDiscountMap().values().iterator().next()).isEqualTo(Money.of(554.3, ProtoCurrencyUnit.RUB));
        }
    }
}
