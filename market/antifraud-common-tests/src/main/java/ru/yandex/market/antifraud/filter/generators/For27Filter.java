package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.PP;
import ru.yandex.market.antifraud.filter.fields.ShopId;
import ru.yandex.market.antifraud.filter.ip.IP;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ru.yandex.market.antifraud.filter.fields.UTM.setUtmTerm;

/**
 * Created by kateleb on 17.06.16.
 */
public class For27Filter implements FilterGenerator {

    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        //Генерируем клики подпадающие под фильтр 27
        clicks.addAll(setUtmTerm(this.generateClicksForFilterWithSameCookie(), "sameCookieBadClicks"));
        clicks.addAll(setUtmTerm(this.generateClicksForFilterWithEmptyCookieSameIp6(), "sameIp6BadClicks"));
        clicks.addAll(setUtmTerm(this.generateClicksWithAnotherTypeId(), "typeId0"));
        clicks.addAll(setUtmTerm(this.generateClicksWithDiffCookie(), "differentCookie"));
        clicks.addAll(setUtmTerm(this.generateClicksWithDiffShopIds(), "differentShopId"));
        clicks.addAll(setUtmTerm(this.generateClicksWith24hourGap(), "withHoursGap"));
        clicks.addAll(setUtmTerm(this.generateClicksWithDiffPP(), "differentPP"));
        return clicks;
    }

    private DateTime timeOfClicks;

    public For27Filter(DateTime dateTime) {
        this.timeOfClicks = dateTime;
    }

    public List<TestClick> generateClicksForFilterWithSameCookie() {
        int shopId = ShopId.generate();
        String cookie = Cookie.generateCookieForClickTime(timeOfClicks);
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, 5);
        int pp = For06Filter.getRandomIncludePP();
        for (TestClick click : clicks) {
            click.setFilter(FilterConstants.FILTER_27);
            click.set("type_id", 1);
            click.set("shop_id", shopId);
            click.set("Cookie", cookie);
            click.set("Pp", pp);
        }
        TestClick goodClick = clicks.get(0);
        goodClick.set("eventtime", timeOfClicks.minusMinutes(5));
        goodClick.setFilter(FilterConstants.FILTER_0);
        goodClick.set("Utm_Campaign", "goodClickFirstInBunch1");
        return clicks;
    }

    public List<TestClick> generateClicksWith24hourGap() {
        DateTime secondClickTime = timeOfClicks.minusMinutes(25);
        DateTime firstClickTime = secondClickTime.minusMinutes(25);
        int shopId = ShopId.generate();
        String cookie = Cookie.generateCookieForClickTime(firstClickTime);
        int pp = For06Filter.getRandomIncludePP();

        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, 3);
        for (TestClick click : clicks) {
            click.set("type_id", 1);
            click.set("shop_id", shopId);
            click.set("cookie", cookie);
            click.set("pp", pp);
        }

        clicks.get(0)
            .setNotFiltered()
            .set("eventtime", firstClickTime)
            .set("show_time", firstClickTime)
            .set("utm_campaign", "goodClick25minsBeforeNext");

        clicks.get(1)
            .setFilter(FilterConstants.FILTER_27)
            .set("eventtime", secondClickTime)
            .set("show_time", secondClickTime)
            .set("utm_campaign", "goodClick25minsBeforeNext");

        clicks.get(2)
            .setFilter(FilterConstants.FILTER_27)
            .set("utm_campaign", "goodClick25minsAfterFirst");
        return clicks;
    }

    public List<TestClick> generateClicksWithDiffPP() {
        int shopId = ShopId.generate();
        String cookie = Cookie.generateCookieForClickTime(timeOfClicks);
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, 3);
        Set<Integer> pps = new HashSet<>();
        for (TestClick click : clicks) {
            click.setFilter(FilterConstants.FILTER_0);
            click.set("type_id", 1);
            click.set("shop_id", shopId);
            click.set("Cookie", cookie);
            int pp = For06Filter.getRandomIncludePP();
            while (!pps.add(pp)) {
                pp = PP.getRandomMarketPP();
            }
            click.set("Pp", pp);
        }
        return clicks;
    }

    public List<TestClick> generateClicksForFilterWithEmptyCookieSameIp6() {
        int shopId = ShopId.generate();
        String ip = IP.generateValidNoYandexIPv4();
        int pp = For06Filter.getRandomIncludePP();
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, 5);
        for (TestClick click : clicks) {
            click.setFilter(FilterConstants.FILTER_27);
            click.set("type_id", 1);
            click.set("shop_id", shopId);
            click.set("Cookie", "");
            click.set("Ip", IP.atonIPv4(ip).toString());
            click.set("Pp", pp);
            click.set("Ip6", IP.getIPv6FromIPv4(ip));
        }
        TestClick goodClick = clicks.get(0);
        goodClick.set("eventtime", timeOfClicks.minusMinutes(5));
        goodClick.setFilter(FilterConstants.FILTER_0);
        goodClick.set("Utm_Campaign", "goodClickFirstInBunch2");
        return clicks;
    }

    public List<TestClick> generateClicksWithDiffShopIds() {
        String cookie = Cookie.generateCookieForClickTime(timeOfClicks);
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, 3);
        int pp = For06Filter.getRandomIncludePP();
        Set<Integer> shopids = new HashSet<>();
        while(shopids.size() != clicks.size()) {
            for (TestClick click : clicks) {
                click.setFilter(FilterConstants.FILTER_0);
                click.set("type_id", 1);
                click.set("Pp", pp);
                click.set("shop_id", ShopId.generate());
                click.set("Cookie", cookie);
                shopids.add(click.get("shop_id", Integer.class));
            }
        }
        return clicks;
    }

    public List<TestClick> generateClicksWithDiffCookie() {
        int shopId = ShopId.generate();
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, 3);
        int pp = For06Filter.getRandomIncludePP();
        for (TestClick click : clicks) {
            click.setFilter(FilterConstants.FILTER_0);
            click.set("type_id", 1);
            click.set("shop_id", ShopId.generate());
            click.set("Cookie", Cookie.generateCookieForClickTime(timeOfClicks));
            click.set("shop_id", shopId);
            click.set("Pp", pp);
        }
        return clicks;
    }

    public List<TestClick> generateClicksWithAnotherTypeId() {
        int shopId = ShopId.generate();
        String cookie = Cookie.generateCookieForClickTime(timeOfClicks);
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, 10);
        int pp = For06Filter.getRandomIncludePP();
        for (TestClick click : clicks) {
            click.setFilter(FilterConstants.FILTER_0);
            click.set("type_id", 0);
            click.set("shop_id", shopId);
            click.set("Cookie", cookie);
            click.set("Pp", pp);
        }
        return clicks;
    }
}
