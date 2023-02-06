package ru.yandex.market.antifraud.filter;

import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.fields.CategId;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.Defaults;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.Fuid;
import ru.yandex.market.antifraud.filter.fields.Ids;
import ru.yandex.market.antifraud.filter.fields.PP;
import ru.yandex.market.antifraud.filter.fields.ShopId;
import ru.yandex.market.antifraud.filter.fields.UTM;
import ru.yandex.market.antifraud.filter.fields.UrlType;
import ru.yandex.market.antifraud.filter.ip.IP;

import java.util.UUID;

public class ShowGenerator {

    public static TestShow uniqueShow(DateTime eventtime) {
        return uniqueShow(eventtime, Ids.uniqueRowId());
    }

    public static TestShow uniqueShow(DateTime eventtime, String rowId) {
        String blockId = ClickGenerator.generateBlockId(eventtime);
        String bsBlockId = "";
        String showUid = ClickGenerator.generateShowUid(blockId);
        String linkId = ClickGenerator.generateShowUid(blockId);
        String position = RndUtil.nextInt(5000) + "";
        String ip = IP.generateValidNoYandexIPv4();
        String fuid = Fuid.generateFuidForClickTime(eventtime);
        int pp = PP.getRandomMarketPP();
        boolean usingPofAsJson = RndUtil.nextBool();
        int pof = ClickGenerator.generatePof(pp);
        String clid = usingPofAsJson ? String.valueOf(RndUtil.nextInt(50) + 1501) : "";
        String distrType = usingPofAsJson ? "1" : "";
        String pofRaw = usingPofAsJson ?
                "{\"mclid\": null, \"clid\": [\"" + pof + "\", \"" + clid + "\"], \"distr_type\": " + distrType + "}" :
                Integer.toString(pof);
        TestShow show = new TestShow();
        show.setRowid(rowId);
        show.setEventtime(eventtime);
        show.setIp(IP.getIPv6FromIPv4(ip));
        show.setYandexUid(Cookie.generateCookieForClickTime(eventtime));
        show.setShowBlockId(blockId);
        show.setShowUid(showUid);
        show.setGoodsCount("1");
        show.setUrl("http://test.show.url/" + UUID.randomUUID().toString());
        show.setGoodsTitle(Defaults.GOODS_TITLE.value(String.class));
        show.setCategId(CategId.generate());
        show.setDiscount("");
        show.setPp(pp);
        show.setGeoId(Defaults.GEO_ID.value(Integer.class));
        show.setShopId(ShopId.generate());
        show.setPriceClick("10");
        show.setPof(pof);
        show.setState(1);
        show.setFilter(FilterConstants.FILTER_0);
        show.setHyperId(Ids.generateHyperId());
        show.setOnstock(0);
        show.setBid(Defaults.BID.value(Integer.class));
        show.setAutobrokerEnabled(1);
        show.setWareId("");
        show.setCtx("0");
        show.setOriginalQuery(Defaults.QUERY.value(String.class));
        show.setGeneration(Defaults.SHOW_GEN.value(String.class));
        show.setHostname(Defaults.HOSTNAME.value(String.class));
        show.setOfferPrice(Defaults.OFFER_PRICE.value(String.class));
        show.setTestTag("0");
        show.setVclusterId(Defaults.NONE.value(String.class));
        show.setContext(Defaults.CONTEXT.value(String.class));
        show.setWareMd5(Ids.generateWareMD5());
        show.setFuid(fuid);
        show.setCpa("0");
        show.setTestBuckets(Defaults.TEST_BUCKETS.value(String.class));
        show.setLinkId(linkId);
        show.setReqId("req_id_8");
        show.setWprid(Defaults.WPRID.value(String.class));
        show.setUserType("0");
        show.setOldPrice(Defaults.OLDPRICE.value(String.class));
        show.setIpGeoId(123456789);
        show.setUtmSource(UTM.SOURCE.value());
        show.setUtmMedium(UTM.MEDIUM.value());
        show.setUtmTerm(UTM.TERM.value());
        show.setUtmCampaign(UTM.CAMPAIGN.value());
        show.setTouch(Defaults.TOUCH.value(Boolean.class));
        show.setSbid(Defaults.SBID.value(Integer.class));
        show.setHomeRegion(Defaults.HOME_REGION.value(Integer.class));
        show.setSubRequestId("");
        show.setBsBlockId(bsBlockId);
        show.setPosition(position);
        show.setMnCtr(Defaults.MN_CTR.value(String.class));
        show.setUuid(Ids.generateUuid());
        show.setNavCatId(Defaults.NAV_CAT_ID.value(Integer.class));
        show.setBestDeal(RndUtil.nextBool() ? "0" : "1");
        show.setCbid("5");
        show.setRankedWith(RndUtil.randomAlphabetic(5));
        show.setIsPriceFrom("0");
        show.setPofRaw(pofRaw);
        show.setVid("");
        show.setClid(clid);
        show.setDistrType(distrType);
        show.setTypeId("0");
        show.setMinBid("0");
        show.setNormalizedByDnormQuery("dnorm " + Defaults.QUERY.value(String.class));
        show.setNormalizedToLowerQuery("lower " + Defaults.QUERY.value(String.class));
        show.setNormalizedToLowerAndSortedQuery("sorted " + Defaults.QUERY.value(String.class));
        show.setNormalizedBySynnormQuery("synnorrm " + Defaults.QUERY.value(String.class));
        show.setPpOi(PP.getRandomPPOi());
        show.setFee(2L);
        show.setShopFee(3L);
        show.setMinFee(2L);
        show.setUrlType(UrlType.getRandomValue());
        show.setPhoneClickRatio(Defaults.CLICK_PHONE_RATIO.value(String.class));
        show.setPhoneThreshold("1");
        show.setFeedId(5L);
        show.setVendorDsId(RndUtil.nextBool() ? "" : String.valueOf(RndUtil.nextInt(1000000) + 1));
        show.setVendorPrice(RndUtil.nextBool() ? "" : String.valueOf(RndUtil.nextInt(1000000) + 1));
        show.setVcBid(RndUtil.nextBool() ? "" : String.valueOf(RndUtil.nextInt(999998) + 1));
        show.setUrlHash(RndUtil.randomAlphabetic(32));
        show.setRecordType(0);
        return show;
    }
}
