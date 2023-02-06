package ru.yandex.market.antifraud.filter.generators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import lombok.Value;
import org.joda.time.DateTime;

import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import static ru.yandex.market.antifraud.filter.fields.FilterConstants.FILTER_0;
import static ru.yandex.market.antifraud.filter.fields.FilterConstants.FILTER_43;

/**
 * @author dzvyagin
 */
public class For43Filter implements FilterGenerator {

    private static final Set<Long> ASSESSORS_UIDS = ImmutableSet.of(111222333L, 111222334L, 111222335L, 111222336L);

    private static final Integer OK_PP = 153;

    private static final DateTime TEST_DT = new DateTime().withMillisOfSecond(0);

    @Override
    public List<TestClick> generate() {
        // clicks from another pp
        ClickParams normalClicks1 =
                ClickParams.builder().amount(30).caseN(1).checkedPuid(false).filter(FILTER_0).build();
        ClickParams assessorsClicks =
                ClickParams.builder().amount(40).caseN(2).checkedPuid(true).filter(FILTER_43).build();
        ClickParams normalClicks2 =
                ClickParams.builder().amount(40).caseN(3).checkedPuid(false).filter(FILTER_0).build();
        return Stream.of(
                generateClicks(normalClicks1),
                generateClicks(assessorsClicks),
                generateClicks(normalClicks2)
        )
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public Set<Long> getAssessorsUids() {
        return ASSESSORS_UIDS;

    }

    private List<TestClick> generateClicks(ClickParams params) {
        String clickCookie = Cookie.generateYandexCookie(TEST_DT);
        String showCookie = Cookie.generateYandexCookie(TEST_DT.minusMinutes(5));
        List<TestClick> result = new ArrayList<>();
        Random random = new Random();
        Long[] uids = ASSESSORS_UIDS.toArray(new Long[]{});
        for (int i = 0; i < params.getAmount(); i++) {
            DateTime clickTime = TEST_DT.plusSeconds((i % 2) * 1000);
            String puid = params.isCheckedPuid() ?
                    String.valueOf(uids[random.nextInt(uids.length)]) :
                    String.valueOf(random.nextLong());
            TestClick click = ClickGenerator.generateUniqueClick("case_" + params.getCaseN() + "_", clickTime)
                    .set("pp", OK_PP)
                    .set("cookie", clickCookie)
                    .set("show_cookie", showCookie)
                    .set("shop_id", 777 + params.getCaseN())
                    .set("puid", puid)
                    .set("clid", random.nextLong())
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
        private boolean checkedPuid;
        private FilterConstants filter;

    }
}
