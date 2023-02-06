package ru.yandex.market.antifraud.filter.generators;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
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
@Slf4j
public class For16Filter implements FilterGenerator {


    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        clicks.addAll(For16Filter.generateClicksCase1(timeOfClicks));
        //клики с пустыми куками не попадают под фильтр
        clicks.addAll(For16Filter.generateClicksCaseEmptyCookie(timeOfClicks));
        return clicks;
    }


    public static List<TestClick> generateClicksCase1(DateTime timeOfClicks) {
        List<TestClick> clicks = new ArrayList<>();
        String query = RandomStringUtils.randomAlphanumeric(8);
        List<String> cookies = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String cookie = Cookie.generateYandexCookie(timeOfClicks.minusMinutes(10 + i));
            cookies.add(cookie);
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            click.set("Pp", RandomUtils.nextBoolean() ? 7 : 28);
            click.set("Referer", Url.generateUrlWithParam("text", query));
            click.set("Cookie", cookie);
            click.set("rowid", "case1_1_" + click.get("rowid", String.class));
            clicks.add(click);
        }
        for (int i = 0; i < 30; i++) {
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            click.set("Cookie", cookies.get(i % cookies.size()));
            click.set("rowid", "case1_2_" + click.get("rowid", String.class));
            clicks.add(click);
        }
        for (TestClick click : clicks) {
            click.setFilter(FilterConstants.FILTER_16);
        }
        return clicks;
    }

    public static List<TestClick>  generateClicksCaseEmptyCookie(DateTime timeOfClicks) {
        List<TestClick> clicks = new ArrayList<>();
        String query = RandomStringUtils.randomAlphanumeric(8);
        List<String> ip6s = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String ip6 = IP.generateValidIPv4AsIPv6();
            ip6s.add(ip6);
            TestClick click = ClickGenerator.generateUniqueClicksWithIp6Only(timeOfClicks, 1).get(0);
            click.set("Pp", RandomUtils.nextBoolean() ? 7 : 28);
            click.set("Referer", Url.generateUrlWithParam("text", query));
            click.set("Ip6", ip6);
            click.set("rowid", "case2_1_" + click.get("rowid", String.class));
            click.setFilter(FilterConstants.FILTER_0);
            clicks.add(click);
        }
        for (int i = 0; i < 30; i++) {
            TestClick click = ClickGenerator.generateUniqueClicksWithIp6Only(timeOfClicks, 1).get(0);
            click.set("Ip6", ip6s.get(i % ip6s.size()));
            click.setFilter(FilterConstants.FILTER_0);
            click.set("rowid", "case2_2_" + click.get("rowid", String.class));
            clicks.add(click);
        }
        return clicks;
    }
}
