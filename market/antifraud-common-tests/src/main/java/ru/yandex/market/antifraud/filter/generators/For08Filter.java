package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.Url;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by entarrion on 26.01.15.
 */
public class For08Filter  implements FilterGenerator{

    @Override
    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 8
        clicks.addAll(For08Filter.generateClicks(timeOfClicks, 1));
        clicks.addAll(For08Filter.generateClicks(timeOfClicks, 15));
        return clicks;
    }

    //https://wiki.yandex-team.ru/market/informationarchitecture/marketstat/antifraud: 01
    public static List<TestClick> generateClicks(DateTime timeOfClicks, int count) {
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, count);
        for (TestClick click : clicks) {
            click.set("referer", Url.generateAccessorInterfaceUrl());
            click.setFilter(FilterConstants.FILTER_8);
        }

        List<TestClick> clicksEmptyCookie = ClickGenerator.generateUniqueClicks(timeOfClicks, count);
        for (TestClick click : clicksEmptyCookie) {
            click.set("referer", Url.generateAccessorInterfaceUrl());
            click.setFilter(FilterConstants.FILTER_8);
            click.set("ip", "");
            click.set("cookie", "");
        }
        clicks.addAll(clicksEmptyCookie);
        return clicks;
    }
}
