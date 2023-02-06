package ru.yandex.market.api.partner.controllers.auction.bids;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.hamcrest.MockitoHamcrest;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.xml.sax.SAXException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.auction.AuctionController;
import ru.yandex.market.api.partner.controllers.auction.model.OfferBids;
import ru.yandex.market.api.partner.controllers.auction.model.RecommendationTarget;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiReportOfferExistenceValidator;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.CheckOffersResponseParser;
import ru.yandex.market.api.partner.report.ApiMarketReportService;
import ru.yandex.market.common.parser.InputStreamParser;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.DefaultMarketReportParserFactory;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.err.AuctionGroupOwnershipViolationException;
import ru.yandex.market.core.auction.err.BidIdTypeConflictException;
import ru.yandex.market.core.auction.err.BidValueLimitsViolationException;
import ru.yandex.market.core.auction.err.InvalidOfferNameException;
import ru.yandex.market.core.auction.err.InvalidSearchQueryException;
import ru.yandex.market.core.auction.matchers.MarketSearchRequestMatchers;
import ru.yandex.market.core.auction.model.AuctionBidStatus;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionNAReason;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.core.auction.recommend.ParallelSearchRecommendationParser;
import ru.yandex.market.mbi.report.MarketSearchService;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class AuctionControllerFunctionalCommon extends FunctionalTest {

    /**
     * Наименование оффера - замокано в ответах репорта для проверки существования оффера.
     * Используется при установке ставок как поле searchQuery и как title при установке ставок по title name.
     * Зачем это - см {@link ApiReportOfferExistenceValidator#correctBidFieldsIfFound}.
     *
     * @see {@link #mockReportRecommendatorAnswer(String, List)}
     */
    protected static final String REPORT_ANSWERED_TITLE = "Часы Casio AQ-S810W-2A";
    /**
     * См {@link #REPORT_ANSWERED_TITLE}.
     */
    protected static final AuctionOfferId TITLE_CORRECTED_ID = new AuctionOfferId(REPORT_ANSWERED_TITLE);
    /**
     * См {@link #REPORT_ANSWERED_TITLE}.
     */
    protected static final String SEARCH_QUERY = REPORT_ANSWERED_TITLE;
    protected static final String REPORT_TITLE = "GPS-навигаторы Explay PN-970TV";
    protected static final Set<Integer> POSITIONS_1 = ImmutableSet.of(1);
    protected static final int POSITION_1 = 1;
    protected static final int POSITION_3 = 3;
    protected static final Set<Integer> POSITIONS_UNSPECIFIED = Collections.emptySet();
    protected static final Set<Integer> POSITIONS_1_2_3 = ImmutableSet.of(1, 2, 3);
    protected static final String RESOURCE_PREFIX = "resources/";
    protected static final long CAMPAIGN_ID_10774 = 10774L;
    protected static final long SHOP_ID_774 = 774L;
    protected static final long FEED_ID_100 = 100L;
    protected static final AuctionOfferId TITLE_ID_501 = new AuctionOfferId("titleOffer_501");
    protected static final AuctionOfferId TITLE_ID_502 = new AuctionOfferId("titleOffer_502");
    protected static final AuctionOfferId FEED_OID_100_1001 = new AuctionOfferId(FEED_ID_100, "1001");
    protected static final AuctionOfferId FEED_OID_100_EXTENDED = new AuctionOfferId(FEED_ID_100, "1001abcd-_.abcd");
    protected static final AuctionOfferId FEED_OID_100_1002 = new AuctionOfferId(FEED_ID_100, "1002");
    protected static final BigInteger BID_VALUE_0 = BigInteger.ZERO;
    protected static final BigInteger BID_VALUE_1 = BigInteger.ONE;
    protected static final BigInteger BID_VALUE_10 = BigInteger.TEN;
    protected static final BigInteger BID_VALUE_11 = new BigInteger("11");
    protected static final BigInteger BID_VALUE_12 = new BigInteger("12");
    protected static final BigInteger BID_VALUE_13 = new BigInteger("13");
    protected static final BigInteger BID_VALUE_15 = BigInteger.valueOf(15);
    protected static final BigInteger BID_VALUE_27 = BigInteger.valueOf(27);
    protected static final BigInteger BID_VALUE_7069 = BigInteger.valueOf(7069);
    protected static final BigInteger BID_VALUE_7997 = BigInteger.valueOf(7997);
    protected static final BigInteger BID_VALUE_5158 = BigInteger.valueOf(5158);
    protected static final BigInteger BID_VALUE_8400 = BigInteger.valueOf(8400);
    protected static final BigInteger BID_RESET = null;
    static final Date BID_MOD_TIME = Date.from(
            LocalDateTime.parse(
                    "2018-01-01T23:45:12",
                    DateTimeFormatter.ISO_DATE_TIME
            ).toInstant(ZoneOffset.ofHours(0))
    );
    private static final Logger LOG = LoggerFactory.getLogger(AuctionControllerFunctionalCommon.class);
    private static final String REPORT_ANSWER_RESOURCE = "report/";
    @Qualifier(value = "shortAuctionCardMarketReportParserFactory")
    @Autowired
    protected DefaultMarketReportParserFactory shortAuctionCardMarketReportParserFactory;

    @Qualifier(value = "marketSearchMarketReportParserFactory")
    @Autowired
    protected DefaultMarketReportParserFactory marketSearchMarketReportParserFactory;

    @Qualifier("marketReportService")
    @Autowired
    protected ApiMarketReportService apiMarketReportService;

    @Autowired
    protected MarketSearchService marketSearchService;

    @Autowired
    protected AuctionService auctionService;

    protected static String loadAsString(String fileName) {
        return StringTestUtil.getString(
                AuctionControllerFunctionalCommon.class,
                RESOURCE_PREFIX + fileName
        );
    }

    protected static InputStream loadAsStream(String fileName) {
        return Objects.requireNonNull(
                AuctionControllerFunctionalCommon.class.getResourceAsStream(RESOURCE_PREFIX + fileName),
                "Failed to load res from: " + RESOURCE_PREFIX + fileName
        );
    }

    protected static void assertResponseVsExpectedFile(
            String apiResponseContentFile,
            ResponseEntity<String> response,
            Format format
    ) throws JSONException {
        String expectedOutput = loadAsString(apiResponseContentFile);
        String actualOutput = response.getBody();
        assertEqualsWithFormat(expectedOutput, actualOutput, format);
    }

    protected static void assertEqualsWithFormat(
            String expectedOutput,
            String actualOutput,
            Format format
    ) throws JSONException {

        LOG.debug("Actual string: " + actualOutput);
        LOG.debug("Expected string: " + expectedOutput);
        if (format == Format.JSON) {
            JSONAssert.assertEquals(expectedOutput, actualOutput, JSONCompareMode.NON_EXTENSIBLE);
        } else if (format == Format.XML) {
            MbiAsserts.assertXmlEquals(expectedOutput, actualOutput);
        } else {
            throw new IllegalStateException("Unexpected format passed");
        }

    }

    /**
     * Строим набор ставок в виде {@link AuctionOfferBid} на основе идентифкатора и значений компонент.
     */
    protected static List<AuctionOfferBid> prepareBidsForIds(
            List<AuctionOfferId> offerIds,
            Map<BidPlace, BigInteger> bidsMap,
            Date modDate,
            AuctionBidStatus status
    ) {
        return offerIds.stream()
                .map(
                        id -> {
                            AuctionOfferBid bid = new AuctionOfferBid(
                                    SHOP_ID_774,
                                    id,
                                    SEARCH_QUERY,
                                    0,
                                    new AuctionBidValues(bidsMap)
                            );
                            bid.setModifedDate(modDate);
                            bid.setStatus(status);
                            return bid;
                        }
                )
                .collect(Collectors.toList());
    }

    protected static List<AuctionOfferBid> prepareBidsForIds(
            List<AuctionOfferId> offerIds,
            Map<BidPlace, BigInteger> bidsMap
    ) {
        return prepareBidsForIds(offerIds, bidsMap, null, null);
    }

    protected static void mockApiOfferInfoReportAnswer(ApiMarketReportService apiMarketReportService,
                                                       String resourceFilePath)
            throws IOException, SAXException {

        ApiReportOfferExistenceValidator.ReportOfferExistenceParser parser
                = new ApiReportOfferExistenceValidator.ReportOfferExistenceParser();
        parser.parseXmlStream(loadAsStream(resourceFilePath));

        doReturn(CompletableFuture.completedFuture(parser))
                .when(apiMarketReportService)
                .async(
                        MockitoHamcrest.argThat(MarketSearchRequestMatchers.hasPlace(MarketReportPlace.API_OFFERINFO)),
                        any()
                );
    }

    protected static void mockCheckOffersReportAnswer(
            ApiMarketReportService apiMarketReportService,
            String resourceFilePath
    ) throws IOException {

        final CheckOffersResponseParser parser = new CheckOffersResponseParser();
        parser.parse(loadAsStream(resourceFilePath));

        doReturn(CompletableFuture.completedFuture(parser.getResult()))
                .when(apiMarketReportService)
                .async(
                        MockitoHamcrest.argThat(MarketSearchRequestMatchers.hasPlace(MarketReportPlace.CHECK_OFFERS)),
                        any()
                );
    }

    /**
     * Вспомогательный метод, который конвертирует <code>List<List<Object>></code> в Stream<Arguments> при этому
     * итерируя по форматам {@link Format}.
     */
    public static Stream<Arguments> buildArgsOverFormats(List<List<Object>> testCasesCore) {
        Collection<Collection<Object>> testCases = new LinkedList<>();

        for (Format fmt : ImmutableList.of(Format.XML, Format.JSON)) {
            testCasesCore.forEach(
                    tc -> {
                        List<Object> nl = new ArrayList<>(tc);
                        nl.add(fmt);
                        testCases.add(nl);
                    }
            );
        }

        return testCases.stream().map(c -> Arguments.of(c.toArray()));
    }

    /**
     * Строим набор ставок на основе идентифкатора и значений для компонент + выставялем флаги характерыне для
     * внутренней логики работы контроллера.
     * Используется для сравнения значений переданных в {@link AuctionService} на основе {@link OfferBids} поулченных
     * после обработки ответов в {@link ApiReportOfferExistenceValidator}.
     * См {@link AuctionController#setBids(long, OfferBids, long)}
     */
    protected static List<AuctionOfferBid> prepareBidsPassedToAuctionService(
            List<AuctionOfferId> offerIds,
            Map<BidPlace, BigInteger> bidsMap,
            String searchQuery
    ) {
        return offerIds.stream()
                .map(id -> {
                            AuctionOfferBid newId = new AuctionOfferBid(
                                    SHOP_ID_774,
                                    id,
                                    searchQuery,
                                    AuctionService.KEEP_GROUP_ID,
                                    new AuctionBidValues(bidsMap)
                            );

                            newId.setApi(true);
                            return newId;
                        }
                )
                .collect(Collectors.toList());
    }

    protected static List<AuctionOfferBid> prepareBidsPassedToAuctionService(
            List<AuctionOfferId> offerIds,
            Map<BidPlace, BigInteger> bidsMap
    ) {
        return prepareBidsPassedToAuctionService(offerIds, bidsMap, SEARCH_QUERY);
    }

    /**
     * Мок магазина как использующего идентификацию по id.
     */
    protected void mockTitleIdShop(long shopId) {
        when(auctionService.getAuctionOfferIdType(shopId))
                .thenReturn(AuctionOfferIdType.TITLE);
    }

    /**
     * Мок магазина как использующего идентификацию по title.
     */
    protected void mockOfferIdShop(long shopId) {
        when(auctionService.getAuctionOfferIdType(shopId))
                .thenReturn(AuctionOfferIdType.SHOP_OFFER_ID);
    }

    protected ResponseEntity<String> sendPost(String url, String body, MediaType mediaType) {
        return sendRequest(url, body, mediaType, HttpMethod.POST);
    }

    protected ResponseEntity<String> sendPut(String url, String body, MediaType mediaType) {
        return sendRequest(url, body, mediaType, HttpMethod.PUT);
    }

    protected ResponseEntity<String> sendRequest(String url, String body, MediaType mediaType, HttpMethod method) {
        return FunctionalTestHelper.makeRequestWithContentType(url, method, body, String.class, mediaType);
    }

    protected void mockCanManageAuction(long shopId) {
        when(auctionService.canManageAuction(shopId))
                .thenReturn(AuctionNAReason.NONE);

    }

    protected void mockCanMakeMbid(long shopId) {
        when(auctionService.canMakeMbid(shopId))
                .thenReturn(true);
    }

    /**
     * Мок выдачи для {@link AuctionService#getOfferBids(long, Collection)} на основе переданных идентификаторов.
     */
    protected void mockExistingBidsForGet(long shopId,
                                          List<AuctionOfferId> queryOfferIds,
                                          List<AuctionOfferBid> mockedAnswer
    ) {
        when(
                auctionService.getOfferBids(
                        eq(shopId),
                        (Collection) MockitoHamcrest.argThat(
                                Matchers.containsInAnyOrder(
                                        queryOfferIds.toArray()
                                )
                        )
                )
        ).thenReturn(
                mockedAnswer
        );
    }

    /**
     * tricky метод мока ответа рпеорта. предполагается что в рамках теста используется только один из рекомендаторов
     * - это упрощает логику мока.
     * Спаренные таргеты для search/market-search также не рассматриваем.
     */
    protected void mockReportRecommendatorAnswer(String resourceFilePath, List<RecommendationTarget> targets)
            throws IOException {
        InputStreamParser parser;

        if (targets.contains(RecommendationTarget.MODEL_CARD)
                || targets.contains(RecommendationTarget.MODEL_CARD_CPA)
        ) {
            parser = shortAuctionCardMarketReportParserFactory.newParser();

        } else if (targets.contains(RecommendationTarget.MARKET_SEARCH)) {
            parser = marketSearchMarketReportParserFactory.newParser();

        } else if (targets.contains(RecommendationTarget.SEARCH)) {
            parser = new ParallelSearchRecommendationParser();

        } else {
            throw new IllegalArgumentException("Unknown targets for parser mock: " + targets);
        }

        InputStream str = loadAsStream(REPORT_ANSWER_RESOURCE + resourceFilePath);
        parser.parse(str);
        doReturn(CompletableFuture.completedFuture(parser))
                .when(apiMarketReportService)
                .async(
                        MockitoHamcrest.argThat(
                                MarketSearchRequestMatchers.hasPlace(MarketReportPlace.BIDS_RECOMMENDER)),
                        any()
                );

        doReturn(CompletableFuture.completedFuture(parser))
                .when(marketSearchService)
                .executeAsync(
                        any(),
                        any(ParallelSearchRecommendationParser.class)
                );

    }

    /**
     * Вспомогательный метод для валидации переданных в сервис {@link AuctionService#setOfferBids(long, List, long)}
     * данных.
     */
    protected List<AuctionOfferBid> extractAuctionSetOffersBids(long shopId) {

        ArgumentCaptor<List> bidListCaptor = ArgumentCaptor.forClass(List.class);

        try {
            verify(auctionService)
                    .setOfferBids(eq(shopId), bidListCaptor.capture(), anyLong());
        } catch (BidIdTypeConflictException
                | InvalidSearchQueryException
                | AuctionGroupOwnershipViolationException
                | BidValueLimitsViolationException
                | InvalidOfferNameException e) {
            throw new RuntimeException("Exception during auction service interaction", e);
        }

        return (List<AuctionOfferBid>) bidListCaptor.getValue();
    }


    protected String urlV2PostRecommended(
            Format format,
            List<RecommendationTarget> targets,
            Set<Integer> positions
    ) throws URISyntaxException {

        return BiddingRequestUrlBuilder.builder(urlBasePrefix, CAMPAIGN_ID_10774)
                .setVersion(2)
                .setTailPath("/bids/recommended")
                .setPositions(positions)
                .setTargets(targets)
                .setFormat(format)
                .build();
    }

    protected String urlV2PostRecommendedNew(
            Format format,
            List<RecommendationTarget> targets,
            Set<Integer> positions
    ) throws URISyntaxException {

        return BiddingRequestUrlBuilder.builder(urlBasePrefix, CAMPAIGN_ID_10774)
                .setVersion(2)
                .setTailPath("/auction/recommendations/bids")
                .setPositions(positions)
                .setTargets(targets)
                .setFormat(format)
                .build();
    }

    protected String urlV1PostRecommended(
            Format format,
            List<RecommendationTarget> targets,
            Set<Integer> positions
    ) throws URISyntaxException {

        return BiddingRequestUrlBuilder.builder(urlBasePrefix, CAMPAIGN_ID_10774)
                .setVersion(1)
                .setTailPath("/bids/recommended")
                .setPositions(positions)
                .setTargets(targets)
                .setFormat(format)
                .build();
    }

    protected String urlV2PutRecommended(
            Format format,
            List<RecommendationTarget> targets,
            Integer position
    ) throws URISyntaxException {
        return BiddingRequestUrlBuilder.builder(urlBasePrefix, CAMPAIGN_ID_10774)
                .setVersion(2)
                .setTailPath("/bids/recommended")
                .setPosition(position)
                .setTargets(targets)
                .setFormat(format)
                .build();
    }

    protected String urlPutRecommendedNew(
            Format format,
            List<RecommendationTarget> targets,
            Integer position
    ) throws URISyntaxException {
        return BiddingRequestUrlBuilder.builder(urlBasePrefix, CAMPAIGN_ID_10774)
                .setTailPath("/auction/recommendations/bids")
                .setPosition(position)
                .setTargets(targets)
                .setFormat(format)
                .build();
    }

    protected String urlV2PutRecommendedNew(
            Format format,
            List<RecommendationTarget> targets,
            Integer position
    ) throws URISyntaxException {
        return urlV2PutRecommendedNew(format, targets, position, null, null);
    }

    protected String urlV2PutRecommendedNew(
            Format format,
            List<RecommendationTarget> targets,
            Integer position,
            BigDecimal offset,
            BigDecimal maxBid
    ) throws URISyntaxException {
        return BiddingRequestUrlBuilder.builder(urlBasePrefix, CAMPAIGN_ID_10774)
                .setVersion(2)
                .setTailPath("/auction/recommendations/bids")
                .setPosition(position)
                .setTargets(targets)
                .setFormat(format)
                .setOffset(offset)
                .setMaxBid(maxBid)
                .build();
    }

    protected String urlV1PutRecommended(
            Format format,
            List<RecommendationTarget> targets,
            Integer position
    ) throws URISyntaxException {

        return BiddingRequestUrlBuilder.builder(urlBasePrefix, CAMPAIGN_ID_10774)
                .setVersion(1)
                .setTailPath("/bids/recommended")
                .setPosition(position)
                .setTargets(targets)
                .setFormat(format)
                .build();
    }

    protected String v2Bids(Format format) throws URISyntaxException {
        return BiddingRequestUrlBuilder.builder(urlBasePrefix, CAMPAIGN_ID_10774)
                .setVersion(2)
                .setTailPath("/bids")
                .setFormat(format)
                .build();
    }

    protected String v2BidsNew(Format format) throws URISyntaxException {
        return BiddingRequestUrlBuilder.builder(urlBasePrefix, CAMPAIGN_ID_10774)
                .setVersion(2)
                .setTailPath("/auction/bids")
                .setFormat(format)
                .build();
    }

    protected String v1Bids(Format format) throws URISyntaxException {
        return BiddingRequestUrlBuilder.builder(urlBasePrefix, CAMPAIGN_ID_10774)
                .setVersion(1)
                .setTailPath("/bids")
                .setFormat(format)
                .build();
    }

    /**
     * Отправляем запрос на установку ставок с заданным payload содержимым и типом.
     * Ожидаем, что произошла ошибка и запрос в биддинг не был отправелн.
     * Проверяем получаемый в api ответ и сериализованную структуру в соответствии с форматом.
     *
     * @param apiRequestContentFile  - путь к файлу с фактическим содержимым запроса в АПИ
     * @param apiResponseContentFile - путь к файлу с ожидаемым содержимым ответа в АПИ
     * @param format                 - формат запроса и ответа в апи
     */
    protected void setBidsAndVerifyFailed(
            String apiRequestContentFile,
            String apiResponseContentFile,
            String url,
            Format format
    ) throws Exception {
        //моки
        mockCanManageAuction(SHOP_ID_774);
        mockCanMakeMbid(SHOP_ID_774);

        //запрос а api
        String query = loadAsString(apiRequestContentFile);
        ResponseEntity<String> response = sendPut(url, query, format.getContentType());

        verify(auctionService, never())
                .setOfferBids(anyLong(), anyList(), anyLong());

        //проверяем содержимое ответа в api
        assertResponseVsExpectedFile(apiResponseContentFile, response, format);
    }

    static class BiddingRequestBuilder {
        private final Map<BidPlace, BigInteger> bids;

        public BiddingRequestBuilder() {
            bids = new HashMap<>();
        }

        public BiddingRequestBuilder(BigInteger valueIfNotSpecified) {
            this();
            bids.put(BidPlace.SEARCH, valueIfNotSpecified);
            bids.put(BidPlace.CARD, valueIfNotSpecified);
            bids.put(BidPlace.MARKET_PLACE, BID_RESET);
            bids.put(BidPlace.MARKET_SEARCH, BID_RESET);
            bids.put(BidPlace.FLAG_DONT_PULL_BIDS, valueIfNotSpecified);
        }

        static BiddingRequestBuilder builder(BigInteger valueIfNotSpecified) {
            return new BiddingRequestBuilder(valueIfNotSpecified);
        }

        static BiddingRequestBuilder builder() {
            return new BiddingRequestBuilder();
        }

        BiddingRequestBuilder setBid(BidPlace place, BigInteger value) {
            bids.put(place, value);
            return this;
        }

        Map<BidPlace, BigInteger> build() {
            return bids;
        }

    }

    private static class BiddingRequestUrlBuilder {
        private String basePath;
        private Integer version;
        private Integer position;
        private Set<Integer> positions;
        private Format format;
        private List<RecommendationTarget> targets;
        private Long campaignId;
        private String tailPath;
        private BigDecimal offset;
        private BigDecimal maxBid;

        public static BiddingRequestUrlBuilder builder(String basePath, long campaignId) {
            return new BiddingRequestUrlBuilder().setBasePath(basePath).setCampaignId(campaignId);
        }

        public BiddingRequestUrlBuilder setBasePath(String basePath) {
            this.basePath = basePath;
            return this;
        }

        public BiddingRequestUrlBuilder setVersion(Integer version) {
            this.version = version;
            return this;
        }

        public BiddingRequestUrlBuilder setPosition(Integer position) {
            this.position = position;
            return this;
        }

        public BiddingRequestUrlBuilder setPositions(Set<Integer> positions) {
            this.positions = positions;
            return this;
        }

        public BiddingRequestUrlBuilder setFormat(Format format) {
            this.format = format;
            return this;
        }

        public BiddingRequestUrlBuilder setTargets(List<RecommendationTarget> targets) {
            this.targets = targets;
            return this;
        }

        public BiddingRequestUrlBuilder setCampaignId(Long campaignId) {
            this.campaignId = campaignId;
            return this;
        }

        public BiddingRequestUrlBuilder setTailPath(String tailPath) {
            this.tailPath = tailPath;
            return this;
        }

        public BiddingRequestUrlBuilder setOffset(BigDecimal offset) {
            this.offset = offset;
            return this;
        }

        public BiddingRequestUrlBuilder setMaxBid(BigDecimal maxBid) {
            this.maxBid = maxBid;
            return this;
        }

        public String build() throws URISyntaxException {
            String path = basePath;
            if (version != null) {
                path += "/v" + String.valueOf(version);
            }
            path += "/campaigns/" + String.valueOf(campaignId);
            path += tailPath;

            if (format != null) {
                path += ("." + format.toString().toLowerCase());
            }

            URIBuilder uriBuilder = new URIBuilder(path);
            if (position != null) {
                uriBuilder.addParameter("position", String.valueOf(position));
            }

            if (offset != null) {
                uriBuilder.addParameter("offset", offset.toString());
            }

            if (maxBid != null) {
                uriBuilder.addParameter("max_bid", maxBid.toString());
            }

            if (CollectionUtils.isNotEmpty(targets)) {
                targets.forEach(target -> uriBuilder.addParameter("target", target.getCode()));
            }

            String uri = uriBuilder.toString();

            //avoid urlencode
            if (CollectionUtils.isNotEmpty(positions)) {
                uri += "&positions=" + Joiner.on(",").join(positions);
            }

            return uri;
        }
    }

}
