package ru.yandex.market.antifraud.filter.generators;

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
public class For09And10Filter implements FilterGenerator {
    private static int[] PP = {6, 7, 27};

    @Override
    public List<TestClick> generate() {
        throw new UnsupportedOperationException("Choose exact generator method");
    }

    public List<TestClick> generateFor9Filter() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        clicks.addAll(For09And10Filter.generateFor09FilterCase01(timeOfClicks));
        clicks.addAll(For09And10Filter.generateFor09FilterCase02(timeOfClicks));
        return clicks;
    }

    public List<TestClick> generateFor10Filter() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        clicks.addAll(For09And10Filter.generateFor10FilterCase01(timeOfClicks));
        clicks.addAll(For09And10Filter.generateFor10FilterCase02(timeOfClicks));
        return clicks;
    }

    public static List<TestClick> generateFor09FilterCase01(DateTime timeOfClicks) {
        List<TestClick> clicks = new ArrayList<>();
        String uah = "10";
        int geoId = 2;
        for (int i = 0; i < 10; i++) {
            String cookie = Cookie.generateCookieForClickTime(timeOfClicks);
            DateTime dt = timeOfClicks;
            for (int j = 0; j < 3; j++) {
                dt = dt.plusSeconds(20);
                int pp = PP[j % PP.length];
                clicks.add(generateClickWithCookie(dt, cookie, uah, pp, geoId, FilterConstants.FILTER_9));
            }
            clicks.add(generateClickWithCookie(timeOfClicks, cookie, uah, 3, geoId, FilterConstants.FILTER_0));
        }
        return clicks;
    }

    public static List<TestClick> generateFor09FilterCase02(DateTime timeOfClicks) {
        List<TestClick> clicks = new ArrayList<>();
        String uah = "10";
        int geoId = 2;
        for (int i = 0; i < 10; i++) {
            String ip6 = IP.generateValidIPv4AsIPv6();
            DateTime dt = timeOfClicks;
            for (int j = 0; j < 3; j++) {
                dt = dt.plusSeconds(20);
                int pp = PP[j % PP.length];
                clicks.add(generateClickWithIp6(dt, ip6, uah, pp, geoId, FilterConstants.FILTER_9));
            }
            clicks.add(generateClickWithIp6(timeOfClicks, ip6, uah, 3, geoId, FilterConstants.FILTER_0));
        }

        // empty cookie to make filter use ip6 instead
        clicks.forEach(click -> click.set("cookie", ""));
        return clicks;
    }

    public static List<TestClick> generateFor10FilterCase01(DateTime timeOfClicks) {
        List<TestClick> clicks = generateFor09FilterCase01(timeOfClicks);
        String referer = clicks.get(0).get("referer", String.class);
        String cookie = clicks.get(0).get("cookie", String.class);
        String uah = clicks.get(0).get("uah", String.class);
        int geoId = 2;
        int pp = clicks.get(0).get("pp", Integer.class);
        for (int i = 0; i < 10; i++) {
            clicks.add(generateClickWithCookie(timeOfClicks, cookie, uah, pp, geoId, referer, FilterConstants.FILTER_9));
        }
        cookie = Cookie.generateCookieForClickTime(timeOfClicks);
        for (int i = 0; i < 10; i++) {
            if (i < 6) {
                clicks.add(generateClickWithCookie(timeOfClicks.plusSeconds(i * 60), cookie, uah, pp, geoId, referer, FilterConstants.FILTER_10));
            } else {
                clicks.add(generateClickWithCookie(timeOfClicks.plusSeconds(i * 60), cookie, uah, pp, geoId, FilterConstants.FILTER_10));
            }
        }
        return clicks;
    }

    public static List<TestClick> generateFor10FilterCase02(DateTime timeOfClicks) {
        List<TestClick> clicks = generateFor09FilterCase02(timeOfClicks);
        String referer = clicks.get(0).get("referer", String.class);
        String ip6 = clicks.get(0).get("ip6", String.class);
        String uah = clicks.get(0).get("uah", String.class);
        int geoId = 2;
        int pp = clicks.get(0).get("pp", Integer.class);
        for (int i = 0; i < 10; i++) {
            clicks.add(generateClickWithIp6(timeOfClicks, ip6, uah, pp, geoId, referer, FilterConstants.FILTER_9));
        }
        ip6 = IP.generateValidIPv4AsIPv6();
        for (int i = 0; i < 10; i++) {
            if (i < 6) {
                clicks.add(generateClickWithIp6(timeOfClicks.plusSeconds(i * 60), ip6, uah, pp, geoId, referer, FilterConstants.FILTER_10));
            } else {
                clicks.add(generateClickWithIp6(timeOfClicks.plusSeconds(i * 60), ip6, uah, pp, geoId, FilterConstants.FILTER_10));
            }
        }

        // empty cookie to make filter use ip6 instead
        clicks.forEach(click -> click.set("cookie", ""));
        return clicks;
    }

    private static TestClick generateClickWithCookie(DateTime timeOfClicks, String cookie, String uah, int pp, int geoId, FilterConstants filter) {
        TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
        click.set("cookie", cookie);
        return setGeneralFields(click, uah, pp, geoId, filter);
    }

    private static TestClick generateClickWithIp6(DateTime timeOfClicks, String ip6, String uah, int pp, int geoId, FilterConstants filter) {
        TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
        click.set("ip6", ip6);
        return setGeneralFields(click, uah, pp, geoId, filter);
    }

    private static TestClick setGeneralFields(TestClick click, String uah, int pp, int geoId, FilterConstants filter) {
        click.set("pp", pp);
        click.set("geo_id", geoId);
        click.set("uah", uah);
        click.setFilter(filter);
        return click;
    }

    private static TestClick setReferer(TestClick click, String referer) {
        click.set("referer", referer);
        return click;
    }

    private static TestClick generateClickWithCookie(DateTime timeOfClicks, String cookie, String uah, int pp, int geoId, String referer, FilterConstants filter) {
        return setReferer(generateClickWithCookie(timeOfClicks, cookie, uah, pp, geoId, filter), referer);
    }

    private static TestClick generateClickWithIp6(DateTime timeOfClicks, String ip6, String uah, int pp, int geoId, String referer, FilterConstants filter) {
        return setReferer(generateClickWithIp6(timeOfClicks, ip6, uah, pp, geoId, filter), referer);
    }

}
