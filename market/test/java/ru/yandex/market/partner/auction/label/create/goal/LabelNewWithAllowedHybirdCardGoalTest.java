package ru.yandex.market.partner.auction.label.create.goal;

import java.util.Arrays;
import java.util.Collection;

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
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.BidModificationLabelManager;
import ru.yandex.market.partner.auction.BulkUpdateRequest;
import ru.yandex.market.partner.auction.label.ExpectedLabelBidsStats;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static ru.yandex.market.partner.auction.AuctionBulkCommon.CARD_IRRELEVANT_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.UPD_REQ_SHOP_100_GID2_SOME_Q;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createUReqByName;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.mockAuctionService;
import static ru.yandex.market.partner.auction.label.ExpectedLabelBidsStats.RECOMMENDED_OK;
import static ru.yandex.market.partner.auction.label.LabelCommon.testLabelModificationMethod;

/**
 * Маркировка новых ставок для карточного типа.
 * Тип связи более ничего не значит для рекомендаций. Кейсы будут поерзаны с удалением
 * {@link AuctionBidComponentsLink}.
 *
 * @author vbudnev
 */
@RunWith(Parameterized.class)
public class LabelNewWithAllowedHybirdCardGoalTest extends AbstractParserTest {
    @InjectMocks
    private static AuctionBulkOfferBidsServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> servantlet;
    @InjectMocks
    private static BidModificationLabelManager bidModificationLabelManager;

    @Mock
    private static AuctionService mockedAuctionService;
    private BulkUpdateRequest request;
    private ExpectedLabelBidsStats expectedLabelBidsStats;

    public LabelNewWithAllowedHybirdCardGoalTest(BulkUpdateRequest request, ExpectedLabelBidsStats expectedLabelBidsStats) {
        this.request = request;
        this.expectedLabelBidsStats = expectedLabelBidsStats;
    }

    @Parameterized.Parameters(name = "{index}: request={0} expectedLabelBidsStatus={1})")
    public static Collection<Object[]> testCases() {
        return Arrays.asList(
                new Object[][]{
                        {createUReqByName(OFFER_NAME_1, CARD_IRRELEVANT_PLACE, null), RECOMMENDED_OK}
                }
        );
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        mockAuctionService(mockedAuctionService);
    }

    @Test
    public void test_labelBidModificationByType() throws AuctionValidationException {
        testLabelModificationMethod(request, expectedLabelBidsStats, UPD_REQ_SHOP_100_GID2_SOME_Q, servantlet, bidModificationLabelManager);
    }

}