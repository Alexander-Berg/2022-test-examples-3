package ru.yandex.market.antifraud.filter.generators;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kateleb on 27.11.18
 */
@Slf4j
public class For35Filter implements FilterGenerator {

    private static final String MARKET_REQ_ID = "aaaaa";
    private static final String MARKET_REQ_ID2 = "bbbbb";
    private static final String MARKET_REQ_ID3 = "ccccc";
    private static final String MARKET_REQ_ID4 = "ddddd";

    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime().withMillisOfSecond(0);
        //генерим клики по условию: больше одного клика
        clicks.addAll(generateClicksWithmptyCookie(timeOfClicks));
        clicks.addAll(generateClicksWithoutEmptyCookie(timeOfClicks));
        clicks.addAll(generateClicksWithEmptyCookieAnotherDay(timeOfClicks));
        clicks.addAll(generateClicksWithmptyCookieAnotherPP(timeOfClicks));

        return clicks;
    }


    private static List<TestClick> generateClicksWithmptyCookie(DateTime timeOfClicks) {

        List<TestClick> clicks = new ArrayList<>();
        // these must be rolled back
        for (int i = 0; i < 10; i++) {
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            String cookie = i == 0 ? "" : "1111111" + i;
            click.set("cookie", cookie);
            click.set("pp", 21);
            click.set("req_id", MARKET_REQ_ID);
            click.set("rowid", "case_1_" + click.get("rowid", String.class));
            click.setFilter(FilterConstants.FILTER_35);
            clicks.add(click);
        }

        System.out.println("Generated clicks\n" + clicks);
        return clicks;
    }


    private static List<TestClick> generateClicksWithoutEmptyCookie(DateTime timeOfClicks) {
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            click.set("cookie", "2222222" + i);
            click.set("pp", 21);
            click.set("req_id", MARKET_REQ_ID2);
            click.set("rowid", "case_2_" + click.get("rowid", String.class));
            click.setFilter(FilterConstants.FILTER_0);
            clicks.add(click);
        }

        return clicks;
    }


    private static List<TestClick> generateClicksWithEmptyCookieAnotherDay(DateTime timeOfClicks) {
        // these must be rolled back
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            String cookie = i == 0 ? "" : "3333333" + i;
            click.set("cookie", cookie);
            click.set("req_id", MARKET_REQ_ID3);
            click.set("pp", 21);
            click.set("rowid", "case_3_" + click.get("rowid", String.class));
            click.setFilter(FilterConstants.FILTER_35);
            clicks.add(click);
        }
        // these must be not (clicks for another day, requid is bad only for next day)
        for (int i = 0; i < 10; i++) {
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks.minusDays(1), 1).get(0);
            click.set("cookie", "4444444" + i);
            click.set("req_id", MARKET_REQ_ID3);
            click.set("pp", 21);
            click.set("rowid", "case_3_" + click.get("rowid", String.class));
            click.setFilter(FilterConstants.FILTER_0);
            clicks.add(click);
        }

        return clicks;
    }

    private static List<TestClick> generateClicksWithmptyCookieAnotherPP(DateTime timeOfClicks) {

        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            String cookie = i == 0 ? "" : "1111111" + i;
            click.set("cookie", cookie);
            click.set("pp", 7);
            click.set("req_id", MARKET_REQ_ID4);
            click.set("rowid", "case_4_" + click.get("rowid", String.class));
            click.setFilter(FilterConstants.FILTER_0);
            clicks.add(click);
        }

        System.out.println("Generated clicks\n" + clicks);
        return clicks;
    }

}
