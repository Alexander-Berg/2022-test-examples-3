package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.Url;
import ru.yandex.market.antifraud.filter.ip.IP;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by entarrion on 26.01.15.
 */
public class For13Filter implements FilterGenerator {
    public static List<TestClick> generateClicksGreaterThanThresholdValue(DateTime timeOfClicks) {
        return generateClicks(timeOfClicks, 5, FilterConstants.FILTER_13);
    }

    public static List<TestClick> generateClicksLessThanThresholdValue(DateTime timeOfClicks) {
        return generateClicks(timeOfClicks, 4, FilterConstants.FILTER_0);
    }

    public static List<TestClick> generateClicks(DateTime timeOfClicks, int count, FilterConstants filter) {
        List<TestClick> clicks = new ArrayList<>();
        String cookie = Cookie.generateCookieForClickTime(timeOfClicks);
        for (int i = 0; i < count; i++) {
            String referer = Url.generateUrlWithRandomPath("market-click2.yandex.ru");
            clicks.add(generateClick(timeOfClicks, referer, cookie, filter));
        }
        return clicks;
    }

    public static List<TestClick> generateClicksWithEmptyCookie(DateTime timeOfClicks) {
        List<TestClick> clicks = new ArrayList<>();
        String ip6 = IP.generateValidIPv4AsIPv6();
        for (int i = 0; i < 6; i++) {
            String referrer = Url.generateUrlWithRandomPath("market-click2.yandex.ru");
            TestClick click = generateClickWithEmptyCookie(timeOfClicks, referrer, ip6, FilterConstants.FILTER_13);
            clicks.add(click);
        }
        return clicks;
    }

    private static TestClick generateClick(DateTime timeOfClicks, String referrer, String cookie, FilterConstants filter) {
        TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
        click.set("Referer", referrer);
        click.set("Cookie", cookie);
        click.setFilter(filter);
        return click;
    }

    private static TestClick generateClickWithEmptyCookie(DateTime timeOfClicks, String referrer, String ip6, FilterConstants filter) {
        TestClick click = ClickGenerator.generateUniqueClicksWithIp6Only(timeOfClicks, 1).get(0);
        click.set("Ip6", ip6);
        click.set("Referer", referrer);
        click.setFilter(filter);
        return click;
    }

    @Override
    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики не подпадающие под фильтр 13 (По граничному условию)
        clicks.addAll(For13Filter.generateClicksLessThanThresholdValue(timeOfClicks));
        //Генерируем клики подпадающие под фильтр 13 (По граничному условию)
        clicks.addAll(For13Filter.generateClicksGreaterThanThresholdValue(timeOfClicks));
        //Генерируем клики подпадающие под фильтр 13 (с пустым куки)
        clicks.addAll(For13Filter.generateClicksWithEmptyCookie(timeOfClicks));
        return clicks;
    }
}
