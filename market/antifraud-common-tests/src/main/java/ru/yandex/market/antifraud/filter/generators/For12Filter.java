package ru.yandex.market.antifraud.filter.generators;

import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.ip.IP;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by entarrion on 26.01.15.
 */
public class For12Filter implements FilterGenerator {
    public static List<TestClick> generateClicksGreaterThanThresholdValue(DateTime timeOfClicks) {
        return generateClicks(timeOfClicks, 21, FilterConstants.FILTER_12);
    }

    public static List<TestClick> generateClicksLessThanThresholdValue(DateTime timeOfClicks) {
        return generateClicks(timeOfClicks, 20, FilterConstants.FILTER_0);
    }

    public static List<TestClick> generateClicks(DateTime timeOfClicks, int count, FilterConstants filter) {
        List<TestClick> clicks = new ArrayList<>();
        String cookie = Cookie.generateCookieForClickTime(timeOfClicks);
        int pof = 114477;
        for (int i = 0; i < count; i++) {
            int pp = RandomUtils.nextBoolean() ? 1000 : 1001;
            clicks.add(generateClick(timeOfClicks, cookie, pof, pp, filter));
        }
        return clicks;
    }

    private static TestClick generateClick(DateTime timeOfClicks, String cookie, int pof, int pp, FilterConstants filter) {
        TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
        click.set("cookie", cookie);
        click.set("pof", pof);
        click.set("pp", pp);
        click.setFilter(filter);
        return click;
    }

    public static List<TestClick> generateClicksWithEmptyCookie(DateTime timeOfClicks) {
        List<TestClick> clicks = new ArrayList<>();
        String ip6 = IP.generateValidIPv4AsIPv6();
        int pof = 114477;
        for (int i = 0; i < 22; i++) {
            int pp = RandomUtils.nextBoolean() ? 1000 : 1001;
            clicks.add(generateClickWithEmptyCookie(timeOfClicks, ip6, pof, pp, FilterConstants.FILTER_12));
        }
        return clicks;
    }

    private static TestClick generateClickWithEmptyCookie(DateTime timeOfClicks, String ip6, int pof, int pp, FilterConstants filter) {
        TestClick click = ClickGenerator.generateUniqueClicksWithIp6Only(timeOfClicks, 1).get(0);
        click.set("ip6", ip6);
        click.set("pof", pof);
        click.set("pp", pp);
        click.setFilter(filter);
        return click;
    }

    @Override
    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики не подпадающие под фильтр 12 (По граничному условию)
        clicks.addAll(For12Filter.generateClicksLessThanThresholdValue(timeOfClicks));
        //Генерируем клики подпадающие под фильтр 12 (По граничному условию)
        clicks.addAll(For12Filter.generateClicksGreaterThanThresholdValue(timeOfClicks));
        //Генерируем клики подпадающие под фильтр 12 (c пустыми куками)
        clicks.addAll(For12Filter.generateClicksWithEmptyCookie(timeOfClicks));
        return clicks;
    }
}