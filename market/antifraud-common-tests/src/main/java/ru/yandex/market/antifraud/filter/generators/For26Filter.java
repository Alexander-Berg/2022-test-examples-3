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

import static ru.yandex.market.antifraud.filter.fields.UTM.setUtmTerm;

/**
 * Created by kateleb on 20.06.16
 */
public class For26Filter implements FilterGenerator {
    public static String CLICK_DAEMON_ERROR_REFERRER = "market-click2.yandex.ru";
    private DateTime timeOfClicks;

    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        clicks.addAll(setUtmTerm(this.generateClicksForFilterForMergedConditions(), "allConditionAtOnce"));
        clicks.addAll(setUtmTerm(this.generateClicksForRefererWithoutSlash(), "allConditionAtOnceNoSlash"));
        clicks.addAll(setUtmTerm(this.generateClicksForFilterForSeparateConditions(), "diffConditionsFofDiffClicks"));
        clicks.addAll(setUtmTerm(this.generateClicksForSameSubnet(), "notEnoughDifferentSubnets"));
        clicks.addAll(setUtmTerm(this.generateClicksForBorderReferrerConditions(), "notEnoughBadReferrers"));
        clicks.addAll(setUtmTerm(this.generateClicksForSeparateDays(), "allConditionsButInSeparateDays"));
        return clicks;
    }

    public For26Filter(DateTime dateTime) {
        this.timeOfClicks = dateTime;
    }

    /*req_id — псевдоуникальный индектификатор блока показов
     реферер "market-click2.yandex.ru" будет у кликов, на которые кликдемон показал ошибку (невозможно расшифровать и т.п.)
    Откатываем, если
      на один req_id приходится минимум 2 клика из разных подсетей
      И минимум 3 клика с реферером "market-click2.yandex.ru".
      в рамках одного календарного дня*/

    private static List<TestClick> generateUniqueClicks(DateTime timeOfClicks, int count) {
        return ClickGenerator.generateUniqueClicks(timeOfClicks, count);
    }

    public List<TestClick> generateClicksForFilterForSeparateConditions() {
        String reqId = generateReqId();
        List<String> ips = new ArrayList<>(IP.generateIpFromDifferentSubnets(2));
        List<TestClick> clicks = generateUniqueClicks(timeOfClicks, 10);
        for (int i = 0; i < clicks.size(); i++) {
            TestClick click = clicks.get(i);
            click.setFilter(FilterConstants.FILTER_26);
            click.set("req_id", reqId);
            if (i < 2) { // 2 clicks from different subnets
                click.set("Ip", IP.atonIPv4(ips.get(i)).toString());
                click.set("Ip6", IP.getIPv6FromIPv4(ips.get(i)));
                click.set("Utm_Campaign", "clickFromSubnet_" + i);
            } else if (i >= 2 && i < 5) { //3 clicks with clickdaemon error
                click.set("Referer", clickDaemonErrorReferrer());
                click.set("Utm_Campaign", "clickDaemonReferer");
            } else { // 5 regular clicks having some reqid
                click.set("Utm_Campaign", "otherClicksWithBadReqId");
            }
        }
        return clicks;
    }

    public List<TestClick> generateClicksForFilterForMergedConditions() {
        String reqId = generateReqId();
        List<String> ips = new ArrayList<>(IP.generateIpFromDifferentSubnets(3));
        List<TestClick> clicks = generateUniqueClicks(timeOfClicks, 3);
        for (int i = 0; i < clicks.size(); i++) {
            TestClick click = clicks.get(i);
            click.setFilter(FilterConstants.FILTER_26);
            click.set("req_id", reqId);
            click.set("ip6", IP.getIPv6FromIPv4(ips.get(i)));
            click.set("utm_campaign", "clickForSubnet_" + i);
            click.set("referer", clickDaemonErrorReferrer());
        }
        return clicks;
    }

    public List<TestClick> generateClicksForRefererWithoutSlash() {
        List<TestClick> clicks = generateClicksForFilterForMergedConditions();
        clicks.forEach(click -> click.setNotFilteredByReasonOf("referer", "http://" + CLICK_DAEMON_ERROR_REFERRER));
        return clicks;
    }

    public List<TestClick> generateClicksForSameSubnet() {
        String reqId = generateReqId();
        String ip = IP.getIPv6FromIPv4(IP.generateValidNoYandexIPv4());
        List<TestClick> clicks = generateUniqueClicks(timeOfClicks, 4);

        for (TestClick click: clicks) {
            click.setFilter(FilterConstants.FILTER_0);
            click.set("req_id", reqId);
            click.set("ip6", ip);
            click.set("referer", clickDaemonErrorReferrer());
        }
        return clicks;
    }

    public List<TestClick> generateClicksForBorderReferrerConditions() {
        String reqId = generateReqId();
        List<String> ips = new ArrayList<>(IP.generateIpFromDifferentSubnets(2));
        List<TestClick> clicks = generateUniqueClicks(timeOfClicks, 4);
        for (int i = 0; i < clicks.size(); i++) {
            TestClick click = clicks.get(i);
            click.setFilter(FilterConstants.FILTER_0);
            click.set("req_id", reqId);
            if (i < 2) { // 2 clicks from different subnets
                click.set("Ip", IP.atonIPv4(ips.get(i)).toString());
                click.set("Ip6", IP.getIPv6FromIPv4(ips.get(i)));
                click.set("Utm_Campaign", "clickFromSubnet_" + i);
            } else { // 2 clicks with clickdaemon error
                click.set("Referer", clickDaemonErrorReferrer());
            }
        }
        return clicks;
    }

    public List<TestClick> generateClicksForSeparateDays() {
        String reqId = generateReqId();
        List<String> ips = new ArrayList<>(IP.generateIpFromDifferentSubnets(3));
        List<TestClick> clicks = generateUniqueClicks(timeOfClicks, 6);
        DateTime anotherDay = timeOfClicks.minusHours(25);

        for (int i = 0; i < clicks.size(); i++) {
            TestClick click = clicks.get(i);
            click.setFilter(FilterConstants.FILTER_0);
            click.set("req_id", reqId);
            if (i < 2) { // 2 clicks from different subnets for this day
                click.set("ip6", IP.getIPv6FromIPv4(ips.get(i)));
                click.set("utm_campaign", "clickFromSubnet_" + i);
            } else if (i < 4) { //2 clicks with clickdaemon error for one day
                click.set("referer", clickDaemonErrorReferrer());
                click.set("utm_campaign", "badRefererForThisDay");
            } else { //2 clicks with clickdaemon error for another day
                click.set("referer", clickDaemonErrorReferrer());
                click.set("eventtime", anotherDay);
                click.set("show_time", anotherDay);
                click.set("utm_campaign", "badRefererForAnotherDay");
            }
        }
        return clicks;
    }

    private String clickDaemonErrorReferrer() {
        return (RandomUtils.nextBoolean() ? "http://" : "https://") + CLICK_DAEMON_ERROR_REFERRER + "/" + 7;
    }

    private static int reqIdCounter = 0;
    private static String generateReqId() {
        return "test_req_id_" + (++reqIdCounter);
    }
}
