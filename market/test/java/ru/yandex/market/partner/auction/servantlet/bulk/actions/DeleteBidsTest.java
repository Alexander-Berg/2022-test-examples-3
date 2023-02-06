package ru.yandex.market.partner.auction.servantlet.bulk.actions;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.hamcrest.MockitoHamcrest;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.market.core.auction.model.AuctionBidStatus;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.auction.AuctionService.DEFAULT_GROUP_ID;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasGroupId;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasId;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasResetedBidValues;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasShopId;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasStatus;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasBidResetCount;
import static ru.yandex.market.partner.auction.matchers.MockServResponseFeatureMatchers.hasWarnings;

/**
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class DeleteBidsTest extends AuctionBulkServantletlMockBase {

    @BeforeEach
    void before() {
        auctionBulkOfferBidsServantlet.configure();

        usefullServResponse = new MockServResponse();

        mockServRequestCrudActionDELETE();
        mockServRequestIdentificationParams();
        mockRegionsAndTariff();
    }

    /**
     * Если в хранилище изменяемой ставки нет, вызова на сброс быть не должно.
     */
    @DisplayName("Для ставок, отсутствующих в биддинге, запроса на сброс не происходит")
    @Test
    void test_delete_should_notCallAuctionServiceMethod_when_passedCorrectValuesButNoExistingBids() {
        mockServantletPassedArgs(
                "offerName=" + SOME_TITLE_OFFER_ID.getId()
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();
        assertThat(setOfferBidsArgument, empty());

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasBidResetCount(0));
    }

    /**
     * Для существующих в хранилище ставок, происходит сброс актуальных значений.
     */
    @DisplayName("Сброс ставок по TITLE. Старый формат контейнера")
    @Test
    void test_delete_should_callAuctionServiceMethod_when_passedCorrectOfferTitles() {
        mockAuctionExistingBid(SOME_TITLE_OFFER_ID, PARAM_DATASOURCE_ID);

        mockServantletPassedArgs(
                "offerName=" + SOME_TITLE_OFFER_ID.getId()
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(passedBidValue, hasResetedBidValues());

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasBidResetCount(1));
    }

    @DisplayName("Сброс ставок по FEED-OFFER_ID. Новый формат контейнера")
    @Test
    void test_delete_should_callAuctionServiceMethod_when_passedCorrectFeedOfferIds() {
        mockAuctionExistingBid(SOME_FEED_OFFER_ID, PARAM_DATASOURCE_ID);

        mockServantletPassedArgs(
                "offer.size=1" +
                        "&offer1.id=" + SOME_FEED_OFFER_ID.getId() +
                        "&offer1.feedId=" + SOME_FEED_OFFER_ID.getFeedId()
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_FEED_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(passedBidValue, hasResetedBidValues());

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasBidResetCount(1));
    }

    @DisplayName("Сброс ставок по TITLE. Новый формат контейнера")
    @Test
    void test_delete_should_callAuctionServiceMethod_when_passedCorrectOfferIds() {
        mockAuctionExistingBid(SOME_TITLE_OFFER_ID, PARAM_DATASOURCE_ID);

        mockServantletPassedArgs(
                "offer.size=1" +
                        "&offer1.id=" + SOME_TITLE_OFFER_ID.getId()
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(passedBidValue, hasResetedBidValues());

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasBidResetCount(1));
    }

    /**
     * Явный тест на то, что даже если ставка в хранилище имеет статус {@link AuctionBidStatus#ERROR_OFFER_NOT_FOUND},
     * все равно разрешаем ее сбрасывать.
     * Были проблемы с этим связанные.
     */
    @DisplayName("Для ставок со статусом ERROR_OFFER_NOT_FOUND также происходит сброс")
    @Test
    void test_delete_should_allowBidReset_when_bidStatusIsNotFound() {
        //ставка в статусе не найдено в фиде
        AuctionOfferBid bidInNotFoundStatus = new AuctionOfferBid(
                PARAM_DATASOURCE_ID,
                SOME_TITLE_OFFER_ID,
                SOME_TITLE_OFFER_ID.getId(),
                DEFAULT_GROUP_ID,
                //не имеет значения какие именно компоненты заданы
                new AuctionBidValues(ImmutableMap.of(BidPlace.MARKET_PLACE, BigInteger.TEN))
        );
        bidInNotFoundStatus.setStatus(AuctionBidStatus.ERROR_OFFER_NOT_FOUND);

        //мок сущестования стави в статусе not-found при запросе по идентификаторам офферов
        when(
                auctionService.getOfferBids(
                        ArgumentMatchers.eq(PARAM_DATASOURCE_ID),
                        (Collection) MockitoHamcrest.argThat(
                                containsInAnyOrder(SOME_TITLE_OFFER_ID)
                        )
                )
        ).thenReturn(ImmutableList.of(bidInNotFoundStatus));

        //параметры запроса
        mockServantletPassedArgs(
                "offerName=" + SOME_TITLE_OFFER_ID.getId()
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));
        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(passedBidValue, hasStatus(AuctionBidStatus.ERROR_OFFER_NOT_FOUND));
        assertThat(passedBidValue, hasResetedBidValues());

        assertThat(usefullServResponse, not(hasWarnings()));
        assertThat(usefullServResponse, hasBidResetCount(1));
    }
}

