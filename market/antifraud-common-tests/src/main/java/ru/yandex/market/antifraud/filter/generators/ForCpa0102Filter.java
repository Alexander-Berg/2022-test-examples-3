package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by kateleb on 07.09.16
 */
public class ForCpa0102Filter implements FilterGenerator {

    public List<TestClick> generateCpaFor01Filter() {
        //кликер для старья
        return cpasForClicker(false);
    }

    public List<TestClick> generateCpaFor02Filter() {
        //кликер для офферкард
        return cpasForClicker(true);
    }

    private List<TestClick> cpasForClicker(boolean isOffercard) {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики не подпадающие под фильтр 3 (По граничному условию)
        clicks.addAll(ForCpa0102Filter.generateCpaClicksLessThanThresholdValue(timeOfClicks, isOffercard));
        //Генерируем клики подпадающие под фильтр 3 (По граничному условию)
        clicks.addAll(ForCpa0102Filter.generateCpaClicksGreaterThanThresholdValue(timeOfClicks, isOffercard));
        //Генерируем клики подпадающие под фильтр 3 с пустым куки (По граничному условию)
        clicks.addAll(ForCpa0102Filter.generateCpaClicksWithEmptyCookie(timeOfClicks, isOffercard));
        //Генерируем клики подпадающие под фильтр 3
        clicks.addAll(ForCpa0102Filter.generateCpaClicks(timeOfClicks, 120, isOffercard));
        return clicks;
    }

    public static List<TestClick> generateCpaClicksGreaterThanThresholdValue(DateTime timeOfClicks, boolean isOffercard) {
        return generateCpaClicks(timeOfClicks, 101, isOffercard);
    }


    public static List<TestClick> generateCpaClicksWithEmptyCookie(DateTime timeOfClicks, boolean isOfferCard) {
        return generateClicksWithoutCookie(ClickGenerator.generateUniqueCpaClicksWithIp6Only(timeOfClicks, 101, isOfferCard), isOfferCard);
    }

    public static List<TestClick> generateCpaClicksLessThanThresholdValue(DateTime timeOfClicks, boolean isOffercard) {
        return generateCpaClicks(timeOfClicks, 100, isOffercard);
    }

    public static List<TestClick> generateCpaClicks(DateTime timeOfClicks, int count, boolean isOfferCard) {
        return generateClicksWithCookie(ClickGenerator.generateUniqueCpaClicks(timeOfClicks, count, isOfferCard), isOfferCard);
    }

    private static  List<TestClick> generateClicksWithoutCookie(List<TestClick> clicks, boolean isOfferCard) {
        FilterConstants filter = isOfferCard ? FilterConstants.FILTER_2 : FilterConstants.FILTER_1;
        clicks = clicks.stream().filter(it -> it != null).collect(Collectors.toList());
        int count = clicks.size();
        TestClick click = clicks.stream().filter(it -> it.get("ip6") != null && !it.get("ip6", String.class).trim().isEmpty()).findFirst().get();
        String ip4 = click.get("ip", String.class);
        String ip6 = click.get("ip6", String.class);
        return clicks.stream().map(it -> {
            it.set("cookie", "");
            it.set("ip", ip4);
            it.set("ip6", ip6);
            it.setFilter(count > 100 ? filter : FilterConstants.FILTER_0);
            return it;
        }).collect(Collectors.toList());
    }

    private static List<TestClick>  generateClicksWithCookie(List<TestClick> clicks, boolean isOfferCard) {
        FilterConstants filter = isOfferCard ? FilterConstants.FILTER_2 : FilterConstants.FILTER_1;
        clicks = clicks.stream().filter(it -> it != null).collect(Collectors.toList());
        int count = clicks.size();
        String cookie = clicks.stream().filter(it -> it.get("cookie") != null && !it.get("cookie", String.class).trim().isEmpty()).findFirst().get().get("cookie", String.class);
        return clicks.stream().map(it -> {
            it.set("cookie", cookie);
            it.setFilter(count > 100 ? filter : FilterConstants.FILTER_0);
            return it;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TestClick> generate() {
        throw new UnsupportedOperationException("Use exact generation method");
    }
}
