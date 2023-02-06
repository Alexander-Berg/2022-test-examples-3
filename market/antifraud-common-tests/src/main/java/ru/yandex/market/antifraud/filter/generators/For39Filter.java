package ru.yandex.market.antifraud.filter.generators;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.Referrer;
import ru.yandex.market.antifraud.filter.fields.Url;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class For39Filter implements FilterGenerator {

    private static final int TARGET_PP = 21;
    private static final int ANOTHER_PP = 22;

    private static final DateTime TEST_DT = new DateTime().withMillisOfSecond(0);

    private static final int FRESH_COOKIE_THREASHOLD = 1800; // Кука молодая, если живет меньше полчаса
    private static final int YANG_COOKIE_TIME = FRESH_COOKIE_THREASHOLD - 100;
    private static final int OLD_COOKIE_TIME = FRESH_COOKIE_THREASHOLD + 100;

    public List<TestClick> generate() {
        return Stream.of(
            // case for all conditions => all filtered
            generateClicks(1, 10, FilterConstants.FILTER_39, TARGET_PP, YANG_COOKIE_TIME, 30),
            // break condition - MAX(click_ts) - MIN(click_ts) / COUNT(*) < 3.1 and count(*) > 6
            generateClicks(2, 10, FilterConstants.FILTER_0, TARGET_PP, YANG_COOKIE_TIME, 31),
            // break condition - (click_ts - cookie_ts) < 1800
            generateClicks(3, 10, FilterConstants.FILTER_0, TARGET_PP, OLD_COOKIE_TIME, 30),
            // break condition - pp = 21
            generateClicks(4, 10, FilterConstants.FILTER_0, ANOTHER_PP, YANG_COOKIE_TIME, 30),
            // break condition - MAX(click_ts) - MIN(click_ts) / COUNT(*) < 1.1 and count(*) <= 6
            generateClicks(5, 5, FilterConstants.FILTER_0, TARGET_PP, YANG_COOKIE_TIME, 6),
            // case for condition - MAX(click_ts) - MIN(click_ts) / COUNT(*) < 1.1 and count(*) <= 6
            generateClicks(6, 5, FilterConstants.FILTER_39, TARGET_PP, YANG_COOKIE_TIME, 5),
            // break condition - count(*) = COUNT(distinct url)
            generateClicksWithSameUrl(7, 5, FilterConstants.FILTER_0, TARGET_PP, YANG_COOKIE_TIME, 5),
            // case when we are filtering by case=1 and using the same cookie with pp != 21
            generateClicksFor2PpTest(8)
        )
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private static List<TestClick> generateClicks(int caseN, int amount, FilterConstants filter, int pp,
                                                  int cookieCreationDelay, int clickTimeDeviation) {
        String cookie = Cookie.generateYandexCookie(TEST_DT.minusSeconds(cookieCreationDelay));
        String referer = Referrer.generate();
        return IntStream.range(0, amount)
            .mapToObj(i -> {
                    DateTime clickTime = TEST_DT.plusSeconds((i % 2) * clickTimeDeviation);
                    return ClickGenerator.generateUniqueClick("case_" + caseN + "_", clickTime)
                        .set("pp", pp)
                        .set("cookie", cookie)
                        .set("referer", referer)
                        .setFilter(filter);
                }
            )
            .collect(Collectors.toList());
    }

    private static List<TestClick> generateClicksWithSameUrl(int caseN, int amount, FilterConstants filter, int pp,
                                                             int cookieCreationDelay, int clickTimeDeviation) {
        String predefinedUrl = Url.generateRandomUrl();
        List<TestClick> clicks = generateClicks(caseN, amount, filter, pp, cookieCreationDelay, clickTimeDeviation);
        clicks.forEach(c -> c.set("url", predefinedUrl));
        return clicks;
    }

    private static List<TestClick> generateClicksFor2PpTest(int caseN) {
        List<TestClick> clicks =
            generateClicks(caseN, 10, FilterConstants.FILTER_39, TARGET_PP, YANG_COOKIE_TIME, 30);
        String cookie = (String) clicks.get(0).get("cookie");

        generateClicks(caseN, 10, FilterConstants.FILTER_39, ANOTHER_PP, YANG_COOKIE_TIME, 31)
            .forEach(c -> {
                c.set("cookie", cookie);
                clicks.add(c);
            });

        return clicks;
    }
}
