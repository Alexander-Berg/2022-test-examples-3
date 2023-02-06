package ru.yandex.market.antifraud.filter.generators;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

/**
 * Created by kateleb on 02.02.18
 */
@Slf4j
public class For33Filter implements FilterGenerator {


    private static final int SAFE_COOKIE_DELAY_MINUTES = 60;

    private static final Integer NON_MARKET_CLID = -10;
    private List<Integer> clids;

    public For33Filter(List<Integer> clids) {
        this.clids = clids;
    }

    private List<TestClick> generateClicksCase1(DateTime timeOfClicks) {
        //значения с запасом
        return generateCase(timeOfClicks, 14, 20, 8,
                clids.get(0), FilterConstants.FILTER_33, "case1");
    }

    private List<TestClick> generateClicksCase2(DateTime timeOfClicks) {
        //почти четко почти граничные значения
        return generateCase(timeOfClicks, 14, 10, 3,
                clids.get(2), FilterConstants.FILTER_33, "case2");
    }

    private List<TestClick> generateClicksCase3(DateTime timeOfClicks) {
        //пустой куки
        return generateCase(timeOfClicks, null, 10, 5,
                clids.get(3), FilterConstants.FILTER_0, "case3");
    }

    private List<TestClick> generateClicksCase4(DateTime timeOfClicks) {
        //пустой клид
        return generateCase(timeOfClicks, 14, 10, 5,
                null, FilterConstants.FILTER_0, "case4");
    }

    private List<TestClick> generateClicksCase5(DateTime timeOfClicks) {
        //немаркетный клид
        return generateCase(timeOfClicks, 14, 10, 4,
                NON_MARKET_CLID, FilterConstants.FILTER_0, "case5");
    }

    private List<TestClick> generateClicksCase6(DateTime timeOfClicks) {
        //меньше 10 кликов
        return generateCase(timeOfClicks, 14, 9, 3,
                clids.get(3), FilterConstants.FILTER_0, "case6");
    }

    private List<TestClick> generateClicksCase7(DateTime timeOfClicks) {
        //куки старше 15 минут
        return generateCase(timeOfClicks, 16, 10, 4,
                clids.get(4), FilterConstants.FILTER_0, "case7");
    }

    private List<TestClick> generateClicksCase8(DateTime timeOfClicks) {
        //мало плохих кликов
        return generateCase(timeOfClicks, 14, 11, 3,
                clids.get(5), FilterConstants.FILTER_0, "case8");
    }

    private List<TestClick> generateClicksCase9(DateTime timeOfClicks) {
        //все условия соблюдены, клики от 1 пользователя - по текущим договоренностям все равно откатываем
        String qvaziBadCookie = Cookie.generateYandexCookie(timeOfClicks.minusMinutes(14));
        List<TestClick> clicks = new ArrayList<>();
        //плохие
        for (int i = 0; i < 4; i++) {
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            click.set("cookie", qvaziBadCookie);
            click.set("clid", clids.get(6));
            click.set("rowid", "case9_1_" + click.get("rowid", String.class));
            click.setFilter(FilterConstants.FILTER_33);
            clicks.add(click);
        }

        for (int i = 0; i < 6; i++) {
            //хорошие
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks.plusMinutes(10), 1).get(0);
            click.set("cookie", qvaziBadCookie);
            click.set("clid", clids.get(6));
            click.set("rowid", "case9_2_" + click.get("rowid", String.class));
            clicks.add(click);
        }
        return clicks;
    }

    private List<TestClick> generateClicksCase10(DateTime timeOfClicks) {
        //клид 0
        return generateCase(timeOfClicks, 14, 11, 4,
                0, FilterConstants.FILTER_0, "case10");
    }

    private List<TestClick> generateCase(DateTime timeOfClicks, Integer cookieCreationDelay,
                                         int tolalClidClicksCount, int badClidClicksCount, Integer marketClid,
                                         FilterConstants filter, String caseId) {
        List<TestClick> clicks = new ArrayList<>();

        // these must be rolled back
        for (int i = 0; i < badClidClicksCount; i++) {
            String qvaziBadCookie = cookieCreationDelay == null ? null :
                    Cookie.generateYandexCookie(timeOfClicks.minusMinutes(cookieCreationDelay));
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            click.set("cookie", qvaziBadCookie);
            click.set("clid", marketClid);
            click.set("rowid", caseId + "_1_" + click.get("rowid", String.class));
            click.setFilter(filter);
            clicks.add(click);
        }

        // these must not
        for (int i = 0; i < (tolalClidClicksCount - badClidClicksCount); i++) {
            String cookie = Cookie.generateYandexCookie(timeOfClicks.minusMinutes(SAFE_COOKIE_DELAY_MINUTES));
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            click.set("cookie", cookie);
            click.set("clid", marketClid);
            click.set("rowid", caseId + "_2_" + click.get("rowid", String.class));
            clicks.add(click);
        }
        return clicks;
    }

    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime().withMillisOfSecond(0);
        //генерим клики по условию: один клид из маркетных, не меньше 10 кликов разных пользователей
        //из них больше 30проц кликов от разных пользователей
        //откатываем только клики от свежих пользователей

        clicks.addAll(generateClicksCase1(timeOfClicks));
        clicks.addAll(generateClicksCase2(timeOfClicks));
        clicks.addAll(generateClicksCase3(timeOfClicks));
        clicks.addAll(generateClicksCase4(timeOfClicks));
        clicks.addAll(generateClicksCase5(timeOfClicks));
        clicks.addAll(generateClicksCase6(timeOfClicks));
        clicks.addAll(generateClicksCase7(timeOfClicks));
        clicks.addAll(generateClicksCase8(timeOfClicks));
        clicks.addAll(generateClicksCase9(timeOfClicks));
        clicks.addAll(generateClicksCase10(timeOfClicks));

        return clicks;
    }

}
