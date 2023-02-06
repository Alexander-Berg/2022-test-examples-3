package ru.yandex.market.abo.core.offer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.abo.core.offer.report.Cpa;
import ru.yandex.market.abo.core.offer.report.IndexType;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.offer.report.ReportHelper;
import ru.yandex.market.abo.core.offer.report.ReportParam;
import ru.yandex.market.abo.core.offer.report.ShopSwitchedOffException;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.common.report.model.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author kukabara
 */
@Disabled
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-report.xml")
public class OfferServiceTest {
    private static final int TEST_SHOP = 774;
    private static final int MIN_PRICE = 1000;

    @Autowired
    private OfferService offerService;

    @Autowired
    private OfferService psOfferService;

    @Test
    public void testOfferInfo() throws Exception {
        String offerId = "523338";
        Long feedId = 398537L;
        long regionId = 11285L;
//        http://report.tst.vs.market.yandex.net:17051/yandsearch?ip=127.0.0.1&place=offerinfo&rids=213&show-urls=shop&regset=1&pp=18&
// feed_shoffer_id=398537-523338&client=abo
        Offer offer = offerService.findFirstWithParams(
                ReportParam.from(ReportParam.Type.FEED_OFFER_IDS,
                        Collections.singleton(new FeedOfferId(offerId, feedId))),
                ReportParam.from(ReportParam.Type.REGION_ID, regionId),
                ReportParam.from(ReportParam.Type.SUPPRESS_HIDDEN, 0)
        );
        assertNotNull(offer);
        assertEquals(feedId, offer.getFeedId());
        assertEquals(offerId, offer.getShopOfferId());
    }

    /**
     * Parse url from https://abo.market.yandex-team.ru/check/url
     */
    @Test
    public void testParseSearchUrl() throws Exception {
        String urlOffers = "http://market.yandex.ru/offers.xml?modelid=7151875&hid=90586&hyperid=7151875&" +
                "hideduplicate=0&mcpriceto=2570&fesh=23542";
        MarketSearchRequest request = ReportHelper.createRequestFromSearchUrl(
                urlOffers);
        assertEquals("23542", request.getParams().get("fesh").iterator().next());
        assertEquals("7151875", request.getParams().get("modelid").iterator().next());
        assertEquals("2570", request.getParams().get("mcpriceto").iterator().next());

        urlOffers = "http://market.yandex.ru/offers.xml?modelid=7268138&hid=90555&hyperid=7268138&grhow=shop&" +
                "how=aprice&np=1&fesh=148947";
        request = ReportHelper.createRequestFromSearchUrl(urlOffers);
        assertEquals("148947", request.getParams().get("fesh").iterator().next());

        String urlSearch = "http://market.yandex.ru/search.xml?hid=90668&how=aprice&np=1&text=&mcpricefrom=900&" +
                "mcpriceto=900&fesh=34845&glfilter=6126378:6126748&glfilter=6126560:6126727,6126644";
        request = ReportHelper.createRequestFromSearchUrl(urlSearch);
        assertEquals("34845", request.getParams().get("fesh").iterator().next());
        assertEquals("90668", request.getParams().get("hid").iterator().next());
        assertEquals("aprice", request.getParams().get("how").iterator().next());
    }

    @Test
    public void testByUrl() throws Exception {
        String url = "https://market.yandex.ru/product/1721921261/offers?hid=91491&track=tabs&glfilter=7893318%3A153043";
        List<Offer> offers = offerService.findWithParams(ReportParam.from(ReportParam.Type.URL, url));
        System.out.println(offers);
    }

    @Test
    public void testOffers() throws Exception {
        List<Offer> offers = offerService.findWithParams(ReportParam.from(ReportParam.Type.QUERY, "iphone"));
        assertFalse(offers.isEmpty());
    }

    @Test
    public void testModels() throws Exception {
        List<Model> models = offerService.findModels(Arrays.asList(12911795L, 12911796L));
        assertFalse(models.isEmpty());
        assertEquals(2, models.size());
    }

    @Test
    public void testCount() {
        ReportParam reportParam = ReportParam.from(ReportParam.Type.QUERY, "iphone");
        int newCount = offerService.countReportOffers(IndexType.MAIN, reportParam);
        assertTrue(newCount > 0);
    }

    @Test
    public void testCategories() {
        long shopId = 155L;
        ReportParam reportParam = ReportParam.from(ReportParam.Type.SHOP_ID, shopId);
        Map<Long, Long> categoriesWithOffersCount2 = offerService.findShopCategoriesWithOffersCount(
                IndexType.MAIN, reportParam
        );
        assertTrue(categoriesWithOffersCount2.size() > 0);
    }

