package ru.yandex.market.partner.auction.servantlet.bulk.actions;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.auction.AuctionService.DEFAULT_GROUP_ID;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasGroupId;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasId;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasLinkType;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasPlaceBid;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasSearchQuery;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasShopId;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_2;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_3;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_0_FOR_774;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_1_FOR_774;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SOME_QUERY;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.assertSuccessValidBidCreation;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasBidUpdateCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasFinalBidUpdateCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasValidBidCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasWarnings;

/**
 * Создание ставок с явно заданным значением.
 * Корневой сценарий проверки - проверяем, в каком виде передаются данные в сервис биддинга(в мок).
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class CreateBidsByValueTest extends AuctionBulkServantletlMockBase {

    @BeforeEach
    void beforeEach() {
        auctionBulkOfferBidsServantlet.configure();
        generalBulkServantletInit(recommendationsService);

        mockBidLimits();
        usefullServResponse = new MockServResponse();

        auctionBulkOfferBidsServantlet.setRecommendationsService(recommendationsService);

        mockServRequestCrudActionUPDATE();
        mockServRequestIdentificationParams();
        mockRegionsAndTariff();

        mockAuctionHasNoBids();
    }

    /**
     * Проверка создания с явно указанной группой per offer.
     */
    @Test
    void test_createBid_when_specifiedGroupPerOffer() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);

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
                "&searchQuery=" + SOME_QUERY +
                "&req1.bid.value=" + 2 +
                "&req1.newGroup=" + GROUP_ID_1
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);

        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(GROUP_ID_1));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(passedBidValue, hasSearchQuery(SOME_QUERY));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_2));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasBidUpdateCount(1));
        assertThat(usefullServResponse, hasValidBidCount(1));
        assertThat(usefullServResponse, hasFinalBidUpdateCount(1));
    }

    /**
     * Создание ставки с заданием явно указанных компонент, без подгрузки доп значений из репорта.
     * Поисковый запрос задан в общем виде.
     * Группа не задана - выставляется по умолчанию.
     * <p>
     * Проверяем, что в сервис биддинга передаются ожидаемые значения:
     * - компоненты из запроса
     * - идентификатор из запроса
     * - группа дефолтная
     * - тип связи переданный не имеет значения
     */
    @Test
    void test_createBid_when_passedCorrectValues_should_callAuctionServiceMethod() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&searchQuery=" + SOME_QUERY +
                "&req1.fee.value=1" +
                "&req1.cbid.value=3" +
                "&req1.bid.value=2" +
                "&req1.linkType=" + AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);

        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(passedBidValue, hasSearchQuery(SOME_QUERY));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, AUCTION_OFFER_BID_VALUE_1));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_2));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, AUCTION_OFFER_BID_VALUE_3));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasBidUpdateCount(1));
        assertThat(usefullServResponse, hasValidBidCount(1));
        assertThat(usefullServResponse, hasFinalBidUpdateCount(1));
    }

    /**
     * Создание ставки с заданием явно указанных компонент, без подгрузки доп значений из репорта.
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
    @DisplayName("Создание ставок с флагом unified")
    @Test
    void test_createBidWhenUnifiedSet() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&searchQuery=" + SOME_QUERY +
                "&req1.fee.value=1" +
                "&req1.cbid.value=3" +
                "&req1.bid.value=2" +
                "&req1.linkType=" + AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY +
                "&unified=true"
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);

        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(passedBidValue, hasSearchQuery(SOME_QUERY));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, AUCTION_OFFER_BID_VALUE_2));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_2));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, null));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasBidUpdateCount(1));
        assertThat(usefullServResponse, hasValidBidCount(1));
        assertThat(usefullServResponse, hasFinalBidUpdateCount(1));
    }

    /**
     * Проверка создания с явно указанным поисковым запросом + общим поисковым запросом.
     * per offer должен перегружать запрос заданный общий для оффера.
     * <p>
     * Остальные параметры неважны и используются просто для валидного запроса.
     */
    @Test
    void test_createBid_when_searchQueryGeneralAndPerOffer_should_usePerOffer() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&searchQuery=" + SOME_QUERY +
                "&req1.bid.value=" + 2 +
                "&req1.searchQuery=overloaded_query"
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);

        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(passedBidValue, hasSearchQuery("overloaded_query"));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_2));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasBidUpdateCount(1));
        assertThat(usefullServResponse, hasValidBidCount(1));
        assertThat(usefullServResponse, hasFinalBidUpdateCount(1));
    }

    /**
     * Можно создать ставку без общего поискового запроса, но с пооферным.
     * <p>
     * Остальные параметры неважны и используются просто для валидного запроса.
     */
    @Test
    void test_createBid_when_searchQueryOnlyInOfferData() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.bid.value=2" +
                "&req1.searchQuery=narrow_offer_query"
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);

        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(passedBidValue, hasSearchQuery("narrow_offer_query"));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_2));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasBidUpdateCount(1));
        assertThat(usefullServResponse, hasValidBidCount(1));
        assertThat(usefullServResponse, hasFinalBidUpdateCount(1));
    }

    /**
     * Можно создавть bid-only ставку без типа связи - тогда это будет дефолтный
     * тип - {@link AuctionBidComponentsLink#DEFAULT_LINK_TYPE}.
     */
    @Test
    void test_createBid_when_bidOnlyWithoutLinkType_should_createWithDefaultLinkType() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.bid.value=" + 1 +
                "&req1.searchQuery=" + SOME_QUERY
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(passedBidValue, hasSearchQuery(SOME_QUERY));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_1));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasValidBidCount(1));
        assertThat(usefullServResponse, hasBidUpdateCount(1));
        assertThat(usefullServResponse, hasFinalBidUpdateCount(1));
    }

    /**
     * В случае задания флага offer_title_as_search_query, в качестве поискового запроса прикапывается именование оффера
     * {@link AuctionOfferBid#offerId}.
     */
    @DisplayName("Создание ставки с маркером offer_title_as_search_query=true")
    @Test
    void test_createBidByTitleOffer_when_explicitSearchQueryFlag() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.bid.value=" + 1 +
                "&req1.searchQuery=" + SOME_QUERY +
                "&searchQuery=someGeneralQuery" +
                "&offer_title_as_search_query=true"

        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(passedBidValue, hasSearchQuery(SOME_OFFER_NAME));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_1));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertSuccessValidBidCreation(usefullServResponse, 1);
    }

    /**
     * Для {@link AuctionOfferIdType#SHOP_OFFER_ID} в случае задания флага offer_title_as_search_query,
     * в качестве поискового запроса прикапывается именование, полученное в ответе репорта.
     */
    @DisplayName("Создание ставки с маркером offer_title_as_search_query=true")
    @Test
    void test_createBidByIdOffer_when_explicitSearchQueryFlag() {
        mockShopAuctionType(AuctionOfferIdType.SHOP_OFFER_ID);
        mockTitles(ImmutableMap.of(SOME_FEED_OFFER_ID, "reportObtainedTitle"));
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerId=" + SOME_FEED_OFFER_ID.getId() +
                "&req1.feedId=" + SOME_FEED_OFFER_ID.getFeedId() +
                "&req1.bid.value=" + 1 +
                "&req1.searchQuery=" + SOME_QUERY +
                "&searchQuery=someGeneralQuery" +
                "&offer_title_as_search_query=true"

        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_FEED_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(passedBidValue, hasSearchQuery("reportObtainedTitle"));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_1));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertSuccessValidBidCreation(usefullServResponse, 1);
    }

}
