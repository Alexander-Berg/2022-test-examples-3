package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.ShopId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static ru.yandex.market.antifraud.filter.fields.UTM.setUtmTerm;

/**
 * Created by kateleb on 22.08.16
 */
public class For28Filter implements FilterGenerator {

    public List<TestClick> generate() {
        List<TestClick> clicks = new ArrayList<>();
        //Генерируем клики подпадающие под фильтр 28
        clicks.addAll(setUtmTerm(this.generateClicksWithDeliveryToCountry(), "country"));
        clicks.addAll(setUtmTerm(this.generateClicksWithDeliveryToRegion(), "region"));
        clicks.addAll(setUtmTerm(this.generateClicksWithDeliveryToSng(), "sng"));
        clicks.addAll(setUtmTerm(this.generateClicksWithNoDelivery(), "noDelivery"));
        clicks.addAll(setUtmTerm(this.generateClicksWithNoDeliveryByIpGeo(), "noDeliveryIpGeo"));
        return clicks;
    }

    private DateTime timeOfClicks;

    public For28Filter(DateTime dateTime) {
        this.timeOfClicks = dateTime;
    }


    public List<TestClick> generateClicksWithDeliveryToCountry() {
        Map.Entry<Integer, Integer> testcase = getRandomShopGeo(ShopId.getShopsWithDeliveryToCountry());
        int shopId = testcase.getKey();
        int geoId = testcase.getValue();
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, 5);
        for (TestClick click : clicks) {
            click.setFilter(FilterConstants.FILTER_0);
            click.set("type_id", 1);
            click.set("shop_id", shopId);
            click.set("geo_id", geoId);
            click.set("Utm_Campaign", "deliveredToCountry");
        }
        return clicks;
    }

    public List<TestClick> generateClicksWithDeliveryToRegion() {
        Map.Entry<Integer, Integer> testcase = getRandomShopGeo(ShopId.getShopsWithDeliveryToRegion());
        int shopId = testcase.getKey();
        int geoId = testcase.getValue();
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, 5);
        for (TestClick click : clicks) {
            click.setFilter(FilterConstants.FILTER_0);
            click.set("type_id", 1);
            click.set("shop_id", shopId);
            click.set("geo_id", geoId);
            click.set("Utm_Campaign", "deliveredToRegion");
        }
        return clicks;
    }

    public List<TestClick> generateClicksWithDeliveryToSng() {
        Map.Entry<Integer, Integer> testcase = getRandomShopGeo(ShopId.getShopsForSngDelivery());
        int shopId = testcase.getKey();
        int geoId = testcase.getValue();
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, 5);
        for (TestClick click : clicks) {
            click.setFilter(FilterConstants.FILTER_0);
            click.set("type_id", 1);
            click.set("shop_id", shopId);
            click.set("geo_id", geoId);
            click.set("Utm_Campaign", "deliveredToSng");
        }
        return clicks;
    }

    public List<TestClick> generateClicksWithNoDelivery() {
        Map.Entry<Integer, Integer> testcase = getRandomShopGeo(ShopId.getShopsWithoutDeliveryToRegion());
        int shopId = testcase.getKey();
        int geoId = testcase.getValue();
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, 5);
        for (TestClick click : clicks) {
            click.setFilter(FilterConstants.FILTER_28);
            click.set("type_id", 1);
            click.set("shop_id", shopId);
            click.set("geo_id", geoId);
            click.set("Utm_Campaign", "no_delivery");
        }
        return clicks;
    }

    public List<TestClick> generateClicksWithNoDeliveryByIpGeo() {
        Map.Entry<Integer, Integer> testcase = getRandomShopGeo(ShopId.getShopsWithoutDeliveryToRegion());
        int shopId = testcase.getKey();
        int geoId = testcase.getValue();
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, 5);
        for (TestClick click : clicks) {
            click.setFilter(FilterConstants.FILTER_28);
            click.set("type_id", 1);
            click.set("shop_id", shopId);
            click.set("geo_id", 0);
            click.set("ip_geo_id", geoId);
            click.set("Utm_Campaign", "no_delivery_ip_geo");
        }
        return clicks;
    }

    private static Map.Entry<Integer, Integer> getRandomShopGeo(Map<Integer, Integer> shopsWithoutDelivery) {
        Set<Map.Entry<Integer, Integer>> testcases = shopsWithoutDelivery.entrySet().stream().collect(toSet());
        return RndUtil.choice(testcases);
    }
}
