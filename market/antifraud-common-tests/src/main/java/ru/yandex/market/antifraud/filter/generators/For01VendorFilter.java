package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 30.01.17.
 */
public class For01VendorFilter implements FilterGenerator {
    //https://wiki.yandex-team.ru/market/informationarchitecture/marketstat/antifraud: 01
    public static List<TestClick> generateClicksWithFilter(DateTime timeOfClicks, int count) {
        return generateClicksWithFilter(ClickGenerator.generateUniqueVendorClicks(timeOfClicks, count));
    }

    public static List<TestClick> generateClicksWithEmptyCookieWithFilter(DateTime timeOfClicks, int count) {
        return generateClicksWithEmptyCookieWithFilter(ClickGenerator.generateUniqueVendorClicks(timeOfClicks, count));
    }

    public static List<TestClick> generateClicksWithoutFilterType1(DateTime timeOfClicks, int count) {
        return generateClicksWithoutFilterType1(ClickGenerator.generateUniqueVendorClicks(timeOfClicks, count));
    }

    public static List<TestClick> generateClicksWithoutFilterType2(DateTime timeOfClicks, int count) {
        return generateClicksWithoutFilterType2(ClickGenerator.generateUniqueVendorClicks(timeOfClicks, count));
    }

    public static List<TestClick> generateClicksWithFilter(List<TestClick> clicks) {
        return applyFunctionToClicks(clicks, withFilter());
    }

    public static List<TestClick> generateClicksWithEmptyCookieWithFilter(List<TestClick> clicks) {
        return applyFunctionToClicks(clicks, withEmptyCookieWithFilter());
    }

    public static List<TestClick> generateClicksWithoutFilterType1(List<TestClick> clicks) {
        return applyFunctionToClicks(clicks, withoutFilterType1());
    }

    public static List<TestClick> generateClicksWithoutFilterType2(List<TestClick> clicks) {
        return applyFunctionToClicks(clicks, withoutFilterType1());
    }

    private static List<TestClick> applyFunctionToClicks(List<TestClick> clicks, Function<TestClick, TestClick> func) {
        return clicks.stream().map(it -> func.apply(it)).collect(Collectors.toList());
    }

    private static Function<TestClick, TestClick> withoutFilterType1() {
        return it -> {
            it.set("show_time",
                    it.get("eventtime", DateTime.class).withMillisOfDay(0).minusDays(1).plusMinutes(1));
            it.setFilter(FilterConstants.FILTER_0);
            return it;
        };
    }

    private static Function<TestClick, TestClick> withFilter() {
        return it -> {
            it.set("show_time",
                    it.get("eventtime", DateTime.class).withMillisOfDay(0).minusDays(1).minusMinutes(1));
            it.setFilter(FilterConstants.FILTER_1);
            return it;
        };
    }

    private static Function<TestClick, TestClick> withEmptyCookieWithFilter() {
        return withFilter().andThen(it -> {
            it.set("ip", "");
            it.set("cookie", "");
            return it;
        });
    }

    @Override
    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 1
        clicks.addAll(For01VendorFilter.generateClicksWithFilter(timeOfClicks, 1));
        clicks.addAll(For01VendorFilter.generateClicksWithFilter(timeOfClicks, 15));
        clicks.addAll(For01VendorFilter.generateClicksWithEmptyCookieWithFilter(timeOfClicks, 1));
        clicks.addAll(For01VendorFilter.generateClicksWithEmptyCookieWithFilter(timeOfClicks, 15));
        clicks.addAll(For01VendorFilter.generateClicksWithoutFilterType1(timeOfClicks, 1));
        clicks.addAll(For01VendorFilter.generateClicksWithoutFilterType1(timeOfClicks, 15));
        clicks.addAll(For01VendorFilter.generateClicksWithoutFilterType2(timeOfClicks, 1));
        clicks.addAll(For01VendorFilter.generateClicksWithoutFilterType2(timeOfClicks, 15));
        return clicks;
    }
}