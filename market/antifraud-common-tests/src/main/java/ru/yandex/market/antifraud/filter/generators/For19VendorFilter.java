package ru.yandex.market.antifraud.filter.generators;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static ru.yandex.market.antifraud.filter.fields.UTM.setUtmTerm;

/**
 * Created by kateleb on 26.06.19.
 */
public class For19VendorFilter implements FilterGenerator {
    private static final Set<Integer> EXCLUDE_PP = ImmutableSet.copyOf(Arrays.asList(
        480, 481, 482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 497, 498, 499,
        706, 707, 710, 713, 721, 722, 725, 726, 728, 730, 750, 760,
        980, 981, 990, 991,
        1000, 1001, 1002, 420
    ));

    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 19
        clicks.addAll(setUtmTerm(For19VendorFilter.generateClicksFor19FilterCase1(timeOfClicks), "case1"));
        clicks.addAll(setUtmTerm(For19VendorFilter.generateClicksFor19FilterCase2(timeOfClicks), "case2"));
        clicks.addAll(setUtmTerm(For19VendorFilter.generateClicksFor19FilterCase4(timeOfClicks), "case4"));
        clicks.addAll(setUtmTerm(For19VendorFilter.generateClicksFor19FilterCase7(timeOfClicks), "case7"));
        clicks.addAll(setUtmTerm(For19VendorFilter.generateClicksFor19FilterCase10(timeOfClicks), "case10"));
        clicks.addAll(setUtmTerm(For19VendorFilter.generateClicksFor19FilterCase13(timeOfClicks), "case13"));
        clicks.addAll(setUtmTerm(For19VendorFilter.generateClicksFor19FilterCase14(timeOfClicks), "case14"));
        return clicks;
    }


    public static List<TestClick> generateClicksFor19FilterCase1(DateTime timeOfClicks) {
        // 10 кликов с одного ip с пустым referer
        String ip = IP.generateValidIPv6();
        int pp = getRandomIncludePP();
        return IntStream.range(0, 10)
            .mapToObj(i -> generateClick(timeOfClicks, pp, "", ip, FilterConstants.FILTER_19))
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

    private static TestClick generateClick(DateTime timeOfClicks, int pp, String referer, String ip6, FilterConstants filter) {
        return generateClick(timeOfClicks, pp, referer, ip6, filter, Defaults.GEO_ID.value(Integer.class), Defaults.GEO_ID.value(Integer.class));
    }

    private static TestClick generateClick(DateTime timeOfClicks, int pp, String referer, String ip6, FilterConstants filter, int geoId, int ipGeoId) {
        TestClick click = ClickGenerator.generateUniqueVendorClicks(timeOfClicks, 1).get(0);
        click.set("pp", pp);
        click.set("ip6", ip6);
        click.set("referer", referer);
        click.set("geo_id", geoId);
        click.set("ip_geo_id", ipGeoId);
        click.setFilter(filter);
        return click;
    }
}
