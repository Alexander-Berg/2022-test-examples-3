package ru.yandex.market.partner.auction.servantlet.bulk;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.SearchResults;
import ru.yandex.market.common.report.parser.xml.GeneralMarketReportXmlParser;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.matchers.MarketSearchRequestMatchers;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.WithCount;
import ru.yandex.market.core.auction.recommend.BidRecommendator;
import ru.yandex.market.partner.auction.AuctionBulkCommon;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.AuctionBulkValidator;
import ru.yandex.market.partner.auction.BidComponentsInferenceManager;
import ru.yandex.market.partner.auction.BidModificationLabelManager;
import ru.yandex.market.partner.auction.BidModificationManager;
import ru.yandex.market.partner.auction.GoalUpdateBidsHelperService;
import ru.yandex.market.partner.auction.RecommendationsService;
import ru.yandex.market.partner.auction.ReportRecommendationService;
import ru.yandex.market.partner.auction.servantlet.AuctionServantletMockBase;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static org.hamcrest.CoreMatchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.auction.AuctionService.DEFAULT_GROUP_ID;

/**
 * Вспомогательные методы для {@link AuctionBulkOfferBidsServantlet} специфичных тестов.
 */
public class AuctionBulkServantletlMockBase extends AuctionServantletMockBase {
    protected AuctionBulkValidator auctionBulkValidator = new AuctionBulkValidator();

    @InjectMocks
    protected ReportRecommendationService recommendationsService;

    @InjectMocks
    protected AuctionBulkOfferBidsServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> auctionBulkOfferBidsServantlet;

    @InjectMocks
    protected BidComponentsInferenceManager bidComponentsInferenceManager;

    @InjectMocks
    protected BidModificationLabelManager bidModificationLabelManager;

    @InjectMocks
    protected BidModificationManager bidModificationManager;

    protected void mockCheckHomeRegionInIndex() {
        when(marketReportParserFactory.newParser())
                .thenReturn(Mockito.mock(GeneralMarketReportXmlParser.class));

        when(
                marketReportService.async(
                        MockitoHamcrest.argThat(allOf(
                                MarketSearchRequestMatchers.hasPlace(MarketReportPlace.PRIME),
                                MarketSearchRequestMatchers.hasClient(AuctionServantletMockBase.PARTNER_INTERFACE_CLIENT)
                        )), any()
                )
        ).thenReturn(
                CompletableFuture.completedFuture(
                        new Pair<>(new SearchResults(),
                                Collections.emptyList()
                        )
                )
        );
    }

    /**
     * Непустое множество ставок, для мока {@link AuctionService} для дефолтной группы ставок.
     * На данный момент это НЕ обязательно полностью валидные данные.
     * Задача этого мока - удовлетворить precondition-ом для того, чтобы дойти до вызовов методов объектов
     * {@link BidRecommendator} на непустом множестве ТП.
     * <p>
     * offer id ставка создается с unknown search query, как будто ее поставили через ПАПИ
     *
     * @param title какую ставку засунуть в группу - title или offer id
     */
    protected void mockAuctionServicePartialBidsForDefaultGroup(boolean title) {
        List<AuctionOfferBid> someBids = ImmutableList.of(
                title ?
                        AuctionBulkCommon.createFakeExistingTitleOfferForAuctionService(
                                PARAM_DATASOURCE_ID,
                                DEFAULT_GROUP_ID,
                                SOME_OFFER_NAME
                        )
                        :
                        AuctionBulkCommon.createFakeExistingOfferIdForAuctionService(
                                PARAM_DATASOURCE_ID,
                                DEFAULT_GROUP_ID,
                                SOME_OFFER_ID,
                                SOME_FEED_ID
                        )
        );

        //мокаем отсутствие групп ставок в биддинге
        when(auctionService.getGroupBids(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(new WithCount<>(
                                someBids.size(),
                                someBids
                        )
                );

    }

    protected void generalBulkServantletInit(RecommendationsService recommendationsService) {
        auctionBulkOfferBidsServantlet.setRecommendationsService(recommendationsService);
        auctionBulkOfferBidsServantlet.setBidComponentsInferenceManager(bidComponentsInferenceManager);
        auctionBulkOfferBidsServantlet.setBidModificationLabelManager(bidModificationLabelManager);
        auctionBulkOfferBidsServantlet.setBidModificationManager(bidModificationManager);
        auctionBulkOfferBidsServantlet.setAuctionBulkValidator(auctionBulkValidator);
        auctionBulkOfferBidsServantlet.setOfferTitleClarificationService(offerTitleClarificationService);
        auctionBulkOfferBidsServantlet.setGoalUpdateBidsHelperService(
                new GoalUpdateBidsHelperService(bidComponentsInferenceManager, recommendationsService)
        );
    }
}
