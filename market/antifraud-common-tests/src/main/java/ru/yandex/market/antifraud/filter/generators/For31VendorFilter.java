package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 30.01.17.
 */
public class For31VendorFilter implements FilterGenerator {
    public static List<TestClick> generateClicksType1(DateTime timeOfClicks, int count) {
        return generateClicksType1(ClickGenerator.generateUniqueVendorClicks(timeOfClicks, count));
    }

    public static List<TestClick> generateClicksType2(DateTime timeOfClicks, int count) {
        return generateClicksType2(ClickGenerator.generateUniqueVendorClicks(timeOfClicks, count));
    }

    public static List<TestClick> generateClicksType1(List<TestClick> clicks) {
        String showUid = clicks.get(0).get("show_uid", String.class);
        clicks = applyFunctionToClicks(clicks, withShowUidWithFilter(showUid));
        clicks.get(0).setFilter(FilterConstants.FILTER_0);
        return clicks;
    }

    public static List<TestClick> generateClicksType2(List<TestClick> clicks) {
        String showUid = clicks.get(0).get("show_uid", String.class);
        clicks = applyFunctionToClicks(clicks, withShowUidAndEmptyCookieWithFilter(showUid));
        clicks.get(0).setFilter(FilterConstants.FILTER_0);
        return clicks;
    }

    private static List<TestClick> applyFunctionToClicks(List<TestClick> clicks, Function<TestClick, TestClick> func) {
        return clicks.stream()
                .sorted(Comparator.comparing(it -> it.get("eventtime", DateTime.class)))
                .sorted(Comparator.comparing(it -> it.get("rowid", String.class)))
                .map(func::apply).collect(Collectors.toList());
    }

    private static Function<TestClick, TestClick> withShowUidWithFilter(final String showUid) {
        return it -> {
            it.set("show_uid", showUid);
            it.setFilter(FilterConstants.FILTER_31);
            return it;
        };
    }

    private static Function<TestClick, TestClick> withShowUidAndEmptyCookieWithFilter(final String linkId) {
        return withShowUidWithFilter(linkId).andThen(it -> {
            it.set("ip", "");
            it.set("cookie", "");
            return it;
        });
    }

    @Override
    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 31 (По граничному условию)
        clicks.addAll(For31VendorFilter.generateClicksType1(timeOfClicks, 2));
        clicks.addAll(For31VendorFilter.generateClicksType2(timeOfClicks, 2));
        //Генерируем клики подпадающие под фильтр 31
        clicks.addAll(For31VendorFilter.generateClicksType1(timeOfClicks, 15));
        clicks.addAll(For31VendorFilter.generateClicksType2(timeOfClicks, 15));
        return clicks;
    }
}
