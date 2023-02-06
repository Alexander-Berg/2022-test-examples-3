package ru.yandex.market.partner.auction.servantlet.category;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.model.AuctionBidStatus;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionCategoryBid;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.core.auction.model.MarketCategory;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.partner.auction.AuctionBulkCommon;
import ru.yandex.market.partner.auction.AuctionCategoryBidsServantlet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link AuctionCategoryBidsServantlet}.
 */
@DisplayName("Структура/формат ответа auctionCategoryBids")
class CategoryBidsManagementFunctionalTest extends FunctionalTest {

    private static final Set<String> IGNORE_RESPONSE_ATTRIBUTES = ImmutableSet.of("host", "executing-time");
    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final long SHOP_ID_774 = 774;
    private static final long CAMPAIGN_ID_10774 = 10774;
    private static final long CATEGORY_ID_ALL = MarketCategory.ALL.getId();
    private static final long CATEGORY_ID_BOOKS = MarketCategory.BOOK.getId();
    private static final AuctionBidValues ALL_COMPONENTS_SET = AuctionBulkCommon.MIN_VALUES;
    private static final AuctionBidValues CBID_ONLY_COMPONENTS_SET =
            new AuctionBidValues.Builder().bid(BidPlace.CARD, 15).build();
    private static final AuctionBidValues BID_ONLY_COMPONENTS_SET =
            new AuctionBidValues.Builder().bid(BidPlace.SEARCH, 25).build();
    @Autowired
    private AuctionService auctionService;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(auctionService);
    }

    @DisplayName("Получение установленных значений")
    @Test
    @DbUnitDataSet(before = "db/CategoryBidsManagementFunctionalTest.before.csv")
    void test_getBids() throws ParseException {
        mockAuction(ALL_COMPONENTS_SET);
        final String url = baseUrl + "/auctionCategoryBids?id={campaignId}";
        final ResponseEntity<String> actualResponse = FunctionalTestHelper.get(url, CAMPAIGN_ID_10774);
        assertResponseVsExpectedFile("auction_category_response.xml", actualResponse);
    }

    @DisplayName("Получение установленных значений (unified=true)")
    @Test
    @DbUnitDataSet(before = "db/CategoryBidsManagementFunctionalTest.before.csv")
    void test_getBidsUnified() throws ParseException {
        mockAuction(ALL_COMPONENTS_SET);
        final String url = baseUrl + "/auctionCategoryBids?id={campaignId}&unified=true";
        final ResponseEntity<String> actualResponse = FunctionalTestHelper.get(url, CAMPAIGN_ID_10774);
        assertResponseVsExpectedFile("auction_category_response_unified.xml", actualResponse);
    }

    @DisplayName("Получение установленных значений cbid-only (unified=true)")
    @Test
    @DbUnitDataSet(before = "db/CategoryBidsManagementFunctionalTest.before.csv")
    void test_getBidsUnified_and_bidOnly() throws ParseException {
        mockAuction(CBID_ONLY_COMPONENTS_SET);
        final String url = baseUrl + "/auctionCategoryBids?id={campaignId}&unified=true";
        final ResponseEntity<String> actualResponse = FunctionalTestHelper.get(url, CAMPAIGN_ID_10774);
        assertResponseVsExpectedFile("auction_category_response_unified_cbid_only.xml", actualResponse);
    }

    @DisplayName("Получение установленных значений bid-only (unified=true)")
    @Test
    @DbUnitDataSet(before = "db/CategoryBidsManagementFunctionalTest.before.csv")
    void test_getBidsUnified_and_cbidOnly() throws ParseException {
        mockAuction(BID_ONLY_COMPONENTS_SET);
        final String url = baseUrl + "/auctionCategoryBids?id={campaignId}&unified=true";
        final ResponseEntity<String> actualResponse = FunctionalTestHelper.get(url, CAMPAIGN_ID_10774);
        assertResponseVsExpectedFile("auction_category_response_unified_bid_only.xml", actualResponse);
    }

    private void mockAuction(AuctionBidValues auctionBidValues) throws ParseException {

        final AuctionCategoryBid someBidsForAllCategory = new AuctionCategoryBid(
                SHOP_ID_774,
                CATEGORY_ID_ALL,
                auctionBidValues
        );

        someBidsForAllCategory.setStatus(AuctionBidStatus.PUBLISHED);

        final Date someModificationDate = FORMAT.parse("2018-01-02 06:04:05");

        someBidsForAllCategory.setModifedDate(someModificationDate);

        final List<AuctionCategoryBid> mockedBids = ImmutableList.of(
                someBidsForAllCategory,
                new AuctionCategoryBid(SHOP_ID_774, CATEGORY_ID_BOOKS, AuctionBidValues.fromSameBids(456))
        );

        when(auctionService.getCategoryBids(SHOP_ID_774))
                .thenReturn(mockedBids);

        //мок лимитов
        when(auctionService.getBidValuesLimitsForCategories(anyLong(), any(Set.class)))
                .thenReturn(
                        ImmutableMap.of(
                                CATEGORY_ID_ALL, AuctionBulkCommon.LIMITS,
                                CATEGORY_ID_BOOKS, AuctionBulkCommon.LIMITS
                        )
                );

    }

    private void assertResponseVsExpectedFile(
            String expectedResponseFile,
            ResponseEntity<String> response
    ) {
        String expectedOutput = StringTestUtil.getString(this.getClass(), expectedResponseFile);
        String actualOutput = response.getBody();
        System.out.println(actualOutput);
        MbiAsserts.assertXmlEquals(
                expectedOutput,
                actualOutput,
                IGNORE_RESPONSE_ATTRIBUTES
        );
    }
}
