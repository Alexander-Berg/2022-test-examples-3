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
import static ru.yandex.market.antifraud.filter.fields.FilterConstants.FILTER_44;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 07.11.2019
 */
public class For44Filter implements FilterGenerator {
    private static final DateTime TEST_DT = new DateTime().withMillisOfSecond(0);

    private final List<String> sarYandexUids;

    public For44Filter(List<String> sarYandexUids) {
        this.sarYandexUids = sarYandexUids;
    }

    @Override
    public List<TestClick> generate() {
        long baseShopId = 12345L;
        String normalCookie = "normalClicks";
        // обычные клики
        For44Filter.ClickParams normalClicks =
                ClickParams.builder().amount(10).caseN(1).filter(FILTER_0).shopId(baseShopId).cookie(normalCookie).build();

        // САР клики составляют половину от всех кликов магазина -> фильтруем с нужным cookie
        For44Filter.ClickParams sarFilteredClicksPart1 =
                ClickParams.builder().amount(25).caseN(2).filter(FILTER_0).shopId(baseShopId + 1).cookie(normalCookie).build();

        For44Filter.ClickParams sarFilteredClicksPart2 =
                ClickParams.builder().amount(25).caseN(2).filter(FILTER_44).shopId(baseShopId + 1).cookie(sarYandexUids.get(0)).build();

        // САР клики составляют много меньше 10% всех кликов -> НЕ фильтруем ничего
        For44Filter.ClickParams sarNonFilteredClicksPart1 =
                ClickParams.builder().amount(25).caseN(3).filter(FILTER_0).shopId(baseShopId + 2).cookie(normalCookie).build();

        For44Filter.ClickParams sarNonFilteredClicksPart2 =
                ClickParams.builder().amount(1).caseN(3).filter(FILTER_0).shopId(baseShopId + 2).cookie(sarYandexUids.get(1)).build();

        // САР клики составляют ровно 10% от всех кликов магазина -> НЕ фильтруем ничего
        For44Filter.ClickParams sarNonFilteredBorderClicksPart1 =
                ClickParams.builder().amount(9).caseN(4).filter(FILTER_0).shopId(baseShopId + 3).cookie(normalCookie).build();

        For44Filter.ClickParams sarNonFilteredBorderClicksPart2 =
                ClickParams.builder().amount(1).caseN(4).filter(FILTER_0).shopId(baseShopId + 3).cookie(sarYandexUids.get(2)).build();

        return Stream.of(
                generateClicks(normalClicks),
                generateClicks(sarFilteredClicksPart1),
                generateClicks(sarFilteredClicksPart2),
                generateClicks(sarNonFilteredClicksPart1),
                generateClicks(sarNonFilteredClicksPart2),
                generateClicks(sarNonFilteredBorderClicksPart1),
                generateClicks(sarNonFilteredBorderClicksPart2)
        )
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<TestClick> generateClicks(For44Filter.ClickParams params) {
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
