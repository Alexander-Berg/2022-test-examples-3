package ru.yandex.market.antifraud.filter.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTime;

import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

/**
 * Created by entarrion on 26.01.15.
 */
public class For06Filter implements FilterGenerator {

    public static ImmutableList<Integer> INCLUDE_PP =
            ImmutableList.of(7, 28, 238, 239, 38, 48);

    public static Integer getRandomIncludePP() {
        return For06Filter.INCLUDE_PP.get(RandomUtils.nextInt(0, For06Filter.INCLUDE_PP.size()));
    }

    private static List<TestClick> generateClicksWithoutUuidWithoutCookie(String rowIdPrefix,
                                                                          DateTime timeOfClicks,
                                                                          boolean includePP,
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
            it.set("pp", includePP ? getRandomIncludePP() : 2);
            it.setFilter((count > 8 && !includePP) ? FilterConstants.FILTER_6 : FilterConstants.FILTER_0);
            return it;
        }).collect(Collectors.toList());
    }

    private static List<TestClick> generateClicksWithoutUuidWithCookie(String rowIdPrefix,
                                                                       DateTime timeOfClicks,
                                                                       int pp,
                                                                       int clicksCount) {
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(rowIdPrefix, timeOfClicks, clicksCount);
        clicks = clicks.stream().filter(Objects::nonNull).collect(Collectors.toList());
        int count = clicks.size();
        String cookie = clicks.stream().filter(it -> it.get("cookie", String.class) != null && !it.get("cookie",
                String.class).trim().isEmpty()).findFirst().get().get("cookie", String.class);
        return clicks.stream().map(it -> {
            it.set("Uuid", "undefined");
            it.set("cookie", cookie);
            it.set("pp", pp);
            it.setFilter(count > 8 ? FilterConstants.FILTER_6 : FilterConstants.FILTER_0);
            return it;
        }).collect(Collectors.toList());
    }

    private static List<TestClick> generateClicksWithOneUuid(String rowIdPrefix,
                                                             DateTime timeOfClicks,
                                                             int pp,
                                                             int clicksCount) {
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(rowIdPrefix, timeOfClicks, clicksCount);
        clicks = clicks.stream().filter(Objects::nonNull).collect(Collectors.toList());
        int count = clicks.size();
        String uuid = clicks.stream().filter(it -> it.get("Uuid", String.class) != null && !it.get("Uuid",
                String.class).trim().isEmpty()).findFirst().get().get("cookie", String.class);
        return clicks.stream().map(it -> {
            it.set("Uuid", uuid);
            it.set("pp", pp);
            it.setFilter(count > 8 ? FilterConstants.FILTER_6 : FilterConstants.FILTER_0);
            return it;
        }).collect(Collectors.toList());
    }


    @Override
    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();

        //Генерируем клики не подпадающие под фильтр 6 с uuid (По граничному условию)
        clicks.addAll(For06Filter.generateClicksWithOneUuid("case1", timeOfClicks, 2, 8));
        //Генерируем клики подпадающие под фильтр 6 с uuid (По граничному условию)
        clicks.addAll(For06Filter.generateClicksWithOneUuid("case2", timeOfClicks, 2, 10));

        //Генерируем клики не подпадающие под фильтр 6 с пустым uuid, но заполненным cookie (По граничному условию)
        clicks.addAll(For06Filter.generateClicksWithoutUuidWithCookie("case3", timeOfClicks, 2, 8));
        //Генерируем клики подпадающие под фильтр 6 с пустым uuid, но заполненным cookie (По граничному условию)
        clicks.addAll(For06Filter.generateClicksWithoutUuidWithCookie("case4", timeOfClicks, 2, 10));

        //Генерируем клики не подпадающие под фильтр 6 с пустыми uuid и cookie (По граничному условию)
        clicks.addAll(For06Filter.generateClicksWithoutUuidWithoutCookie("case6", timeOfClicks, false, 8));
        //Генерируем клики подпадающие под фильтр 6 с пустыми uuid и cookie (По граничному условию)
        clicks.addAll(For06Filter.generateClicksWithoutUuidWithoutCookie("case7", timeOfClicks, false, 10));


        //Генерируем клики не подпадающие под фильтр 6 по PP
        clicks.addAll(For06Filter.generateClicksWithoutUuidWithoutCookie("case8", timeOfClicks, true, 20));

        return clicks;
    }
}
