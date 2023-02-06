package ru.yandex.market.billing.core.cpa_auction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.core.FunctionalTest;
import ru.yandex.market.billing.core.cpa_auction.model.BonusInfo;
import ru.yandex.market.billing.core.cpa_auction.model.BonusType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Тесты для {@link CpaAuctionBillingDao}
 */
public class CpaAuctionBillingDaoTest extends FunctionalTest {

    private static final LocalDate JULY_1_2021 = LocalDate.of(2021, Month.JULY, 1);
    private static final LocalDate AUGUST_6_2021 = LocalDate.of(2021, Month.AUGUST, 6);
    private static final BigDecimal RUB_100 = BigDecimal.valueOf(1000000, 2);
    private static final BigDecimal RUB_600 = BigDecimal.valueOf(6000000, 2);
    private static final BigDecimal RUB_200 = BigDecimal.valueOf(2000000, 2);
    private static final BigDecimal RUB_1000 = BigDecimal.valueOf(10000000, 2);

    @Autowired
    private CpaAuctionBillingDao cpaAuctionBillingDao;

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
            Map<Long, List<BonusInfo>> expectedRemainBonuses
    ) {
        Map<Long, List<BonusInfo>> remainBonus = cpaAuctionBillingDao.getPartnersBonuses(partners, billingDate);
        assertThat(remainBonus.size(), equalTo(expectedRemainBonuses.size()));
        for (var entry : expectedRemainBonuses.entrySet()) {
            List<BonusInfo> actual = remainBonus.get(entry.getKey());
            List<BonusInfo> expected = expectedRemainBonuses.get(entry.getKey());

            actual.sort(Comparator.comparing(BonusInfo::getBonusAccountId));
            expected.sort(Comparator.comparing(BonusInfo::getBonusAccountId));

            IntStream.range(0, actual.size()).forEach(i -> {
                assertThat(actual.get(i).getBonusAccountId(), equalTo(expected.get(i).getBonusAccountId()));
                assertThat(actual.get(i).getBonusSum().toBigInteger(),
                        equalTo(expected.get(i).getBonusSum().toBigInteger()));
            });
        }
    }

    private static List<BonusInfo> getRemainders(Map<Long, BigDecimal> remaindersMap) {
        List<BonusInfo> remainders = new ArrayList<>();
        for (var entry : remaindersMap.entrySet()) {
            remainders.add(new BonusInfo(1L, entry.getKey(), entry.getValue(), AUGUST_6_2021,
                    BonusType.NEWBIE));
        }
        return remainders;
    }

    private static Stream<Arguments> testGetRemainBonusTestData() {
        return Stream.of(
                // взяли бонус из remain_bonus за вчера
                Arguments.of(Set.of(1L), JULY_1_2021, Map.of(1L, getRemainders(Map.of(1L, RUB_100)))),
                // взяли бонус из remain_bonus за дату пораньше
                Arguments.of(Set.of(2L), JULY_1_2021, Map.of(2L, getRemainders(Map.of(1L, RUB_100)))),
                // взяли бонус из начально начисленных
                Arguments.of(Set.of(3L), JULY_1_2021, Map.of(3L, getRemainders(Map.of(1L, RUB_1000)))),
                Arguments.of(Set.of(4L), JULY_1_2021, Map.of()), // этому партнеру вообще никогда не начисляли бонусов
                Arguments.of(Set.of(5L), JULY_1_2021, Map.of()), // бонус есть, но протух
                Arguments.of(Set.of(6L), JULY_1_2021, Map.of()), // бонус есть, но протух и даже успел часть потратить
                Arguments.of(Set.of(7L), JULY_1_2021, Map.of()), // бонус есть, но выдали после биллинга
                // бонус есть, но выдали в день биллинга (поэтому не учитываем)
                Arguments.of(Set.of(8L), JULY_1_2021, Map.of()),
                // бонус есть, даже есть траты, поэтому берем бонус за максимальную дату меньше даты биллинга
                Arguments.of(Set.of(9L), JULY_1_2021, Map.of(9L, getRemainders(Map.of(1L, RUB_600)))),
                // бонус не выдавали, но в remain_bonus почему то есть запись (ситуация невозможна, но все таки)
                Arguments.of(Set.of(10L), JULY_1_2021, Map.of()),
                Arguments.of(Set.of(1L, 2L, 9L), JULY_1_2021, Map.of(
                        1L, getRemainders(Map.of(1L, RUB_100)),
                        2L, getRemainders(Map.of(1L, RUB_100)),
                        9L, getRemainders(Map.of(1L, RUB_600))
                )), // запрашиваем на партнера, которого вообще нет в shops_web.partner
                Arguments.of(Set.of(11L), JULY_1_2021, Map.of()),
                // У партнера два бонусных счета, с равным остатком на каждом
                Arguments.of(Set.of(12L), JULY_1_2021, Map.of(12L,
                        getRemainders(Map.of(1L, RUB_600, 2L, RUB_600))
                )),
                // У партнера два бонусных счета, на одном остаток, другой не трогали
                Arguments.of(Set.of(13L), JULY_1_2021, Map.of(13L,
                        getRemainders(Map.of(1L, RUB_600, 2L, RUB_1000))
                )),
                // У партнера два бонусных счета, оба счета не трогали
                Arguments.of(Set.of(14L), JULY_1_2021, Map.of(14L,
                        getRemainders(Map.of(1L, RUB_1000, 2L, RUB_1000))
                )),
                // У партнера два бонусных счета, один из счетов протух
                Arguments.of(Set.of(15L), JULY_1_2021, Map.of(15L,
                        getRemainders(Map.of(2L, RUB_600))
                ))

        );
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("testGetPartnerBonusesTestData")
    @DbUnitDataSet(before = "CpaAuctionDaoTest.testGetMultibonuses.before.csv")
    void testGetPartnerBonuses(
            Collection<Long> partners,
            LocalDate billingDate,
            Map<Long, List<BonusInfo>> expectedPartnersBonuses
    ) {
        Map<Long, List<BonusInfo>> partnersBonuses = cpaAuctionBillingDao.getPartnersBonuses(partners, billingDate);
        assertThat(partnersBonuses.size(), equalTo(expectedPartnersBonuses.size()));
        for (Map.Entry<Long, List<BonusInfo>> entry : expectedPartnersBonuses.entrySet()) {
            List<BonusInfo> actual = partnersBonuses.get(entry.getKey());
            List<BonusInfo> expected = entry.getValue();
            Assertions.assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        }
    }

    private static Stream<Arguments> testGetPartnerBonusesTestData() {
        return Stream.of(
                // взяли бонус из remain_bonus за вчера
                Arguments.of(Set.of(1L), JULY_1_2021, Map.of(1L, List.of(new BonusInfo(1L, RUB_100,
                        LocalDate.of(2021, 8, 6), BonusType.NEWBIE)))),
                // взяли бонус из remain_bonus за дату пораньше -  2 активных бонуса (на тип пока не смотрим)
                Arguments.of(Set.of(2L), JULY_1_2021, Map.of(2L, List.of(new BonusInfo(2L, RUB_100,
                                LocalDate.of(2021, 8, 6), BonusType.NEWBIE),
                        new BonusInfo(2L, RUB_200, LocalDate.of(2021, 12, 31),
                                BonusType.NEWBIE)))),
                Arguments.of(Set.of(3L), JULY_1_2021, Map.of(3L, List.of(new BonusInfo(3L, RUB_1000,
                                LocalDate.of(2021, 8, 6), BonusType.NEWBIE),
                        new BonusInfo(3L, RUB_200, LocalDate.of(2021, 8, 6),
                                BonusType.NEWBIE)
                ))), // взяли бонусы из начально начисленных
                Arguments.of(Set.of(4L), JULY_1_2021, Map.of()), // этому партнеру вообще никогда не
                // начисляли бонусов
                Arguments.of(Set.of(5L), JULY_1_2021, Map.of(5L, List.of(new BonusInfo(5L, RUB_1000,
                        LocalDate.of(2021, 7, 10), BonusType.NEWBIE), new BonusInfo(5L,
                        BigDecimal.ZERO.movePointLeft(2), LocalDate.of(2021, 9, 1),
                        BonusType.NEWBIE)
                ))), // из 3х бонусов 1 протух,1 потрачен (не отдадим на фронт)
                Arguments.of(Set.of(6L), JULY_1_2021, Map.of()), // бонус есть, но протух и даже успел часть
                // потратить
                Arguments.of(Set.of(7L), JULY_1_2021, Map.of()), // бонус есть, но выдали после биллинга
                // бонус есть, но выдали в день биллинга (поэтому не учитываем)
                Arguments.of(Set.of(8L), JULY_1_2021, Map.of()),
                // бонус есть, даже есть траты, поэтому берем бонус за максимальную дату меньше даты биллинга
                Arguments.of(Set.of(9L), JULY_1_2021, Map.of(9L, List.of(new BonusInfo(9L, RUB_600,
                        LocalDate.of(2021, 8, 6), BonusType.NEWBIE)))),
                // бонус не выдавали, но в remain_bonus почему то есть запись (ситуация невозможна, но все таки)
                Arguments.of(Set.of(10L), JULY_1_2021, Map.of()),
                Arguments.of(Set.of(1L, 2L, 9L), JULY_1_2021, Map.of(
                        1L, List.of(new BonusInfo(1L, RUB_100,
                                LocalDate.of(2021, 8, 6), BonusType.NEWBIE)),
                        2L, List.of(new BonusInfo(2L, RUB_100,
                                        LocalDate.of(2021, 8, 6), BonusType.NEWBIE),
                                new BonusInfo(2L, RUB_200, LocalDate.of(2021, 12, 31),
                                        BonusType.NEWBIE)),
                        9L, List.of(new BonusInfo(9L, RUB_600,
                                LocalDate.of(2021, 8, 6),
                                BonusType.NEWBIE))
                )), // запрашиваем на партнера, которого вообще нет в shops_web.partner
                Arguments.of(Set.of(11L), JULY_1_2021, Map.of())
        );
    }

    @Test
    @DbUnitDataSet(before = "CpaAuctionBillingDaoTest.testGetSpentAmount.before.csv")
    void testGetSpentAmount() {
        assertThat(
                cpaAuctionBillingDao.getSpentAmount(1L, LocalDate.of(2021, 7, 1),
                        LocalDate.of(2021, 7, 20)),
                equalTo(400L)
        );
        assertThat(
                cpaAuctionBillingDao.getSpentAmount(1L, LocalDate.of(2021, 7, 11),
                        LocalDate.of(2021, 7, 12)),
                equalTo(100L)
        );
        assertThat(
                cpaAuctionBillingDao.getSpentAmount(1L, LocalDate.of(2021, 8, 1),
                        LocalDate.of(2021, 8, 12)),
                equalTo(0L)
        );
        assertThat(
                cpaAuctionBillingDao.getSpentAmount(2L, LocalDate.of(2021, 7, 1),
                        LocalDate.of(2021, 7, 20)),
                equalTo(0L)
        );
    }

    @Test
    @DbUnitDataSet(before = "CpaAuctionBillingDaoTest.testGetSpentAmount.before.csv")
    void testGetPartnersSpentAmount() {
        Map<Long, Long> partnersSpentAmount = cpaAuctionBillingDao.getPartnersSpentAmount(List.of(1L, 2L, 3L),
                LocalDate.of(2021, 7, 1),
                LocalDate.of(2021, 7, 20));

        assertThat(partnersSpentAmount.get(1L), equalTo(400L));
        assertThat(partnersSpentAmount.get(2L), equalTo(0L));
        assertThat(partnersSpentAmount.get(3L), equalTo(450L));
    }
}
