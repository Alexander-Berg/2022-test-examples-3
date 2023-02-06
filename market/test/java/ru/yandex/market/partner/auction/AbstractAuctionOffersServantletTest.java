package ru.yandex.market.partner.auction;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.market.api.cpa.CpaCategory;
import ru.yandex.market.api.cpa.CpaType;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.SearchResults;
import ru.yandex.market.core.auction.recommend.BidRecommendations;
import ru.yandex.market.core.auction.recommend.OfferAuctionStats;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SOME_TITLE_OFFER_ID;

/**
 * @author vbudnev
 */
public class AbstractAuctionOffersServantletTest {
    private static final int CATEGORY_ID_1 = 1;
    private static final int CATEGORY_ID_2 = 2;
    private static final int CATEGORY_ID_3 = 3;
    private static final int CATEGORY_ID_100500 = 100500;
    private static final String REGIONS_1 = "1";
    private static final String REGIONS_213 = "213";
    private static final String REGIONS_1_2_3 = "1,2,3";
    private static final long REGION_100500 = 100500L;
    private static final long REGION_1 = 1L;
    private static final long REGION_2 = 2L;
    private static final long REGION_3 = 3L;
    private static final long REGION_213 = 213L;
    private static AbstractAuctionOffersServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> ABSTRACT_SERVANTLET_STUB;
    private static Map<Integer, CpaCategory> cpaCategories;

    @BeforeClass
    public static void onlyOnce() {

        ABSTRACT_SERVANTLET_STUB = new AbstractAuctionOffersServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest>() {
            @Override
            public void performRequest(PartnerDefaultRequestHandler.PartnerHttpServRequest request, ServResponse response) {
                //left blank
            }
        };

        cpaCategories = new HashMap<>();
        cpaCategories.put(CATEGORY_ID_1, new CpaCategory(CATEGORY_ID_1, CpaType.CPC_AND_CPA, 200, REGIONS_1));
        cpaCategories.put(CATEGORY_ID_2, new CpaCategory(CATEGORY_ID_2, CpaType.CPA_WITH_CPC_PESSIMIZATION, 200, REGIONS_213));
        cpaCategories.put(CATEGORY_ID_3, new CpaCategory(CATEGORY_ID_3, CpaType.HYBRID_CPA_ONLY, 200, REGIONS_1_2_3));
    }

    private static AuctionOffer buildOfferWithCategoryId(int categoryId) {
        FoundOffer foundOffer = new FoundOffer();
        foundOffer.setHyperCategoryId(categoryId);
        OfferAuctionStats stats = new OfferAuctionStats();
        stats.setOffer(foundOffer);

        //нужен для проверок на anythingFound
        SearchResults searchResults = new SearchResults();
        searchResults.setTotalOffers(1);

        BidRecommendations recommendations = new BidRecommendations(Arrays.asList(stats), searchResults);
        AuctionOffer offer = new AuctionOffer(SOME_TITLE_OFFER_ID);
        //offer.getRecommendatios uses firstNonNull so we can fill only single recommendations block
        offer.setModelCardHybridRecommendations(recommendations);
        return offer;
    }

    @Test
    public void test_fillOfferCpaType_when_noDataForCategoryIdFound_should_ignoreRegionCheck_and_return_CPC_AND_CPA() {
        AuctionOffer offer = buildOfferWithCategoryId(CATEGORY_ID_100500);
        ABSTRACT_SERVANTLET_STUB.fillOfferCpaType(offer, REGION_100500, Collections.EMPTY_MAP);
        assertEquals(CpaType.CPC_AND_CPA, offer.getCpaType());
    }

    @Test
    public void test_fillOfferCpaType_when_categoryCpaType_is_CpcAndCpa_should_ignoreRegionCheck_and_return_CPC_AND_CPA() throws Exception {
        AuctionOffer offer = buildOfferWithCategoryId(CATEGORY_ID_1);
        ABSTRACT_SERVANTLET_STUB.fillOfferCpaType(offer, REGION_100500, cpaCategories);
        assertEquals(CpaType.CPC_AND_CPA, offer.getCpaType());
    }

    @Test
    public void test_fillOfferCpaType_when_categoryCpaType_isNot_CpcAndCpa_and_region_equals_should_return_storedType() throws Exception {
        AuctionOffer offer = buildOfferWithCategoryId(CATEGORY_ID_2);
        ABSTRACT_SERVANTLET_STUB.fillOfferCpaType(offer, REGION_213, cpaCategories);
        assertEquals(CpaType.CPA_WITH_CPC_PESSIMIZATION, offer.getCpaType());
    }

    @Test
    public void test_fillOfferCpaType_when_categoryCpaType_isNot_CpcAndCpa_and_region_existsInList_should_return_storedType() throws Exception {
        AuctionOffer offer = buildOfferWithCategoryId(CATEGORY_ID_3);
        ABSTRACT_SERVANTLET_STUB.fillOfferCpaType(offer, REGION_1, cpaCategories);
        assertEquals(CpaType.HYBRID_CPA_ONLY, offer.getCpaType());
    }

    @Test
    public void test_fillOfferCpaType_when_categoryCpaType_isNot_CpcAndCpa_and_region_DoesNotMatch_should_return_CPC_AND_CPA() throws Exception {
        AuctionOffer offer = buildOfferWithCategoryId(CATEGORY_ID_3);
        ABSTRACT_SERVANTLET_STUB.fillOfferCpaType(offer, REGION_213, cpaCategories);
        assertEquals(CpaType.CPC_AND_CPA, offer.getCpaType());
    }
}