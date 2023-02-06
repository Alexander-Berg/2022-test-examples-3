package ru.yandex.market.antifraud.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import ru.yandex.market.antifraud.filter.fields.CategId;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.Defaults;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.Fuid;
import ru.yandex.market.antifraud.filter.fields.Ids;
import ru.yandex.market.antifraud.filter.fields.PP;
import ru.yandex.market.antifraud.filter.fields.Referrer;
import ru.yandex.market.antifraud.filter.fields.ShopId;
import ru.yandex.market.antifraud.filter.fields.StateConstants;
import ru.yandex.market.antifraud.filter.fields.UTM;
import ru.yandex.market.antifraud.filter.fields.Url;
import ru.yandex.market.antifraud.filter.fields.UrlType;
import ru.yandex.market.antifraud.filter.ip.IP;

import static ru.yandex.market.antifraud.filter.RndUtil.nextInt;

public class ClickGenerator {

    public static List<TestClick> generateUniqueClicks(String rowIdPrefix, DateTime timeOfClicks, int count) {
        List<TestClick> clicks = new ArrayList<>(count);
        while(count --> 0) {
            clicks.add(generateUniqueClick(rowIdPrefix, timeOfClicks));
        }
        return clicks;
    }

    public static List<TestClick> generateUniqueClicks(DateTime timeOfClicks, int count) {
        return generateUniqueClicks("", timeOfClicks, count);
    }

    public static TestClick generateUniqueClick(DateTime timeOfClicks) {
        return generateUniqueClick("", timeOfClicks);
    }

    public static TestClick generateUniqueClick(String rowIdPrefix, DateTime timeOfClicks) {
        String rowId = rowIdPrefix + UUID.randomUUID().toString();
        String blockId = generateBlockId(timeOfClicks);
        String showUid = generateShowUid(blockId);
        String cookie = Cookie.generateCookieForClickTime(timeOfClicks);
        String ip = IP.generateValidNoYandexIPv4();
        int pp = PP.getRandomMarketNoCpaPP();
        String bsBlockId = RndUtil.nextBool() ? generateBsBlockId(timeOfClicks) : "";
        boolean usingPofAsJson = RndUtil.nextBool();
        int pof = generatePof(pp);
        Integer clid = usingPofAsJson ? nextInt(50) + 1501 : null;
        String distrType = usingPofAsJson ? "1" : "";
        String pofRaw = usingPofAsJson ? "{\"mclid\": null, \"clid\": [" + pof + ", " + clid + "], \"distr_type\": " + distrType + "}" : Integer.toString(pof);

        TestClick c = new TestClick();
        c.set("rowid", rowId);
        c.set("fuid", Fuid.generateFuidForClickTime(timeOfClicks));
        c.set("block_id", blockId);
        c.set("shop_id", ShopId.generate());
        c.set("link_id", showUid);
        c.setFilter(FilterConstants.FILTER_0);
        c.set("eventtime", timeOfClicks);
        c.set("url", Url.generateRandomUrl());
        c.set("referer", Referrer.generate());
        c.set("ip", ip);
        c.set("cookie", cookie);
        c.set("show_uid", showUid);
        c.set("categ_id", CategId.generate());
        c.set("discount", "0");
        c.set("pp", pp);
        c.set("price", 25);
        c.set("geo_id", Defaults.GEO_ID.value());
        c.set("pof", pof);
        c.set("state", StateConstants.STATE_1.id());
        c.set("hyper_id", Ids.generateHyperId());
        c.set("hyper_cat_id", Defaults.HYPER_CAT_ID.value());
        c.set("onstock", Defaults.ON_STOCK.value());
        c.set("bid", Defaults.BID.value());
        c.set("autobroker_enabled", Defaults.AUTOBROKER_ENABLED_TRUE);
        c.set("ware_id", "");
        c.set("ip_geo_id", Defaults.GEO_ID.value());
        c.set("offer_price", Defaults.OFFER_PRICE.value());
        c.set("test_tag", Defaults.TEST_TAG.value());
        c.set("uah", RndUtil.randomNumeric(10));
        c.set("vcluster_id", Ids.generateVclusterId());
        c.set("ware_md5", Ids.generateWareMD5());
        c.set("test_buckets", Defaults.TEST_BUCKETS.value());
        c.set("cpa", false);
        c.set("req_id", "req_id_" + RndUtil.randomAlphabetic(8));
        c.set("wprid", Defaults.WPRID.value());
        c.set("user_type", Defaults.USER_TYPE_EXTERNAL);
        c.set("utm_Source", UTM.SOURCE.value());
        c.set("utm_Medium", UTM.MEDIUM.value());
        c.set("utm_Term", UTM.TERM.value());
        c.set("utm_Campaign", UTM.CAMPAIGN.value());
        c.set("Touch", Defaults.TOUCH.value());
        c.set("show_cookie", cookie);
        c.set("Ip6", IP.getIPv6FromIPv4(ip));
        c.set("Sbid", Defaults.SBID.value());
        c.set("sub_request_id", "");
        c.set("bs_block_id", bsBlockId);
        c.set("Position", RndUtil.nextInt(500));
        c.set("show_time", timeOfClicks.minusMinutes(5));
        c.set("nav_cat_id", Defaults.NAV_CAT_ID.value());
        c.set("Uuid", Ids.generateUuid());
        c.set("best_deal", RndUtil.nextBool());
        c.set("Hostname", Defaults.HOST.value());
        c.set("cb_vnd", 0);
        c.set("cp_vnd", 0);
        c.set("vnd_id", -1);
        c.set("dtsrc_id", 0);
        c.set("type_id", 0);
        c.set("phone_click_ratio", Defaults.CLICK_PHONE_RATIO.value());
        c.set("is_price_from", false);
        c.set("pof_raw", pofRaw);
        c.set("vid", null);
        c.set("clid", clid);
        c.set("distr_type", distrType);
        c.set("min_bid", null);
        c.set("pp_oi", PP.getRandomPPOi());
        c.set("bid_type", Defaults.BID_TYPE.value());
        c.set("offer_id", RndUtil.nextInt(5));
        c.set("feed_id", RndUtil.nextInt(5));
        c.set("url_type", UrlType.getRandomValue());
        c.set("promo_type", 1);
        return c;
    }

