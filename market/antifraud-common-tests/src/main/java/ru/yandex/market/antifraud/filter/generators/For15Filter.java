package ru.yandex.market.antifraud.filter.generators;

import org.apache.commons.lang3.RandomUtils;
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
public class For15Filter implements FilterGenerator {
    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        clicks.addAll(For15Filter.generateClicksCase1(timeOfClicks));
        clicks.addAll(For15Filter.generateClicksCaseEmptyCookie(timeOfClicks));
        return clicks;
    }

    public static List<TestClick> generateClicksCase1(DateTime timeOfClicks) {
        // 30 кликов с одним и тем же реферером. 20 из них с 1 фильтром, разных пользователей,
        // 10 без фильтра одного и того же пользователя, у которого еще 5 кликов без данного реферера.
        String referer = Url.generateUrlWithParamWithRandValue("text");
        List<TestClick> clicks = For01Filter.generateClicksWithFilter(timeOfClicks, 20);
        for (TestClick click : clicks) {
            click.set("Referer", referer);
            click.set("Pp", RandomUtils.nextBoolean() ? 7 : 28);
        }
        String cookie = Cookie.generateCookieForClickTime(timeOfClicks);
        for (TestClick click : ClickGenerator.generateUniqueClicks(timeOfClicks, 10)) {
            click.set("Referer", referer);
            click.set("Pp", RandomUtils.nextBoolean() ? 7 : 28);
            click.set("Cookie", cookie);
            click.setFilter(FilterConstants.FILTER_15);
            clicks.add(click);
        }
        for (TestClick click : ClickGenerator.generateUniqueClicks(timeOfClicks, 5)) {
            click.set("Cookie", cookie);
            click.setFilter(FilterConstants.FILTER_15);
            clicks.add(click);
        }
        return clicks;
    }

    public static List<TestClick> generateClicksCaseEmptyCookie(DateTime timeOfClicks) {
        // 30 кликов с одним и тем же реферером. 20 из них с 1 фильтром, разных пользователей,
        // 10 без фильтра одного и того же пользователя, у которого еще 5 кликов без данного реферера.
        String referer = Url.generateUrlWithParamWithRandValue("text");
        List<TestClick> clicks = For01Filter.generateClicksWithFilter(timeOfClicks, 20);
        for (TestClick click : clicks) {
            click.set("Referer", referer);
            click.set("Pp", RandomUtils.nextBoolean() ? 7 : 28);
            click.set("Cookie", "");
            click.set("Ip", "");
        }
        String ip6 = IP.generateValidIPv4AsIPv6();
        for (TestClick click : ClickGenerator.generateUniqueClicksWithIp6Only(timeOfClicks, 10)) {
            click.set("Referer", referer);
            click.set("Pp", RandomUtils.nextBoolean() ? 7 : 28);
            click.set("Ip6", ip6);
            click.setFilter(FilterConstants.FILTER_15);
            clicks.add(click);
        }
        for (TestClick click : ClickGenerator.generateUniqueClicksWithIp6Only(timeOfClicks, 5)) {
            click.set("Ip6", ip6);
            click.setFilter(FilterConstants.FILTER_15);
            clicks.add(click);
        }
        return clicks;
    }
}
