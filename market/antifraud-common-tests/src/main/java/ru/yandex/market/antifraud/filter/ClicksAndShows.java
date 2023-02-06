package ru.yandex.market.antifraud.filter;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.fields.CategId;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.Fuid;
import ru.yandex.market.antifraud.filter.fields.Ids;
import ru.yandex.market.antifraud.filter.fields.ShopId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClicksAndShows {
    protected static final ImmutableList<Integer> showsPP =
            ImmutableList.of(1, 7, 9, 11, 240);
    private int showsCount;
    private int ofaCount;
    private int clicksCount;
    private Map<String, String> showUids;
    private String vclusterId;
    private Integer hyperId;
    private String context;
    private String query;
    private Integer categId;
    private static final ImmutableList<Integer> clicksPP = ImmutableList.of(1, 7, 9, 11, 25, 33);
    private String wmd5;
    private Integer commonPP;
    private int shopId;
    private int ipGeoId;
    private int geoId;
    private List<TestClick> clicks;
    private List<TestClick> ofa;
    private List<TestShow> shows;

    public ClicksAndShows(int clicksCount, int showsCount, DateTime time) {
        this(clicksCount, showsCount, 0, time);
    }

    public ClicksAndShows(int clicksCount, int showsCount, int ofaCount, DateTime time) {
        this.context = "TEST CONTEXT_" + 7;
        this.showUids = new HashMap<>();
        this.hyperId = Ids.generateHyperId();
        this.categId = CategId.generate();
        this.clicksCount = clicksCount;
        this.showsCount = showsCount;
        this.ofaCount = ofaCount;
        this.wmd5 = Ids.generateWareMD5();
        this.shopId = ShopId.generate();
        this.ipGeoId = 213;
        this.shows = getUniqueShow("TEST_SHOW_`CTR_", time);
        this.clicks = getUniqueClick("TEST_CLICK_CTR_", time.plusMinutes(10));
        this.ofa = getUniqueOfa("TEST_OFA_CTR_", time.plusMinutes(15));
    }


    public int getCategId() {
        return categId;
    }

    public String getContext() {
        return context;
    }

    public int getHyperId() {
        return hyperId;
    }

    public String getVclusterId() {
        return vclusterId;
    }

    public long getShowsCount() {
        return showsCount;
    }

    public int getOfaCount() {
        return ofaCount;
    }

    public int getClicksCount() {
        return clicksCount;
    }

    protected int getValidShowPP() {
        int i = RndUtil.nextInt(showsPP.size());
        return showsPP.get(i);
    }

    protected void validateShowUidsForShowsGenerated(DateTime timeOfClicks, int count) {
        while (showUids.size() < count) {
            //this should not happen if shows num > clicks num
            String blockId = ClickGenerator.generateBlockId(timeOfClicks);
            String showUid = ClickGenerator.generateShowUid(blockId);
            showUids.put(showUid, blockId);
        }
    }

    public String getQuery() {
        return query;
    }

    public Integer getGeoId() {
        return geoId;
    }

    public Integer getIpGeoId() {
        return ipGeoId;
    }

    public Integer getShopId() {
        return shopId;
    }

    public String getWmd5() {
        return wmd5;
    }

    public String getId() {
        if ((wmd5 != null) && !wmd5.equals("-1")) {
            return wmd5;
        }

        if ((hyperId != null) && !hyperId.equals(-1)) {
            return hyperId.toString();
        }

        return vclusterId;
    }

    public List<TestShow> getShows() {
        return shows;
    }

    public ClicksAndShows withContext(String context) {
        for (TestShow show : shows) {
            show.setContext(context);
        }
        this.context = context;
        return this;
    }

    public ClicksAndShows withShowUid(String newShowUid) {
        for (TestClick click : clicks) {
            click.set("show_uid", newShowUid);
        }
        for (TestShow show : shows) {
            show.setShowUid(newShowUid);
        }
        return this;
    }

    public ClicksAndShows withIpGeoId(int geoId) {
        this.ipGeoId = geoId;
        for (TestClick click : clicks) {
            click.set("ip_geo_id", geoId);
        }
        for (TestShow show : shows) {
            show.setIpGeoId(geoId);
        }
        return this;
    }

    public ClicksAndShows withGeoId(int geoId) {
        this.geoId = geoId;
        for (TestClick click : clicks) {
            click.set("geo_id", geoId);
        }
        for (TestShow show : shows) {
            show.setGeoId(geoId);
        }
        return this;
    }

    public ClicksAndShows withShopId(int shopId) {
        this.shopId = shopId;
        for (TestClick click : clicks) {
            click.set("shop_id", shopId);
        }
        for (TestShow show : shows) {
            show.setShopId(shopId);
        }
        return this;
    }

    public ClicksAndShows withCategoryId(int cat) {
        return withClickCategoryId(cat).withShowCategoryId(cat).withOfaCategoryId(cat);
    }

    public ClicksAndShows withClickCategoryId(int categoryId) {
        for (TestClick click : clicks) {
            click.set("categ_id", categoryId);
        }
        return this;
    }

    public ClicksAndShows withOfaCategoryId(int categoryId) {
        for (TestClick click : ofa) {
            click.set("categ_id", categoryId);
        }
        return this;
    }

    public ClicksAndShows withShowCategoryId(int categoryId) {
        this.categId = categoryId;
        for (TestShow show : shows) {
            show.setCategId(categoryId);
        }
        return this;
    }

    public ClicksAndShows withValidContext() {
        context = "TEST CONTEXT_" + 7;
        for (TestShow show : shows) {
            show.setContext(context);
        }
        return this;
    }

    public ClicksAndShows withValidQuery() {
        query = "TEST QUERY_" + 7;
        for (TestShow show : shows) {
            show.setOriginalQuery(query);
        }
        return this;
    }

    public ClicksAndShows withPP(int pp) {
        this.commonPP = pp;
        return withClicksPP(pp).withShowsPP(pp).withOfaPP(pp);
    }

    public ClicksAndShows withShowsPP(int pp) {
        for (TestShow show : shows) {
            show.setPp(pp);
        }
        return this;
    }

    public ClicksAndShows withClicksPP(int pp) {
        for (TestClick click : clicks) {
            click.set("pp", pp);
        }
        return this;
    }

    public ClicksAndShows withOfaPP(int pp) {
        for (TestClick click : ofa) {
            click.set("pp", pp);
        }
        return this;
    }

    public ClicksAndShows withWareMd5(String ware) {
        this.wmd5 = ware;
        return withClicksWareMd5(wmd5).withShowsWareMd5(wmd5);
    }

    public ClicksAndShows withShowsWareMd5(String ware) {
        this.wmd5 = ware;
        for (TestShow show : shows) {
            show.setWareMd5(ware);
        }
        return this;
    }

    public ClicksAndShows withClicksWareMd5(String ware) {
        for (TestClick click : clicks) {
            click.set("ware_md5", ware);
        }
        return this;
    }

    public ClicksAndShows withHyperId(int hyper) {
        this.hyperId = hyper;
        return withClicksHyperId(hyper).withShowsHyperId(hyper);
    }

    public ClicksAndShows withShowsHyperId(int hyper) {
        this.hyperId = hyper;
        for (TestShow show : shows) {
            show.setHyperId(hyper);
        }
        return this;
    }

    public ClicksAndShows withClicksHyperId(int hyper) {
        for (TestClick click : clicks) {
            click.set("hyper_id", hyper);
        }
        return this;
    }

    public ClicksAndShows withVclusterId(String vclusterId) {
        this.vclusterId = vclusterId;
        return withClicksVclusterId(vclusterId).withShowsVclusterId(vclusterId);
    }

    public ClicksAndShows withShowsVclusterId(String vclusterId) {
        this.vclusterId = vclusterId;
        for (TestShow show : shows) {
            show.setVclusterId(vclusterId);
        }
        return this;
    }

    public ClicksAndShows withClicksVclusterId(String vclusterId) {
        for (TestClick click : clicks) {
            click.set("vcluster_id", vclusterId);
        }
        return this;
    }

    public ClicksAndShows withClickPrice(String price) {
        for (TestClick click : clicks) {
            click.set("price", price);
        }
        return this;
    }

    public ClicksAndShows withValidClickPP() {
        for (TestClick click : clicks) {
            click.set("pp", getValidClickPP());
        }
        return this;
    }

    public ClicksAndShows withDifferentPPs(int pp1, int pp2) {
        withClicksPP(pp1).withShowsPP(pp1);
        for (int i = 0; i < Math.round(clicksCount / 2); i++) {
            TestClick click = clicks.get(i);
            click.set("pp", pp2);
            shows.stream()
                    .filter(c -> c.getShowUid().equals(click.get("show_uid", String.class)))
                    .forEach(it -> it.setPp(pp2));
        }
        return this;
    }

    public ClicksAndShows withSameValidClickAndShowsPP() {
        int pp = getPPvalidForShowAndClick();

        for (TestClick click : clicks) {
            click.set("pp", pp);
        }

        for (TestShow show : shows) {
            show.setPp(pp);
        }
        return this;
    }

    private int getPPvalidForShowAndClick() {
        int i = RndUtil.nextInt(4);
        return clicksPP.get(i);
    }

    public ClicksAndShows withValidShowPP() {
        int validShowPP = getValidShowPP();
        for (TestShow show : shows) {
            show.setPp(validShowPP);
        }
        return this;
    }

    public List<TestClick> getClicks() {
        return clicks;
    }

    public List<TestClick> getOfa() {
        return ofa;
    }


    private List<TestClick> getUniqueClick(final String rowIdPrefix, final DateTime timeOfClicks) {
        DateTime clickTime = timeOfClicks;
        validateShowUidsForShowsGenerated(clickTime, clicksCount);
        List<TestClick> clicks = new ArrayList<>();
        if (commonPP == null) {
            commonPP = getPPvalidForShowAndClick();
        }
        int i = 0;
        List<String> showUidsList = new ArrayList<>(showUids.keySet());
        for (String rowId : Ids.uniqueRowIdStartingFrom(rowIdPrefix, clicksCount)) {
            String showId = showUidsList.get(i);
            TestClick click = ClickGenerator.generateUniqueClick(clickTime);
            click.set("row_id", rowId);
            click.set("show_uid", showId);
            click.set("link_id", showId);
            click.set("bock_id", showUids.get(showId));
            click.set("ware_md5", wmd5);
            click.set("pp", commonPP);
            click.set("categ_id", categId);
            click.set("yandex_uid", Cookie.generateCookieForClickTime(timeOfClicks));
            click.set("fuid", Fuid.generateFuidForClickTime(timeOfClicks));
            click.set("shop_id", shopId);
            click.set("ip_geo_id", ipGeoId);
            click.setFilter(ru.yandex.market.antifraud.filter.fields.FilterConstants.FILTER_0);
            clicks.add(click);
            clickTime = clickTime.plusSeconds(2);
            i++;
        }
        return clicks;
    }

    private List<TestClick> getUniqueOfa(final String rowIdPrefix, final DateTime timeOfClicks) {
        DateTime clickTime = timeOfClicks;
        validateShowUidsForShowsGenerated(clickTime, ofaCount);
        List<TestClick> clicks = new ArrayList<>();
        if (commonPP == null) {
            commonPP = getPPvalidForShowAndClick();
        }
        int i = 0;
        List<String> showUidsList = new ArrayList<>(showUids.keySet());
        for (String rowId : Ids.uniqueRowIdStartingFrom(rowIdPrefix, ofaCount)) {
            String showId = showUidsList.get(i);
            // performance to the maximum
            // gc-less
            // qpi aware
            // lock free
            TestClick click = ClickGenerator
                    .generateUniqueCpaClicks(clickTime, 1, true).get(0);
            click.set("row_id", rowId);
            click.set("show_uid", showId);
            click.set("link_id", showId);
            click.set("bock_id", showUids.get(showId));
            click.set("ware_md5", wmd5);
            click.set("pp", commonPP);
            click.set("categ_id", categId);
            click.set("yandex_uid", Cookie.generateCookieForClickTime(timeOfClicks));
            click.set("fuid", Fuid.generateFuidForClickTime(timeOfClicks));
            click.set("shop_id", shopId);
            click.set("ip_geo_id", ipGeoId);
            click.set("type_id", "2");
            clicks.add(click);
            clickTime = clickTime.plusSeconds(2);
            i++;
        }
        return clicks;
    }

    private List<TestShow> getUniqueShow(String rowIdPrefix, DateTime timeOfClicks) {
        DateTime clickTime = timeOfClicks;
        List<TestShow> shows = new ArrayList<>();
        commonPP = getPPvalidForShowAndClick();
        List<String> rowIds = Ids.uniqueRowIdStartingFrom(rowIdPrefix, showsCount);
        for (String rowId : rowIds) {
            String blockId = ClickGenerator.generateBlockId(clickTime);
            String showUid = ClickGenerator.generateShowUid(blockId);
            showUids.put(showUid, blockId);
            TestShow show = ShowGenerator.uniqueShow(clickTime);
            show.setShowUid(showUid);
            show.setLinkId(showUid);
            show.setContext(context);
            show.setNormalizedToLowerQuery(context + "_LOWER");
            show.setNormalizedToLowerAndSortedQuery(context + "_LOWER_SORTED");
            show.setNormalizedByDnormQuery(context + "_DNORM");
            show.setNormalizedBySynnormQuery(context + "_SYNNORM");
            show.setShopId(shopId);
            show.setPp(commonPP);
            show.setCategId(categId);
            show.setWareMd5(wmd5);
            show.setIpGeoId(ipGeoId);
            show.setFilter(FilterConstants.FILTER_0);
            show.setShowBlockId(blockId);
            shows.add(show);
            clickTime = clickTime.plusSeconds(2);
        }
        return shows;
    }

    private int getValidClickPP() {
        return RndUtil.choice(clicksPP);
    }


    @Override
    public String toString() {
        return "{" +
                "shows:" + showsCount +
                ", clicks:" + clicksCount +
                ", ofa:" + ofaCount +
                ", id:'" + getId() + "'" +
                ", context:'" + context + "'" +
                ", categId:'" + categId + "'" +
                ", PP:'" + commonPP + "'" +
                ", geoId:'" + geoId + "'" +
                "}";
    }

    public ClicksAndShows withShowRowidPrefix(String rowIdPrefix) {
        List<String> rowIds = Ids.uniqueRowIdStartingFrom(rowIdPrefix, showsCount);
        int i = 0;
        for (TestShow show : shows) {
            show.setRowid(rowIds.get(i));
            i++;
        }
        return this;
    }

    public ClicksAndShows withClicksEventTime(DateTime time) {
        for (TestClick click : clicks) {
            click.set("eventtime", time);
            time = time.plusSeconds(2);
        }
        return this;
    }

    public ClicksAndShows withShowsFilter(FilterConstants filter) {
        for (TestShow show : shows) {
            show.setFilter(filter);
        }
        return this;
    }

    public ClicksAndShows withClicksFilter(FilterConstants filter) {
        for (TestClick click: clicks) {
            click.setFilter(filter);
        }
        return this;
    }

    public ClicksAndShows withCommonClicksLinkId() {
        String linkId = clicks.get(0).get("link_id", String.class);
        for (TestClick click : clicks) {
            click.set("link_id", linkId);
            click.set("show_uid", linkId);
        }
        return this;
    }

    public ClicksAndShows withCommonClicksCookie() {
        String cookie = clicks.get(0).get("yandex_uid", String.class);
        for (TestClick click : clicks) {
            click.set("yandex_uid", cookie);
        }
        return this;
    }

    public ClicksAndShows withCommonShopId() {
        int shopId = ShopId.generate();
        for (TestClick click : clicks) {
            click.set("shop_id", shopId);
        }
        for (TestShow show : shows) {
            show.setShopId(shopId);
        }
        return this;
    }

    public ClicksAndShows withCommonBlockId() {
        DateTime time = shows.get(0).getEventtime();
        String blockId = ClickGenerator.generateBlockId(time);
        showUids.clear();

        for (TestShow show : shows) {
            String showUid = ClickGenerator.generateShowUid(blockId);
            showUids.put(showUid, blockId);
            show.setShowBlockId(blockId);
            show.setShowUid(showUid);
        }

        validateShowUidsForShowsGenerated(time, clicksCount);
        List<String> showUidsList = new ArrayList<>(showUids.keySet());
        int i = 0;
        for (TestClick click : clicks) {
            click.set("show_block_id", blockId);
            click.set("link_id", showUidsList.get(i));
            click.set("show_uid", showUidsList.get(i));
            i++;
        }
        return this;
    }

    public ClicksAndShows withReferer(String referer) {
        for (TestClick click : clicks) {
            click.set("referer", referer);
        }
        return this;
    }

    public ClicksAndShows withOfferPrice(String price) {
        getClicks().forEach(c -> c.set("offer_price", price));
        getShows().forEach(c -> c.setOfferPrice(price));
        getOfa().forEach(c -> c.set("offer_price", price));
        return this;
    }
}
