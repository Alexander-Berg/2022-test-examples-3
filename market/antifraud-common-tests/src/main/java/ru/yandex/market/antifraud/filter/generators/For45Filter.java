package ru.yandex.market.antifraud.filter.generators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Value;
import org.joda.time.DateTime;

import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import static ru.yandex.market.antifraud.filter.fields.FilterConstants.FILTER_0;
import static ru.yandex.market.antifraud.filter.fields.FilterConstants.FILTER_45;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 07.11.2019
 */
public class For45Filter implements FilterGenerator {
    private static final DateTime TEST_DT = new DateTime().withMillisOfSecond(0);

    private final List<String> sarYandexUids;

    public For45Filter(List<String> sarYandexUids) {
        this.sarYandexUids = sarYandexUids;
    }

    @Override
    public List<TestClick> generate() {
        long baseShopId = 12345L;
        String normalCookie = "normalClicks";
        // обычные клики
        For45Filter.ClickParams normalClicks =
                ClickParams.builder().amount(10).caseN(1).filter(FILTER_0).shopId(baseShopId).cookie(normalCookie + "1").build();

        // САР клики составляют половину от всех кликов магазина, но всего ровно 10 кликов -> НЕ фильтруем ничего
        For45Filter.ClickParams sarNonFilteredClicksPart1 =
                ClickParams.builder().amount(5).caseN(2).filter(FILTER_0).shopId(baseShopId + 1).cookie(normalCookie + "2").build();

        For45Filter.ClickParams sarNonFilteredClicksPart2 =
                ClickParams.builder().amount(5).caseN(2).filter(FILTER_0).shopId(baseShopId + 1).cookie(sarYandexUids.get(0)).build();

        // САР клики составляют половину, всего больше 10 кликов на магазин, но на пару магазин-кука нет 10 кликов -> НЕ фильтруем
        For45Filter.ClickParams sarFilteredClicksPart1 =
                ClickParams.builder().amount(6).caseN(3).filter(FILTER_0).shopId(baseShopId + 2).cookie(normalCookie + "3").build();

        For45Filter.ClickParams sarFilteredClicksPart2 =
                ClickParams.builder().amount(6).caseN(3).filter(FILTER_0).shopId(baseShopId + 2).cookie(sarYandexUids.get(1)).build();

        // один магазин, 23 клика, sar > 10% -> фильтруем только те, которых больше 10 на пару
        For45Filter.ClickParams sarFilteredClicks2Part1 =
                ClickParams.builder().amount(5).caseN(4).filter(FILTER_0).shopId(baseShopId + 3).cookie(normalCookie + "4").build();

        For45Filter.ClickParams sarFilteredClicks2Part2 =
                ClickParams.builder().amount(10).caseN(4).filter(FILTER_45).shopId(baseShopId + 3).cookie(normalCookie + "5").build();

        For45Filter.ClickParams sarFilteredClicks2Part3 =
                ClickParams.builder().amount(8).caseN(4).filter(FILTER_0).shopId(baseShopId + 3).cookie(sarYandexUids.get(2)).build();

        // САР клики составляют меньше 10%, всего кликов более 10 -> НЕ фильтруем ничего
        For45Filter.ClickParams sarNonFilteredClicks2Part1 =
                ClickParams.builder().amount(50).caseN(5).filter(FILTER_0).shopId(baseShopId + 4).cookie(normalCookie + "6").build();

        For45Filter.ClickParams sarNonFilteredClicks2Part2 =
                ClickParams.builder().amount(1).caseN(5).filter(FILTER_0).shopId(baseShopId + 4).cookie(sarYandexUids.get(0)).build();

        return Stream.of(
                generateClicks(normalClicks),
                generateClicks(sarFilteredClicksPart1),
                generateClicks(sarFilteredClicksPart2),
                generateClicks(sarNonFilteredClicksPart1),
                generateClicks(sarNonFilteredClicksPart2),
                generateClicks(sarFilteredClicks2Part1),
                generateClicks(sarFilteredClicks2Part2),
                generateClicks(sarFilteredClicks2Part3),
                generateClicks(sarNonFilteredClicks2Part1),
                generateClicks(sarNonFilteredClicks2Part2)
        )
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<TestClick> generateClicks(For45Filter.ClickParams params) {
        List<TestClick> result = new ArrayList<>();
        for (int i = 0; i < params.getAmount(); i++) {
            DateTime clickTime = TEST_DT.plusSeconds((i % 2) * 1000);
            TestClick click = ClickGenerator.generateUniqueClick("case_" + params.getCaseN() + "_", clickTime)
                    .set("cookie", params.getCookie())
                    .set("shop_id", params.getShopId())
                    .setFilter(params.getFilter());
            result.add(click);
        }
        return result;
    }

    @Value
    @Builder
    private static class ClickParams {
        private int caseN;
        private int amount;
        private FilterConstants filter;
        private String cookie;
        private Long shopId;
    }
}
