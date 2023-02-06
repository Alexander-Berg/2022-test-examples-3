package ru.yandex.market.antifraud.filter.generators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import static java.util.stream.Collectors.toList;

/**
 * Created by kateleb on 22.02.19
 */
@Slf4j
public class For38Filter implements FilterGenerator {


    private static final int SAFE_COOKIE_DELAY_MINUTES = 30; // Старая кука - больше 30 минут


    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime().withMillisOfSecond(0);

        clicks.addAll(generateClicksCase1(timeOfClicks));
        clicks.addAll(generateClicksCase2(timeOfClicks));
        clicks.addAll(generateClicksCase3(timeOfClicks));
        clicks.addAll(generateClicksCase4(timeOfClicks));
        clicks.addAll(generateClicksCase5(timeOfClicks));
        clicks.addAll(generateClicksCase6(timeOfClicks));
        clicks.addAll(generateClicksCase7(timeOfClicks));
        return clicks;
    }


    private static List<TestClick> generateIpBadClicks(String ip, DateTime timeOfClicks, Integer cnt,
                                                       Integer distCookieCnt, Integer distRefCount, Integer distReqidCnt,
                                                       Integer cookieCreationDelay,
                                                       FilterConstants filter, String caseId, Integer pp) {
        List<TestClick> clicks = new ArrayList<>();
        List<String> cookies = Stream
            .generate(() -> Cookie.generateYandexCookie(timeOfClicks.minusMinutes(cookieCreationDelay)))
            .limit(distCookieCnt).collect(toList());

        List<String> requids = IntStream.range(0, distReqidCnt).mapToObj(i -> "requid_" + i).collect(toList());
        List<String> referers = IntStream.range(0, distRefCount).mapToObj(i -> "referer_" + i).collect(toList());

        for (int i = 0; i < cnt; i++) {
            String cookie = cookies.size() > i ? cookies.get(i) : cookies.get(0);
            String referer = referers.size() > i ? referers.get(i) : cookies.get(0);
            String reqid = requids.size() > i ? requids.get(i) : cookies.get(0);
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            click.set("cookie", cookie);
            click.set("ip6", ip);
            click.set("rowid", caseId + "_1_" + click.get("rowid", String.class));
            click.set("pp", pp);
            click.set("referer", referer);
            click.set("req_id", reqid);
            click.setFilter(filter);
            clicks.add(click);
        }
        return clicks;
    }


    private static List<TestClick> generateClicksCase1(DateTime timeOfClicks) {
        // ip плохой, если pp=21, куки  свежие
        // в группе (cookie-referer-req-ip) кликов/Nreferer>2 и Ncookie>2
        String caseId = "case1";
        String ipBad = "1::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();

        List<TestClick> forBadIps = generateIpBadClicks(ipBad, timeOfClicks, 10, 3, 3, 1,
            SAFE_COOKIE_DELAY_MINUTES - 10, FilterConstants.FILTER_38, caseId, 21);
        clicks.addAll(forBadIps);
        clicks.addAll(generateOtherIpClicks(ipBad, timeOfClicks, 1, forBadIps.get(0).get("cookie").toString(), FilterConstants.FILTER_38, caseId, 21));
        return clicks;
    }

    //не pp=21 у клика с плохим ip
    private static List<TestClick> generateClicksCase2(DateTime timeOfClicks) {
        // ip плохой, если pp=21, куки свежие
        // в группе (cookie-referer-req-ip) кликов/Nreferer>2 и Ncookie>2
        String caseId = "case2";
        String ipBad = "2::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();

        List<TestClick> forBadIps = generateIpBadClicks(ipBad, timeOfClicks, 10, 3, 3, 1,
            SAFE_COOKIE_DELAY_MINUTES - 10, FilterConstants.FILTER_38, caseId, 21);
        clicks.addAll(forBadIps);
        clicks.addAll(generateOtherIpClicks(ipBad, timeOfClicks, 1, forBadIps.get(0).get("cookie").toString(), FilterConstants.FILTER_0, caseId, 6));
        return clicks;
    }

    //мало разных кук
    private static List<TestClick> generateClicksCase3(DateTime timeOfClicks) {
        // ip плохой, если pp=21, куки  свежие
        // в группе (cookie-referer-req-ip) кликов/Nreferer>2 и Ncookie>2
        String caseId = "case3";
        String ipBad = "3::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();

        List<TestClick> forBadIps = generateIpBadClicks(ipBad, timeOfClicks, 10, 2, 3, 1,
            SAFE_COOKIE_DELAY_MINUTES - 10, FilterConstants.FILTER_0, caseId, 21);
        clicks.addAll(forBadIps);
        clicks.addAll(generateOtherIpClicks(ipBad, timeOfClicks, 1, forBadIps.get(0).get("cookie").toString(), FilterConstants.FILTER_0, caseId, 21));
        return clicks;
    }

    //мало кликов
    private static List<TestClick> generateClicksCase4(DateTime timeOfClicks) {
        // ip плохой, если pp=21, куки  свежие
        // в группе (cookie-referer-req-ip) кликов/Nreferer>2 и Ncookie>2
        String caseId = "case4";
        String ipBad = "4::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();

        List<TestClick> forBadIps = generateIpBadClicks(ipBad, timeOfClicks, 2, 2, 2, 1,
            SAFE_COOKIE_DELAY_MINUTES - 10, FilterConstants.FILTER_0, caseId, 21);
        clicks.addAll(forBadIps);
        clicks.addAll(generateOtherIpClicks(ipBad, timeOfClicks, 1, forBadIps.get(0).get("cookie").toString(), FilterConstants.FILTER_0, caseId, 21));
        return clicks;
    }

    //не свежие куки
    private static List<TestClick> generateClicksCase5(DateTime timeOfClicks) {
        // ip плохой, если pp=21, куки  свежие
        // в группе (cookie-referer-req-ip) кликов/Nreferer>2 и Ncookie>2
        String caseId = "case5";
        String ipBad = "5::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();

        List<TestClick> forBadIps = generateIpBadClicks(ipBad, timeOfClicks, 10, 3, 3, 1,
            SAFE_COOKIE_DELAY_MINUTES + 10, FilterConstants.FILTER_0, caseId, 21);
        clicks.addAll(forBadIps);
        clicks.addAll(generateOtherIpClicks(ipBad, timeOfClicks, 1, forBadIps.get(0).get("cookie").toString(), FilterConstants.FILTER_0, caseId, 21));
        return clicks;
    }

    //не свежие куки у клика с плохим ip
    private static List<TestClick> generateClicksCase6(DateTime timeOfClicks) {
        // ip плохой, если pp=21, куки  свежие
        // в группе (cookie-referer-req-ip) кликов/Nreferer>2 и Ncookie>2
        String caseId = "case6";
        String ipBad = "6::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();

        List<TestClick> forBadIps = generateIpBadClicks(ipBad, timeOfClicks, 10, 3, 3, 1,
            SAFE_COOKIE_DELAY_MINUTES - 10, FilterConstants.FILTER_38, caseId, 21);
        clicks.addAll(forBadIps);
        clicks.addAll(generateOtherIpClicks(ipBad, timeOfClicks, 2,
            Cookie.generateYandexCookie(timeOfClicks.minusMinutes(SAFE_COOKIE_DELAY_MINUTES + 10)), FilterConstants.FILTER_0, caseId, 21));
        return clicks;
    }

    //не pp=21 у кликов для плохого ip
    private static List<TestClick> generateClicksCase7(DateTime timeOfClicks) {
        // ip плохой, если pp=21, куки свежие
        // в группе (cookie-referer-req-ip) кликов/Nreferer>2 и Ncookie>2
        String caseId = "case7";
        String ipBad = "7::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();

        List<TestClick> forBadIps = generateIpBadClicks(ipBad, timeOfClicks, 10, 3, 3, 1,
            SAFE_COOKIE_DELAY_MINUTES - 10, FilterConstants.FILTER_0, caseId, 6);
        clicks.addAll(forBadIps);
        clicks.addAll(generateOtherIpClicks(ipBad, timeOfClicks, 1, forBadIps.get(0).get("cookie").toString(), FilterConstants.FILTER_0, caseId, 21));
        return clicks;
    }

    private static Collection<? extends TestClick> generateOtherIpClicks(String ipBad,
                                                                         DateTime timeOfClicks,
                                                                         int count, String cookie,
                                                                         FilterConstants filter,
                                                                         String caseId, int pp) {
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            click.set("cookie", cookie);
            click.set("ip6", ipBad);
            click.set("rowid", caseId + "_1_" + click.get("rowid", String.class));
            click.set("pp", pp);
            click.setFilter(filter);
            clicks.add(click);
        }
        return clicks;

    }

}
