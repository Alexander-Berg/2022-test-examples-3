package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.Url;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static ru.yandex.market.antifraud.filter.fields.UTM.setUtmTerm;

/**
 * Created by kateleb on 15.03.16.
 */
public class For25Filter implements FilterGenerator {

    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        //Генерируем клики подпадающие под фильтр 25
        clicks.addAll(setUtmTerm(this.getSovetnikClicks(), "sovetnik"));
        clicks.addAll(setUtmTerm(this.generateBadClicks(), "badClicks"));
        clicks.addAll(setUtmTerm(this.generateGoodClicksCase1(), "case1"));
        clicks.addAll(setUtmTerm(this.generateGoodClicksCase2(), "case2"));
        clicks.addAll(setUtmTerm(this.generateGoodClicksCase3(), "case3"));
        clicks.addAll(setUtmTerm(this.generateGoodClicksCase4(), "case4"));
        return clicks;
    }


    private List<TestClick> sovetnikClicks;
    private DateTime sovetnikTime;

    public For25Filter(DateTime dateTime) {
        this.sovetnikTime = dateTime;
        this.sovetnikClicks = generateSovetnikClicks();
    }

    public List<TestClick> getSovetnikClicks() {
        return sovetnikClicks;
    }

    private List<TestClick> generateSovetnikClicks() {
        return ClickGenerator.generateUniqueClicks(sovetnikTime, 5)
                .stream().map(it -> {
                    it.set("Pp", 1002);
                    it.setFilter(FilterConstants.FILTER_0);
                    it.set("Referer", Url.generateRandomUrl());//make referrer's host different
                    it.set("utm_source", "sovetnik-clicks-for-filter-25");
                    return it;
                }).collect(toList());
    }

    public List<TestClick> generateBadClicks() {
        //click eventtime >= sovetnikTime-30 min
        //click.cookie = sovetnik.cookie
        //click.url[host] = sovetnik.referrer[host]
        return sovetnikClicks.stream().map(sc -> {
            TestClick c = ClickGenerator.generateUniqueClick(sc.get("eventtime", DateTime.class).minusMinutes(29));
            c.set("cookie", sc.get("cookie"));
            c.set("url", sc.get("referer"));
            c.setFilter(FilterConstants.FILTER_25);
            return c;
        }).collect(toList());
    }

    public List<TestClick> generateGoodClicksCase1() {
        //click eventtime < sovetnikTime-30 min
        return sovetnikClicks.stream().map(sc -> {
            TestClick c = ClickGenerator.generateUniqueClick(sc.get("eventtime", DateTime.class).minusMinutes(31));
            c.set("cookie", sc.get("cookie"));
            c.set("url", sc.get("referer"));
            c.setFilter(FilterConstants.FILTER_0);
            return c;
        }).collect(toList());
    }

    public List<TestClick> generateGoodClicksCase2() {
        //click.url[host] != sovetnik.referrer[host]
        return sovetnikClicks.stream().map(sc -> {
            TestClick c = ClickGenerator.generateUniqueClick(sc.get("eventtime", DateTime.class).minusMinutes(29));
            c.set("cookie", sc.get("cookie"));
            c.setFilter(FilterConstants.FILTER_0);
            return c;
        }).collect(toList());
    }

    public List<TestClick> generateGoodClicksCase3() {
        //click.cookie != sovetnik.cookie
        return sovetnikClicks.stream().map(sc -> {
            TestClick c = ClickGenerator.generateUniqueClick(sc.get("eventtime", DateTime.class).minusMinutes(29));
            c.set("url", sc.get("referer"));
            c.setFilter(FilterConstants.FILTER_0);
            return c;
        }).collect(toList());
    }

    public List<TestClick> generateGoodClicksCase4() {
        //click eventtime >= sovetnikTime-30 min
        //click.cookie = sovetnik.cookie
        //click.url[host] = sovetnik.referrer[host]
        //click.url[host] = 2500 - no market
        return sovetnikClicks.stream().map(sc -> {
            TestClick c = ClickGenerator.generateUniqueClick(sc.get("eventtime", DateTime.class).minusMinutes(29));
            c.set("url", sc.get("referer"));
            c.set("cookie", sc.get("cookie"));
            c.set("pp", 2500);
            c.setFilter(FilterConstants.FILTER_0);
            return c;
        }).collect(toList());
    }
}
