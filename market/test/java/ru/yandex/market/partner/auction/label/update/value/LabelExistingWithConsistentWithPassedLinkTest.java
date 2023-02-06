package ru.yandex.market.partner.auction.label.update.value;

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
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.BidModificationLabelManager;
import ru.yandex.market.partner.auction.BulkUpdateRequest;
import ru.yandex.market.partner.auction.label.ExpectedLabelBidsStats;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_LINK_CBID_VARIABLE;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_LINK_FEE_VARIABLE;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CPA_ONLY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_FEE_PRIORITY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.NOT_SPECIFIED;
import static ru.yandex.market.core.auction.model.AuctionBidStatus.PUBLISHED;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_774;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.UPD_REQ_SHOP_774_GID_1_SOME_Q;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createAuctionOfferBidWithoutValues;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.mockAuctionService;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.mockOfferBidsForShopAsExisting;
import static ru.yandex.market.partner.auction.label.BulkUpdateRequestCombinations.combinationsA;
import static ru.yandex.market.partner.auction.label.ExpectedLabelBidsStats.UPDATE_OK;
import static ru.yandex.market.partner.auction.label.LabelCommon.testLabelModificationMethod;
import static ru.yandex.market.partner.auction.label.LabelCommon.withExpectedStatus;

/**
 * Изменение значений ставок с использованием явно заданных значений компонент и явно заданного типа связи.
 * Тип связи более ничего не значит для рекомендаций. Кейсы будут поерзаны с удалением
 * {@link AuctionBidComponentsLink}.
 *
 * @author vbudnev
 */
@RunWith(Parameterized.class)
public class LabelExistingWithConsistentWithPassedLinkTest extends AbstractParserTest {
    @InjectMocks
    private static AuctionBulkOfferBidsServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> servantlet;
    @InjectMocks
    private static BidModificationLabelManager bidModificationLabelManager;

    @Mock
    private static AuctionService mockedAuctionService;
    private Pair<BulkUpdateRequest, ExpectedLabelBidsStats> testCase;

    public LabelExistingWithConsistentWithPassedLinkTest(Pair<BulkUpdateRequest, ExpectedLabelBidsStats> testCase) {
        this.testCase = testCase;
    }

    @Parameterized.Parameters(name = "{index}: testCase={0}")
    public static Collection<Object[]> testCases() {
        List<Pair<BulkUpdateRequest, ExpectedLabelBidsStats>> testCases = ImmutableList.<Pair<BulkUpdateRequest, ExpectedLabelBidsStats>>
                builder()
                .addAll(withExpectedStatus(combinationsA("b-c-f-bf-bc-fc-bcf", OFFER_NAME_1, null), UPDATE_OK))

                .addAll(withExpectedStatus(combinationsA("b-c-f-bc-bf-fc-bcf", OFFER_NAME_1, CARD_NO_LINK_FEE_PRIORITY), UPDATE_OK))
                .addAll(withExpectedStatus(combinationsA("b-c-f-bc-bf-fc-bcf", OFFER_NAME_1, CARD_NO_LINK_CBID_PRIORITY), UPDATE_OK))
                .addAll(withExpectedStatus(combinationsA("b-c-f-bc-bf-fc-bcf", OFFER_NAME_1, CARD_LINK_CBID_VARIABLE), UPDATE_OK))
                .addAll(withExpectedStatus(combinationsA("b-c-f-bc-bf-fc-bcf", OFFER_NAME_1, CARD_LINK_FEE_VARIABLE), UPDATE_OK))
                .addAll(withExpectedStatus(combinationsA("b-c-f-bc-bf-fc-bcf", OFFER_NAME_1, CARD_NO_LINK_CPC_ONLY), UPDATE_OK))
                .addAll(withExpectedStatus(combinationsA("b-c-f-bc-bf-fc-bcf", OFFER_NAME_1, CARD_NO_LINK_CPA_ONLY), UPDATE_OK))
                .build();

        return testCases.stream().map(x -> new Object[]{x}).collect(Collectors.toList());
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        mockAuctionService(mockedAuctionService);
    }

    @Test
    public void test_labelBidModificationByTypeForExistingBid() throws AuctionValidationException {
        //существующий тип ставки не важен
        mockOfferBidsForShopAsExisting(
                mockedAuctionService,
                SHOP_ID_774,
                createAuctionOfferBidWithoutValues(SHOP_ID_774, GROUP_ID_1, OFFER_NAME_1, PUBLISHED, NOT_SPECIFIED)
        );

        testLabelModificationMethod(testCase.getLeft(), testCase.getRight(), UPD_REQ_SHOP_774_GID_1_SOME_Q, servantlet, bidModificationLabelManager);
    }

}