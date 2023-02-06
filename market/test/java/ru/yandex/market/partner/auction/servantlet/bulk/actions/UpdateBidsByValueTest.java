package ru.yandex.market.partner.auction.servantlet.bulk.actions;

import java.util.List;

import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.partner.auction.ReportRecommendationService;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.auction.AuctionService.DEFAULT_GROUP_ID;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasGroupId;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasId;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasLinkType;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasPlaceBid;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasSearchQuery;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasShopId;
import static ru.yandex.market.core.auction.model.AuctionBidValues.KEEP_OLD_BID_VALUE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_2;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_0_FOR_774;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_1_FOR_774;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.assertSuccessValidBidUpdate;

/**
 * Измение ставки через явное значение, без использования целей.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class UpdateBidsByValueTest extends AuctionBulkServantletlMockBase {
    @InjectMocks
    private ReportRecommendationService recommendationsService;

    @BeforeEach
    void before() {
        auctionBulkOfferBidsServantlet.configure();
        generalBulkServantletInit(recommendationsService);
        usefullServResponse = new MockServResponse();

        mockServRequestCrudActionUPDATE();
        mockServRequestIdentificationParams();
        mockRegionsAndTariff();


        mockBidLimits();
        mockAuctionExistingBid(SOME_TITLE_OFFER_ID, PARAM_DATASOURCE_ID);
        mockShopAuctionType(AuctionOfferIdType.TITLE);
    }

    /**
     * Изменение bid компоненты - что не требует изменения типа компоненты
     * Без поискового запроса.
     * Мокаем существование ставки для запрашиваемого ТП в сервисе биддинга.
     * Ожидаем что:
     * -в переданных в сервис биддинга значениях отражена только bid компонента, а остальные получают маркер
     * "без изменений".
     * -тип связи не меняется, остается тот, что был в сущестовавшей ставке.
     * -запрос также не меняется.
     */
    @Test
    void test_updateExistingBid_when_explicitBidOnlyValue() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.bid.value=" + 2
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasSearchQuery(SOME_OFFER_NAME));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));

        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_2));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_SEARCH, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertSuccessValidBidUpdate(usefullServResponse, 1);
    }

    /**
     * Тест аналогичный {@link #test_updateExistingBid_when_explicitBidOnlyValue}, но для типа идентификации
     * {@link AuctionOfferIdType#SHOP_OFFER_ID}.
     */
    @Test
    void test_updateExistingBid_when_explicitBidOnlyValue_and_offerId() {
        mockShopAuctionType(AuctionOfferIdType.SHOP_OFFER_ID);
        mockAuctionExistingBid(SOME_FEED_OFFER_ID, PARAM_DATASOURCE_ID);

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerId=" + SOME_FEED_OFFER_ID.getId() +
                "&req1.feedId=" + SOME_FEED_OFFER_ID.getFeedId() +
                "&req1.bid.value=" + 2
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_FEED_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasSearchQuery(SOME_FEED_OFFER_ID.getId()));//автоматически на основе имени в моке генерится
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));

        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_2));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_SEARCH, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertSuccessValidBidUpdate(usefullServResponse, 1);
    }

    /**
     * Меняем cbid компоненту, тип связи и поисковый запрос.
     * Мокаем существование ставки для запрашиваемого ТП в сервисе биддинга.
     * Ожидаем что:
     * -в переданных в сервис биддинга значениях отражена только cbid компонента, а остальные получают маркер
     * "без изменений".
     * -тип связи меняется
     * -запрос меняется.
     */
    @Test
    void test_updateExistingBid_when_explicitCbidValueAndSearchQueryAndLinkType() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.cbid.value=" + 2 +
                "&req1.linkType=" + AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY +
                "&searchQuery=new_query"
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasSearchQuery("new_query"));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));

        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, AUCTION_OFFER_BID_VALUE_2));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_SEARCH, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertSuccessValidBidUpdate(usefullServResponse, 1);
    }

    /**
     * Меняем группу.
     * Компоненты ставки не меняются, тип связи не меняется, поисковый запрос не меняется.
     * Меняется только группа.
     * <p>
     * Мокаем существование ставки для запрашиваемого ТП в сервисе биддинга и запрашиваемую группу.
     */
    @Test
    void test_updateExistingBid_when_changeGroupOnly() {
        //мокаем существование групп для магазина
        when(auctionService.getGroups(PARAM_DATASOURCE_ID))
                .thenReturn(
                        ImmutableList.of(
                                GROUP_0_FOR_774,
                                GROUP_1_FOR_774
                        )
                );

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.newGroup=" + GROUP_ID_1
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(GROUP_ID_1));
        assertThat(passedBidValue, hasSearchQuery(SOME_OFFER_NAME));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));

        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_SEARCH, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_FEE_PRIORITY));

        assertSuccessValidBidUpdate(usefullServResponse, 1);
    }


    /**
     * Проверяем, что поисковый запрос заданный поофферно имеет более выскоий приоритет и именно он доезжает
     * в запрос к серивсу биддинга на изменение.
     */
    @Test
    void test_updateExistingBid_when_passedPerOfferQueryAndGlobalQuery_should_usePerOffer() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&searchQuery=new_general_query" +
                "&req1.searchQuery=new_offer_query" +
                "&req1.bid.value=" + 2
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasSearchQuery("new_offer_query"));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));

        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_2));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_SEARCH, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertSuccessValidBidUpdate(usefullServResponse, 1);
    }

    /**
     * Изменение ставки с заданием явно указанных компонент, без подгрузки доп значений из репорта.
     * Поисковый запрос задан в общем виде.
     * Группа не задана - выставляется по умолчанию.
     * Установлен флаг синхронизации unified.
     * <p>
     * Проверяем, что в сервис биддинга передаются ожидаемые значения:
     * - идентификатор из запроса
     * - группа дефолтная
     * - тип сбрасывается в {@link AuctionBidComponentsLink#CARD_NO_LINK_CPC_ONLY}.
     * - {@link BidPlace#MARKET_PLACE} сбрасывается в null
     * - {@link BidPlace#SEARCH} устанавливается равной {@link BidPlace#CARD}
     */
    @DisplayName("Обновление ставок с флагом unified")
    @Test
    void test_updateBidWhenUnifiedSet() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&searchQuery=new_general_query" +
                "&req1.searchQuery=new_offer_query" +
                "&req1.bid.value=" + 2 +
                "&unified=true"
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasSearchQuery("new_offer_query"));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));

        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_2));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, AUCTION_OFFER_BID_VALUE_2));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, null));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertSuccessValidBidUpdate(usefullServResponse, 1);
    }

    /**
     * В случае задания флага offer_title_as_search_query, в качестве поискового запроса прикапывается именование оффера
     * {@link AuctionOfferBid#offerId}.
     */
    @DisplayName("Обновление ставки с маркером offer_title_as_search_query=true")
    @Test
    void test_updateBid_when_explicitSearchQueryFlag() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&searchQuery=new_general_query" +
                "&req1.searchQuery=new_offer_query" +
                "&req1.bid.value=" + 2 +
                "&offer_title_as_search_query=true"
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasSearchQuery(SOME_OFFER_NAME));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));

        assertSuccessValidBidUpdate(usefullServResponse, 1);
    }
}
