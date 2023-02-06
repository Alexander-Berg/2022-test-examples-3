package ru.yandex.market.antifraud.filter.generators;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import static java.util.stream.Collectors.toList;

/**
 * Created by kateleb on 27.11.18
 */
@Slf4j
public class For36Filter implements FilterGenerator {

    private static final String MARKET_REQ_ID = "aaaaa";

    private static final int COOKIE_TIME_THRESHOLD = 1800;

    public List<TestClick> generate() {
        DateTime timeOfClicks = new DateTime().withMillisOfSecond(0);
        //генерим клики по условию: больше одного клика
        return Stream.of(
            generateClicksWithClickerIpAndReferer(timeOfClicks, FilterConstants.FILTER_0),
            generateClicksWithClickerIpAndReferer(timeOfClicks, FilterConstants.FILTER_36),
            generateClicksWithOldCookie(timeOfClicks),
            generateClicksWithEmptyCookie(timeOfClicks),
            generateClicksWithAnotherPp(timeOfClicks)
        )
            .flatMap(Collection::stream)
            .collect(toList());
    }

    /*
    Для фрода генерируем N + 1 кликов у которых будет N/2 разных ip и N/2 разных referrer.
    N + 1 кликов нужно для того, чтобы получилось (N + 1) / (N/2) > 2
    Для не фрода генерируем N кликов, чтобы (N + 1) / (N/2) == 2
     */
    private static List<TestClick> generateClicksWithClickerIpAndReferer(DateTime timeOfClicks, FilterConstants filter) {
        String clickerCookie = Cookie.generateYandexCookie(timeOfClicks.minusSeconds(
            COOKIE_TIME_THRESHOLD - 1 - RndUtil.nextInt(COOKIE_TIME_THRESHOLD)
        ));

        int n = 10;
        int clicks = filter == FilterConstants.FILTER_0 ? n : n + 1;

        return IntStream.range(0, clicks)
            .mapToObj(i -> {
                TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
                click.set("rowid", "case_" + filter.id() + "_" + click.get("rowid", String.class));
                click.set("pp", 21);
                click.set("cookie", clickerCookie);
                click.set("req_id", MARKET_REQ_ID);
                click.set("ip6", "bbbb::" + (i % (n / 2)));
                click.set("referer", MARKET_REQ_ID + "--referer-" + ((i + 1) % (n / 2)));
                click.setFilter(filter);
                return click;
            })
            .collect(toList());
    }

    private static List<TestClick> generateClicksWithOldCookie(DateTime timeOfClicks) {
        String goodCookie = Cookie.generateYandexCookie(timeOfClicks.minusSeconds(
            COOKIE_TIME_THRESHOLD + RndUtil.nextInt(COOKIE_TIME_THRESHOLD)
        ));

        int n = 10;

        return IntStream.range(0, n + 1)
            .mapToObj(i -> {
                TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
                click.set("rowid", "case_good_" + click.get("rowid", String.class));
                click.set("pp", 21);
                click.set("cookie", goodCookie);
                click.set("req_id", MARKET_REQ_ID);
                click.set("ip6","cccc::" + (i % (n / 2)));
                click.set("referer", MARKET_REQ_ID + "--referer-" + ((i + 1) % (n / 2)));
                click.setFilter(FilterConstants.FILTER_0);
                return click;
            })
            .collect(toList());
    }

    private static List<TestClick> generateClicksWithEmptyCookie(DateTime timeOfClicks) {
        int n = 10;

        return IntStream.range(0, n + 1)
            .mapToObj(i -> {
                TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
                click.set("rowid", "case_old_" + click.get("rowid", String.class));
                click.set("pp", 21);
                click.set("cookie", "");
                click.set("req_id", MARKET_REQ_ID);
                click.set("ip6", "dddd::" + (i % (n / 2)));
                click.set("referer", MARKET_REQ_ID + "--referer-" + ((i + 1) % (n / 2)));
                click.setFilter(FilterConstants.FILTER_0);
                return click;
            })
            .collect(toList());
    }

    private static List<TestClick> generateClicksWithAnotherPp(DateTime timeOfClicks) {
        String clickerCookie = Cookie.generateYandexCookie(timeOfClicks.minusSeconds(
            COOKIE_TIME_THRESHOLD - 1 - RndUtil.nextInt(COOKIE_TIME_THRESHOLD)
        ));

        int n = 10;

        return IntStream.range(0, n + 1)
            .mapToObj(i -> {
                TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
                click.set("rowid", "case_empty_" + click.get("rowid", String.class));
                click.set("pp", 22);
                click.set("cookie", clickerCookie);
                click.set("req_id", MARKET_REQ_ID);
                click.set("ip6", "eeee::" + (i % (n / 2)));
                click.set("referer", MARKET_REQ_ID + "--referer-" + ((i + 1) % (n / 2)));
                click.setFilter(FilterConstants.FILTER_0);
                return click;
            })
            .collect(toList());
    }
}
