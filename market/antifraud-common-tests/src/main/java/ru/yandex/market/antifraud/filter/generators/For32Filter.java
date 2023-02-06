package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.Referrer;
import ru.yandex.market.antifraud.filter.fields.ShopId;


import java.util.ArrayList;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 14.03.17.
 */
public class For32Filter implements FilterGenerator {

    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 32
        clicks.addAll(For32Filter.generateClicksCase1(timeOfClicks));
        clicks.addAll(For32Filter.generateClicksCase2(timeOfClicks));
        clicks.addAll(For32Filter.generateClicksCase3(timeOfClicks));
        clicks.addAll(For32Filter.generateClicksCase4(timeOfClicks));
        clicks.addAll(For32Filter.generateClicksCase5(timeOfClicks));
        return clicks;
    }

    public static List<TestClick> generateClicksCase1(DateTime timeOfClicks) {
        return presetFilter(generateClicksWithRatio(timeOfClicks, 1500, 90), FilterConstants.FILTER_0, "case1");
    }

    public static List<TestClick> generateClicksCase2(DateTime timeOfClicks) {
        String referer = Referrer.generate();
        List<TestClick> result = generateClicksWithRatio(timeOfClicks, referer, 1500, 90);
        result.add(generateBadClick(timeOfClicks, referer));
        return presetFilter(result, FilterConstants.FILTER_32, "case2");
    }

    public static List<TestClick> generateClicksCase3(DateTime timeOfClicks) {
        return presetFilter(generateClicksWithRatio(timeOfClicks, 1499, 91), FilterConstants.FILTER_0, "case3");
    }

    public static List<TestClick> generateClicksCase4(DateTime timeOfClicks) {
        DateTime timeOfClicks2 = timeOfClicks.minusMinutes(4);
        String referer = Referrer.generate();
        List<TestClick> result = new ArrayList<>();
        result.addAll(generateBadClicks(timeOfClicks, referer, 684));
        result.addAll(generateGoodClicks(timeOfClicks, referer, 67));
        result.addAll(generateBadClicks(timeOfClicks2, referer, 684));
        result.addAll(generateGoodClicks(timeOfClicks2, referer, 67));
        return presetFilter(result, FilterConstants.FILTER_32, "case4");
    }

    public static List<TestClick> generateClicksCase5(DateTime timeOfClicks) {
        DateTime timeOfClicks2 = timeOfClicks.minusHours(25);
        String referer = Referrer.generate();
        List<TestClick> result = new ArrayList<>();
        result.addAll(generateBadClicks(timeOfClicks, referer, 705));
        result.addAll(generateGoodClicks(timeOfClicks, referer, 69));
        result.addAll(generateBadClicks(timeOfClicks2, referer, 705));
        result.addAll(generateGoodClicks(timeOfClicks2, referer, 69));
        return presetFilter(result, FilterConstants.FILTER_0, "case5");
    }

    private static List<TestClick> presetFilter(List<TestClick> clicks, FilterConstants filter, String prefix) {
        clicks.forEach(it -> {
            it.setFilter(filter);
            it.set("rowid", prefix + "_" + it.get("rowid", String.class));
        });
        return clicks;
    }

    private static List<TestClick> generateClicksWithRatio(DateTime timeOfClicks, int count, int ratioInPercent) {
        return generateClicksWithRatio(timeOfClicks, Referrer.generate(), count, ratioInPercent);
    }

    private static List<TestClick> generateClicksWithRatio(DateTime timeOfClicks, String referer, int count, int ratioInPercent) {
        checkArgument(ratioInPercent >= 0 && ratioInPercent <= 100,
            "RatioInPercent must be between 0 an 100 (ratioInPercent: %s)", ratioInPercent);
        int goodClicksCount = (count * (100 - ratioInPercent) / 100);
        int badClicksCount = count - goodClicksCount;
        List<TestClick> result = generateGoodClicks(timeOfClicks, referer, goodClicksCount);
        result.addAll(generateBadClicks(timeOfClicks, referer, badClicksCount));
        return result;
    }

    private static List<TestClick> generateBadClicks(DateTime timeOfClicks, String referer, int count) {
        return generateClicks(() -> generateBadClick(timeOfClicks, referer), count);
    }

    private static List<TestClick> generateGoodClicks(DateTime timeOfClicks, String referer, int count) {
        return generateClicks(() -> generateGoodClick(timeOfClicks, referer), count);
    }

    private static List<TestClick> generateClicks(Supplier<TestClick> supplier, int count) {
        return Stream.generate(supplier).limit(count).collect(Collectors.toList());
    }

    private static TestClick generateBadClick(DateTime timeOfClicks, String referer) {
        return generateGoodClick(timeOfClicks, referer)
            .set("ip_geo_id", RndUtil.choice(new int[]{54, 2}));
    }

    private static TestClick generateGoodClick(DateTime timeOfClicks, String referer) {
        return ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0)
            .set("geo_id", 213)
            .set("shop_id", ShopId.generate(213))
            .set("ip_geo_id", 213)
            .set("Referer", referer);
    }
}
