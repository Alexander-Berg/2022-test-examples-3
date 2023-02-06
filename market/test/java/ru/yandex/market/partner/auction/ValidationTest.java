package ru.yandex.market.partner.auction;

import java.util.ArrayList;
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
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.partner.auction.BulkUpdateRequest.Builder;
import ru.yandex.market.partner.auction.request.AuctionBulkRequest;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.matchers.MapHasSize.mapHasSize;
import static ru.yandex.market.core.matchers.MapHasSize.mapIsEmpty;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BIDREQ_222;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_0_DEFAULT;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_3;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_4;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_2;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_100;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_774;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.TITLE_VALUE_UPDATE_REQUESTS;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.TITLE_VALUE_UPDATE_REQUESTS_WITH_2_BROKEN;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.UPD_REQ_SHOP_774_GID_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createBulkServantletURequest;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createEmptyUReqByNameWithGroup;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.mockAuctionService;

/**
 * Валидация запроса создания/изменения ставки.
 *
 * @author vbudnev
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ValidationTest extends AbstractParserTest {
    private static final Map<String, String> warnings = new HashMap<>();
    private static final List<AuctionOfferBid> onlyGroupChangeBids = new ArrayList<>();
    private static final List<AuctionOffer> bidsToBeRecommended = new ArrayList<>();
    private static final List<AuctionOffer> bidsToBeUpdated = new ArrayList<>();
    @InjectMocks
    private static AuctionBulkOfferBidsServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> servantlet;
    @Mock
    private static AuctionService mockedAuctionService;
    private static Map<AuctionOfferId, BulkUpdateRequest> validRequestsByOfferId;

    @Before
    public void before() {
        mockAuctionService(mockedAuctionService);

        warnings.clear();

        onlyGroupChangeBids.clear();
        bidsToBeRecommended.clear();
        bidsToBeUpdated.clear();

        for (Long shopId : ImmutableList.of(SHOP_ID_774, SHOP_ID_100)) {
            when(mockedAuctionService.getOfferBids(eq(shopId), any()))
                    .thenReturn(Collections.emptyList());
        }
    }


    /**
     * Ожидаем warning c not-valid-bid для запросов {@link BulkUpdateRequest} проавлившийх проверку
     */
    @Test
    public void test_validateAndMapToOfferId_should_setWarning_when_passedNonValidTitleBids()
            throws AuctionValidationException {
        validRequestsByOfferId = servantlet.validateAndMapToOfferId(
                TITLE_VALUE_UPDATE_REQUESTS_WITH_2_BROKEN,
                UPD_REQ_SHOP_774_GID_1,
                warnings
        );

        assertThat("Must be warnings: " + warnings, warnings, mapHasSize(2));
    }

    /**
     * Группа указанная в {@link BulkUpdateRequest#newGroup} всегда должна существовать если !=null
     * <p>
     * Ожидаем warning с not-found-group
     * Mock групп в {@link #beforeClass()}
     */
    @Test
    public void test_validateAndMapToOfferId_should_setWarning_when_passedNonExistingNewGroup()
            throws AuctionValidationException {
        servantlet.validateAndMapToOfferId(
                ImmutableList.of(createEmptyUReqByNameWithGroup(OFFER_NAME_1, GROUP_ID_4)),
                createBulkServantletURequest(SHOP_ID_774, GROUP_ID_0_DEFAULT),
                warnings
        );
        assertThat("Must be warnings: " + warnings, warnings, mapHasSize(1));
    }

    /**
     * Ставка у которой не задана ни цель ни какое либо явное значение (но могут быть заданы нормировки по компонентам) - не является валидной.
     * Ожидаем warning с not-valid-bid
     */
    @Test
    public void test_validateAndMapToOfferId_should_setWarning_when_passedAdjacentBidWithoutGoalAndWithoutValue()
            throws AuctionValidationException {
        BidReq malformedBid = BidReq.Builder.builder().withMax(123).build();
        BulkUpdateRequest req = Builder.builder()
                .withCbid(malformedBid)
                .withOfferName(OFFER_NAME_1)
                .build();

        validRequestsByOfferId = servantlet.validateAndMapToOfferId(
                Arrays.asList(req),
                UPD_REQ_SHOP_774_GID_1,
                warnings
        );

        assertThat("Must be warnings: " + warnings, warnings, mapHasSize(1));
    }


    /**
     * Должен создавать идентификатор корректного типа - по {@link AuctionOfferIdType#TITLE}
     */
    @Test
    public void test_validateAndMapToOfferId_should_buildTitleBasedId_when_noErrors()
            throws AuctionValidationException {
        validRequestsByOfferId = servantlet.validateAndMapToOfferId(
                TITLE_VALUE_UPDATE_REQUESTS,
                UPD_REQ_SHOP_774_GID_1,
                warnings
        );

        assertThat("Must be no warnings: " + warnings, warnings, mapIsEmpty());
        assertThat("Result size is incorrect", validRequestsByOfferId, mapHasSize(3));
        for (Map.Entry<AuctionOfferId, BulkUpdateRequest> es : validRequestsByOfferId.entrySet()) {
            assertThat("Validation must produce TITLE offerId", es.getKey().getIdType(), is(AuctionOfferIdType.TITLE));
        }
    }

    /**
     * Группа указанная в {@link AuctionBulkRequest#groupId} всегда должна существовать, само поле есть всегда так как примитив
     * <p>
     * Mock групп в {@link #beforeClass()}
     */
    @Test(expected = AuctionValidationException.class)
    public void test_validateAndMapToOfferId_should_throw_when_passedNonExistingGroupId()
            throws AuctionValidationException {
        servantlet.validateAndMapToOfferId(
                TITLE_VALUE_UPDATE_REQUESTS,
                createBulkServantletURequest(SHOP_ID_774, GROUP_ID_3),
                warnings
        );
    }

    /**
     * Mock групп в {@link #beforeClass()}
     */
    @Test
    public void test_validateAndMapToOfferId_should_notThrow_when_passedCorrectGroup()
            throws AuctionValidationException {
        validRequestsByOfferId = servantlet.validateAndMapToOfferId(
                TITLE_VALUE_UPDATE_REQUESTS,
                createBulkServantletURequest(SHOP_ID_774, GROUP_ID_0_DEFAULT),
                warnings
        );

        assertThat("Result size is incorrect", validRequestsByOfferId, mapHasSize(3));
    }

    /**
     * Значение типа связи переданное в запросе более не имеет значения, и будет перезаписано до выпила класса связи
     * {@link ru.yandex.market.core.auction.model.AuctionBidComponentsLink}.
     */
    @Test
    public void test_validateAndMapToOfferId_shouldNot_setWarning_when_notSpecifiedExplicitlyPassed()
            throws AuctionValidationException {
        BulkUpdateRequest reqWithNotSpecifiedLinkType = Builder.builder()
                .withCbid(BIDREQ_222)
                .withOfferName(OFFER_NAME_1)
                .build();


        validRequestsByOfferId = servantlet.validateAndMapToOfferId(
                Arrays.asList(reqWithNotSpecifiedLinkType),
                UPD_REQ_SHOP_774_GID_1,
                warnings
        );

        assertThat("Must be no warnings:" + warnings, warnings, mapIsEmpty());
    }

    /**
     * Метод validate не делает валидацию совместимости компонент и типов связи.
     */
    @Test
    public void test_validateAndMapToOfferId_shouldNot_checkLinkTypeConsistency() throws AuctionValidationException {

        BulkUpdateRequest reqWithoutLinkType = Builder.builder()
                .withCbid(BIDREQ_222)
                .withOfferName(OFFER_NAME_1)
                .build();

        BulkUpdateRequest reqWithLinkType = Builder.builder()
                .withCbid(BIDREQ_222)
                .withOfferName(OFFER_NAME_2)
                .build();


        validRequestsByOfferId = servantlet.validateAndMapToOfferId(
                Arrays.asList(reqWithLinkType, reqWithoutLinkType),
                UPD_REQ_SHOP_774_GID_1,
                warnings
        );

        assertThat("Must be no warnings:" + warnings, warnings, mapIsEmpty());
        assertThat("Result size is incorrect", validRequestsByOfferId, mapHasSize(2));
    }


}