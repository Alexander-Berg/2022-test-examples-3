package ru.yandex.market.partner.auction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.RecommendationType;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.auction.dto.report.ReportRecommendationsAnswerDto;
import ru.yandex.market.core.auction.dto.report.ReportRecommendationsAnswerOrError;
import ru.yandex.market.core.auction.matchers.ComplexBidMatchers;
import ru.yandex.market.core.auction.matchers.OfferAuctionStatsMatchers;
import ru.yandex.market.core.auction.model.StatusCode;
import ru.yandex.market.core.auction.recommend.BidRecommendations;
import ru.yandex.market.core.auction.recommend.OfferAuctionStats;
import ru.yandex.market.core.report.parser.ReportResponseXmlParser;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasBid;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasHyperId;
import static ru.yandex.market.core.auction.matchers.FoundOfferMatchers.hasUrl;
import static ru.yandex.market.core.auction.matchers.TargetAuctionStatsMatchers.hasCurPosAll;
import static ru.yandex.market.core.auction.matchers.TargetAuctionStatsMatchers.hasCurPosTop;
import static ru.yandex.market.core.auction.matchers.TargetAuctionStatsMatchers.hasTopOffersCount;

/**
 * Тесты для {@link DtoToOldModelConversionUtils}.
 *
 * @author vbudnev
 */
class DtoToOldModelConversionUtilsTest {
    private ReportRecommendationsAnswerDto dto;

