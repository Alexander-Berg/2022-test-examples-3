package ru.yandex.market.billing.cpa_auction;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.cpa_auction.CpaAuctionBilledItem;
import ru.yandex.market.billing.core.cpa_auction.CpaAuctionItemForBilling;
import ru.yandex.market.billing.core.cpa_auction.model.BonusInfo;
import ru.yandex.market.billing.core.cpa_auction.model.BonusType;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link CpaAuctionBillingService}
 */
@ParametersAreNonnullByDefault
public class CpaAuctionBillingServiceTest extends FunctionalTest {

    private static final LocalDate AUGUST_6_2021 = LocalDate.of(2021, Month.AUGUST, 6);
    private static final LocalDate AUGUST_1_2021 = LocalDate.of(2021, Month.AUGUST, 1);
    private static final LocalDate MAY_1_2021 = LocalDate.of(2021, Month.MAY, 1);

    private static final BigDecimal RUB_0 = new BigDecimal("0");
    private static final BigDecimal RUB_0_5 = new BigDecimal("50");
    private static final BigDecimal RUB_1 = new BigDecimal("100");
    private static final BigDecimal RUB_1_5 = new BigDecimal("150");
    private static final BigDecimal RUB_10 = new BigDecimal("1000");
    private static final BigDecimal RUB_21 = new BigDecimal("2100");
    private static final BigDecimal RUB_50 = new BigDecimal("5000");
    private static final BigDecimal RUB_70 = new BigDecimal("7000");
    private static final BigDecimal RUB_78_50 = new BigDecimal("7850");
    private static final BigDecimal RUB_79 = new BigDecimal("7900");
    private static final BigDecimal RUB_80 = new BigDecimal("8000");
    private static final BigDecimal RUB_99_50 = new BigDecimal("9950");
    private static final BigDecimal RUB_100 = new BigDecimal("10000");

