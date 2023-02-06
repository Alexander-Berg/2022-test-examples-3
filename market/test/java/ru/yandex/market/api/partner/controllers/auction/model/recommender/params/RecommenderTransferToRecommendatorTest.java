package ru.yandex.market.api.partner.controllers.auction.model.recommender.params;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.partner.controllers.auction.model.ApiRecommendationsRequest;
import ru.yandex.market.api.partner.controllers.auction.model.AuctionOffer;
import ru.yandex.market.api.partner.controllers.auction.model.RecommendationTarget;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiAuctionAbstractRecommender;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiAuctionMarketSearchRecommender;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiAuctionModelCardRecommender;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiAuctionParallelSearchRecommender;
import ru.yandex.market.common.report.model.SearchResults;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.recommend.BidRecommendationRequest;
import ru.yandex.market.core.auction.recommend.BidRecommendations;
import ru.yandex.market.core.auction.recommend.BidRecommendator;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.core.geobase.model.Region;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Проверяем что параметры переданные в реализации {@link ApiAuctionAbstractRecommender}, корректно
 * передаются в {@link BidRecommendator}.
 */
@ExtendWith(MockitoExtension.class)
class RecommenderTransferToRecommendatorTest {

    private static final Long SHOP_ID = 123L;
    private static final Long LOCAL_DELIVERY_REGION_ID = 7L;
    private static final Region REGION = new Region(213L, "Москва", 1L);
    private static final Set<Integer> EMPTY_POSITIONS = ImmutableSet.of();
    private static final Set<Integer> SOME_POSITIONS = ImmutableSet.of(1, 3, 5);
    /**
     * ТП котоыре нужны только для того чтобы отрабатывал функционал иетрирующий оп офферам.
     * На содержимое этих ТП полагаться нельзя.
     */
    private static final AuctionOffer IRRELEVANT_PARTIAL_OFFER = new AuctionOffer();
    private static final Set<AuctionOffer> IRRELEVANT_PARTIAL_OFFERS = ImmutableSet.of(IRRELEVANT_PARTIAL_OFFER);
    private static final AuctionOfferId SOME_TITLE_OFFER_ID = new AuctionOfferId("SomeOfferId");

    static {
        IRRELEVANT_PARTIAL_OFFER.setOfferId(SOME_TITLE_OFFER_ID);
    }

    @Mock
    private BidRecommendator bidRecommendator;
    @Mock
    private DatasourceService datasourceService;
    /**
     * Набор позиций по умолчанию, подставляемый в рекомендаторы если в запросе явно не заданы.
     */
    private Set<Integer> DEFAULT_RECOMMENDATIONS_POSITION_SET_9 = IntStream.rangeClosed(1, 9).boxed().collect(Collectors.toSet());
    private Set<Integer> DEFAULT_RECOMMENDATIONS_POSITION_SET_10 = IntStream.rangeClosed(1, 10).boxed().collect(Collectors.toSet());
    private Set<Integer> DEFAULT_RECOMMENDATIONS_POSITION_SET_12 = IntStream.rangeClosed(1, 12).boxed().collect(Collectors.toSet());
    @InjectMocks
    private ApiAuctionMarketSearchRecommender apiAuctionMarketSearchRecommender;
    @InjectMocks
    private ApiAuctionParallelSearchRecommender apiAuctionParallelSearchRecommender;
    @InjectMocks
    private ApiAuctionModelCardRecommender apiAuctionModelCardRecommender;

    private static Set<AuctionOffer> prepareOffers() {
        return new HashSet<>(IRRELEVANT_PARTIAL_OFFERS);
    }

    @BeforeEach
    void before() {
        when(datasourceService.getLocalDeliveryRegion(SHOP_ID))
                .thenReturn(LOCAL_DELIVERY_REGION_ID);
    }

    /**
     * При явно указанном в {@link ApiRecommendationsRequest} регионе, вызов {@link BidRecommendator#calculate(BidRecommendationRequest)}
     * идет с переданным занчением.
     * Проверяем для 3х типов рекомендаторов: search,market-search,model-card.
     */
    @Test
    void test_transferParam_when_passeExplicitRegionId_should_transfer() {
        ApiRecommendationsRequest request = ApiRecommendationsRequest.builder()
                .withShopId(SHOP_ID)
                .withRegion(REGION)
                .withOffers(prepareOffers())
                .withPositions(EMPTY_POSITIONS)
                .build();

        Mockito.reset(bidRecommendator);
        mockRecommenderCaluclationSafeCall();
        apiAuctionMarketSearchRecommender.getRecommendations(request);
        assertRecommendatorGetRecommendationsCalledWithRegion(bidRecommendator, REGION.getId());

        Mockito.reset(bidRecommendator);
        mockRecommenderCaluclationSafeCall();
        apiAuctionParallelSearchRecommender.getRecommendations(request);
        assertRecommendatorGetRecommendationsCalledWithRegion(bidRecommendator, REGION.getId());

        Mockito.reset(bidRecommendator);
        mockRecommenderCaluclationSafeCall();
        request = ApiRecommendationsRequest.builder()
                .withShopId(SHOP_ID)
                .withRegion(REGION)
                .withOffers(prepareOffers())
                .withPositions(SOME_POSITIONS)
                .withTargets(ImmutableSet.of(RecommendationTarget.MODEL_CARD))
                .build();

        apiAuctionModelCardRecommender.getRecommendations(request);
        assertRecommendatorGetRecommendationsCalledWithRegion(bidRecommendator, REGION.getId());
    }

