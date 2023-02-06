package ru.yandex.market.antifraud.filter.generators;

import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.ShopId;
import ru.yandex.market.antifraud.filter.fields.Url;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by entarrion on 26.01.15.
 *
 *
 * Currently not used
 */
public class For07Filter implements FilterGenerator {

    @Override
    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики не подпадающие под фильтр 7 (По граничному условию подходят, но подходящие по процентному соотношению кликов)
        clicks.addAll(For07Filter.generateClicksGreaterThanThresholdValueAndNotPercentage(timeOfClicks));
        //Генерируем клики не подпадающие под фильтр 7 (По граничному условию, но подходящие по процентному соотношению кликов)
        clicks.addAll(For07Filter.generateClicksLessThanThresholdValue(timeOfClicks));
        //Генерируем клики подпадающие под фильтр 7 (По граничному условию, но подходящие по процентному соотношению кликов)
        clicks.addAll(For07Filter.generateClicksGreaterThanThresholdValueAndPercentage(timeOfClicks));
        //Генерируем клики подпадающие под фильтр 7 с пустыми куками
        clicks.addAll(For07Filter.generateClicksWithEmptyCookie(timeOfClicks));
        for (TestClick click : clicks) {
            click.setFilter(FilterConstants.FILTER_0);
        }

        return clicks;
    }

    public static List<TestClick> generateClicksGreaterThanThresholdValueAndPercentage(DateTime timeOfClicks) {
        return generateClicks(timeOfClicks, 11, 1, FilterConstants.FILTER_7);
    }

    public static List<TestClick> generateClicksWithEmptyCookie(DateTime timeOfClicks) {
        List<TestClick> clicks = generateClicks(timeOfClicks, 11, 1, FilterConstants.FILTER_7);
        for (TestClick click: clicks) {
            click.set("cookie", "");
            click.set("ip", "");
        }
        return clicks;
    }

    public static List<TestClick> generateClicksGreaterThanThresholdValueAndNotPercentage(DateTime timeOfClicks) {
        return generateClicks(timeOfClicks, 11, 370, FilterConstants.FILTER_0);
    }

    public static List<TestClick> generateClicksLessThanThresholdValue(DateTime timeOfClicks) {
        return generateClicks(timeOfClicks, 10, 5, FilterConstants.FILTER_0);
    }

    public static List<TestClick> generateClicks(DateTime timeOfClicks, int suspiciousClickCount, int goodClickCount, FilterConstants filter) {
        List<TestClick> clicks = new ArrayList<>();
        int shopId = ShopId.generate();
        clicks.addAll(generateSuspiciousClicks(timeOfClicks, shopId, suspiciousClickCount, filter));
        clicks.addAll(generateGoodClicks(timeOfClicks, shopId, goodClickCount));
        return clicks;
    }

    private static List<TestClick> generateGoodClicks(DateTime timeOfClicks, int shopId, int count) {
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            clicks.add(generateGoodClick(timeOfClicks, shopId));
        }
        return clicks;
    }

    private static TestClick generateGoodClick(DateTime timeOfClicks, int shopId) {
        return RandomUtils.nextBoolean() ? generateGoodClickOneType(timeOfClicks, shopId) :
                generateGoodClickTwoType(timeOfClicks, shopId);
    }

    private static TestClick generateGoodClickOneType(DateTime timeOfClicks, int shopId) {
        TestClick click = generateSuspiciousClickOneType(timeOfClicks, shopId, FilterConstants.FILTER_0);
        click.set("pp", click.get("pof", Integer.class).equals(2) ? 3 : 2);
        return click;
    }

    private static TestClick generateGoodClickTwoType(DateTime timeOfClicks, int shopId) {
        TestClick click = generateSuspiciousClick(timeOfClicks, shopId, FilterConstants.FILTER_0);
        String url = Url.generateUrlWithRandomPath(RandomUtils.nextBoolean() ? "market.yandex.net" : "m.market.yandex.ru");
        click.set("url", url);
        return click;
    }

    private static List<TestClick> generateSuspiciousClicks(DateTime timeOfClicks, int shopId, int count, FilterConstants filter) {
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            clicks.add(generateSuspiciousClick(timeOfClicks, shopId, filter));
        }
        return clicks;
    }

    private static TestClick generateSuspiciousClick(DateTime timeOfClicks, int shopId, FilterConstants filter) {
        return RandomUtils.nextBoolean() ? generateSuspiciousClickOneType(timeOfClicks, shopId, filter) :
                generateSuspiciousClickTwoType(timeOfClicks, shopId, filter);
    }

    private static TestClick generateSuspiciousClickOneType(DateTime timeOfClicks, int shopId, FilterConstants filter) {
        int pp = RndUtil.nextBool() ? 1 : 9;
        int pof = RndUtil.nextInt(999) + 1;
        return generateClick(timeOfClicks, shopId, pp, pof, Url.generateRandomUrl(), filter);
    }

    private static TestClick generateSuspiciousClickTwoType(DateTime timeOfClicks, int shopId, FilterConstants filter) {
        int pp = 2;
        return generateClick(timeOfClicks, shopId, pp, pp, Url.generateRandomUrl(), filter);
    }

    private static TestClick generateClick(DateTime timeOfClicks, int shopId, int pp, int pof, String url, FilterConstants filter) {
        TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
        click.set("shop_id", shopId);
        click.set("referer", "");
        click.set("pp", pp);
        click.set("pof", pof);
        click.set("url", url);
        click.setFilter(filter);
        return click;
    }
}