    @DisplayName("Smoke тест на де/сериализацию dto")
    @Test
    void test_smokeDeserializationTest() throws IOException {
        prepareDtoAndReccomendations("report_card_answer.xml");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            new ApiObjectMapperFactory().createXmlMapper().writeValue(out, dto);

            final String actual = out.toString();
            final String expectedXml = StringTestUtil.getString(
                    this.getClass(),
                    "expected.xml"
            );
            MbiAsserts.assertXmlEquals(expectedXml, actual);
        }
    }

    @DisplayName("Трансляция exception при конвоертации из ReportRecommendationsAnswerOrError")
    @Test
    void test_conversionOfFailed() {
        RuntimeException ex = Assertions.assertThrows(
                RuntimeException.class,
                () -> DtoToOldModelConversionUtils.convertToOldCardRecs(
                        ReportRecommendationsAnswerOrError.fromException(
                                new RuntimeException("Original exception msg")
                        )
                )
        );

        MatcherAssert.assertThat(
                ex.getMessage(),
                Matchers.is("Dto conversion unavailable")
        );

        MatcherAssert.assertThat(
                ex.getCause().getMessage(),
                Matchers.is("Original exception msg")
        );

    }

    @DisplayName("Общий тест на преобразование dto к модели")
    @Test
    void test_conversion() throws IOException {
        final BidRecommendations recs = prepareDtoAndReccomendations("report_card_answer.xml");

        assertMetaAndGeneralOfferInfo(recs);
        final OfferAuctionStats stats = recs.getShopOffersAuctionStats().get(0);

        // общая инфа
        MatcherAssert.assertThat(stats.getTargetStatsSize(), Matchers.is(1));
        MatcherAssert.assertThat(stats, OfferAuctionStatsMatchers.hasMinBid(29));
        MatcherAssert.assertThat(stats, OfferAuctionStatsMatchers.hasMinFee(null));

        // информация для целевого карточного блока
        final OfferAuctionStats.TargetAuctionStats targetCardStats = stats.getTargetStats(RecommendationType.CARD);
        MatcherAssert.assertThat(targetCardStats,
                allOf(hasCurPosAll(5),
                        hasCurPosTop(7),
                        hasTopOffersCount(88)
                )
        );
        MatcherAssert.assertThat(targetCardStats.getPriceBlockFirstBid(),
                allOf(
                        ComplexBidMatchers.hasBid(500),
                        ComplexBidMatchers.hasFee(null),
                        ComplexBidMatchers.hasStatus(StatusCode.OK)
                )
        );

        MatcherAssert.assertThat(targetCardStats.getPositionComplexBids().size(), Matchers.is(10));

        // информация по ставкам
        MatcherAssert.assertThat(targetCardStats.getPositionComplexBid(1),
                allOf(
                        ComplexBidMatchers.hasBid(200),
                        ComplexBidMatchers.hasFee(null),
                        ComplexBidMatchers.hasStatus(StatusCode.OK)
                )
        );

        MatcherAssert.assertThat(targetCardStats.getPositionComplexBid(7),
                allOf(
                        ComplexBidMatchers.hasBid(null),
                        ComplexBidMatchers.hasFee(null),
                        ComplexBidMatchers.hasStatus(null)
                )
        );

        MatcherAssert.assertThat(targetCardStats.getPositionComplexBid(8),
                allOf(
                        ComplexBidMatchers.hasBid(50),
                        ComplexBidMatchers.hasFee(null),
                        ComplexBidMatchers.hasStatus(StatusCode.BID_IS_NOT_REACHABLE)
                )
        );

        MatcherAssert.assertThat(targetCardStats.getPositionComplexBid(9),
                allOf(
                        ComplexBidMatchers.hasBid(null),
                        ComplexBidMatchers.hasFee(null),
                        ComplexBidMatchers.hasStatus(StatusCode.FEE_IS_NOT_REACHABLE)
                )
        );

    }

    @DisplayName("Пустые целевые блоки рекомендаций")
    @Test
    void test_conversion_when_emptyTargetBlocks() throws IOException {
        final BidRecommendations recs = prepareDtoAndReccomendations("report_card_empty_target_recs.xml");

        assertMetaAndGeneralOfferInfo(recs);
        final OfferAuctionStats stats = recs.getShopOffersAuctionStats().get(0);

        // общая инфа
        MatcherAssert.assertThat(stats.getTargetStatsSize(), Matchers.is(1));
        MatcherAssert.assertThat(stats, OfferAuctionStatsMatchers.hasMinBid(29));

        // информация для целевого карточного блока
        final OfferAuctionStats.TargetAuctionStats targetCardStats = stats.getTargetStats(RecommendationType.CARD);
        MatcherAssert.assertThat(targetCardStats,
                allOf(hasCurPosAll(5),
                        hasCurPosTop(7),
                        hasTopOffersCount(88)
                )
        );
        MatcherAssert.assertThat(targetCardStats.getPriceBlockFirstBid(), nullValue());

        MatcherAssert.assertThat(targetCardStats.getPositionComplexBids().size(), Matchers.is(0));
    }

    @DisplayName("Пустой общий блок рекомендаций")
    @Test
    void test_conversion_emptyRecBlock() throws IOException {
        final BidRecommendations recs = prepareDtoAndReccomendations("report_card_empty_recommendations_block.xml");

        assertMetaAndGeneralOfferInfo(recs);
        final OfferAuctionStats stats = recs.getShopOffersAuctionStats().get(0);

        // общая инфа
        MatcherAssert.assertThat(stats.getTargetStatsSize(), Matchers.is(0));
        MatcherAssert.assertThat(stats, OfferAuctionStatsMatchers.hasMinBid(29));
    }

    @DisplayName("Отсутствует общий блок рекомендаций")
    @Test
    void test_conversion_noRecBlock() throws IOException {
        final BidRecommendations recs = prepareDtoAndReccomendations("report_card_no_recommendations_block.xml");

        assertMetaAndGeneralOfferInfo(recs);
        final OfferAuctionStats stats = recs.getShopOffersAuctionStats().get(0);

        // общая инфа
        MatcherAssert.assertThat(stats.getTargetStatsSize(), Matchers.is(0));
    }

    @DisplayName("Ответ репорта пустой")
    @Test
    void test_conversion_emptySearchResult() throws IOException {
        final BidRecommendations recs = prepareDtoAndReccomendations("report_empty_search.xml");

        //мета информация
        MatcherAssert.assertThat(recs.anythingFound(), Matchers.is(false));
        MatcherAssert.assertThat(recs.getShopOffersSearchResults().getTotalOffers(), nullValue());
        MatcherAssert.assertThat(recs.getShopOffersAuctionStats(), hasSize(0));
    }

    private void assertMetaAndGeneralOfferInfo(BidRecommendations recs) {
        //мета информация
        MatcherAssert.assertThat(recs.anythingFound(), Matchers.is(true));
        MatcherAssert.assertThat(recs.getShopOffersSearchResults().getTotalOffers(), Matchers.is(1));
        MatcherAssert.assertThat(recs.getShopOffersAuctionStats(), hasSize(1));

        final OfferAuctionStats stats = recs.getShopOffersAuctionStats().get(0);

        // поля оффера
        final FoundOffer foundOffer = stats.getOffer();
        MatcherAssert.assertThat(foundOffer, hasUrl("wwww.example.yandex.ru"));
        MatcherAssert.assertThat(foundOffer, hasHyperId(7021673L));
        MatcherAssert.assertThat(foundOffer, hasBid(1200));
    }

    @Nonnull
    private BidRecommendations prepareDtoAndReccomendations(String fileName) throws IOException {
        final String reportAnswer = StringTestUtil.getString(
                DtoToOldModelConversionUtilsTest.class,
                fileName
        );

        ReportResponseXmlParser<ReportRecommendationsAnswerDto> parser
                = new ReportResponseXmlParser<>(ReportRecommendationsAnswerDto.class);
        try (InputStream in = new ByteArrayInputStream(reportAnswer.getBytes())) {
            parser.parse(in);
            dto = parser.getResult();
        }

        return DtoToOldModelConversionUtils.convertToOldCardRecs(
                ReportRecommendationsAnswerOrError.fromResponse(dto)
        );
    }
}