    /**
     * При НЕ пустом наборе позиций в {@link ApiRecommendationsRequest} регионе, вызов {@link BidRecommendator#calculate(BidRecommendationRequest)}
     * уидет с указанным набором позиций.
     * Проверяем для 3х типов рекомендаторов: search,market-search,model-card.
     */
    @Test
    void test_transferParam_when_passedExplicitPositionsSet_should_transfer() {
        ApiRecommendationsRequest request = ApiRecommendationsRequest.builder()
                .withShopId(SHOP_ID)
                .withOffers(prepareOffers())
                .withPositions(SOME_POSITIONS)
                .withRegion(REGION)
                .build();

        Mockito.reset(bidRecommendator);
        mockRecommenderCaluclationSafeCall();
        apiAuctionMarketSearchRecommender.getRecommendations(request);
        assertRecommendatorGetRecommendationsCalledWithPositions(bidRecommendator, SOME_POSITIONS);

        Mockito.reset(bidRecommendator);
        mockRecommenderCaluclationSafeCall();
        apiAuctionParallelSearchRecommender.getRecommendations(request);
        assertRecommendatorGetRecommendationsCalledWithPositions(bidRecommendator, SOME_POSITIONS);

        Mockito.reset(bidRecommendator);
        mockRecommenderCaluclationSafeCall();
        request = ApiRecommendationsRequest.builder()
                .withShopId(SHOP_ID)
                .withOffers(prepareOffers())
                .withPositions(SOME_POSITIONS)
                .withRegion(REGION)
                .withTargets(ImmutableSet.of(RecommendationTarget.MODEL_CARD))
                .build();

        apiAuctionModelCardRecommender.getRecommendations(request);
        assertRecommendatorGetRecommendationsCalledWithPositions(bidRecommendator, SOME_POSITIONS);

    }

    /**
     * При пустом наборе позиций в {@link ApiRecommendationsRequest} регионе, вызов {@link BidRecommendator#calculate(BidRecommendationRequest)}
     * идет с набором позиций по умолчанию.
     * Проверяем для 3х типов рекомендаторов: search,market-search,model-card.
     */
    @Test
    void test_transferParam_when_noPassedEmptyPositionsSet_should_transferDefault() {
        ApiRecommendationsRequest request = ApiRecommendationsRequest.builder()
                .withShopId(SHOP_ID)
                .withOffers(prepareOffers())
                .withPositions(EMPTY_POSITIONS)
                .withRegion(REGION)
                .build();

        Mockito.reset(bidRecommendator);
        mockRecommenderCaluclationSafeCall();
        apiAuctionMarketSearchRecommender.getRecommendations(request);
        assertRecommendatorGetRecommendationsCalledWithPositions(bidRecommendator, DEFAULT_RECOMMENDATIONS_POSITION_SET_12);

        Mockito.reset(bidRecommendator);
        mockRecommenderCaluclationSafeCall();
        apiAuctionParallelSearchRecommender.getRecommendations(request);
        assertRecommendatorGetRecommendationsCalledWithPositions(bidRecommendator, DEFAULT_RECOMMENDATIONS_POSITION_SET_9);

        Mockito.reset(bidRecommendator);
        mockRecommenderCaluclationSafeCall();
        request = ApiRecommendationsRequest.builder()
                .withShopId(SHOP_ID)
                .withOffers(prepareOffers())
                .withPositions(EMPTY_POSITIONS)
                .withRegion(REGION)
                .withTargets(ImmutableSet.of(RecommendationTarget.MODEL_CARD))
                .build();

        apiAuctionModelCardRecommender.getRecommendations(request);
        assertRecommendatorGetRecommendationsCalledWithPositions(bidRecommendator, DEFAULT_RECOMMENDATIONS_POSITION_SET_10);
    }

    private void assertRecommendatorGetRecommendationsCalledWithRegion(BidRecommendator recommendator, Long expectedRegionId) {
        ArgumentCaptor<BidRecommendationRequest> calculateCaptor = ArgumentCaptor.forClass(BidRecommendationRequest.class);
        verify(recommendator).calculate(calculateCaptor.capture());

        assertThat(
                calculateCaptor.getValue().getRegionId(),
                is(expectedRegionId)
        );
    }

    private void assertRecommendatorGetRecommendationsCalledWithPositions(BidRecommendator recommendator, Set<Integer> positions) {
        ArgumentCaptor<BidRecommendationRequest> calculateCaptor = ArgumentCaptor.forClass(BidRecommendationRequest.class);
        verify(recommendator).calculate(calculateCaptor.capture());

        assertThat(
                calculateCaptor.getValue().getTargetPositions(),
                is(positions)
        );
    }

    /**
     * В рамках теста нас интересует только логика до вызова bidRecommendator.calculate с точки зрения переданных
     * аргументов, но чтобы последующий код не разваливался, подпираем.
     */
    private void mockRecommenderCaluclationSafeCall() {
        when(bidRecommendator.calculate(ArgumentMatchers.any()))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                new BidRecommendations(Collections.emptyList(), new SearchResults())
                        )
                );
    }
}
