package ru.yandex.market.partner.auction.label;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.err.AuctionValidationException;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.BidModificationLabelManager;
import ru.yandex.market.partner.auction.BulkUpdateRequest;
import ru.yandex.market.partner.auction.Labels;
import ru.yandex.market.partner.auction.request.AuctionBulkRequest;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_FEE_PRIORITY;
import static ru.yandex.market.core.auction.model.AuctionBidStatus.PUBLISHED;
import static ru.yandex.market.core.matchers.MapHasSize.mapIsEmpty;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_111;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_2;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_3;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_2;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_100;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_774;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.UPD_REQ_SHOP_100_GID2;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createAuctionOfferBidWithoutValues;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createBulkServantletURequest;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createCbidUReqByName;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createEmptyUReqByNameWithGroup;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.mockAuctionService;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.mockOfferBidsForShopAsExisting;

/**
 * Общие тесты для {@link BidModificationLabelManager#labelBidModificationByType}
 *
 * @author vbudnev
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class LabelGeneralTest extends AbstractParserTest {
    private static final Map<String, String> warnings = new HashMap<>();
    private static final Labels LABELS = new Labels(1);
    @InjectMocks
    private static AuctionBulkOfferBidsServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> servantlet;
    @InjectMocks
    private static BidModificationLabelManager bidModificationLabelManager;
    @Mock
    private static AuctionService mockedAuctionService;

    @Before
    public void before() {
        warnings.clear();

        LABELS.clear();

        for (Long shopId : ImmutableList.of(SHOP_ID_774, SHOP_ID_100)) {
            when(
                    mockedAuctionService.getOfferBids(
                            eq(shopId),
                            any()
                    )
            ).thenReturn(Collections.emptyList());
        }

        mockAuctionService(mockedAuctionService);
    }

    /**
     * Разрешено изменение группы (без цели и значения) для уже существующих ставок
     */
    @Test
    public void test_labelBidModificationByType_fillOnlyGroupChangeBids_should_allowExistingBidModification_when_passedGroupOnlyChangeRequests() throws AuctionValidationException {
        when(mockedAuctionService.getOfferBids(eq(SHOP_ID_100), any()))
                .thenReturn(
                        Arrays.asList(
                                createAuctionOfferBidWithoutValues(SHOP_ID_100, GROUP_ID_3, OFFER_NAME_1, PUBLISHED),
                                createAuctionOfferBidWithoutValues(SHOP_ID_100, GROUP_ID_3, OFFER_NAME_2, PUBLISHED)
                        )
                );

        List<BulkUpdateRequest> ID_CHANGE_GROUP_2_UPDATE_REQUESTS = Arrays.asList(
                createEmptyUReqByNameWithGroup(OFFER_NAME_1, GROUP_ID_2),
                createEmptyUReqByNameWithGroup(OFFER_NAME_2, GROUP_ID_2)
        );

        bidModificationLabelManager.labelBidModificationByType(
                UPD_REQ_SHOP_100_GID2,
                servantlet.validateAndMapToOfferId(
                        ID_CHANGE_GROUP_2_UPDATE_REQUESTS,
                        UPD_REQ_SHOP_100_GID2,
                        warnings
                ),
                LABELS,
                warnings
        );

        assertThat("Must be no warnings: " + warnings, warnings, mapIsEmpty());
        assertThat("bidsToBeUpdated must be empty", LABELS.getBidsToBeValueUpdated(), empty());
        assertThat("bidsToBeRecommended build failed", LABELS.getBidsToBeRecommended(), empty());
        assertThat("onlyGroupChangeBids must be empty", LABELS.getOnlyGroupChangeBids(), hasSize(2));
    }

    /**
     * label должен обновлять searchQuery для уже существующей ставки, если передан в запросе.
     */
    @Test
    public void test_labelBidModificationByType_should_updateBidSearchQuery_when_passedInRequest() throws AuctionValidationException {
        AuctionOfferBid existingFakeBid = createAuctionOfferBidWithoutValues(SHOP_ID_100, GROUP_ID_3, OFFER_NAME_1, PUBLISHED);
        existingFakeBid.setSearchQuery("old_query");

        mockOfferBidsForShopAsExisting(mockedAuctionService,
                SHOP_ID_100,
                existingFakeBid
        );

        BulkUpdateRequest changeReq = createCbidUReqByName(OFFER_NAME_1, BID_CENTS_111, CARD_NO_LINK_FEE_PRIORITY);

        AuctionBulkRequest servantletReq = createBulkServantletURequest(
                SHOP_ID_100,
                GROUP_ID_3,
                "some_new_query"
        );

        bidModificationLabelManager.labelBidModificationByType(
                servantletReq,
                servantlet.validateAndMapToOfferId(
                        ImmutableList.of(changeReq),
                        servantletReq,
                        warnings
                ),
                LABELS,
                warnings
        );

        assertThat("Must be no warnings: " + warnings, warnings, mapIsEmpty());
        assertThat("bidsToBeUpdated must be empty", LABELS.getBidsToBeValueUpdated(), hasSize(1));
        assertThat("bidsToBeRecommended build failed", LABELS.getBidsToBeRecommended(), empty());
        assertThat("onlyGroupChangeBids must be empty", LABELS.getOnlyGroupChangeBids(), empty());

        assertThat(existingFakeBid.getSearchQuery(), is("some_new_query"));
    }

}