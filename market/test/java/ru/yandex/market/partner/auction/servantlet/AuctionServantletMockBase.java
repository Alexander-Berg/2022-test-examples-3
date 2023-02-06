package ru.yandex.market.partner.auction.servantlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.common.framework.http.HttpServResponse;
import ru.yandex.market.api.cpa.CPAPlacementService;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.SearchResults;
import ru.yandex.market.common.report.parser.xml.GeneralMarketReportXmlParserFactory;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.BidLimits;
import ru.yandex.market.core.auction.dto.report.ReportRecommendationsAnswerOrError;
import ru.yandex.market.core.auction.err.AuctionGroupOwnershipViolationException;
import ru.yandex.market.core.auction.err.BidIdTypeConflictException;
import ru.yandex.market.core.auction.err.BidValueLimitsViolationException;
import ru.yandex.market.core.auction.err.InvalidOfferNameException;
import ru.yandex.market.core.auction.err.InvalidSearchQueryException;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionBidStatus;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionBidValuesLimits;
import ru.yandex.market.core.auction.model.AuctionCategoryBid;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.core.auction.model.WithCount;
import ru.yandex.market.core.auction.recommend.BidRecommendationRequest;
import ru.yandex.market.core.auction.recommend.BidRecommendations;
import ru.yandex.market.core.auction.recommend.BidRecommendator;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.model.Region;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.security.Campaignable;
import ru.yandex.market.core.tariff.TariffService;
import ru.yandex.market.core.tariff.model.Tariff;
import ru.yandex.market.partner.auction.AuctionBulkCommon;
import ru.yandex.market.partner.auction.LightOfferExistenceChecker;
import ru.yandex.market.partner.auction.OfferTitleClarificationService;
import ru.yandex.market.partner.auction.servantlet.bulk.PartiallyRecommendatorsFactory;
import ru.yandex.market.partner.auction.view.SerializationGate;
import ru.yandex.market.partner.servant.DataSourceable;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.common.framework.core.AbstractCrudServantlet.ACTION_PARAM_NAME;
import static ru.yandex.common.framework.core.AbstractCrudServantlet.DELETE_ACTION_VALUE;
import static ru.yandex.common.framework.core.AbstractCrudServantlet.REQUEST_ACTION_VALUE;
import static ru.yandex.common.framework.core.AbstractCrudServantlet.UPDATE_ACTION_VALUE;
import static ru.yandex.market.core.auction.AuctionService.DEFAULT_GROUP_ID;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_2;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_3;

/**
 * Базовые mock-и компонент,вспомогательные методы и поля, используемые для тестов следующих компонент:
 * {@link ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet}
 * {@link ru.yandex.market.partner.auction.SearchAuctionOffersServantlet}
 */
public abstract class AuctionServantletMockBase {

    protected static final Long PARAM_CAMPAIGN_ID = 100123L;
    protected static final BigDecimal PARAM_QUALITY_FACTOR = new BigDecimal("0.4");
    protected static final Long PARAM_DATASOURCE_ID = 774L;
    protected static final Long PARAM_REGION_ID = 213L;
    protected static final Long PARAM_INCORRECT_REGION_ID = 123456L;
    protected static final Long LOCAL_DELIVERY_REGION_ID = 2L;
    protected static final String SOME_OFFER_NAME = "someOfferName";
    protected static final Long SOME_FEED_ID = 10067L;
    protected static final String SOME_OFFER_ID = "someOfferId";
    protected static final AuctionOfferId SOME_TITLE_OFFER_ID = new AuctionOfferId(SOME_OFFER_NAME);
    protected static final AuctionOfferId SOME_FEED_OFFER_ID = new AuctionOfferId(SOME_FEED_ID, SOME_OFFER_ID);
    protected static final AuctionOfferId SHOFFER_ID_200304546_1482033 = new AuctionOfferId(200304546L, "1482033");
    protected static final String SOME_SEARCH_QUERY = "someSearchQuery";
    private static final String PRIME_RESOURCE_PATH = "./serialization/resources/";

    public static final String PARTNER_INTERFACE_CLIENT = "partnerinterface";

    @Mock(extraInterfaces = {Campaignable.class, DataSourceable.class})
    public PartnerDefaultRequestHandler.PartnerHttpServRequest servRequest;
    /**
     * Для тестов, в которых нет необходиммости разбирать состав ответа.
     */
    @Mock
    public HttpServResponse servResponse;
    @Mock
    public TariffService tariffService;
    @Mock
    public ProtocolService protocolService;
    @Mock
    public RegionService regionService;
    @Mock
    public DatasourceService datasourceService;
    @Mock
    public AuctionService auctionService;
    @Mock
    public BidLimits bidLimits;
    @Mock
    public CPAPlacementService cpaPlacementService;
    @Mock
    public GeneralMarketReportXmlParserFactory marketReportParserFactory;
    @Mock
    public AsyncMarketReportService marketReportService;
    @Mock
    public BidRecommendator modelCardBidRecommendator;
    @Mock
    public BidRecommendator parallelSearchBidRecommendator;
    @Mock
    public BidRecommendator marketSearchBidRecommendator;
    @Mock
    public LightOfferExistenceChecker mockedExistenceChecker;
    @Mock
    public OfferTitleClarificationService offerTitleClarificationService;
    protected MockServResponse usefullServResponse;

