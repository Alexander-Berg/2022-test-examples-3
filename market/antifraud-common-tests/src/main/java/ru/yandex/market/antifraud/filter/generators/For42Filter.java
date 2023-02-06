package ru.yandex.market.antifraud.filter.generators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Value;
import org.joda.time.DateTime;

import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.stat.PpUtil;

import static ru.yandex.market.antifraud.filter.fields.FilterConstants.FILTER_0;
import static ru.yandex.market.antifraud.filter.fields.FilterConstants.FILTER_42;

/**
 * @author dzvyagin
 */
public class For42Filter implements FilterGenerator {

    private static final Integer OK_PP = 153;

    private static final DateTime TEST_DT = new DateTime().withMillisOfSecond(0);

    @Override
    public List<TestClick> generate() {
        // clicks from another pp
        ClickParams normalClicks =
                ClickParams.builder().amount(30).caseN(1).checkedPP(false).filter(FILTER_0).puid("123").clid(321L).build();
        ClickParams anonymousClicks =
                ClickParams.builder().amount(40).caseN(2).checkedPP(true).filter(FILTER_42).puid("").clid(322L).build();
        ClickParams anonymousOkClicks =
                ClickParams.builder().amount(40).caseN(3).checkedPP(true).filter(FILTER_0).puid("").clid(323L).build();
        ClickParams logonOkOkClicks =
                ClickParams.builder().amount(5).caseN(4).checkedPP(true).filter(FILTER_0).puid("22").clid(323L).build();
        return Stream.of(
                generateClicks(normalClicks),
                generateClicks(anonymousClicks),
                generateClicks(anonymousOkClicks),
                generateClicks(logonOkOkClicks)
        )
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<TestClick> generateClicks(ClickParams params) {
        String clickCookie = Cookie.generateYandexCookie(TEST_DT);
        String showCookie = Cookie.generateYandexCookie(TEST_DT.minusMinutes(5));
        List<TestClick> result = new ArrayList<>();
        Random random = new Random();
        Integer[] pps = PpUtil.partnerWidgetPps().toArray(new Integer[]{});
        for (int i = 0; i < params.getAmount(); i++) {
            DateTime clickTime = TEST_DT.plusSeconds((i % 2) * 1000);
            Integer pp = params.isCheckedPP() ? pps[random.nextInt(pps.length)] : OK_PP;
            TestClick click = ClickGenerator.generateUniqueClick("case_" + params.getCaseN() + "_", clickTime)
                    .set("pp", pp)
                    .set("cookie", clickCookie)
                    .set("show_cookie", showCookie)
                    .set("shop_id", 777 + params.getCaseN())
                    .set("puid", params.getPuid())
                    .set("clid", params.getClid())
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
        private boolean checkedPP;
        private FilterConstants filter;
        private String puid;
        private Long clid;
    }
}
