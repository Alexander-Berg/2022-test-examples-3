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
 * Created by entarrion on 26.01.15.
 */
public class For02Filter implements FilterGenerator {

    @Override
    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 2 (По граничному условию)
        clicks.addAll(For02Filter.generateClicksType1(timeOfClicks, 2));
        clicks.addAll(For02Filter.generateClicksType2(timeOfClicks, 2));
        //Генерируем клики подпадающие под фильтр 2
        clicks.addAll(For02Filter.generateClicksType1(timeOfClicks, 15));
        clicks.addAll(For02Filter.generateClicksType2(timeOfClicks, 15));
        return clicks;
    }

    public static List<TestClick> generateClicksType1(DateTime timeOfClicks, int count) {
        return generateClicksType1(ClickGenerator.generateUniqueClicks(timeOfClicks, count));
    }

    public static List<TestClick> generateClicksType2(DateTime timeOfClicks, int count) {
        return generateClicksType2(ClickGenerator.generateUniqueClicks(timeOfClicks, count));
    }

    public static List<TestClick> generateClicksType1(List<TestClick> clicks) {
        String linkId = clicks.get(0).get("link_id", String.class);
        clicks = applyFunctionToClicks(clicks, withLinkIdWithFilter(linkId));
        clicks.get(0).setFilter(FilterConstants.FILTER_0);
        return clicks;
    }

    public static List<TestClick> generateClicksType2(List<TestClick> clicks) {
        String linkId = clicks.get(0).get("link_id", String.class);
        clicks = applyFunctionToClicks(clicks, withLinkIdAndEmptyCookieWithFilter(linkId));
        clicks.get(0).setFilter(FilterConstants.FILTER_0);
        return clicks;
    }

    private static List<TestClick> applyFunctionToClicks(List<TestClick> clicks, Function<TestClick, TestClick> func) {
        return clicks.stream()
                .sorted(Comparator.comparing(it -> it.get("eventtime", DateTime.class)))
                .sorted(Comparator.comparing(it -> it.get("rowid", String.class)))
                .map(it -> func.apply(it)).collect(Collectors.toList());
    }

    private static Function<TestClick, TestClick> withLinkIdWithFilter(final String linkId) {
        return it -> {
            it.set("link_id", linkId);
            it.setFilter(FilterConstants.FILTER_2);
            return it;
        };
    }

    private static Function<TestClick, TestClick> withLinkIdAndEmptyCookieWithFilter(final String linkId) {
        return withLinkIdWithFilter(linkId).andThen(it -> {
            it.set("ip", "");
            it.set("cookie", "");
            return it;
        });
    }
}
