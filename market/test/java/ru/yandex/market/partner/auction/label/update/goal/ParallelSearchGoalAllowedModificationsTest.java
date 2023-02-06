package ru.yandex.market.partner.auction.label.update.goal;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.err.AuctionValidationException;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.BidModificationLabelManager;
import ru.yandex.market.partner.auction.BulkUpdateRequest;
import ru.yandex.market.partner.auction.label.ExpectedLabelBidsStats;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CPA_ONLY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.NOT_SPECIFIED;
import static ru.yandex.market.core.auction.model.AuctionBidStatus.PUBLISHED;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_2;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_2;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_3;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.PARALLEL_SEARCH_IRRELEVANT_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_100;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.UPD_REQ_SHOP_100_GID2_SOME_Q;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createAuctionOfferBidWithoutValues;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createUReqByName;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.mockAuctionService;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.mockOfferBidsForShopAsExisting;
import static ru.yandex.market.partner.auction.label.ExpectedLabelBidsStats.RECOMMENDED_OK;
import static ru.yandex.market.partner.auction.label.LabelCommon.testLabelModificationMethod;

/**
 * Изменение значений существующих ставок с использованием целей для параллельного поиска на основе типа связи оффера,
 * переданного в запросе.
 *
 * @author vbudnev
 */
@RunWith(Parameterized.class)
public class ParallelSearchGoalAllowedModificationsTest extends AbstractParserTest {
    @InjectMocks
    private static AuctionBulkOfferBidsServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> servantlet;
    @InjectMocks
    private static BidModificationLabelManager bidModificationLabelManager;

    @Mock
    private static AuctionService mockedAuctionService;
    private Pair<BulkUpdateRequest, ExpectedLabelBidsStats> testCase;

    public ParallelSearchGoalAllowedModificationsTest(Pair<BulkUpdateRequest, ExpectedLabelBidsStats> testCase) {
        this.testCase = testCase;
    }

    @Parameterized.Parameters(name = "{index}: testCase={0}")
    public static Collection<Object[]> testCases() {

        List<Pair<BulkUpdateRequest, ExpectedLabelBidsStats>> testCases = ImmutableList.of(
                Pair.of(createUReqByName(OFFER_NAME_1, PARALLEL_SEARCH_IRRELEVANT_PLACE, null), RECOMMENDED_OK),
                Pair.of(createUReqByName(OFFER_NAME_2, PARALLEL_SEARCH_IRRELEVANT_PLACE, null), RECOMMENDED_OK),
                Pair.of(createUReqByName(OFFER_NAME_3, PARALLEL_SEARCH_IRRELEVANT_PLACE, null), RECOMMENDED_OK)
        );

        return testCases.stream().map(x -> new Object[]{x}).collect(Collectors.toList());
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        mockAuctionService(mockedAuctionService);
    }

    @Test
    public void test_labelBidModificationByType_fillBidsToBeRecommended_shouldNot_allowExistingGoalModificationWithAnyCardLinkTypes_when_goalIsParallelSearch() throws AuctionValidationException {
        mockOfferBidsForShopAsExisting(mockedAuctionService,
                SHOP_ID_100,
                createAuctionOfferBidWithoutValues(SHOP_ID_100, GROUP_ID_2, OFFER_NAME_1, PUBLISHED, NOT_SPECIFIED),
                createAuctionOfferBidWithoutValues(SHOP_ID_100, GROUP_ID_2, OFFER_NAME_2, PUBLISHED, CARD_NO_LINK_CPA_ONLY)
        );

        testLabelModificationMethod(testCase.getLeft(), testCase.getRight(), UPD_REQ_SHOP_100_GID2_SOME_Q, servantlet, bidModificationLabelManager);
    }
}