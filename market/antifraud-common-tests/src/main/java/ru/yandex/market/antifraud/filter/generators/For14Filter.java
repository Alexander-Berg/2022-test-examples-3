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
public class For14Filter implements FilterGenerator {

    public static List<TestClick> genenerateClicksForOneUser(DateTime timeOfClicks) {
        return generateClicksForUsers(timeOfClicks, 10, 1, FilterConstants.FILTER_0);
    }

    public static List<TestClick> genenerateClicksForTwoUser(DateTime timeOfClicks) {
        return generateClicksForUsers(timeOfClicks, 10, 2, FilterConstants.FILTER_14);
    }

    public static List<TestClick> generateClicksForUsers(DateTime timeOfClicks, int clickCount, int userCount, FilterConstants filter) {
        List<TestClick> clicks = new ArrayList<>();
        List<String> cookies = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            cookies.add(Cookie.generateYandexCookie(timeOfClicks.minusSeconds(45)));
        }
        String ip = IP.generateValidNoYandexIPv4();
        for (int i = 0; i < clickCount; i++) {
            String cookie = cookies.get(i % userCount);
            int pp = RandomUtils.nextInt(0, 1000);
            clicks.add(generateClick(timeOfClicks, pp, ip, cookie, filter));
        }
        return clicks;
    }

    private static TestClick generateClick(DateTime timeOfClicks, int pp, String ip, String cookie, FilterConstants filter) {
        TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
        click.set("Pp", pp);
        click.set("Ip", IP.atonIPv4(ip).toString());
        click.set("Ip6", IP.getIPv6FromIPv4(ip));
        click.set("Cookie", cookie);
        click.setFilter(filter);
        return click;
    }

    public static List<TestClick> generateClicksWithEmptyCookie(DateTime timeOfClicks) {
        List<TestClick> clicks = new ArrayList<>();
        List<String> ip6s = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ip6s.add(IP.generateValidIPv4AsIPv6());
        }
        for (int i = 0; i < 10; i++) {
            String ip6 = ip6s.get(i % 2);
            int pp = RandomUtils.nextInt(0, 1000);
            clicks.add(generateClickWithEmptyCookie(timeOfClicks, pp, ip6, FilterConstants.FILTER_0));
        }
        return clicks;
    }

    private static TestClick generateClickWithEmptyCookie(DateTime timeOfClicks, int pp, String ip6, FilterConstants filter) {
        TestClick click = ClickGenerator.generateUniqueClicksWithIp6Only(timeOfClicks, 1).get(0);
        click.set("Pp", pp);
        click.set("Ip6", ip6);
        click.setFilter(filter);
        return click;
    }

    @Override
    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики не подпадающие под фильтр 14 (1 сотрудник)
        clicks.addAll(For14Filter.genenerateClicksForOneUser(timeOfClicks));
        //Генерируем клики подпадающие под фильтр 14 (2 сотрудника)
        clicks.addAll(For14Filter.genenerateClicksForTwoUser(timeOfClicks));
        //Генерируем клики не подпадающие под фильтр 14 (c пустыми куки)
        clicks.addAll(For14Filter.generateClicksWithEmptyCookie(timeOfClicks));
        return clicks;
    }
}