    @Autowired
    private CpaAuctionBillingService cpaAuctionBillingService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    TestableClock clock;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-07-01T00:00:00.00Z"), ZoneOffset.UTC);
    }

    @ParameterizedTest(name = "[{index}]: {6}")
    @MethodSource("testCalculateAmountTestData")
    void testCalculateAmount(
            BigDecimal feeAmount,
            Map<Long, List<BonusInfo>> availableBonusMap,
            long partnerId,
            BigDecimal expectedTotalAmount,
            BigDecimal expectedBonusCompensation,
            @Nullable BigDecimal expectedRemainBonus,
            String description
    ) {
        CpaAuctionBillingService.BigDecimalPair bigDecimalPair = cpaAuctionBillingService.calculateAmount(
                feeAmount,
                availableBonusMap,
                partnerId
        );

        assertThat("total amount", bigDecimalPair.getTotalAmount(), equalTo(expectedTotalAmount));
        assertThat("bonus compensation", bigDecimalPair.getBonusCompensation(),
                equalTo(expectedBonusCompensation));
        if (availableBonusMap.get(partnerId) != null) {
            var remainder = BigDecimal.ZERO;
            for (var r : availableBonusMap.get(partnerId)) {
                remainder = remainder.add(r.getBonusSum());
            }
            assertThat("remain bonus", remainder, equalTo(expectedRemainBonus));
        }

    }

    @Test
    void testCalculateAmountMultiBonus() {
        List<BonusInfo> remainders = new ArrayList<>();
        remainders.add(new BonusInfo(1L, 1L, RUB_50, AUGUST_6_2021, BonusType.NEWBIE));
        remainders.add(new BonusInfo(1L, 2L, RUB_50, AUGUST_1_2021, BonusType.NEWBIE));
        remainders.add(new BonusInfo(1L, 3L, RUB_50, MAY_1_2021, BonusType.NEWBIE));

        Map<Long, List<BonusInfo>> partnerBonus = new HashMap<>();
        partnerBonus.put(1L, remainders);

        cpaAuctionBillingService.calculateAmount(
                RUB_80,
                partnerBonus,
                1L
        );

        assertThat("bigger expiry date", partnerBonus.get(1L).get(0).getBonusSum(), equalTo(RUB_0));
        assertThat("less expiry date", partnerBonus.get(1L).get(1).getBonusSum(), equalTo(RUB_21));
        assertThat("should be untouched", partnerBonus.get(1L).get(2).getBonusSum(), equalTo(RUB_50));
    }

    private static List<BonusInfo> getRemainders(Map<Long, BigDecimal> remaindersMap) {
        List<BonusInfo> remainders = new ArrayList<>();
        for (var entry : remaindersMap.entrySet()) {
            remainders.add(new BonusInfo(1L, entry.getKey(), entry.getValue(), AUGUST_6_2021,
                    BonusType.NEWBIE));
        }
        return remainders;
    }

    private static Stream<Arguments> testCalculateAmountTestData() {
        return Stream.of(
                Arguments.of(//1
                        RUB_0_5,
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, RUB_100)))),
                        1L,
                        RUB_0_5,
                        RUB_0,
                        RUB_100,
                        "fee меньше рубля, ничего не будет скомпенсировано"
                ),
                Arguments.of(//2
                        RUB_1_5,
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, RUB_100)))),
                        1L,
                        RUB_1,
                        RUB_0_5,
                        RUB_99_50,
                        "fee 1.5р, будет скомпенсировано 50 коп, 1р уедет в тлог"
                ),
                Arguments.of(//3
                        RUB_80,
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, RUB_100)))),
                        1L,
                        RUB_1,
                        RUB_79,
                        RUB_21,
                        "fee 80р, бонусов 100р, будет скомпенсировано 79 руб, , 1р уедет в тлог"
                ),
                Arguments.of(//4
                        RUB_80,
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, RUB_80)))),
                        1L,
                        RUB_1,
                        RUB_79,
                        RUB_1,
                        "fee 80р, бонусов 80р, будет скомпенсировано 79р, 1р уедет в тлог"
                ),
                Arguments.of(//5
                        RUB_80,
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, RUB_70)))),
                        1L,
                        RUB_10,
                        RUB_70,
                        RUB_0,
                        "fee 80р, бонусов 70р , в тлог уедет 10р, бонус обнулится"
                ),
                Arguments.of(//6
                        RUB_100,
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, RUB_0_5)))),
                        1L,
                        RUB_99_50,
                        RUB_0_5,
                        RUB_0,
                        "fee 100р, бонусов 50коп, в тлог уедет 99.5р, бонус обнулится"
                ),
                Arguments.of(//7
                        RUB_80,
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, RUB_0)))),
                        1L,
                        RUB_80,
                        RUB_0,
                        RUB_0,
                        "fee 80р, бонусов 0. в тлог уедет вся сумма"
                ),
                Arguments.of(//8
                        RUB_80,
                        new HashMap<>(),
                        1L,
                        RUB_80,
                        RUB_0,
                        null,
                        "fee 80р, бонусов вообще нет. в тлог уедет вся сумма"
                ),
                Arguments.of(//9
                        RUB_80,
                        new HashMap<>(Map.of(2L, getRemainders(Map.of(1L, RUB_100)))),
                        1L,
                        RUB_80,
                        RUB_0,
                        null,
                        "бонус есть, но на другого мерча"
                ),
                Arguments.of(//10
                        RUB_80,
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, RUB_79)))),
                        1L,
                        RUB_1,
                        RUB_79,
                        RUB_0,
                        "fee 80, бонусов 79р, в тлог уедет 1р, бонус обнулится"
                ),
                Arguments.of(//11
                        RUB_80,
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, RUB_78_50)))),
                        1L,
                        RUB_1_5,
                        RUB_78_50,
                        RUB_0,
                        "fee 80, бонусов 78.5р, в тлог уедет 1.5р, бонус обнулится"
                ),
                Arguments.of(//12
                        RUB_80,
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, RUB_50, 2L, RUB_50)))),
                        1L,
                        RUB_1,
                        RUB_79,
                        RUB_21,
                        "fee 80р, бонусов 100р, 2 счета, будет скомпенсировано 79 руб, , 1р уедет в тлог"
                )
        );
    }

    @Test
    @DisplayName("Тест на одного мерча, в пределах которого списываем деньги за разные товарные позиции")
    void testCalculateAmountManyTimes() {
        Map<Long, List<BonusInfo>> bonusMap = new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, RUB_100))));
        // списали 9 рублей бонусами, 1р уехал в тлог
        cpaAuctionBillingService.calculateAmount(
                RUB_10, bonusMap, 1L
        );
        // списали еще 69 рублей, 1р уехал в тлог
        cpaAuctionBillingService.calculateAmount(
                RUB_70, bonusMap, 1L
        );
        //итого бонусов осталось: 100 - 9 - 69 = 22р, 1 бонусный счет
        assertThat(bonusMap.get(1L).get(0).getBonusSum(), equalTo(new BigDecimal("2200")));
    }

    @Test
    @DbUnitDataSet(
            before = "CpaAuctionBillingServiceTest.testServiceFull.before.csv",
            after = "CpaAuctionBillingServiceTest.testServiceFull.after.csv"
    )
    void testServiceFull() {
        cpaAuctionBillingService.process(LocalDate.of(2021, Month.JULY, 1));
    }

    @Test
    @DbUnitDataSet(
            before = "CpaAuctionBillingServiceTest.testServiceSpecificPartners.before.csv",
            after = "CpaAuctionBillingServiceTest.testServiceSpecificPartners.after.csv"
    )
    void testServiceSpecificPartners() {
        cpaAuctionBillingService.processForPartners(LocalDate.of(2021, Month.JULY, 1),
                List.of(2L, 5L));
    }

    @Test
    @DbUnitDataSet(
            before = "CpaAuctionBillingServiceTest.testServiceSpecificPartners.before.csv",
            after = "CpaAuctionBillingServiceTest.testSpentAmountPrecalc.after.csv"
    )
    void testPrecalcSpentAmount() {
        clock.setFixed(Instant.parse("2021-07-02T00:00:00.00Z"), ZoneOffset.UTC);
        cpaAuctionBillingService.process(LocalDate.of(2021, Month.JULY, 1));
        cpaAuctionBillingService.process(LocalDate.of(2021, Month.JULY, 2));
    }

    @Test
    @DbUnitDataSet(
            before = "CpaAuctionBillingServiceTest.testServiceSpecificPartners.before.csv"
    )
    void testServiceGetInvalidPartners() {
        clock.setFixed(Instant.parse("2021-07-01T00:00:00.00Z"), ZoneOffset.UTC);
        Assertions.assertEquals(
                Set.of(8L),
                cpaAuctionBillingService.getInvalidPartners(
                        LocalDate.of(2021, Month.JULY, 1), List.of(2L, 5L, 8L)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testServiceSkipDateTestData")
    void testServiceSkipDate(
            LocalDate billingDate,
            String envValue,
            boolean expectedSkip
    ) {
        environmentService.setValue("mbi.billing.cpa.auction.billing.start.date", envValue);
        CpaAuctionBillingService mockService = new CpaAuctionBillingService(
                null, null, null,
                null, environmentService,
                Clock.fixed(LocalDate.of(2021, Month.JULY, 2)
                        .atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));


        boolean shouldSkip = mockService.shouldSkipDay(billingDate);
        assertThat(shouldSkip, equalTo(expectedSkip));
    }

    @ParameterizedTest(name = "[{index}]")
    @DisplayName("Проверяем, что обиленные значения целые.")
    @MethodSource("testAmountBonusAnIntegerTestData")
    void testAmountBonusAnInteger(CpaAuctionItemForBilling itemToBill,
                                  Map<Long, List<BonusInfo>> partnerBonus) {
        CpaAuctionBilledItem result = cpaAuctionBillingService.billItem(itemToBill, partnerBonus);
        assertTrue(isIntegerValue(result.getAmount()), "Amount is not integer value");
        assertTrue(isIntegerValue(result.getAmountBeforeBonus()), "AmountBeforeBonus is not integer value");
        assertTrue(isIntegerValue(result.getSpentBonus()), "SpentBonus is not integer value");
    }

    private boolean isIntegerValue(BigDecimal bigDecimal) {
        return bigDecimal.stripTrailingZeros().scale() <= 0;
    }

    private static CpaAuctionItemForBilling.Builder getAbstractBuilder() {
        return CpaAuctionItemForBilling.builder()
                .setPartnerId(1L)
                .setItemId(1L)
                .setItemsCount(1)
                .setTrantime(LocalDateTime.MAX);
    }

    private static Stream<Arguments> testAmountBonusAnIntegerTestData() {
        return Stream.of(
                Arguments.of(getAbstractBuilder()
                                .setIntFee(1)
                                .setItemPrice(BigDecimal.valueOf(1)).build(),
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, BigDecimal.ZERO))))),
                Arguments.of(getAbstractBuilder()
                                .setIntFee(1)
                                .setItemPrice(BigDecimal.valueOf(10232323)).build(),
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, BigDecimal.ZERO))))),
                Arguments.of(getAbstractBuilder()
                                .setIntFee(23)
                                .setItemPrice(BigDecimal.valueOf(10232323)).build(),
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, BigDecimal.valueOf(100))))),
                Arguments.of(getAbstractBuilder()
                                .setIntFee(23)
                                .setItemPrice(BigDecimal.valueOf(10232323.113)).build(),
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, BigDecimal.valueOf(100))))),
                Arguments.of(getAbstractBuilder()
                                .setIntFee(23)
                                .setItemPrice(BigDecimal.valueOf(10232323.113)).build(),
                        new HashMap<>(Map.of(1L, getRemainders(Map.of(1L, BigDecimal.valueOf(1000000000000L)))))
        ))));
    }

    private static Stream<Arguments> testServiceSkipDateTestData() {
        return Stream.of(
                Arguments.of(LocalDate.of(2021, Month.JUNE, 27), "2021-06-28", true),
                Arguments.of(LocalDate.of(2021, Month.JUNE, 27), "", true),
                Arguments.of(LocalDate.of(2021, Month.JUNE, 27), null, true),
                Arguments.of(LocalDate.of(2021, Month.JUNE, 27), "bla-bla-bla", true),
                Arguments.of(LocalDate.of(2021, Month.JUNE, 27), "2021-06-27", false),
                Arguments.of(LocalDate.of(2021, Month.JUNE, 27), "2021-06-26", false)
        );
    }
}