    @Test
    public void testFind() throws Exception {
        int pageSize = 100;
        List<Offer> offers = offerService.findWithParams(
                ReportParam.from(ReportParam.Type.SHOP_ID, TEST_SHOP),
                ReportParam.from(ReportParam.Type.PAGE_SIZE, pageSize)
        );
        for (Offer o : offers) {
            assertEquals(Long.valueOf(TEST_SHOP), o.getShopId());
        }
        assertTrue(offers.size() <= pageSize);

        System.out.println(offers);
    }

    @Test
    public void testRandomizedOffersByShop() throws Exception {
        List<Offer> o = offerService.findWithParams(
                ReportParam.from(ReportParam.Type.SHOP_ID, TEST_SHOP),
                ReportParam.from(ReportParam.Type.CPA, Cpa.ExceptNo),
                ReportParam.from(ReportParam.Type.SORT, ReportParam.Value.SORT_BY_RANDOM),
                ReportParam.from(ReportParam.Type.CACHE, ReportParam.Value.CACHE_NO),
                ReportParam.from(ReportParam.Type.PRICE_FROM, MIN_PRICE)
        );
        System.out.println(o.size());
    }

    @Test
    void testFindBlue() throws ShopSwitchedOffException {
        long shopId = 10323642; // 10280775 - ps, 10362641 - ps
        List<Offer> offers = offerService.findWithParams(
                ReportParam.from(ReportParam.Type.SHOP_ID, shopId),
                ReportParam.from(ReportParam.Type.CPA, Cpa.Real),
                ReportParam.from(ReportParam.Type.SUPPLIER_ID, shopId),
                ReportParam.from(ReportParam.Type.COLLAPSE, ReportParam.Value.COLLAPSE_NO),
                ReportParam.from(ReportParam.Type.IGNORE_HAS_GONE, true),
                ReportParam.from(ReportParam.Type.REGION_ID, Regions.MOSCOW),
                ReportParam.from(ReportParam.Type.SORT, ReportParam.Value.SORT_BY_PRICE_ASC),
                ReportParam.from(ReportParam.Type.PAGE_SIZE, 50)
        );
        System.out.println(offers);
    }

    @Test
    void findDsbs() throws ShopSwitchedOffException {
        long shopId = 10456107; // dsbs shop
//        long shopId = 10456106; // dsbs shop
//        long shopId = 10537925  ; // dsbs shop
//        long shopId = 10534439  ; // dsbs shop
//        long shopId = 10464888  ; // dsbs shop
        List<Offer> offers = offerService.findWithParams(
//        List<Offer> offers = psOfferService.findWithParams(
                ReportParam.from(ReportParam.Type.SHOP_ID, shopId),
                ReportParam.from(ReportParam.Type.SUPPLIER_ID, shopId),
                ReportParam.from(ReportParam.Type.COLLAPSE, ReportParam.Value.COLLAPSE_NO),
                ReportParam.from(ReportParam.Type.REGION_ID, Regions.MOSCOW),
                ReportParam.from(ReportParam.Type.SORT, ReportParam.Value.SORT_BY_PRICE_ASC),
                ReportParam.from(ReportParam.Type.PAGE_SIZE, 50),
                ReportParam.from(ReportParam.Type.EXPERIMENT, ReportParam.Value.SHOW_DSBS_HIDDEN),
                ReportParam.from(ReportParam.Type.CPA, Cpa.Real)
        );
        System.out.println(offers);
    }

    @Test
    void cartDiffReportSearch() throws ShopSwitchedOffException {
//        long shopId = 10323642; // blue shop
//        FeedOfferId feedOfferId = new FeedOfferId("10323642-new.dimensions.481339", 200426700L);
//        long shopId = 10456106; // dsbs shop
//        FeedOfferId feedOfferId = new FeedOfferId("1426017", 200671971L);
        long shopId = 10456107; // dsbs shop
        FeedOfferId feedOfferId = new FeedOfferId("1426017", 200709584L);
        List<Offer> offers = offerService.findWithParams(
                ReportParam.from(ReportParam.Type.CPA, Cpa.Real),
//                ReportParam.from(ReportParam.Type.SHOP_ID, shopId),
//                ReportParam.from(ReportParam.Type.SUPPLIER_ID, shopId),
                ReportParam.from(ReportParam.Type.COLLAPSE, ReportParam.Value.COLLAPSE_NO),
                ReportParam.from(ReportParam.Type.FEED_OFFER_IDS, Set.of(feedOfferId)),
                ReportParam.from(ReportParam.Type.SUPPRESS_HIDDEN, 0),
                ReportParam.from(ReportParam.Type.EXPERIMENT, ReportParam.Value.SHOW_DSBS_HIDDEN),
                ReportParam.from(ReportParam.Type.REG_SET, ReportParam.Value.REG_SET_ON),
                ReportParam.from(ReportParam.Type.REGION_ID, Regions.MOSCOW)
//                ReportParam.from(ReportParam.Type.RGB, ru.yandex.market.common.report.model.Color.BLUE)
                );
        System.out.println(offers);
    }
}
