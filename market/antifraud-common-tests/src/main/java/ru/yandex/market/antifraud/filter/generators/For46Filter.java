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
import static ru.yandex.market.antifraud.filter.fields.FilterConstants.FILTER_46;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 29.11.2019
 */
public class For46Filter implements FilterGenerator {
    private static final DateTime TEST_DT = new DateTime().withMillisOfSecond(0);

    @Override
    public List<TestClick> generate() {
        long baseShopId = 12345L;
        String normalCookie = "normalClicks";
        // обычные клики
        For46Filter.ClickParams normalClicks1 =
                ClickParams.builder().amount(5).caseN(1).filter(FILTER_0).referer("--https://market.yandex.ru/shop").pp(8).build();
        For46Filter.ClickParams normalClicks2 =
                ClickParams.builder().amount(5).caseN(1).filter(FILTER_0).referer("--https://market.yandex.ru/shop").pp(7).build();
        For46Filter.ClickParams normalClicks3 =
                ClickParams.builder().amount(5).caseN(1).filter(FILTER_0).referer("--https://market.yandex.ru/shop--").pp(8).build();

        //клики для фильтрации
        For46Filter.ClickParams filteredClicks =
                ClickParams.builder().amount(5).caseN(2).filter(FILTER_46).referer("--https://market.yandex.ru/shop--123").pp(7).build();

        return Stream.of(
                generateClicks(normalClicks1),
                generateClicks(normalClicks2),
                generateClicks(normalClicks3),
                generateClicks(filteredClicks)
        )
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<TestClick> generateClicks(For46Filter.ClickParams params) {
        List<TestClick> result = new ArrayList<>();
        for (int i = 0; i < params.getAmount(); i++) {
            DateTime clickTime = TEST_DT.plusSeconds((i % 2) * 1000);
            TestClick click = ClickGenerator.generateUniqueClick("case_" + params.getCaseN() + "_", clickTime)
                    .set("referer", params.getReferer())
                    .set("pp", params.getPp())
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
        private String referer;
        private Integer pp;
    }
}
