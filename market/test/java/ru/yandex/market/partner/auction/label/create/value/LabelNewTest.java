package ru.yandex.market.partner.auction.label.create.value;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.err.AuctionValidationException;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.BidModificationLabelManager;
import ru.yandex.market.partner.auction.BulkUpdateRequest;
import ru.yandex.market.partner.auction.label.ExpectedLabelBidsStats;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static ru.yandex.market.partner.auction.AuctionBulkCommon.UPD_REQ_SHOP_774_GID_1_SOME_Q;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.mockAuctionService;
import static ru.yandex.market.partner.auction.label.BulkUpdateRequestCombinations.combinationsA;
import static ru.yandex.market.partner.auction.label.ExpectedLabelBidsStats.UPDATE_OK;
import static ru.yandex.market.partner.auction.label.LabelCommon.testLabelModificationMethod;
import static ru.yandex.market.partner.auction.label.LabelCommon.withExpectedStatus;

/**
 * Маркировка новых ставок с указанием явного значения.
 * Тип связи более ничего не значит для рекомендаций. Кейсы будут поерзаны с удалением
 * {@link AuctionBidComponentsLink}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class LabelNewTest extends AbstractParserTest {
    @InjectMocks
    private static AuctionBulkOfferBidsServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> servantlet;
    @InjectMocks
    private static BidModificationLabelManager bidModificationLabelManager;
    @Mock
    private static AuctionService mockedAuctionService;

    private static Stream<Arguments> testCases() {
        List<Pair<BulkUpdateRequest, ExpectedLabelBidsStats>> args =
                ImmutableList.<Pair<BulkUpdateRequest, ExpectedLabelBidsStats>>
                        builder()
                        .addAll(withExpectedStatus(combinationsA("b-c-f-bc-bf-fc-bcf"), UPDATE_OK))
                        .build();

        return args.stream().map(p -> Arguments.of(p.getLeft(), p.getRight()));
    }

    @BeforeEach
    void beforeEach() {
        mockAuctionService(mockedAuctionService);
    }

    @DisplayName("Размекта новых ставок в labelBidModificationByType")
    @MethodSource("testCases")
    @ParameterizedTest(name = "[{index}]")
    void test_labelBidModificationByType(BulkUpdateRequest request, ExpectedLabelBidsStats stats) throws AuctionValidationException {
        testLabelModificationMethod(request, stats, UPD_REQ_SHOP_774_GID_1_SOME_Q, servantlet, bidModificationLabelManager);
    }

}
