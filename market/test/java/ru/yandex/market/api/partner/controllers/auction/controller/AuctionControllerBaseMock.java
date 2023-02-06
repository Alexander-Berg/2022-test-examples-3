package ru.yandex.market.api.partner.controllers.auction.controller;

import java.util.Collections;

import org.mockito.Mock;

import ru.yandex.market.api.partner.controllers.auction.model.OfferBidsRecommendations;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiAuctionAbstractRecommender;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiAuctionMarketSearchRecommender;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiAuctionModelCardRecommender;
import ru.yandex.market.api.partner.controllers.util.FeedHelper;
import ru.yandex.market.api.partner.request.PartnerServletRequest;
import ru.yandex.market.api.partner.request.PartnerServletResponse;
import ru.yandex.market.api.partner.view.ContainerModel;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.BidLimits;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionBidValuesLimits;
import ru.yandex.market.core.auction.model.AuctionNAReason;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.model.Region;
import ru.yandex.market.core.protocol.ProtocolService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Базовые mock-и компоннет и константы для тестов контроллера.
 */
public class AuctionControllerBaseMock {
    public static final Long PARAM_CAMPAIGN_ID = 1L;
    public static final String SOME_OFFER_NAME = "someOfferName";
    public static final Long PARAM_REGION_ID = 213L;
    public static final Long SOME_LOCAL_REGION_ID = 456L;
    public static final Long PARAM_REGION_NOT_SPECIFIED = null;
    public static final Long PARAM_INVALID_REGION_ID = 123456789L;
    public static final Long SHOP_WITH_TITLE_OFFERS_IDENTIFICATION = 1001L;
    public static final Long SHOP_WITH_ID_OFFERS_IDENTIFICATION = 1000L;
    public static final Long PARAM_CAMPAIGN_WITH_TITLE_OFFERS_IDENTIFICATION = 5000L;
    public static final Long PARAM_CAMPAIGN_WITH_ID_OFFERS_IDENTIFICATION = 5001L;
    @Mock
    public PartnerServletRequest request;
    @Mock
    public PartnerServletResponse response;
    @Mock
    public ProtocolService protocolService;
    @Mock
    public RegionService regionService;
    @Mock
    public CampaignService campaignService;
    @Mock
    public AuctionService auctionService;
    @Mock
    public FeedHelper feedHelper;
    @Mock
    public BidLimits bidLimits;
    @Mock
    public ApiAuctionAbstractRecommender parallelSearchRecommender;
    @Mock
    public ApiAuctionModelCardRecommender modelCardRecommender;
    @Mock
    public ApiAuctionMarketSearchRecommender marketSearchRecommender;
    @Mock
    public DatasourceService datasourceService;
    @Mock
    public ContainerModel.Factory containerModelFactory;

    public void mockRegions() {
        when(regionService.getRegion(PARAM_REGION_ID))
                .thenReturn(new Region(PARAM_REGION_ID, "region", null));
    }

    public void mockCampaignsToShops() {
        when(campaignService.getDatasourceId(PARAM_CAMPAIGN_WITH_TITLE_OFFERS_IDENTIFICATION))
                .thenReturn(SHOP_WITH_TITLE_OFFERS_IDENTIFICATION);

        when(campaignService.getDatasourceId(PARAM_CAMPAIGN_WITH_ID_OFFERS_IDENTIFICATION))
                .thenReturn(SHOP_WITH_ID_OFFERS_IDENTIFICATION);
    }

    public void mockAuction() {
        when(auctionService.getAuctionOfferIdType(SHOP_WITH_ID_OFFERS_IDENTIFICATION))
                .thenReturn(AuctionOfferIdType.SHOP_OFFER_ID);
        when(auctionService.canManageAuction(SHOP_WITH_ID_OFFERS_IDENTIFICATION))
                .thenReturn(AuctionNAReason.NONE);


        when(auctionService.getAuctionOfferIdType(SHOP_WITH_TITLE_OFFERS_IDENTIFICATION))
                .thenReturn(AuctionOfferIdType.TITLE);
        when(auctionService.canManageAuction(SHOP_WITH_TITLE_OFFERS_IDENTIFICATION))
                .thenReturn(AuctionNAReason.NONE);
    }

    public void mockRecommendersWithEmptyRepsonses() {
        OfferBidsRecommendations EMPTY_RECOMMENDATIONS = new OfferBidsRecommendations(Collections.emptyList(), false);
        when(parallelSearchRecommender.getRecommendations(any()))
                .thenReturn(EMPTY_RECOMMENDATIONS);
        when(marketSearchRecommender.getRecommendations(any()))
                .thenReturn(EMPTY_RECOMMENDATIONS);
        when(modelCardRecommender.getRecommendations(any()))
                .thenReturn(EMPTY_RECOMMENDATIONS);
    }

    protected void mockBidLimits() {
        AuctionBidValues minValues = AuctionBidValues.fromSameBids(1);
        AuctionBidValues maxValues = AuctionBidValues.fromSameBids(5000);
        when(bidLimits.limits())
                .thenReturn(new AuctionBidValuesLimits(minValues, maxValues));

    }

}
