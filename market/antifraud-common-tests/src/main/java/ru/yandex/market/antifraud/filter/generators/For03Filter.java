package ru.yandex.market.antifraud.filter.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.joda.time.DateTime;

import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

/**
 * Created by entarrion on 26.01.15.
 */
public class For03Filter implements FilterGenerator {

    private static List<TestClick> generateClicksWithoutUuidWithoutCookie(String rowIdPrefix,
                                                                          DateTime timeOfClicks,
                                                                          int clicksCount) {
        List<TestClick> clicks = ClickGenerator
                .generateUniqueClicksWithIp6Only(rowIdPrefix, timeOfClicks, clicksCount)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        int count = clicks.size();
        TestClick click = clicks.stream().filter(it -> it.get("ip6", String.class) != null && !it.get("ip6",
                String.class).trim().isEmpty()).findFirst().get();
        String ip4 = click.get("ip", String.class);
        String ip6 = click.get("ip6", String.class);
        return clicks.stream().map(it -> {
            it.set("Uuid", "undefined");
            it.set("cookie", "");
            it.set("ip", ip4);
            it.set("ip6", ip6);
            it.setFilter(count > 100 ? FilterConstants.FILTER_3 : FilterConstants.FILTER_0);
            return it;
        }).collect(Collectors.toList());
    }

    private static List<TestClick> generateClicksWithoutUuidWithCookie(String rowIdPrefix,
                                                                       DateTime timeOfClicks,
                                                                       int clicksCount) {
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(rowIdPrefix, timeOfClicks, clicksCount);
        clicks = clicks.stream().filter(Objects::nonNull).collect(Collectors.toList());
        int count = clicks.size();
        String cookie = clicks.stream().filter(it -> it.get("cookie", String.class) != null && !it.get("cookie",
                String.class).trim().isEmpty()).findFirst().get().get("cookie", String.class);
        return clicks.stream().map(it -> {
            it.set("Uuid", "undefined");
            it.set("cookie", cookie);
            it.setFilter(count > 100 ? FilterConstants.FILTER_3 : FilterConstants.FILTER_0);
            return it;
        }).collect(Collectors.toList());
    }

    private static List<TestClick> generateClicksWithOneUuid(String rowIdPrefix,
                                                             DateTime timeOfClicks,
                                                             int clicksCount) {
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(rowIdPrefix, timeOfClicks, clicksCount);
        clicks = clicks.stream().filter(Objects::nonNull).collect(Collectors.toList());
        int count = clicks.size();
        String uuid = clicks.stream().filter(it -> it.get("Uuid", String.class) != null && !it.get("Uuid",
                String.class).trim().isEmpty()).findFirst().get().get("cookie", String.class);
        return clicks.stream().map(it -> {
            it.set("Uuid", uuid);
            it.setFilter(count > 100 ? FilterConstants.FILTER_3 : FilterConstants.FILTER_0);
            return it;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();

        //Генерируем клики не подпадающие под фильтр 3 c uuid (По граничному условию)
        clicks.addAll(For03Filter.generateClicksWithOneUuid("case1", timeOfClicks, 100));
        //Генерируем клики подпадающие под фильтр 3 c uuid (По граничному условию)
        clicks.addAll(For03Filter.generateClicksWithOneUuid("case2", timeOfClicks, 120));

        //Генерируем клики не подпадающие под фильтр 3 с пустым uuid, но заполненной cookie (По граничному условию)
        clicks.addAll(For03Filter.generateClicksWithoutUuidWithCookie("case3", timeOfClicks, 100));
        //Генерируем клики подпадающие под фильтр 3 с пустым uuid, но заполненной cookie (По граничному условию)
        clicks.addAll(For03Filter.generateClicksWithoutUuidWithCookie("case4", timeOfClicks, 120));

        //Генерируем клики не подпадающие под фильтр 3 с пустыми uuid и cookie (По граничному условию)
        clicks.addAll(For03Filter.generateClicksWithoutUuidWithoutCookie("case5", timeOfClicks, 100));
        //Генерируем клики подпадающие под фильтр 3 с пустыми uuid и cookie (По граничному условию)
        clicks.addAll(For03Filter.generateClicksWithoutUuidWithoutCookie("case6", timeOfClicks, 120));

        return clicks;
    }

}
