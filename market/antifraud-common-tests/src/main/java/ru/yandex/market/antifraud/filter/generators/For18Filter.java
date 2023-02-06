package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by entarrion on 26.01.15.
 */
public class For18Filter implements FilterGenerator {

    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 18
        clicks.addAll(For18Filter.generateClicks(timeOfClicks, 1));
        clicks.addAll(For18Filter.generateClicks(timeOfClicks, 15));
        return clicks;
    }

    //https://wiki.yandex-team.ru/market/informationarchitecture/marketstat/antifraud: 01
    public static List<TestClick> generateClicks(DateTime timeOfClicks, int count) {
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, count);
        for (TestClick click : clicks) {
            // clicks.price > 8400
            click.set("price", RndUtil.nextInt(1000) + 8401);
            click.setFilter(FilterConstants.FILTER_18);
        }

        List<TestClick> clicksEmptyCookie = ClickGenerator.generateUniqueClicks(timeOfClicks, count);
        for (TestClick click : clicksEmptyCookie) {
            click.set("price", RndUtil.nextInt(1000) + 8401);
            click.setFilter(FilterConstants.FILTER_18);
            click.set("Cookie", "");
            click.set("Ip", "");
        }

        return clicks;
    }
}