    public static String generateBlockId(DateTime timeOfClicks) {
        return RndUtil.randomNumeric(10) + String.valueOf(timeOfClicks.getMillis() / 1000);
    }

    public static String generateShowUid(String blockId) {
        return blockId +
                StringUtils.leftPad(Integer.toString(UrlType.getRandomValue()), 2, "0") +
                StringUtils.leftPad(getRandomPosition(), 3, "0");
    }



    private static String generateBsBlockId(DateTime timeOfClick) {
        String clickTimeText = timeOfClick.toString("yyyyMMddHHmmss");
        String eventId = RndUtil.randomNumeric(10);
        return "2" + eventId + clickTimeText;
    }

    public static int generatePof(int pp) {
        if (pp < 1000) {
            return RndUtil.nextBool() ? pp : getClid();
        }
        return pp;
    }

    public static int getClid() {
        //user came to market from outside: https://wiki.yandex-team.ru/Market/Projects/PartnerProgram/pof
        int startClidPof = 500;
        return startClidPof + nextInt(269);
    }


    public static ImmutableList<Integer> BENFORD_DISTRIBUTION =
            ImmutableList.of(23, 36, 46, 53, 59, 64, 69, 73, 76, 80, 82, 85, 88, 90, 92, 94, 96, 98, 99);
    private static String getRandomPosition() {
        Integer next = RndUtil.nextInt(100);
        int index;
        for (index = 0; index < BENFORD_DISTRIBUTION.size(); index++) {
            if (BENFORD_DISTRIBUTION.get(index) > next) {
                break;
            }
        }
        return String.valueOf(index + 1);
    }

    public static List<TestClick> generateUniqueClicksWithIp6Only(DateTime timeOfClicks, int count) {
        return generateUniqueClicksWithIp6Only("", timeOfClicks, count);
    }

    public static List<TestClick> generateUniqueClicksWithIp6Only(String rowIdPrefix, DateTime timeOfClicks, int count) {
        List<TestClick> clicks = generateUniqueClicks(rowIdPrefix, timeOfClicks, count);
        for(TestClick click: clicks) {
            click.set("cookie", "");
            click.set("fuid", "");
            click.set("ip", "");
        }
        return clicks;
    }

    public static List<TestClick> generateUniqueCpaClicksWithIp6Only(DateTime timeOfClicks, int count, boolean isOfferCard) {
        List<TestClick> cpaClicks = generateUniqueClicksWithIp6Only(timeOfClicks, count);
        setOffercard(cpaClicks, isOfferCard);
        return cpaClicks;
    }

    public static List<TestClick> generateUniqueCpaClicks(DateTime timeOfClicks, int count, boolean isOfferCard) {
        List<TestClick> cpaClicks = generateUniqueClicks(timeOfClicks, count);
        setOffercard(cpaClicks, isOfferCard);
        return cpaClicks;
    }

    private static void setOffercard(List<TestClick> cpaClicks, boolean isOfferCard) {
        cpaClicks.forEach(c -> {
            if(isOfferCard) {
                c.set("type_id", 2);
            }
            else {
                c.set("type_id", null);
            }
        });
    }

    public static List<TestClick> generateUniqueVendorClicks(DateTime timeOfClicks, int count) {
        return generateUniqueClicks(timeOfClicks, count);
    }

    public static List<TestClick> generateUniqueVendorClicksWithIp6Only(DateTime timeOfClicks, int count) {
        return generateUniqueClicksWithIp6Only(timeOfClicks, count);
    }
}