    /**
     * use {@link #extractRecommendatorLoadPassedRequest}
     */
    @Deprecated
    protected BidRecommendationRequest extractRecommendatorPassedRequest(BidRecommendator recommendator) {
        ArgumentCaptor<BidRecommendationRequest> requestCaptor
                = ArgumentCaptor.forClass(BidRecommendationRequest.class);
        verify(recommendator).calculate(requestCaptor.capture());

        return requestCaptor.getValue();
    }

    protected BidRecommendationRequest extractRecommendatorLoadPassedRequest(BidRecommendator recommendator) {
        ArgumentCaptor<BidRecommendationRequest> requestCaptor
                = ArgumentCaptor.forClass(BidRecommendationRequest.class);
        verify(recommendator).loadRecommendations(requestCaptor.capture());

        return requestCaptor.getValue();
    }

    protected void mockServRequestIdentificationParams() {
        when(((Campaignable) servRequest).getCampaignId())
                .thenReturn(PARAM_CAMPAIGN_ID);
        when(((DataSourceable) servRequest).getDatasourceId())
                .thenReturn(PARAM_DATASOURCE_ID);
    }

    protected void mockServRequestCrudActionREAD() {
        when(servRequest.getParam(eq(ACTION_PARAM_NAME), anyBoolean()))
                .thenReturn(REQUEST_ACTION_VALUE);
    }

    protected void mockServRequestCrudActionDELETE() {
        when(servRequest.getParam(eq(ACTION_PARAM_NAME), anyBoolean()))
                .thenReturn(DELETE_ACTION_VALUE);
    }

    protected void mockServRequestCrudActionUPDATE() {
        when(servRequest.getParam(eq(ACTION_PARAM_NAME), anyBoolean()))
                .thenReturn(UPDATE_ACTION_VALUE);
    }

