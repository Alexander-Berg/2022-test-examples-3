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
import ru.yandex.market.core.auction.model.AuctionBidStatus;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.BidModificationLabelManager;
import ru.yandex.market.partner.auction.BulkUpdateRequest;
import ru.yandex.market.partner.auction.label.ExpectedLabelBidsStats;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY;
import static ru.yandex.market.core.auction.model.AuctionBidStatus.ERROR_OFFER_NOT_FOUND;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_2;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.HYBRID_CARD_PREM_FIRST_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_2;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_100;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.UPD_REQ_SHOP_100_GID2_SOME_Q;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createAuctionOfferBidWithoutValues;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createUReqByName;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.mockAuctionService;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.mockOfferBidsForShopAsExisting;
import static ru.yandex.market.partner.auction.label.ExpectedLabelBidsStats.HAS_WARNINGS;
import static ru.yandex.market.partner.auction.label.LabelCommon.testLabelModificationMethod;

/**
 * Если ставка для данного идентификатора существует но имеет статус {@link AuctionBidStatus#ERROR_OFFER_NOT_FOUND}
 * то изменения с использованием цели не принимаются.
 *
 * @author vbudnev
 */
@RunWith(Parameterized.class)
public class LabelExistingGoalModificationWhenNotFoundTest extends AbstractParserTest {
    @InjectMocks
    private static AuctionBulkOfferBidsServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> servantlet;
    @InjectMocks
    private static BidModificationLabelManager bidModificationLabelManager;

    @Mock
    private static AuctionService mockedAuctionService;
    private Pair<BulkUpdateRequest, ExpectedLabelBidsStats> testCase;

    public LabelExistingGoalModificationWhenNotFoundTest(Pair<BulkUpdateRequest, ExpectedLabelBidsStats> testCase) {
        this.testCase = testCase;
    }

    @Parameterized.Parameters(name = "{index}: testCase={0}")
    public static Collection<Object[]> testCases() {

        List<Pair<BulkUpdateRequest, ExpectedLabelBidsStats>> testCases = ImmutableList.of(
                Pair.of(createUReqByName(OFFER_NAME_2, HYBRID_CARD_PREM_FIRST_PLACE, CARD_NO_LINK_CBID_PRIORITY), HAS_WARNINGS)
        );

        return testCases.stream().map(x -> new Object[]{x}).collect(Collectors.toList());

    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        mockAuctionService(mockedAuctionService);
    }

    @Test
    public void test_labelModificationByType() throws AuctionValidationException {
        mockOfferBidsForShopAsExisting(mockedAuctionService,
                SHOP_ID_100,
                createAuctionOfferBidWithoutValues(SHOP_ID_100, GROUP_ID_2, OFFER_NAME_2, ERROR_OFFER_NOT_FOUND)
        );

        testLabelModificationMethod(testCase.getLeft(), testCase.getRight(), UPD_REQ_SHOP_100_GID2_SOME_Q, servantlet, bidModificationLabelManager);
    }

}