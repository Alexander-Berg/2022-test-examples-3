package ru.yandex.market.antifraud.filter.generators;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

/**
 * Created by kateleb on 30.01.19
 */
@Slf4j
public class For37Filter implements FilterGenerator {


    private static final int SAFE_COOKIE_DELAY_MINUTES = 30*24*60; // Старая кука - меньше 30 дней от роду


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
        clicks.addAll(generateClicksCase8(timeOfClicks));
        return clicks;
    }



    private static List<TestClick> generateIpBadClicks(String ip, DateTime timeOfClicks, Integer cnt,
                                                       Integer cookieCreationDelay,
                                                       FilterConstants filter, String caseId, Integer pp, Integer typeid) {
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < cnt; i++) {
            String cookie = Cookie.generateYandexCookie(timeOfClicks.minusMinutes(cookieCreationDelay));
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            click.set("cookie", cookie);
            click.set("ip6", ip);
            click.set("rowid", caseId + "_1_" + click.get("rowid", String.class));
            click.set("pp", pp);
            click.set("type_id", typeid);
            click.setFilter(filter);
            clicks.add(click);
        }
        return clicks;
    }

    private static List<TestClick> generateIpBadClicksSameCookie(String ip, DateTime timeOfClicks, Integer cnt,
                                                       Integer cookieCreationDelay,
                                                       FilterConstants filter, String caseId, Integer pp, Integer typeid) {
        List<TestClick> clicks = new ArrayList<>();
        String cookie = Cookie.generateYandexCookie(timeOfClicks.minusMinutes(cookieCreationDelay));
        for (int i = 0; i < cnt; i++) {
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            click.set("cookie", cookie);
            click.set("ip6", ip);
            click.set("rowid", caseId + "_1_" + click.get("rowid", String.class));
            click.set("pp", pp);
            click.set("type_id", typeid);
            click.setFilter(filter);
            clicks.add(click);
        }
        return clicks;
    }

    private static List<TestClick> generateIpOKClicks(String ip, DateTime timeOfClicks, Integer cnt,
                                                      FilterConstants filter, String caseId, Integer pp, Integer typeid) {
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < cnt; i++) {
            String cookie = Cookie.generateYandexCookie(timeOfClicks.minusDays(50));
            TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
            click.set("cookie", cookie);
            click.set("ip6", ip);
            click.set("rowid", caseId + "_1_" + click.get("rowid", String.class));
            click.set("pp", pp);
            click.set("type_id", typeid);
            click.setFilter(filter);
            clicks.add(click);
        }
        return clicks;
    }




    private static List<TestClick> generateClicksCase1(DateTime timeOfClicks) {
        //10/11 кликов c ip  - со старой кукой, откатываем только клики с пп=21 и type_id=1
        String caseId = "10of11oldDiffPP";
        String ipBad = "1::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();
        clicks.addAll(generateIpBadClicks(ipBad, timeOfClicks, 10, SAFE_COOKIE_DELAY_MINUTES-60, FilterConstants.FILTER_0, caseId, 6, 1));
        clicks.addAll(generateIpOKClicks(ipBad, timeOfClicks, 1, FilterConstants.FILTER_37, caseId, 21, 1));
        return clicks;
    }

    private static List<TestClick> generateClicksCase2(DateTime timeOfClicks) {
        //10/11 кликов c ip  - со старой кукой, откатываем только клики с пп=21 и type_id=1
        String caseId = "10of11oldPP21";
        String ipBad = "2::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();
        clicks.addAll(generateIpBadClicks(ipBad, timeOfClicks, 10, SAFE_COOKIE_DELAY_MINUTES-60, FilterConstants.FILTER_37, caseId, 21, 1));
        clicks.addAll(generateIpOKClicks(ipBad, timeOfClicks, 1, FilterConstants.FILTER_37, caseId, 21, 1));
        return clicks;
    }

    private static List<TestClick> generateClicksCase3(DateTime timeOfClicks) {
        //10/11 кликов c ip  - с почти старой кукой (но нет), в итоге не откатываем
        String caseId = "noOldPP21";
        String ipBad = "3::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();
        clicks.addAll(generateIpBadClicks(ipBad, timeOfClicks, 10, SAFE_COOKIE_DELAY_MINUTES+60, FilterConstants.FILTER_0, caseId, 21, 1));
        clicks.addAll(generateIpOKClicks(ipBad, timeOfClicks, 1, FilterConstants.FILTER_0, caseId, 21, 1));
        return clicks;
    }


    private static List<TestClick> generateClicksCase4(DateTime timeOfClicks) {
        //10/11 кликов c ip - со старой кукой, но нет кликов с нужными pp+type_id
        String caseId = "10of11oldPP21TypeId2";
        String ipBad = "4::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();
        clicks.addAll(generateIpBadClicks(ipBad, timeOfClicks, 10, SAFE_COOKIE_DELAY_MINUTES-60, FilterConstants.FILTER_0, caseId, 6, 1));
        clicks.addAll(generateIpOKClicks(ipBad, timeOfClicks, 1, FilterConstants.FILTER_0, caseId, 21, 2));
        return clicks;
    }

    private static List<TestClick> generateClicksCase5(DateTime timeOfClicks) {
        //10/11 кликов c ip - со старой кукой, откатываем только клики с type_id = 1
        String caseId = "10of11oldPP21TypeId2and1";
        String ipBad = "5::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();
        clicks.addAll(generateIpBadClicks(ipBad, timeOfClicks, 10, SAFE_COOKIE_DELAY_MINUTES-60, FilterConstants.FILTER_37, caseId, 21, 1));
        clicks.addAll(generateIpOKClicks(ipBad, timeOfClicks, 1, FilterConstants.FILTER_0, caseId, 21, 2));
        return clicks;
    }

    private static List<TestClick> generateClicksCase6(DateTime timeOfClicks) {
        //8/11 кликов c ip - со старой кукой, нужно больше 0.9, не откатываем
        String caseId = "8of11old";
        String ipBad = "6::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();
        clicks.addAll(generateIpBadClicks(ipBad, timeOfClicks, 8, SAFE_COOKIE_DELAY_MINUTES-60, FilterConstants.FILTER_0, caseId, 21, 1));
        clicks.addAll(generateIpOKClicks(ipBad, timeOfClicks, 3, FilterConstants.FILTER_0, caseId, 21, 1));
        return clicks;
    }

    private static List<TestClick> generateClicksCase7(DateTime timeOfClicks) {
        //9/10 кликов c ip - со старой кукой, нужно больше 10 кликов и больше 0.9 плохих, не откатываем
        String caseId = "9of10old";
        String ipBad = "7::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();
        clicks.addAll(generateIpBadClicks(ipBad, timeOfClicks, 9, SAFE_COOKIE_DELAY_MINUTES-60, FilterConstants.FILTER_0, caseId, 21, 1));
        clicks.addAll(generateIpOKClicks(ipBad, timeOfClicks, 1, FilterConstants.FILTER_0, caseId, 21, 1));
        return clicks;
    }

    private static List<TestClick> generateClicksCase8(DateTime timeOfClicks) {
        //"Плохих" кликов больше 10, но все с одной кукой, считаем как 1 => нет кворума, не откатываем
        String caseId = "10of11oldSameCookie";
        String ipBad = "8::00:00:11:22:33";
        List<TestClick> clicks = new ArrayList<>();
        clicks.addAll(generateIpBadClicksSameCookie(ipBad, timeOfClicks, 10, SAFE_COOKIE_DELAY_MINUTES-60, FilterConstants.FILTER_0, caseId, 21, 1));
        clicks.addAll(generateIpOKClicks(ipBad, timeOfClicks, 1, FilterConstants.FILTER_0, caseId, 21, 1));
        return clicks;
    }


}
