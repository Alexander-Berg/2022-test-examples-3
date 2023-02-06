package ru.yandex.market.partner.auction.label.create;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.err.AuctionValidationException;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.BidModificationLabelManager;
import ru.yandex.market.partner.auction.BulkUpdateRequest;
import ru.yandex.market.partner.auction.Labels;
import ru.yandex.market.partner.auction.request.AuctionBulkRequest;

import static ru.yandex.market.core.matchers.MapHasSize.mapHasSize;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.TITLE_VALUE_UPDATE_REQUESTS;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.UPD_REQ_SHOP_774_GID_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.UPD_REQ_SHOP_774_GID_1_SOME_Q;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.mockAuctionService;
import static ru.yandex.market.partner.auction.BulkUpdateRequest.Builder.builder;

/**
 * @author vbudnev
 */

/**
 * Соаздние ставок с использованием цели
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AuctionBulkOfferBidsServantletInvalidBidCreationTest extends AbstractParserTest {

    private static final Map<String, String> warnings = new HashMap<>();
    private static final Labels LABELS = new Labels();
    @InjectMocks
    private static BidModificationLabelManager bidModificationLabelManager;
    @InjectMocks
    private static AuctionBulkOfferBidsServantlet servantlet;
    @Mock
    private static AuctionService mockedAuctionService;

    @Before
    public void before() {
        warnings.clear();
        LABELS.clear();
        mockAuctionService(mockedAuctionService);
    }

    /**
     * При задании новой ставки обязательно указывать query
     * <br>Ожидаем warning блок
     * <br>Mock групп в {@link #beforeClass()}
     */
    @Test
    public void test_labelBidModificationByType_should_setWarning_when_newBidsRequestHasNoQuery()
            throws AuctionValidationException {
        bidModificationLabelManager.labelBidModificationByType(
                UPD_REQ_SHOP_774_GID_1,
                servantlet.validateAndMapToOfferId(
                        TITLE_VALUE_UPDATE_REQUESTS,
                        UPD_REQ_SHOP_774_GID_1,
                        warnings
                ),
                LABELS,
                warnings
        );

        assertThat("Must be warnings: " + warnings, warnings, mapHasSize(3));
    }

    /**
     * При задании ставки обязательно должна быть указана либо цель либо явное значение какой-либо из компонент.
     */
    @Test
    public void test_labelBidModificationByType_should_setWarning_when_bidHasNoGoalNoValue()
            throws AuctionValidationException {
        AuctionBulkRequest auctionRequest = UPD_REQ_SHOP_774_GID_1_SOME_Q;

        List<BulkUpdateRequest> titleUpdateRequestsWithGroup = Arrays.asList(
                builder()
                        .withOfferName(OFFER_NAME_1)
                        .build()
        );

        bidModificationLabelManager.labelBidModificationByType(
                auctionRequest,
                servantlet.validateAndMapToOfferId(
                        titleUpdateRequestsWithGroup,
                        auctionRequest,
                        warnings
                ),
                LABELS,
                warnings
        );

        assertThat("Must be warnings: " + warnings, warnings, mapHasSize(1));
    }


}