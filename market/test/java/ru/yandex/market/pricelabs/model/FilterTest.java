package ru.yandex.market.pricelabs.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.types.FilterCategoryType;
import ru.yandex.market.pricelabs.model.types.FilterMinBidMode;
import ru.yandex.market.pricelabs.model.types.FilterOfferType;
import ru.yandex.market.pricelabs.model.types.FilterPriceType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class FilterTest {

    @Test
    void testFilterSerializationEmpty() {
        var filter = new Filter();
        var json = Utils.toJsonPrettyString(filter);

        log.info("JSON:\n{}", json);
        log.info("Java: {}", filter);
        var restoredFilter = Utils.fromJsonString(json, Filter.class);

        assertEquals(filter, restoredFilter);
    }

    @Test
    void testFilterSerialization() {
        var filter = Utils.fromJsonResource("pricelabs/models/filter.json", Filter.class);
        assertNotNull(filter);
        var json = Utils.toJsonPrettyString(filter);

        log.info("JSON:\n{}", json);
        log.info("Java: {}", filter);
        var restoredFilter = Utils.fromJsonString(json, Filter.class);

        assertEquals(filter, restoredFilter);

    }

    @Test
    void testFilterDeserialization() {
        var expect = new Filter();
        expect.setShop_id(10);
        expect.setFeed_id(15);
        expect.setFilter_id(1500);
        expect.setCreated_at(Instant.ofEpochMilli(1542259723634L));
        expect.setUpdated_at(Instant.ofEpochMilli(1567644992055L));
        expect.setName("Тестовое имя");
        expect.setDescription("Тестовое описание");
        expect.setQuery("offer");
        expect.set_query_only_id(true);
        expect.setCategory_type(FilterCategoryType.NAME);
        expect.setCategory("cat1");
        expect.setCategories_by_id(Set.of(100L, 200L, 300L));
        expect.setPrice_from(7560);
        expect.setPrice_to(38720);
        expect.setOffer_type(FilterOfferType.ALL);
        expect.setVendors(Set.of("V1", "Vendor", "Test2"));
        expect.setMarket_vendors(Set.of(4, 1, 2));

        var rp = new Filter.RelativePrice();
        rp.setPrice_type_from(FilterPriceType.TOP5_AVG);
        rp.setPrice_from_strict_inequality(true);
        rp.setPrice_delta_from(57365);
        rp.setShop_rating_from(721345440);
        rp.setPrice_type_to(FilterPriceType.MAX);
        rp.setPrice_delta_to(32854);
        rp.setShop_rating_to(993760319);
        rp.setInclude_shipping(true);
        rp.setOnly_local_region(true);
        expect.setRelativePrices(rp);

        expect.setCount_offers_in_model(true);
        expect.setAvailable(false);

        var sh = new Filter.Shipping();
        sh.setFree_shipping(true);
        sh.setWith_discount(false);
        sh.setCut_price(null);
        expect.setShipping(sh);

        var pp = new Filter.PurchasePrice();
        pp.setPurchase_price_from(37557);
        pp.setPurchase_price_to(28581);
        expect.setPurchasePrice(pp);

        var cp = new Filter.CardPosition();
        cp.setCard_position_from(1);
        cp.setCard_position_to(5);
        expect.setCardPosition(cp);

        var st = new Filter.Strategies();
        st.setHas_strategy(true);
        st.setReached_strategy(true);
        expect.setStrategies(st);

        var b = new Filter.Bids();
        b.setOwn_bid_from(11893);
        b.setOwn_bid_to(48995);
        b.setDont_up_to_min(true);
        b.setMin_bid_from(13207);
        b.setMin_bid_to(35676);
        b.setMin_bid_mode(FilterMinBidMode.NOT_MATCHED);
        b.setRecommended_bid_position(4);
        b.setRecommended_bid_from(21125);
        b.setRecommended_bid_to(51442);
        expect.setBids(b);

        var c = new Filter.Clicks();
        c.setClicks_days(14);
        c.setClicks_count_from(13);
        c.setClicks_count_to(65);
        c.setClicks_price_from(8578);
        c.setClicks_price_to(20454);
        expect.setClicks(c);

        var o = new Filter.Orders();
        o.setOrders_days(14);
        o.setOrders_count_from(42);
        o.setOrders_count_to(62);
        o.setOrders_cpo_from(21963);
        o.setOrders_cpo_to(37105);
        o.setOrders_conversion_from(266.25);
        o.setOrders_conversion_to(382.95);
        o.setOrders_drr_from(3146);
        o.setOrders_drr_to(42380);
        expect.setOrders(o);

        expect.setHidden_offers(true);
        expect.setId_list(new ObjectLinkedOpenHashSet<>(List.of("offer1", "o2", "3")));
        expect.setSort_order(5);
        expect.setHash("...");

        Map<String, Filter.ParamFilter> params = new Object2ObjectLinkedOpenHashMap<>();
        params.put("param1", new Filter.ParamFilter(1, 100));
        params.put("param2", new Filter.ParamFilter(0, 255));
        params.put("TBHTBceCZWOgZbQS", new Filter.ParamFilter(10, 0));
        expect.setParams(params);

        var filter = Utils.fromJsonResource("pricelabs/models/filter.json", Filter.class);
        assertEquals(expect, filter);
    }
}
