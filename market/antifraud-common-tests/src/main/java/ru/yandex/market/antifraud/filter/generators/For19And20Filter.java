package ru.yandex.market.antifraud.filter.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;

import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.Defaults;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.Referrer;
import ru.yandex.market.antifraud.filter.ip.IP;

import static java.util.stream.Collectors.toList;
import static ru.yandex.market.antifraud.filter.fields.UTM.setUtmTerm;

/**
 * Created by entarrion on 15.09.15.
 */
public class For19And20Filter implements FilterGenerator {
    private static final Set<Integer> EXCLUDE_PP = ImmutableSet.copyOf(Arrays.asList(
        480, 481, 482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 497, 498, 499,
        706, 707, 710, 713, 721, 722, 725, 726, 728, 730, 750, 760,
        980, 981, 990, 991,
        1000, 1001, 1002, 420,
        307, 321, 322, 328, 330, 331, 332, 333, 334, 335, 350, 360,
        507, 521, 522, 528, 530, 531, 532, 533, 534, 535, 550, 560
    ));
    private static final int GEO_ID = 400;
    private static final int IP_GEO_ID = 402;
    private static final String SEARCH_APP_ICOOKIE = "2";
    private static final String SOME_ICOOKIE = "2000000000";

    public List<TestClick> generateFor19Filter() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 19
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor19FilterCase1(timeOfClicks), "case1"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor19FilterCase2(timeOfClicks), "case2"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor19FilterCase4(timeOfClicks), "case4"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor19FilterCase7(timeOfClicks), "case7"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor19FilterCase10(timeOfClicks), "case10"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor19FilterCase13(timeOfClicks), "case13"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor19FilterCase14(timeOfClicks), "case14"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor19FilterCase15(timeOfClicks), "case15"));
        return clicks;
    }

    public List<TestClick> generateFor20Filter() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 20
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase1(timeOfClicks), "case1"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase4(timeOfClicks), "case4"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase5(timeOfClicks), "case5"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase6(timeOfClicks), "case6"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase7(timeOfClicks), "case7"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase8(timeOfClicks), "case8"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase9(timeOfClicks), "case9"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase10(timeOfClicks), "case10"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase11(timeOfClicks), "case11"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase12(timeOfClicks), "case12"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase13(timeOfClicks), "case13"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase14(timeOfClicks), "case14"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase15(timeOfClicks), "case15"));
        clicks.addAll(setUtmTerm(For19And20Filter.generateClicksFor20FilterCase16(timeOfClicks), "case16"));
        return clicks;
    }

    public static List<TestClick> generateClicksFor19FilterCase1(DateTime timeOfClicks) {
        // 10 кликов с одного ip с пустым referer
        String ip = IP.generateValidIPv6();
        int pp = getRandomIncludePP();
        return IntStream.range(0, 10)
            .mapToObj(i -> generateClick(timeOfClicks, pp, "", ip, FilterConstants.FILTER_19, SOME_ICOOKIE))
            .collect(toList());
    }

    public static List<TestClick> generateClicksFor19FilterCase2(DateTime timeOfClicks) {
        // 10 кликов с одного ip с пустым referer but one click with other pp
        List<TestClick> clicks = generateClicksFor19FilterCase1(timeOfClicks);
        clicks.get(0).set("pp", getRandomIncludePP((int) clicks.get(1).get("pp")));
        clicks.forEach(TestClick::setNotFiltered);
        return clicks;
    }

    public static List<TestClick> generateClicksFor19FilterCase4(DateTime timeOfClicks) {
        // 9 кликов с одного ip с пустым referer
        return generateClicksFor19FilterCase1(timeOfClicks).stream()
            .limit(9)
            .peek(TestClick::setNotFiltered)
            .collect(toList());
    }

    public static List<TestClick> generateClicksFor19FilterCase7(DateTime timeOfClicks) {
        // 10 кликов с одного ip (8 с пустым referer и 2 с не пустым referer)
        List<TestClick> clicks = generateClicksFor19FilterCase1(timeOfClicks);
        clicks.stream().limit(2).forEach(click -> click.setNotFilteredByReasonOf("referer", Referrer.generate()));
        return clicks;
    }

    public static List<TestClick> generateClicksFor19FilterCase10(DateTime timeOfClicks) {
        // 10 кликов с одного ip (7 с пустым referer и 3 с не пустым referer)
        List<TestClick> clicks = generateClicksFor19FilterCase1(timeOfClicks);
        clicks.stream().limit(3).forEach(click -> click.set("referer", Referrer.generate()));
        clicks.forEach(TestClick::setNotFiltered);
        return clicks;
    }

    public static List<TestClick> generateClicksFor19FilterCase13(DateTime timeOfClicks) {
        // 10 кликов с одного ip (8 с пустым referer и 2 с не пустым referer) и с pp не из INCLUDE_PP
        int notIncludePP = getRandomNotIncludePP();
        return generateClicksFor19FilterCase7(timeOfClicks).stream()
            .peek(click -> click.setNotFilteredByReasonOf("pp", notIncludePP))
            .collect(toList());
    }

    public static List<TestClick> generateClicksFor19FilterCase14(DateTime timeOfClicks) {
        // 10 кликов с одного ip (8 с пустым referer и 2 с не пустым referer) и с pp не 420
        int notIncludePP = getNewNotIncludePP();
        return generateClicksFor19FilterCase7(timeOfClicks).stream()
            .peek(click -> click.setNotFilteredByReasonOf("pp", notIncludePP))
            .collect(toList());
    }

    public static List<TestClick> generateClicksFor19FilterCase15(DateTime timeOfClicks) {
        // 10 кликов с одного ip с пустым referer
        String ip = IP.generateValidIPv6();
        int pp = getRandomIncludePP();
        List<TestClick> list = IntStream.range(0, 10)
                .mapToObj(i -> generateClick(timeOfClicks, pp, "", ip, FilterConstants.FILTER_19, SOME_ICOOKIE))
                .collect(toList());
        // ещё 10 точно таких же кликов, но с кукой поискового приложения. Такие клики не берём в расчёт
        List<TestClick> list2 = IntStream.range(0, 10)
                .mapToObj(i -> generateClick(timeOfClicks, pp, "", ip, FilterConstants.FILTER_0, SEARCH_APP_ICOOKIE))
                .collect(toList());
        list.addAll(list2);
        return list;
    }

    public static List<TestClick> generateClicksFor20FilterCase1(DateTime timeOfClicks) {
        // 10 кликов с одного ip с пустым referer и geo_id!=ip_geo_id (подпадает под 19 фильтр)
        String ip = IP.getIPv6FromIPv4(IP.generateValidIPv4());
        int pp = getRandomIncludePP();
        return IntStream.range(0, 10)
            .mapToObj(i -> generateClick(timeOfClicks, pp, "", ip, FilterConstants.FILTER_19, GEO_ID, IP_GEO_ID, SOME_ICOOKIE))
            .collect(toList());
    }

    public static List<TestClick> generateClicksFor20FilterCase4(DateTime timeOfClicks) {
        // 9 кликов с одного ip с пустым referer
        return generateClicksFor20FilterCase1(timeOfClicks).stream()
            .limit(9)
            .peek(click -> click.setFilter(FilterConstants.FILTER_20))
            .collect(toList());
    }

    public static List<TestClick> generateClicksFor20FilterCase5(DateTime timeOfClicks) {
        // 9 кликов с одной подсети(ipv4) с пустым referer
        int pp = getRandomIncludePP();
        return generateIPv4FromSubnet(9).stream()
            .map(IP::getIPv6FromIPv4)
            .map(ip -> generateClick(timeOfClicks, pp, "", ip, FilterConstants.FILTER_20, GEO_ID, IP_GEO_ID, SOME_ICOOKIE))
            .collect(toList());
    }

    public static List<TestClick> generateClicksFor20FilterCase6(DateTime timeOfClicks) {
        // 9 кликов с одной подсети(ipv6) с пустым referer
        int pp = getRandomIncludePP();
        return generateIPv6FromSubnet(9).stream()
            .map(ip -> generateClick(timeOfClicks, pp, "", ip, FilterConstants.FILTER_20, GEO_ID, IP_GEO_ID, SOME_ICOOKIE))
            .collect(toList());
    }

    // todo: 10 кликов разных ip но из одной подсети

    public static List<TestClick> generateClicksFor20FilterCase7(DateTime timeOfClicks) {
        // 5 кликов с одного ip (4 с пустым referer и 1 с не пустым referer)
        List<TestClick> clicks = generateClicksFor20FilterCase4(timeOfClicks).subList(0, 5);
        clicks.get(0).setNotFilteredByReasonOf("referer", Referrer.generate());
        return clicks;
    }

    public static List<TestClick> generateClicksFor20FilterCase8(DateTime timeOfClicks) {
        // 5 кликов с одной подсети(ipv4) (4 с пустым referer и 1 с не пустым referer)
        List<TestClick> clicks = generateClicksFor20FilterCase5(timeOfClicks).subList(0, 5);
        clicks.get(0).setNotFilteredByReasonOf("referer", Referrer.generate());
        return clicks;
    }

    public static List<TestClick> generateClicksFor20FilterCase9(DateTime timeOfClicks) {
        // 5 кликов с одной подсети(ipv6) (4 с пустым referer и 1 с не пустым referer)
        List<TestClick> clicks = generateClicksFor20FilterCase6(timeOfClicks).subList(0, 5);
        clicks.get(0).setNotFilteredByReasonOf("referer", Referrer.generate());
        return clicks;
    }

    public static List<TestClick> generateClicksFor20FilterCase10(DateTime timeOfClicks) {
        // 9 кликов с одного ip (7 с пустым referer и 2 с не пустым referer)
        List<TestClick> clicks = generateClicksFor20FilterCase4(timeOfClicks);
        clicks.get(0).set("referer", Referrer.generate());
        clicks.get(1).set("referer", Referrer.generate());
        clicks.forEach(TestClick::setNotFiltered);
        return clicks;
    }

    public static List<TestClick> generateClicksFor20FilterCase11(DateTime timeOfClicks) {
        // 9 кликов с одной подсети(ipv4) (7 с пустым referer и 2 с не пустым referer)
        List<TestClick> clicks = generateClicksFor20FilterCase5(timeOfClicks);
        clicks.get(0).set("referer", Referrer.generate());
        clicks.get(1).set("referer", Referrer.generate());
        clicks.forEach(TestClick::setNotFiltered);
        return clicks;
    }

    public static List<TestClick> generateClicksFor20FilterCase12(DateTime timeOfClicks) {
        // 5 кликов с одной подсети(ipv6) (7 с пустым referer и 2 с не пустым referer)
        List<TestClick> clicks = generateClicksFor20FilterCase6(timeOfClicks);
        clicks.get(0).set("referer", Referrer.generate());
        clicks.get(1).set("referer", Referrer.generate());
        clicks.forEach(TestClick::setNotFiltered);
        return clicks;
    }

    public static List<TestClick> generateClicksFor20FilterCase13(DateTime timeOfClicks) {
        // 9 кликов с одного ip с пустым referer и с pp не из INCLUDE_PP
        int pp = getRandomNotIncludePP();
        return generateClicksFor20FilterCase4(timeOfClicks).stream()
            .peek(click -> click.setNotFilteredByReasonOf("pp", pp))
            .collect(toList());
    }
    public static List<TestClick> generateClicksFor20FilterCase16(DateTime timeOfClicks) {
        // 9 кликов с одного ip с пустым referer и с pp 420
        int pp = getNewNotIncludePP();
        return generateClicksFor20FilterCase4(timeOfClicks).stream()
            .peek(click -> click.setNotFilteredByReasonOf("pp", pp))
            .collect(toList());
    }


    public static List<TestClick> generateClicksFor20FilterCase14(DateTime timeOfClicks) {
        // 9 кликов с одной подсети(ipv4) с пустым referer и с pp не из INCLUDE_PP
        int pp = getRandomNotIncludePP();
        return generateClicksFor20FilterCase5(timeOfClicks).stream()
            .peek(click -> click.setNotFilteredByReasonOf("pp", pp))
            .collect(toList());
    }

    public static List<TestClick> generateClicksFor20FilterCase15(DateTime timeOfClicks) {
        // 9 кликов с одной подсети(ipv6) с пустым referer и с pp не из INCLUDE_PP
        int pp = getRandomNotIncludePP();
        return generateClicksFor20FilterCase6(timeOfClicks).stream()
            .peek(click -> click.setNotFilteredByReasonOf("pp", pp))
            .collect(toList());
    }

    private static Set<String> generateIPv4FromSubnet(int count) {
        return IP.generateRandomSetUniqueIp4FromRange(IP.generateValidIPv4() + "/24", count);
    }

    private static Set<String> generateIPv6FromSubnet(int count) {
        return IP.generateRandomSetUniqueIp6FromRange(IP.generateValidIPv6() + "/96", count);
    }

    private static int getRandomIncludePP(Integer... excludePp) {
        Set<Integer> excludeAllPp = Sets.union(EXCLUDE_PP, Sets.newHashSet(excludePp));
        int maxPp = Collections.max(excludeAllPp);
        return IntStream.range(0, 1000)
            .map(i -> RndUtil.nextInt(maxPp))
            .filter(pp -> !excludeAllPp.contains(pp))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("We have bad rnd util"));
    }

    private static int getRandomNotIncludePP() {
        return RndUtil.choice(EXCLUDE_PP);
    }

    private static int getNewNotIncludePP() {
        return 420;
    }

    private static TestClick generateClick(DateTime timeOfClicks, int pp, String referer, String ip6,
                                           FilterConstants filter, String icookie) {
        return generateClick(timeOfClicks, pp, referer, ip6, filter, Defaults.GEO_ID.value(Integer.class),
                Defaults.GEO_ID.value(Integer.class), icookie);
    }

    private static TestClick generateClick(DateTime timeOfClicks, int pp, String referer, String ip6,
                                           FilterConstants filter, int geoId, int ipGeoId, String icookie) {
        TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
        click.set("pp", pp);
        click.set("ip6", ip6);
        click.set("referer", referer);
        click.set("geo_id", geoId);
        click.set("ip_geo_id", ipGeoId);
        click.set("icookie", icookie);
        click.setFilter(filter);
        return click;
    }

    @Override
    public List<TestClick> generate() {
        throw new UnsupportedOperationException("Choose exact generator method");
    }
}
