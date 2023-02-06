package ru.yandex.autotests.reporting.api.steps;

import java.time.LocalDateTime;
import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.autotests.market.stat.util.JsonUtils;
import ru.yandex.autotests.market.stat.util.RandomUtils;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.CATEGORIES;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.DOMAIN;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.PERIOD;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.REGIONS;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.SHOP;

/**
 * Created by kateleb on 21.11.16.
 */
public class ReportingApiParamsProvider {
    public static List<String> bigCompShops = Arrays.asList(
        "ulmart.ru", "citilink.ru", "rus.onlinetrade.ru", "svyaznoy.ru",
        "eldorado.ru", "mediamarkt.ru", "beeline.ru", "moscow.shop.megafon.ru", "spb.shop.megafon.ru", "shop.mts.ru",
        "moscow.ulmart.ru", "spb.citilink.ru", "ekat.citilink.ru", "kazan.onlinetrade.ru", "ekb.svyaznoy.ru",
        "spb.svyaznoy.ru", "ekb.eldorado.ru", "ekaterinburg.mediamarkt.ru", "kazan.mediamarkt.ru", "moskva.beeline.ru",
        "kazan.beeline.ru", "moscow.shop.megafon.ru", "tatarstan.shop.megafon.ru",
        "shop.mts.ru", "spb.shop.mts.ru");
    public static List<String> biggestShops = Arrays.asList("svyaznoy.ru", "beeline.ru", "beeline.ru", "megafon.ru");
    public static List<Integer> bigRegions = Arrays.asList(166, 225, 3, 1, 213, 10174, 2, 11111, 11119, 43, 11162, 54, 33);
    public static List<Integer> bigCategories = Arrays.asList(90401, 6427100, 91013, 10604359, 91009, 91031, 91083);
    public static List<Integer> someRegions = Arrays.asList(149, 33, 159, 187, 225, 241, 20524, 20525, 20526, 20527,
        20528, 20529, 20530, 20531, 20532, 20533, 20534, 20535, 20536, 20537,
        20538, 20539, 20540, 20541, 20542, 20543, 20544, 3, 1, 213, 10174, 2, 11111, 11119, 43);
    public static List<Integer> capitals = Arrays.asList(2, 213);
    private static List<String> AVAILABLE_REGIONS = Arrays.asList("Городской округ Калининград", "Новосибирск", "Республика Татарстан",
        "Волгоградская область", "Петрозаводский городской округ", "Камчатский край", "Республика Дагестан",
        "Санкт-Петербург и Ленинградская область", "Забайкальский край", "СНГ", "Винницкая область", "Кызыл", "Дальневосточный федеральный округ");

    private ReportingApiHandleSteps steps;

    public ReportingApiParamsProvider(ReportingApiHandleSteps steps) {
        this.steps = steps;

    }

    public Map<String, Object> getParamsForBigShop() {
        Map<String, Object> params = new HashMap<>();
        String shop = RandomUtils.choice(bigCompShops);
        String[] parts = shop.split("\\.");
        String domain = parts.length > 1 ? parts[parts.length - 2] + "." + parts[parts.length - 1] : shop;
        params.put(DOMAIN, domain);
        params.put(SHOP, shop);
        params.put(PERIOD, LocalDateTime.now().minusDays(10));
        params.put(CATEGORIES, getAtLeast2Items(bigCategories));
        params.put(REGIONS, getAtLeast2Items(bigRegions));
        return params;
    }

    public Map<String, Object> getParamsForBestShops() {
        Map<String, Object> params = new HashMap<>();
        String domain = RandomUtils.choice(biggestShops);
        params.put(DOMAIN, domain);
        params.put(SHOP, domain);
        params.put(PERIOD, LocalDateTime.now().minusDays(10));
        params.put(CATEGORIES, getAtLeast2Items(bigCategories));
        params.put(REGIONS, Collections.singletonList(capitals.get(new Random().nextInt(capitals.size()))));
        return params;
    }

    public Map<String, Object> getParamsForBigShopManyParams() {
        Map<String, Object> params = new HashMap<>();
        String shop = RandomUtils.choice(bigCompShops);
        String[] parts = shop.split("\\.");
        String domain = parts.length > 1 ? parts[parts.length - 2] + "." + parts[parts.length - 1] : shop;
        params.put(DOMAIN, domain);
        params.put(SHOP, shop);
        params.put(PERIOD, LocalDateTime.now().minusDays(10));
        List<String> categories = JsonUtils.getAllValuesForMemberName(steps.getCategories(), "id").stream().limit(20).collect(toList());
        params.put(CATEGORIES, categories);
        params.put(REGIONS, someRegions);
        return params;
    }

    private List<Integer> getAtLeast2Items(List<Integer> fromList) {
        Set<Integer> data = new HashSet<>();
        while (data.size() < 2) {
            data.add(RandomUtils.choice(fromList));
        }
        return new ArrayList<>(data);
    }

    @Step
    public String getAvailableRegionName() {
        //TODO: выгружаются не все регионы, а только нужные. Нужные определяются по тсв-файлу, нужно будет читать из него
        String region = RandomUtils.choice(AVAILABLE_REGIONS);
        Attacher.attachTestData(region);
        return region;
    }

}
