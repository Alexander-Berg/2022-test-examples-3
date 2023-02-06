package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import java.util.ArrayList;
import java.util.List;

public class For01Filter implements FilterGenerator {

    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 1
        clicks.addAll(For01Filter.generateClicksWithFilter(timeOfClicks, 1));
        clicks.addAll(For01Filter.generateClicksWithFilter(timeOfClicks, 15));
        clicks.addAll(For01Filter.generateClicksWithEmptyCookieWithFilter(timeOfClicks, 1));
        clicks.addAll(For01Filter.generateClicksWithEmptyCookieWithFilter(timeOfClicks, 15));
        clicks.addAll(For01Filter.generateClicksWithoutFilterType1(timeOfClicks, 1));
        clicks.addAll(For01Filter.generateClicksWithoutFilterType1(timeOfClicks, 15));
        clicks.addAll(For01Filter.generateClicksWithoutFilterType2(timeOfClicks, 1));
        clicks.addAll(For01Filter.generateClicksWithoutFilterType2(timeOfClicks, 15));
        return clicks;
    }

    //https://wiki.yandex-team.ru/market/informationarchitecture/marketstat/antifraud: 01
    public static List<TestClick> generateClicksWithFilter(DateTime timeOfClicks, int count) {
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, count);
        clicks.forEach(For01Filter::withFilter);
        return clicks;
    }

    private static List<TestClick> generateClicksWithEmptyCookieWithFilter(DateTime timeOfClicks, int count) {
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, count);
        clicks.forEach(For01Filter::withFilter);
        clicks.forEach(For01Filter::emptyCookie);
        return clicks;
    }

    private static List<TestClick> generateClicksWithoutFilterType1(DateTime timeOfClicks, int count) {
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, count);
        clicks.forEach(For01Filter::withoutFilterType1);
        return clicks;
    }

    private static List<TestClick> generateClicksWithoutFilterType2(DateTime timeOfClicks, int count) {
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, count);
        clicks.forEach(For01Filter::withoutFilterType2);
        return clicks;
    }

    private static void withoutFilterType2(TestClick click) {
        click.set("show_time", click.get("eventtime", DateTime.class).withMillisOfDay(0).minusMinutes(1));
        click.setFilter(FilterConstants.FILTER_0);
    }

    private static void withoutFilterType1(TestClick click) {
        click.set("show_time",
                click.get("eventtime", DateTime.class).withMillisOfDay(0).minusDays(1).plusMinutes(1));
        click.setFilter(FilterConstants.FILTER_0);
    }

    private static void withFilter(TestClick click) {
        click.set("show_time",
                click.get("eventtime", DateTime.class).withMillisOfDay(0).minusDays(1).minusMinutes(1));
        click.setFilter(FilterConstants.FILTER_1);
    }

    private static void emptyCookie(TestClick click) {
        click.set("ip", "");
        click.set("cookie", "");
    }
}