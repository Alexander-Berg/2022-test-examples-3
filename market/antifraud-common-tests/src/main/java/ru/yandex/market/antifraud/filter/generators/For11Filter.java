package ru.yandex.market.antifraud.filter.generators;

import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by entarrion on 26.01.15.
 */
public class For11Filter implements FilterGenerator {
    public static List<TestClick> generateClicks(DateTime timeOfClicks, int count) {
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, count);
        for (TestClick click : clicks) {
            click.set("referer", "");
            click.set("pp", RandomUtils.nextBoolean() ? 1000 : 1001);
            click.set("utm_medium", "notEmptyCookie");
            click.setFilter(FilterConstants.FILTER_0);
        }

        List<TestClick> clicksEmptyCookie = ClickGenerator.generateUniqueClicks(timeOfClicks, count);
        for (TestClick click : clicksEmptyCookie) {
            click.set("referer", "");
            click.set("cookie", "");
            click.set("utm_medium", "emptyCookie");
            click.set("pp", RandomUtils.nextBoolean() ? 1000 : 1001);
            click.setFilter(FilterConstants.FILTER_11);
        }
        clicks.addAll(clicksEmptyCookie);

        List<TestClick> clicksEmptyCookieIp = ClickGenerator.generateUniqueClicks(timeOfClicks, count);
        for (TestClick click : clicksEmptyCookieIp) {
            click.set("referer", "");
            click.set("ip", "");
            click.set("cookie", "");
            click.set("utm_medium", "emptyIp");
            click.set("pp", RandomUtils.nextBoolean() ? 1000 : 1001);
            click.setFilter(FilterConstants.FILTER_11);
        }
        clicks.addAll(clicksEmptyCookieIp);
        return clicks;
    }

    @Override
    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 11
        clicks.addAll(For11Filter.generateClicks(timeOfClicks, 1));
        clicks.addAll(For11Filter.generateClicks(timeOfClicks, 15));
        return clicks;
    }
}
