package ru.yandex.market.antifraud.filter.generators;

import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.ip.IP;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static ru.yandex.market.antifraud.filter.fields.UTM.setUtmTerm;

/**
 * Created by kateleb on 10.02.16.
 */
public class For24Filter implements FilterGenerator {
    private final String badIp;
    private String suspiciousIp;
    private String ipWithBorderDelay;

    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 24
        clicks.addAll(setUtmTerm(this.generateBadClicksForCase1(timeOfClicks), "case1"));
        clicks.addAll(setUtmTerm(this.generateClicksForCase2(timeOfClicks), "case2"));
        clicks.addAll(setUtmTerm(this.generateClicksForCase3(timeOfClicks), "case3"));
        clicks.addAll(setUtmTerm(this.generateClicksForCase4(timeOfClicks), "case4"));
        clicks.addAll(setUtmTerm(this.generateClicksForCase5(timeOfClicks), "case5"));
        clicks.addAll(setUtmTerm(this.generateClicksForCase6(timeOfClicks), "case6"));
        return clicks;
    }

    public For24Filter() {
        badIp = IP.generateValidNoYandexIPv4();
        suspiciousIp = IP.generateValidNoYandexIPv4();
        while (badIp.equals(suspiciousIp)) {
            suspiciousIp = IP.generateValidNoYandexIPv4();
        }
        ipWithBorderDelay = IP.generateValidNoYandexIPv4();
        while (ipWithBorderDelay.equals(suspiciousIp) || ipWithBorderDelay.equals(badIp)) {
            ipWithBorderDelay = IP.generateValidNoYandexIPv4();
        }
    }

    public List<TestClick> generateBadClicksForCase1(DateTime timeOfClicks) {
        List<TestClick> clicksFor24FilterTest = new ArrayList<>();
        //bad ip6 : having >=10 clicks and average click-show lag more than 4500
        List<TestClick> clicksForBadIp6 = generateGoodClicksIp6AndDelay(timeOfClicks, badIp, 10, 5000);
        //bad clicks: having pp=[37,38], referrer = "" and bad ip;
        List<TestClick> clicksWithBadIps = generateClicksWithMobilePP(badIp, timeOfClicks, "", FilterConstants.FILTER_24);
        clicksFor24FilterTest.addAll(clicksForBadIp6);
        clicksFor24FilterTest.addAll(clicksWithBadIps);
        return clicksFor24FilterTest;
    }

    public List<TestClick> generateClicksForCase2(DateTime timeOfClicks) {
        List<TestClick> clicksFor24FilterTest = new ArrayList<>();
        //not bad ip6 : having 7+2 <10 clicks (but and average click-show lag more than 4500)
        List<TestClick> clicksForBorderIp6 = generateGoodClicksIp6AndDelay(timeOfClicks, suspiciousIp, 7, 5000);
        List<TestClick> clicksWithBorderIps = generateClicksWithMobilePP(suspiciousIp, timeOfClicks, "", FilterConstants.FILTER_0);
        clicksFor24FilterTest.addAll(clicksWithBorderIps);
        clicksFor24FilterTest.addAll(clicksForBorderIp6);
        return clicksFor24FilterTest;
    }


    public List<TestClick> generateClicksForCase3(DateTime timeOfClicks) {
        List<TestClick> clicksFor24FilterTest = new ArrayList<>();
        //not bad ip6 : having >=10 clicks but and auverage click-show lag less than 4500)
        List<TestClick> clicksForNotBadIp6 = generateGoodClicksIp6AndDelay(timeOfClicks, ipWithBorderDelay, 11, 4000);
        List<TestClick> clicksWithNotBadIps = generateClicksWithMobilePP(ipWithBorderDelay, timeOfClicks, "", FilterConstants.FILTER_0);
        clicksFor24FilterTest.addAll(clicksForNotBadIp6);
        clicksFor24FilterTest.addAll(clicksWithNotBadIps);
        return clicksFor24FilterTest;
    }

    public List<TestClick> generateClicksForCase4(DateTime timeOfClicks) {
        //bad ip6 : having >=10 clicks but and click-show lag less than 4500
        //but not empty referrer
        List<TestClick> clicksWithNotEmptyReferrer = generateClicksWithMobilePP(badIp, timeOfClicks, "http://test-mstat-referrer", FilterConstants.FILTER_0);
        return clicksWithNotEmptyReferrer;
    }

    public List<TestClick> generateClicksForCase5(DateTime timeOfClicks) {
        //bad ip6 : having >=10 clicks but and click-show lag less than 4500
        //but not mobile pp
        List<TestClick> clicksWithNotMobilePP = generateClicksWithMobilePP(badIp, timeOfClicks, "", FilterConstants.FILTER_0);
        clicksWithNotMobilePP.stream().forEach(it -> it.set("Pp", 8));
        return clicksWithNotMobilePP;
    }

    public List<TestClick> generateClicksForCase6(DateTime timeOfClicks) {
        //bad ip6 : having >=10 clicks but and click-show lag less than 4500
        //but not target day pp
        List<TestClick> clicksAnotherDay = generateClicksWithMobilePP(badIp, timeOfClicks.minusDays(2), "", FilterConstants.FILTER_0);
        return clicksAnotherDay;
    }

    private List<TestClick> generateClicksWithMobilePP(String ip4, DateTime timeOfClicks, String referrer, FilterConstants filter) {
        return ClickGenerator.generateUniqueClicks(timeOfClicks, 2)
                .stream().map(it -> {
                    it.set("Ip", IP.atonIPv4(ip4).toString());
                    it.set("Ip6", IP.getIPv6FromIPv4(ip4));
                    it.set("Referer", referrer);
                    it.set("Pp", RandomUtils.nextBoolean() ? 37 : 38);
                    it.setFilter(filter);
                    //so that average click-show lag time for bad ip was not impacted
                    it.set("show_time", it.get("eventtime", DateTime.class).minusSeconds(4499));
                    return it;
                }).collect(toList());
    }

    private List<TestClick> generateGoodClicksIp6AndDelay(DateTime timeOfClicks, String badIp, int count, int delay) {
        return ClickGenerator.generateUniqueClicks(timeOfClicks, count)
                .stream().map(it -> {
                    it.set("Ip", IP.atonIPv4(badIp).toString());
                    it.set("Ip6", IP.getIPv6FromIPv4(badIp));
                    it.set("show_time", it.get("eventtime", DateTime.class).minusSeconds(delay));
                    it.setFilter(FilterConstants.FILTER_0);
                    return it;
                }).collect(toList());
    }


}
