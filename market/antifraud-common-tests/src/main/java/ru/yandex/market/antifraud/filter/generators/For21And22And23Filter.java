package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.Ids;
import ru.yandex.market.antifraud.filter.fields.PP;
import ru.yandex.market.antifraud.filter.fields.ShopId;
import ru.yandex.market.antifraud.filter.ip.IP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static ru.yandex.market.antifraud.filter.fields.UTM.setUtmTerm;

/**
 * Created by entarrion on 08.09.15.
 */
public class For21And22And23Filter implements FilterGenerator {
    private static final Random RND = new Random();
    private static final int[] INCLUDE_PP = new int[]{1, 9, 11, 20, 404, 405};

    public List<TestClick> generateFor21Filter() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 21
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor21FilterCase1(timeOfClicks), "case1"));
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor21FilterCase2(timeOfClicks), "case2"));
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor21FilterCase3(timeOfClicks), "case3"));
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor21FilterCase4(timeOfClicks), "case4"));
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor21FilterCase5(timeOfClicks), "case5"));
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor21FilterCase6(timeOfClicks), "case6"));
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor21FilterCase7(timeOfClicks), "case7"));
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor21FilterCase8(timeOfClicks), "case8"));
        return clicks;
    }

    public List<TestClick> generateFor22Filter() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 22
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor22FilterCase1(timeOfClicks), "case1"));
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor22FilterCase2(timeOfClicks), "case2"));
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor22FilterCase3(timeOfClicks), "case3"));
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor22FilterCase4(timeOfClicks), "case4"));
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor22FilterCase5(timeOfClicks), "case5"));
        return clicks;
    }

    public List<TestClick> generateFor23Filter() {
        List<TestClick> clicks = new ArrayList<>();
        DateTime timeOfClicks = new DateTime();
        //Генерируем клики подпадающие под фильтр 23
        clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor23FilterCase1(timeOfClicks), "case1"));
        //clicks.addAll(setUtmTerm(For21And22And23Filter.generateClicksFor23FilterCase2(timeOfClicks), "case2"));
        return clicks;
    }

    public static List<TestClick> generateClicksFor21FilterCase1(DateTime timeOfClicks) {
        // 10 кликов с одного ip4, у которых 5 пар ip-адрес - оффер
        String ip4 = IP.generateValidIPv4();
        String ip4Dec = IP.atonIPv4(ip4).toString();
        String ip6 = IP.getIPv6FromIPv4(ip4);
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String waremd5 = Ids.generateWareMD5();
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_21));
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_21));
        }
        return clicks;
    }

    public static List<TestClick> generateClicksFor21FilterCase2(DateTime timeOfClicks) {
        // 10 кликов с одного ip6, у которых 5 пар ip-адрес - оффер
        String ip6 = IP.generateValidIPv6();
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String waremd5 = Ids.generateWareMD5();
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), "", ip6, FilterConstants.FILTER_21));
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), "", ip6, FilterConstants.FILTER_21));
        }
        return clicks;
    }

    public static List<TestClick> generateClicksFor21FilterCase3(DateTime timeOfClicks) {
        // 30 кликов с одного ip4, у которых 14 пар ip-адрес - оффер два клика не подпадающих в такую пару.
        String ip4 = IP.generateValidIPv4();
        String ip4Dec = IP.atonIPv4(ip4).toString();
        String ip6 = IP.getIPv6FromIPv4(ip4);
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            String waremd5 = Ids.generateWareMD5();
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_21));
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_21));
        }
        clicks.add(generateClick(timeOfClicks, Ids.generateWareMD5(), getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_0));
        clicks.add(generateClick(timeOfClicks, Ids.generateWareMD5(), getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_0));
        return clicks;
    }

    public static List<TestClick> generateClicksFor21FilterCase4(DateTime timeOfClicks) {
        // 30 кликов с одного ip6, у которых 14 пар ip-адрес - оффер два клика не подпадающих в такую пару.
        String ip6 = IP.generateValidIPv6();
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            String waremd5 = Ids.generateWareMD5();
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), "", ip6, FilterConstants.FILTER_21));
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), "", ip6, FilterConstants.FILTER_21));
        }
        clicks.add(generateClick(timeOfClicks, Ids.generateWareMD5(), getRandomIncludePP(), "", ip6, FilterConstants.FILTER_0));
        clicks.add(generateClick(timeOfClicks, Ids.generateWareMD5(), getRandomIncludePP(), "", ip6, FilterConstants.FILTER_0));
        return clicks;
    }

    public static List<TestClick> generateClicksFor21FilterCase5(DateTime timeOfClicks) {
        // 30 кликов с одного ip4, у которых 26 подозрительных кликов ip-адрес - оффер и три клика не подпадающих не подозрительные.
        String ip4 = IP.generateValidIPv4();
        String ip4Dec = IP.atonIPv4(ip4).toString();
        String ip6 = IP.getIPv6FromIPv4(ip4);
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            String waremd5 = Ids.generateWareMD5();
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_0));
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_0));
        }
        clicks.add(generateClick(timeOfClicks, clicks.get(0).get("ware_md5", String.class), getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_0));
        clicks.add(generateClick(timeOfClicks, Ids.generateWareMD5(), getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_0));
        clicks.add(generateClick(timeOfClicks, Ids.generateWareMD5(), getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_0));
        clicks.add(generateClick(timeOfClicks, Ids.generateWareMD5(), getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_0));
        return clicks;
    }

    public static List<TestClick> generateClicksFor21FilterCase6(DateTime timeOfClicks) {
        // 30 кликов с одного ip6, у которых 26 подозрительных кликов ip-адрес - оффер и три клика не подпадающих не подозрительные.
        String ip6 = IP.generateValidIPv6();
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            String waremd5 = Ids.generateWareMD5();
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), "", ip6, FilterConstants.FILTER_0));
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), "", ip6, FilterConstants.FILTER_0));
        }
        clicks.add(generateClick(timeOfClicks, clicks.get(0).get("ware_md5", String.class), getRandomIncludePP(), "", ip6, FilterConstants.FILTER_0));
        clicks.add(generateClick(timeOfClicks, Ids.generateWareMD5(), getRandomIncludePP(), "", ip6, FilterConstants.FILTER_0));
        clicks.add(generateClick(timeOfClicks, Ids.generateWareMD5(), getRandomIncludePP(), "", ip6, FilterConstants.FILTER_0));
        clicks.add(generateClick(timeOfClicks, Ids.generateWareMD5(), getRandomIncludePP(), "", ip6, FilterConstants.FILTER_0));
        return clicks;
    }

    public static List<TestClick> generateClicksFor21FilterCase7(DateTime timeOfClicks) {
        // 10 кликов с одного ip4, у которых 5 пар ip-адрес - оффер и с pp не из INCLUDE_PP
        String ip4 = IP.generateValidIPv4();
        String ip4Dec = IP.atonIPv4(ip4).toString();
        String ip6 = IP.getIPv6FromIPv4(ip4);
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String waremd5 = Ids.generateWareMD5();
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomNotIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_0));
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomNotIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_0));
        }
        return clicks;
    }

    public static List<TestClick> generateClicksFor21FilterCase8(DateTime timeOfClicks) {
        // 10 кликов с одного ip6, у которых 5 пар ip-адрес - оффер и с pp не из INCLUDE_PP
        String ip6 = IP.generateValidIPv6();
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String waremd5 = Ids.generateWareMD5();
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomNotIncludePP(), "", ip6, FilterConstants.FILTER_0));
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomNotIncludePP(), "", ip6, FilterConstants.FILTER_0));
        }
        return clicks;
    }
    //==================================== 22 ==========================================================================
    public static List<TestClick> generateClicksFor22FilterCase1(DateTime timeOfClicks) {
        // 3 клика не из РФ.
        String ip4 = IP.generateValidIPv4();
        String ip4Dec = IP.atonIPv4(ip4).toString();
        String ip6 = IP.getIPv6FromIPv4(ip4);
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map.Entry<Integer, Integer> testcase = getRandomShopGeo(ShopId.getShopsForeignDelivery());
            int shopId = testcase.getKey();
            int geoId = testcase.getValue();
            clicks.add(generateClick(timeOfClicks, getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_22, geoId, geoId, shopId));
        }
        return clicks;
    }

    public static List<TestClick> generateClicksFor22FilterCase2(DateTime timeOfClicks) {
        // 2 клика не из РФ.
        String ip4 = IP.generateValidIPv4();
        String ip4Dec = IP.atonIPv4(ip4).toString();
        String ip6 = IP.getIPv6FromIPv4(ip4);
        List<TestClick> clicks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Map.Entry<Integer, Integer> testcase = getRandomShopGeo(ShopId.getShopsForeignDelivery());
            int shopId = testcase.getKey();
            int geoId = testcase.getValue();
            clicks.add(generateClick(timeOfClicks, getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_0, geoId, geoId, shopId));
        }
        return clicks;
    }

    public static List<TestClick> generateClicksFor22FilterCase3(DateTime timeOfClicks) {
        // 10 клика из которых 9 не из РФ и 1 из РФ.
        String ip6 = IP.generateValidIPv6();
        List<TestClick> clicks = new ArrayList<>();
        int geoId = 213;
        clicks.add(generateClick(timeOfClicks, getRandomIncludePP(), "", ip6, FilterConstants.FILTER_22, geoId, geoId, ShopId.generate(geoId)));
        for (int i = 0; i < 9; i++) {
            Map.Entry<Integer, Integer> testcase = getRandomShopGeo(ShopId.getShopsForeignDelivery());
            int shopId = testcase.getKey();
            geoId = testcase.getValue();
            clicks.add(generateClick(timeOfClicks, getRandomIncludePP(), "", ip6, FilterConstants.FILTER_22, geoId, geoId, shopId));
        }
        return clicks;
    }

    public static List<TestClick> generateClicksFor22FilterCase4(DateTime timeOfClicks) {
        // 11 клика из которых 10 не из РФ и 1 из РФ.
        String ip4 = IP.generateValidIPv4();
        String ip4Dec = IP.atonIPv4(ip4).toString();
        String ip6 = IP.getIPv6FromIPv4(ip4);
        List<TestClick> clicks = new ArrayList<>();
        int geoId = 213;
        clicks.add(generateClick(timeOfClicks, getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_22, geoId, geoId, ShopId.generate(geoId)));
        for (int i = 0; i < 10; i++) {
            Map.Entry<Integer, Integer> testcase = getRandomShopGeo(ShopId.getShopsForeignDelivery());
            int shopId = testcase.getKey();
            geoId = testcase.getValue();
            clicks.add(generateClick(timeOfClicks, getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_22, geoId, geoId, shopId));
        }
        return clicks;
    }

    public static List<TestClick> generateClicksFor22FilterCase5(DateTime timeOfClicks) {
        // 11 клика из которых 10 не из РФ и 1 из РФ и с pp не из INCLUDE_PP
        String ip4 = IP.generateValidIPv4();
        String ip4Dec = IP.atonIPv4(ip4).toString();
        String ip6 = IP.getIPv6FromIPv4(ip4);
        List<TestClick> clicks = new ArrayList<>();
        int geoId = 213;//to avoid 28 filter which works when shopId does not deliver to geoId
        clicks.add(generateClick(timeOfClicks, getRandomNotIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_0, geoId, geoId, ShopId.generate(geoId)));
        for (int i = 0; i < 10; i++) {
            Map.Entry<Integer, Integer> testcase = getRandomShopGeo(ShopId.getShopsForeignDelivery());
            int shopId = testcase.getKey();
            geoId = testcase.getValue();
            clicks.add(generateClick(timeOfClicks, getRandomNotIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_0, geoId, geoId, shopId));
        }
        return clicks;
    }

    //==================================== 23 ==========================================================================
    public static List<TestClick> generateClicksFor23FilterCase1(DateTime timeOfClicks) {
        // 100 кликов с одного ip4, у которых 12 одинаковых офферов.
        String ip4 = IP.generateValidIPv4();
        String ip4Dec = IP.atonIPv4(ip4).toString();
        String ip6 = IP.getIPv6FromIPv4(ip4);
        List<TestClick> clicks = new ArrayList<>();
        String waremd5 = Ids.generateWareMD5();
        for (int i = 0; i < 12; i++) {
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_23));
        }
        for (int i = 0; i < 88; i++) {
            clicks.add(generateClick(timeOfClicks, Ids.generateWareMD5(), getRandomIncludePP(), ip4Dec, ip6, FilterConstants.FILTER_23));
        }
        return clicks;
    }

    public static List<TestClick> generateClicksFor23FilterCase2(DateTime timeOfClicks) {
        // 100 кликов с одного ip4, у которых 11 одинаковых офферов.
        String ip6 = IP.generateValidIPv6();
        List<TestClick> clicks = new ArrayList<>();
        String waremd5 = Ids.generateWareMD5();
        for (int i = 0; i < 11; i++) {
            clicks.add(generateClick(timeOfClicks, waremd5, getRandomIncludePP(), "", ip6, FilterConstants.FILTER_0));
        }
        for (int i = 0; i < 89; i++) {
            clicks.add(generateClick(timeOfClicks, Ids.generateWareMD5(), getRandomIncludePP(), "", ip6, FilterConstants.FILTER_0));
        }
        return clicks;
    }


    private static int getRandomIncludePP() {
        return INCLUDE_PP[RND.nextInt(INCLUDE_PP.length)];
    }

    private static int getRandomNotIncludePP() {
        return PP.getRandomMarketNoCpaPP();
    }

    private static TestClick generateClick(DateTime timeOfClicks, String waremd5, int pp, String ip4, String ip6, FilterConstants filter) {
        TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
        click.set("ware_md5", waremd5);
        click.set("Pp", pp);
        click.set("Ip", ip4);
        click.set("Ip6", ip6);
        click.setFilter(filter);
        return click;
    }

    private static TestClick generateClick(DateTime timeOfClicks, int pp, String ip4, String ip6, FilterConstants filter, int geoId, int ipGeoId, int shopId) {
        TestClick click = ClickGenerator.generateUniqueClicks(timeOfClicks, 1).get(0);
        click.set("Pp", pp);
        click.set("Ip", ip4);
        click.set("Ip6", ip6);
        click.set("geo_id", geoId);
        click.set("ip_geo_id",ipGeoId);
        click.set("shop_id",shopId);
        click.setFilter(filter);

        return click;
    }

    private static Map.Entry<Integer, Integer> getRandomShopGeo(Map<Integer, Integer> shopsWithoutDelivery) {
        Set<Map.Entry<Integer, Integer>> testcases = shopsWithoutDelivery.entrySet().stream().collect(toSet());
        return RndUtil.choice(testcases);
    }

    @Override
    public List<TestClick> generate() {
        throw new UnsupportedOperationException("Choose exact generator method");
    }
}
