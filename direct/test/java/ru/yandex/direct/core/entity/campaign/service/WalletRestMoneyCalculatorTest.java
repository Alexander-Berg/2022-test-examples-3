package ru.yandex.direct.core.entity.campaign.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import one.util.streamex.EntryStream;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.direct.core.entity.campaign.container.WalletsWithCampaigns;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.WalletCampaign;
import ru.yandex.direct.core.entity.campaign.model.WalletRestMoney;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class WalletRestMoneyCalculatorTest {

    @Parameter
    public WalletsWithCampaigns walletsWithCampaigns;

    @Parameter(1)
    public Collection<Campaign> campaigns;

    @Parameter(2)
    public Map<Long, BigDecimal> expectedWalletRestMoneyByCampaignIds;

    @Parameter(3)
    public Map<Long, BigDecimal> expectedWalletRestMoneyByWalletCampaignIds;

    @Test
    public void checkResults() {
        WalletRestMoneyCalculator calculator = new WalletRestMoneyCalculator(walletsWithCampaigns);

        Map<Long, BigDecimal> actualWalletRestMoneyByCampaignIds =
                moneyMapToExpectMap(calculator.getWalletRestMoneyByCampaignIds(campaigns));
        assertThat(actualWalletRestMoneyByCampaignIds).hasSameSizeAs(expectedWalletRestMoneyByCampaignIds);

        Map<Long, BigDecimal> actualWalletRestMoneyByWalletCampaignIds =
                moneyMapToExpectMap(calculator.getWalletRestMoneyByWalletCampaignIds());
        assertThat(actualWalletRestMoneyByWalletCampaignIds).hasSameSizeAs(expectedWalletRestMoneyByWalletCampaignIds);

        SoftAssertions softly = new SoftAssertions();
        for (Map.Entry<Long, BigDecimal> expectedEntry : expectedWalletRestMoneyByCampaignIds.entrySet()) {
            BigDecimal actual = actualWalletRestMoneyByCampaignIds.get(expectedEntry.getKey());
            BigDecimal expected = expectedEntry.getValue();
            softly.assertThat(actual.longValue()).isEqualTo(expected.longValue());
        }
        for (Map.Entry<Long, BigDecimal> expectedEntry : expectedWalletRestMoneyByWalletCampaignIds.entrySet()) {
            BigDecimal actual = actualWalletRestMoneyByWalletCampaignIds.get(expectedEntry.getKey());
            BigDecimal expected = expectedEntry.getValue();
            softly.assertThat(actual.longValue()).isEqualTo(expected.longValue());
        }

        softly.assertAll();
    }

    @Parameters
    public static Iterable<Object[]> params() {
        return asList(

                // проверка: положительные остатки на кампаниях не учитываются

                new TestCase()
                        .wallet(1, 500, 200)
                        .campaign(2, 1, 70, 30)
                        .campaign(3, 1, 1000, 901)
                        .campaign(4, 400, 20)   // <- кампания не под общим счётом
                        .requestCampaignIds(1, 3, 4)
                        .expectWalletRestMoneyByCampaignIds(ImmutableMap.of(
                                1, 300,
                                3, 300,
                                4, 0))
                        .expectWalletRestMoneyByWalletCampaignIds(ImmutableMap.of(1, 300))
                        .build(),
                new TestCase()
                        .wallet(1, 500, 200)
                        .campaign(2, 1, 70, 30)
                        .campaign(3, 1, 1000, 901)
                        .requestCampaignIds(2)
                        .expectWalletRestMoneyByCampaignIds(ImmutableMap.of(2, 300))
                        .expectWalletRestMoneyByWalletCampaignIds(ImmutableMap.of(1, 300))
                        .build(),

                // проверка учёта компенсаций отрицательных остатков

                new TestCase()
                        .wallet(1, 500, 100)        // остаток = 400
                        .campaign(2, 1, 1050, 1000) // остаток = 50
                        .campaign(3, 1, 2150, 2000) // остаток = 150
                        .campaign(4, 1, 3000, 3100) // остаток = -100, недокрут – это норм для неотключаемого ОС
                        .campaign(5, 1, 4000, 4000) // остаток = 0
                        .requestCampaignIds(1, 2, 3, 4, 5)
                        .expectWalletRestMoneyByCampaignIds(ImmutableMap.of(
                                1, 300,
                                2, 300,
                                3, 300,
                                4, 300,
                                5, 300))
                        .expectWalletRestMoneyByWalletCampaignIds(ImmutableMap.of(1, 300))
                        .build(),
                new TestCase()  // то же самое, что выше, только запросим результат по одной кампании
                        .wallet(1, 500, 100)
                        .campaign(2, 1, 1050, 1000) // остаток = 50
                        .campaign(3, 1, 2150, 2000) // остаток = 150
                        .campaign(4, 1, 3000, 3100) // остаток = -100, недокрут – это норм для неотключаемого ОС
                        .campaign(5, 1, 4000, 4000) // остаток = 0
                        .requestCampaignIds(3)
                        .expectWalletRestMoneyByCampaignIds(ImmutableMap.of(3, 300))
                        .expectWalletRestMoneyByWalletCampaignIds(ImmutableMap.of(1, 300))
                        .build(),

                // corner case: empty data, empty request
                new TestCase()
                        .expectWalletRestMoneyByCampaignIds(emptyMap())
                        .expectWalletRestMoneyByWalletCampaignIds(emptyMap())
                        .build(),

                // empty data: non-empty request
                new TestCase()
                        .campaign(2, 5, 1000, 500)
                        .requestCampaignIds(2)
                        .expectWalletRestMoneyByCampaignIds(ImmutableMap.of(2, 0))
                        .expectWalletRestMoneyByWalletCampaignIds(emptyMap())
                        .build(),

                // multiple wallets
                new TestCase()
                        .wallet(1, 500, 200)
                        .wallet(2, 500, 100)
                        .campaign(3, 1, 300, 100)
                        .campaign(4, 1, 400, 100)
                        .campaign(5, 2, 500, 100)
                        .campaign(6, 2, 600, 100)
                        .requestCampaignIds(3, 4, 5, 6)
                        .expectWalletRestMoneyByCampaignIds(ImmutableMap.of(
                                3, 300,
                                4, 300,
                                5, 400,
                                6, 400))
                        .expectWalletRestMoneyByWalletCampaignIds(ImmutableMap.of(
                                1, 300,
                                2, 400))
                        .build()
        );
    }

    private static class TestCase {
        /**
         * По этой мапе будет создан инстанс {@link WalletsWithCampaigns} – аргумент конструктора калькулятора.
         */
        private final Multimap<WalletCampaign, WalletCampaign> multimap = HashMultimap.create();

        /**
         * Кампании, которые поступят в качестве аргумента метода
         * {@link WalletRestMoneyCalculator#getWalletRestMoneyByCampaignIds}.
         */
        private final Collection<WalletCampaign> argCampaigns = new ArrayList<>();

        /**
         * Все кампании и кошельки, проиндексированные по id.
         */
        private final Map<Integer, WalletCampaign> index = new HashMap<>();

        /**
         * Ожидаемый результат метода {@link WalletRestMoneyCalculator#getWalletRestMoneyByCampaignIds}
         */
        private Map<Long, BigDecimal> expectedWalletRestMoneyByCampaignIds;

        /**
         * Ожидаемый результат метода {@link WalletRestMoneyCalculator#getWalletRestMoneyByWalletCampaignIds}
         */
        private Map<Long, BigDecimal> expectedWalletRestMoneyByWalletCampaignIds;

        TestCase wallet(int id, Integer sum, Integer spent) {
            WalletCampaign wallet = new Campaign()
                    .withId((long) id)
                    .withSum(new BigDecimal(sum))
                    .withSumSpent(new BigDecimal(spent));
            index.put(id, wallet);
            return this;
        }

        TestCase campaign(int id, int walletId, Integer sum, Integer spent) {
            Campaign campaign = new Campaign()
                    .withId((long) id)
                    .withWalletId((long) walletId)
                    .withSum(new BigDecimal(sum))
                    .withSumSpent(new BigDecimal(spent));
            if (index.containsKey(walletId)) {
                multimap.put(index.get(walletId), campaign);
            }
            index.put(id, campaign);
            return this;
        }

        TestCase campaign(int id, Integer sum, Integer spent) {
            Campaign campaign = new Campaign()
                    .withId((long) id)
                    .withSum(new BigDecimal(sum))
                    .withSumSpent(new BigDecimal(spent));
            index.put(id, campaign);
            return this;
        }

        TestCase requestCampaignIds(Integer... ids) {
            stream(ids).map(index::get).filter(Objects::nonNull).forEach(argCampaigns::add);
            return this;
        }

        TestCase expectWalletRestMoneyByCampaignIds(Map<Integer, Integer> idsToValues) {
            expectedWalletRestMoneyByCampaignIds = integerMapToExpectMap(idsToValues);
            return this;
        }

        TestCase expectWalletRestMoneyByWalletCampaignIds(Map<Integer, Integer> idsToValues) {
            expectedWalletRestMoneyByWalletCampaignIds = integerMapToExpectMap(idsToValues);
            return this;
        }

        Object[] build() {
            return new Object[]{
                    new WalletsWithCampaigns(multimap.keySet(), multimap.values()),
                    argCampaigns,
                    expectedWalletRestMoneyByCampaignIds,
                    expectedWalletRestMoneyByWalletCampaignIds
            };
        }
    }

    private static Map<Long, BigDecimal> moneyMapToExpectMap(Map<Long, WalletRestMoney> moneyMap) {
        return EntryStream.of(moneyMap)
                .mapValues(wrm -> wrm.getRest().bigDecimalValue())
                .toMap();
    }

    private static Map<Long, BigDecimal> integerMapToExpectMap(Map<Integer, Integer> integerMap) {
        return EntryStream.of(integerMap)
                .mapKeys(Long::valueOf)
                .mapValues(BigDecimal::new)
                .toMap();
    }

}