    protected void mockRecommendationServiceEmptyCalculateResult() {
        //пустые рекомендации, содержимое не должно использоваться. Нужны только для mock-ов вызовов.
        BidRecommendations MUST_NOT_BE_USED_RECOMMENDATIONS
                = new BidRecommendations(Collections.emptyList(), new SearchResults());

        ReportRecommendationsAnswerOrError MUST_NOT_BE_USED_ANSWER
                = Mockito.mock(ReportRecommendationsAnswerOrError.class);

        when(parallelSearchBidRecommendator.calculate(any(BidRecommendationRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(MUST_NOT_BE_USED_RECOMMENDATIONS));

        when(modelCardBidRecommendator.loadRecommendations(any(BidRecommendationRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(MUST_NOT_BE_USED_ANSWER));

        when(marketSearchBidRecommendator.calculate(any(BidRecommendationRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(MUST_NOT_BE_USED_RECOMMENDATIONS));

    }

    /**
     * Мокаем параметры {@link PartnerDefaultRequestHandler.PartnerHttpServRequest} на основе строки
     * формата "arg1=val1&arg2=val2&arg3=val3"
     */
    protected void mockServantletPassedArgs(String s) {
        Arrays.stream(s.split("&")).forEach(optionLine -> {
                    String[] paramValuePair = optionLine.split("=");
                    mockServRequestOptionalParam(paramValuePair[0], paramValuePair[1]);
                }
        );
    }

    //todo rename to nullable param?
    private void mockServRequestOptionalParam(String paramName, @Nonnull Object value) {
        when(servRequest.getParam(eq(paramName), anyBoolean()))
                .thenReturn(String.valueOf(value));
    }

    protected void mockShopAuctionType(AuctionOfferIdType type) {
        when(auctionService.getAuctionOfferIdType(PARAM_DATASOURCE_ID))
                .thenReturn(type);
    }

    protected void mockBidLimits() {
        AuctionBidValues minValues = AuctionBidValues.fromSameBids(1);
        AuctionBidValues maxValues = AuctionBidValues.fromSameBids(5000);

        when(bidLimits.limits())
                .thenReturn(new AuctionBidValuesLimits(minValues, maxValues));

    }

    /**
     * Дает положительный результать при проверке {@link AbstractAuctionServantlet#canManageAuction}.
     */
    protected void mockRegionsAndTariff() {
        when(tariffService.getTariffByCampaign(PARAM_CAMPAIGN_ID))
                .thenReturn(Mockito.mock(Tariff.class));

        when(datasourceService.getLocalDeliveryRegion(PARAM_DATASOURCE_ID))
                .thenReturn(LOCAL_DELIVERY_REGION_ID);

        when(regionService.getRegion(PARAM_REGION_ID)).thenReturn(new Region(PARAM_REGION_ID, "Region", null));
        when(regionService.getRegion(LOCAL_DELIVERY_REGION_ID)).thenReturn(new Region(LOCAL_DELIVERY_REGION_ID, "Local Region", null));

    }


    /**
     * Перегруженный варианта для {@link #mockAuctionExistingBid(AuctionOfferId, long, long)}, использующий дефолтную
     * группу.
     */
    protected void mockAuctionExistingBid(AuctionOfferId offerId, long shopId) {
        mockAuctionExistingBid(offerId, DEFAULT_GROUP_ID, shopId);
    }

    /**
     * Мокаем существование ставки в биддинге (значение полей кроме идентификатора могут быть произвольными)
     * Для корректноого получения "существующих" ставко по id, запрос на получение {@link AuctionService#getOfferBids}
     * должен содержать только один такой-же идентификатор, так как это улсовие используется в матчере.
     */
    protected void mockAuctionExistingBid(AuctionOfferBid existingBid, AuctionOfferId offerId, long groupId, long shopId) {

        List<AuctionOfferBid> existingBids = ImmutableList.of(existingBid);
        //мок при запросе по идентификаторам офферов
        when(
                auctionService.getOfferBids(
                        ArgumentMatchers.eq(shopId),
                        (Collection) MockitoHamcrest.argThat(
                                containsInAnyOrder(offerId)
                        )
                )
        ).thenReturn(existingBids);

        //мок при запросе по идентификатору группы
        when(
                auctionService.getGroupBids(
                        ArgumentMatchers.eq(shopId),
                        ArgumentMatchers.eq(groupId),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(
                new WithCount<>(
                        existingBids.size(),
                        existingBids
                )
        );

        //мок при запросе по идентификатору группы
        when(
                auctionService.getGroupOffers(
                        ArgumentMatchers.eq(shopId),
                        ArgumentMatchers.eq(groupId),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(
                new WithCount<>(
                        existingBids.size(),
                        existingBids.stream().map(x -> x.getOfferId().getId()).collect(Collectors.toList())
                )
        );

    }

    protected void mockAuctionExistingBid(AuctionOfferId offerId, long groupId, long shopId, AuctionBidComponentsLink linkType) {
        AuctionOfferBid existingBid = new AuctionOfferBid(
                shopId,
                offerId,
                offerId.getId(),
                groupId,
                //конкретные цифры неважны для мока, но для удобства проверок пусть заданы все плейсы
                new AuctionBidValues(
                        ImmutableMap.of(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_1,
                                BidPlace.CARD, AUCTION_OFFER_BID_VALUE_2,
                                BidPlace.MARKET_PLACE, AUCTION_OFFER_BID_VALUE_3
                        )
                )
        );
        existingBid.setStatus(AuctionBidStatus.PUBLISHED);
        existingBid.setLinkType(linkType);

        mockAuctionExistingBid(existingBid, offerId, groupId, shopId);
    }


    protected void mockAuctionExistingBid(AuctionOfferId offerId, long groupId, long shopId) {
        mockAuctionExistingBid(offerId, groupId, shopId, AuctionBidComponentsLink.DEFAULT_LINK_TYPE);
    }

    /**
     * Хранилище на все запросы отдает пустые коллекции.
     */
    protected void mockAuctionHasNoBids() {
        when(
                auctionService.getOfferBids(
                        anyLong(),
                        anyCollection()
                )
        ).thenReturn(Collections.emptyList());

        when(
                auctionService.getGroupOffers(
                        anyLong(),
                        anyLong(),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(
                new WithCount<>(
                        0,
                        Collections.emptyList()
                )
        );

        when(
                auctionService.getGroupBids(
                        anyLong(),
                        anyLong(),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(
                new WithCount<>(
                        0,
                        Collections.emptyList()
                )
        );

    }

    /**
     * Вспомогательный tricky метод для поиска в ответе контейнера с данными по ставкам.
     * Используем, так как сравнивать в виде xml для выдачи ставок - черезчур громоздко.
     */
    protected List<SerializationGate.AuctionOffer> extractBidsFromServResponse(MockServResponse mockServResponse) {
        for (Object obj : mockServResponse.getData()) {
            if (obj instanceof ArrayList) {
                ArrayList potentialBidsBlock = ((ArrayList) obj);
                if (potentialBidsBlock.size() != 0) {
                    if (potentialBidsBlock.get(0) instanceof SerializationGate.AuctionOffer) {
                        return potentialBidsBlock;
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Получаем коллекцию со значениями ставок, которая была использована при установке ставок в
     * {@link AuctionService#setOfferBids(long, List, long)}.
     */
    @SuppressWarnings("unchecked")
    protected List<AuctionOfferBid> extractAuctionSetOffersBids() {

        ArgumentCaptor<List> bidListCaptor = ArgumentCaptor.forClass(List.class);

        try {
            verify(auctionService)
                    .setOfferBids(anyLong(), bidListCaptor.capture(), anyLong());
        } catch (BidIdTypeConflictException
                | InvalidSearchQueryException
                | AuctionGroupOwnershipViolationException
                | BidValueLimitsViolationException
                | InvalidOfferNameException e) {
            throw new RuntimeException("Exception during auction service interaction", e);
        }

        return (List<AuctionOfferBid>) bidListCaptor.getValue();
    }

    /**
     * Получаем коллекцию со значениями ставок, которая была использована при установке ставок в
     * {@link AuctionService#setCategoryBids(long, List, long)}.
     */
    @SuppressWarnings("unchecked")
    protected List<AuctionCategoryBid> extractAuctionSetCategoryBids() {

        ArgumentCaptor<List> bidListCaptor = ArgumentCaptor.forClass(List.class);

        try {
            verify(auctionService)
                    .setCategoryBids(anyLong(), bidListCaptor.capture(), anyLong());
        } catch (BidValueLimitsViolationException e) {
            throw new RuntimeException("Exception during auction service interaction", e);
        }

        return (List<AuctionCategoryBid>) bidListCaptor.getValue();
    }

    protected void mockOfferMissing() {
        Mockito.when(mockedExistenceChecker.getOfferInfo(anyObject()))
                .thenReturn(CompletableFuture.completedFuture(AuctionBulkCommon.OFFER_NOT_FOUND));
    }

    protected void mockOfferExists() {
        Mockito.when(mockedExistenceChecker.getOfferInfo(anyObject()))
                .thenReturn(CompletableFuture.completedFuture(AuctionBulkCommon.OFFER_EXISTS));
    }

    protected void mockTitles(Map<AuctionOfferId, String> mockedAnswer) {
        Mockito.when(offerTitleClarificationService.loadHumanReadableTitles(anyLong(), anyCollection()))
                .thenReturn(mockedAnswer);
    }

    /**
     * search сервантлет во время работы дважды ходит в репорт с разными задачами(получить офферы по запросу и получить домашний регион)
     * но очень близким набором параметров, отличаем мои на основе пустого/не пустого query запроса.
     */
    protected void mockSearchReportAnswers() throws IOException {

        //кусок выдачи place=prime
        //language=json
        final String primePartialAnswer = "{" +
                "  \"search\": {" +
                "    \"total\": 1," +
                "    \"totalOffers\": 1," +
                "    \"totalOffersBeforeFilters\": 93," +
                "    \"totalModels\": 0," +
                "    \"shops\": 1," +
                "    \"totalShopsBeforeFilters\": 1," +
                "    \"results\": [" +
                "      {" +
                "        \"entity\": \"offer\"," +
                "        \"titles\": {" +
                "          \"raw\": \"someOfferName\"" +
                "        }," +
                "        \"model\": {" +
                "          \"id\": 6100705" +
                "        }," +
                "        \"shop\": {" +
                "          \"entity\": \"shop\"," +
                "          \"id\": 10206649," +
                "          \"feed\": {" +
                "            \"id\": \"200304546\"," +
                "            \"offerId\": \"1482033\"," +
                "            \"categoryId\": \"44\"" +
                "          }," +
                "          \"homeRegion\": {" +
                "            \"entity\": \"region\"," +
                "            \"id\": 225," +
                "            \"name\": \"Россия\"," +
                "            \"lingua\": {" +
                "              \"name\": {" +
                "              }" +
                "            }" +
                "          }" +
                "        }" +
                "      }" +
                "    ]" +
                "  }" +
                "}";

        try (InputStream stream = new ByteArrayInputStream(primePartialAnswer.getBytes(StandardCharsets.UTF_8))) {
            PartiallyRecommendatorsFactory.mockAsyncServiceForPrime(
                    stream,
                    marketReportService,
                    SOME_SEARCH_QUERY
            );
        }

        //запрос для homeRegion
        try (InputStream stream = new ByteArrayInputStream(primePartialAnswer.getBytes(StandardCharsets.UTF_8))) {
            PartiallyRecommendatorsFactory.mockAsyncServiceForPrime(
                    stream,
                    marketReportService,
                    ""
            );
        }
    }

}
