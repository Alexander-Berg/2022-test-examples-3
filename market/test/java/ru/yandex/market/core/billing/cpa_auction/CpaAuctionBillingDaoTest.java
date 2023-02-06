package ru.yandex.market.core.billing.cpa_auction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.billing.cpa_auction.model.CpaAuctionBilledItem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Тесты для {@link CpaAuctionBillingDao}
 */
public class CpaAuctionBillingDaoTest extends FunctionalTest {

    private static final LocalDate JULY_1_2021 = LocalDate.of(2021, Month.JULY, 1);
    private static final BigDecimal RUB_100 = new BigDecimal("10000");
    private static final BigDecimal RUB_600 = new BigDecimal("60000");
    private static final BigDecimal RUB_1000 = new BigDecimal("100000");

    @Autowired
    private CpaAuctionBillingDao cpaAuctionBillingDao;

    @Autowired
    private CpaAuctionBillingDao pgCpaAuctionBillingDao;

    @Test
    @DisplayName("Тест на получение партнеров для обиливания")
    @DbUnitDataSet(before = "CpaAuctionBillingDaoTest.testGetNettingPartners.before.csv")
    void testGetNettingPartners() {
        List<Long> nettingPartners = cpaAuctionBillingDao.getNettingPartners(JULY_1_2021);
        assertThat(nettingPartners, hasSize(1));
        assertThat(nettingPartners.get(0), equalTo(2L));
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("testGetRemainBonusTestData")
    @DbUnitDataSet(before = "CpaAuctionBillingDaoTest.testGetRemainBonus.before.csv")
    void testGetRemainBonus(
            Collection<Long> partners,
            LocalDate billingDate,
            Map<Long, BigDecimal> expectedRemainBonuses
    ) {
        Map<Long, BigDecimal> remainBonus = cpaAuctionBillingDao.getRemainBonus(partners, billingDate);
        assertThat(remainBonus.size(), equalTo(expectedRemainBonuses.size()));
        for (Map.Entry<Long, BigDecimal> entry : expectedRemainBonuses.entrySet()) {
            assertThat(remainBonus.get(entry.getKey()), comparesEqualTo(entry.getValue()));
        }
    }

    private static Stream<Arguments> testGetRemainBonusTestData() {
        return Stream.of(
                Arguments.of(Set.of(1L), JULY_1_2021, Map.of(1L, RUB_100)), // взяли бонус из remain_bonus за вчера
                Arguments.of(Set.of(2L), JULY_1_2021, Map.of(2L, RUB_100)), // взяли бонус из remain_bonus за дату
                // пораньше
                Arguments.of(Set.of(3L), JULY_1_2021, Map.of(3L, RUB_1000)), // взяли бонус из начально начисленных
                Arguments.of(Set.of(4L), JULY_1_2021, Map.of()), // этому партнеру вообще никогда не начисляли бонусов
                Arguments.of(Set.of(5L), JULY_1_2021, Map.of()), // бонус есть, но протух
                Arguments.of(Set.of(6L), JULY_1_2021, Map.of()), // бонус есть, но протух и даже успел часть потратить
                Arguments.of(Set.of(7L), JULY_1_2021, Map.of()), // бонус есть, но выдали после биллинга
                Arguments.of(Set.of(8L), JULY_1_2021, Map.of()), // бонус есть, но выдали в день биллинга (поэтому не
                // учитываем)
                Arguments.of(Set.of(9L), JULY_1_2021, Map.of(9L, RUB_600)), // бонус есть, даже есть траты, поэтому
                // берем бонус за максимальную дату меньше даты биллинга
                Arguments.of(Set.of(10L), JULY_1_2021, Map.of()), // бонус не выдавали, но в remain_bonus почему то
                // есть запись (ситуация невозможна, но все таки)
                Arguments.of(Set.of(1L, 2L, 9L), JULY_1_2021, Map.of(
                        1L, RUB_100,
                        2L, RUB_100,
                        9L, RUB_600
                )),
                Arguments.of(Set.of(11L), JULY_1_2021, Map.of()) // запрашиваем на партнера, которого вообще нет в
                // shops_web.partner
        );
    }

    @Test
    @DbUnitDataSet(before = "CpaAuctionBillingDaoTest.testGetSpentAmount.before.csv")
    void testGetSpentAmount() {
        assertThat(
                cpaAuctionBillingDao.getSpentAmount(1L, LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 20)),
                equalTo(400L)
        );
        assertThat(
                cpaAuctionBillingDao.getSpentAmount(1L, LocalDate.of(2021, 7, 11), LocalDate.of(2021, 7, 12)),
                equalTo(100L)
        );
        assertThat(
                cpaAuctionBillingDao.getSpentAmount(1L, LocalDate.of(2021, 8, 1), LocalDate.of(2021, 8, 12)),
                equalTo(0L)
        );
        assertThat(
                cpaAuctionBillingDao.getSpentAmount(2L, LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 20)),
                equalTo(0L)
        );
    }

    @Test
    @DbUnitDataSet(before = "CpaAuctionBillingDaoTest.testReplication.before.csv")
    void testGetDailyModifiedDatesForItems() {
        List<LocalDate> dailyModifiedDatesForItems =
                pgCpaAuctionBillingDao.getDailyModifiedDatesForItems(LocalDateTime.of(2022, 5, 20, 23, 0));
        assertThat(dailyModifiedDatesForItems.size(), equalTo(1));
        assertThat(dailyModifiedDatesForItems.get(0), equalTo(LocalDate.of(2022, 5, 18)));
    }


    @Test
    @DbUnitDataSet(before = "CpaAuctionBillingDaoTest.testReplication.before.csv")
    void testGetBilledAmountByDate() {
        List<CpaAuctionBilledItem> cpaAuctionBilledItems =
                pgCpaAuctionBillingDao.getCpaAuctionBilledItems(LocalDate.of(2022, 5, 17), LocalDate.of(2022, 5, 18));
        assertThat(cpaAuctionBilledItems.size(), equalTo(1));
        assertThat(cpaAuctionBilledItems.get(0).getItemId(), equalTo(111L));
    }

}
