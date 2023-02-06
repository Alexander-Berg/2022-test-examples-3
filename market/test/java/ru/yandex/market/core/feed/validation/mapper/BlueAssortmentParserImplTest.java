package ru.yandex.market.core.feed.validation.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.assortment.model.FeedProcessingResult;
import ru.yandex.market.core.feed.model.FeedFileType;
import ru.yandex.market.core.feed.validation.ValidationUtils;
import ru.yandex.market.core.feed.validation.model.FeedParsingReportInfo;
import ru.yandex.market.core.feed.validation.model.FeedParsingResultInfo;
import ru.yandex.market.core.indexer.model.IndexerError;
import ru.yandex.market.core.indexer.model.IndexerErrorLevel;
import ru.yandex.market.mbi.core.feed.FeedProcessingStats;
import ru.yandex.market.proto.indexer.v2.BlueAssortment;

import static ru.yandex.market.core.offer.model.united.UnitedOfferTestUtil.buildUnitedOffer;
import static ru.yandex.market.core.offer.model.united.UnitedOfferTestUtil.buildUnitedOfferFromInputOffer;

/**
 * Date: 28.01.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class BlueAssortmentParserImplTest extends FunctionalTest {

    private static final Collection<IndexerError> INDEXER_ERRORS = List.of(
            new IndexerError.Builder()
                    .setShopSku("200")
                    .setCode("35B")
                    .setPosition("55:9")
                    .setLevel(IndexerErrorLevel.WARNING)
                    .setText("Invalid value for attribute")
                    .setDetails("{\"attrName\":\"unit\",\"attrValue\":\"\",\"tagName\":\"age\"," +
                            "\"context\":\"[line 55, col 9]\",\"offer\":\"200\",\"code\":\"35B\"}")
                    .build(),
            new IndexerError.Builder()
                    .setShopSku("302")
                    .setCode("451")
                    .setPosition("71:32")
                    .setLevel(IndexerErrorLevel.ERROR)
                    .setText("Offer price is not specified")
                    .setDetails("{\"reason\":\"is empty\",\"tagName\":\"price\"," +
                            "\"context\":\"[line 71, col 32]\",\"offer\":\"302\",\"code\":\"451\"}")
                    .build(),
            new IndexerError.Builder()
                    .setShopSku("")
                    .setCode("532")
                    .setPosition("12:39")
                    .setLevel(IndexerErrorLevel.FATAL)
                    .setText("Category ID is not a number: ")
                    .setDetails("{\"categoryId\":\"\",\"code\":\"532\"}")
                    .build());

    private static final FeedProcessingStats FEED_PROCESSING_STATS = new FeedProcessingStats.Builder()
            .setAcceptedOffers(10)
            .setNewOffers(10)
            .setProcessedOffers(10)
            .setDeclinedOffers(0)
            .setTotalOffers(10)
            .build();

    @Autowired
    private BlueAssortmentParser blueAssortmentParser;

    @DisplayName("BlueAssortment без информации разобрался корректно")
    @Test
    void parseCheckResult_empty_successful() {
        BlueAssortment.CheckResult checkResult = getCheckResult("empty");
        FeedParsingReportInfo reportInfo = FeedParsingReportInfo.builder()
                .build();

        Assertions.assertThat(blueAssortmentParser.parseCheckResult(checkResult, reportInfo))
                .isEqualTo(FeedParsingResultInfo.builder()
                        .withReportInfo(reportInfo)
                        .withResultStatus(FeedProcessingResult.ERROR)
                        .build());
    }

    @DisplayName("BlueAssortment только с ошибками разобрался корректно")
    @Test
    void parseCheckResult_onlyError_successful() {
        BlueAssortment.CheckResult checkResult = getCheckResult("onlyError");
        FeedParsingReportInfo reportInfo = FeedParsingReportInfo.builder()
                .build();

        Assertions.assertThat(blueAssortmentParser.parseCheckResult(checkResult, reportInfo))
                .isEqualTo(FeedParsingResultInfo.builder()
                        .withReportInfo(reportInfo)
                        .withIndexerErrors(INDEXER_ERRORS)
                        .withFileType(FeedFileType.CSV)
                        .withResultStatus(FeedProcessingResult.OK)
                        .withProcessingStats(new FeedProcessingStats.Builder()
                                .setAcceptedOffers(5)
                                .setNewOffers(5)
                                .setProcessedOffers(5)
                                .setDeclinedOffers(0)
                                .setTotalOffers(5)
                                .build())
                        .build());
    }

    @DisplayName("BlueAssortment с InputOffer разобрался корректно")
    @Test
    void parseCheckResult_inputOffer_successful() {
        BlueAssortment.CheckResult checkResult = getCheckResult("inputOffer");
        FeedParsingReportInfo reportInfo = FeedParsingReportInfo.builder()
                .withReturnCode(13L)
                .build();

        Assertions.assertThat(blueAssortmentParser.parseCheckResult(checkResult, reportInfo))
                .isEqualTo(FeedParsingResultInfo.builder()
                        .withReportInfo(reportInfo)
                        .withIndexerErrors(INDEXER_ERRORS)
                        .withUnitedOffer(buildUnitedOfferFromInputOffer("300"))
                        .withFileType(FeedFileType.YML)
                        .withResultStatus(FeedProcessingResult.ERROR)
                        .withProcessingStats(FEED_PROCESSING_STATS)
                        .build());
    }

    @DisplayName("BlueAssortment с warning")
    @Test
    void parseCheckResult_inputOfferWithWarning_successful() {
        BlueAssortment.CheckResult checkResult = getCheckResult("inputOffer");
        FeedParsingReportInfo reportInfo = FeedParsingReportInfo.builder()
                .withReturnCode(2L)
                .build();

        Assertions.assertThat(blueAssortmentParser.parseCheckResult(checkResult, reportInfo))
                .isEqualTo(FeedParsingResultInfo.builder()
                        .withReportInfo(reportInfo)
                        .withIndexerErrors(INDEXER_ERRORS)
                        .withUnitedOffer(buildUnitedOfferFromInputOffer("300"))
                        .withFileType(FeedFileType.YML)
                        .withResultStatus(FeedProcessingResult.WARNING)
                        .withProcessingStats(FEED_PROCESSING_STATS)
                        .build());
    }

    @DisplayName("BlueAssortment c DataCamp.Offer разобрался корректно")
    @Test
    void parseCheckResult_dataCampOffer_successful() {
        BlueAssortment.CheckResult checkResult = getCheckResult("dataCampOffer");
        FeedParsingReportInfo reportInfo = FeedParsingReportInfo.builder()
                .withGlobalError(new IndexerError.Builder()
                        .setCode("451")
                        .setPosition("71:32")
                        .build())
                .build();

        Assertions.assertThat(blueAssortmentParser.parseCheckResult(checkResult, reportInfo))
                .isEqualTo(FeedParsingResultInfo.builder()
                        .withReportInfo(reportInfo)
                        .withUnitedOffer(buildUnitedOffer(123456, "0516465165"))
                        .withFileType(FeedFileType.XLS)
                        .withResultStatus(FeedProcessingResult.ERROR)
                        .withProcessingStats(FEED_PROCESSING_STATS)
                        .build());
    }

    @DisplayName("BlueAssortment со всей информацией разобрался корректно")
    @Test
    void parseCheckResult_full_successful() {
        assertFullParseCheckResult("full", INDEXER_ERRORS);
    }

    @DisplayName("BlueAssortment со всей информацией разобрался корректно, но содержит максимальное количество ошибок")
    @Test
    void parseCheckResult_fullWithErrorLimitReached_successful() {
        ArrayList<IndexerError> indexerErrors = new ArrayList<>(INDEXER_ERRORS);
        indexerErrors.add(ValidationUtils.ERROR_LIMIT_REACHED);

        assertFullParseCheckResult("fullWithErrorLimitReached", indexerErrors);
    }

    private void assertFullParseCheckResult(@Nonnull String name,
                                            @Nonnull Collection<IndexerError> indexerErrors) {
        BlueAssortment.CheckResult checkResult = getCheckResult(name);
        FeedParsingReportInfo reportInfo = FeedParsingReportInfo.builder()
                .build();

        Assertions.assertThat(blueAssortmentParser.parseCheckResult(checkResult, reportInfo))
                .isEqualTo(FeedParsingResultInfo.builder()
                        .withReportInfo(reportInfo)
                        .withIndexerErrors(indexerErrors)
                        .withUnitedOffer(buildUnitedOffer(123456, "0516465165"))
                        .withFileType(FeedFileType.YML)
                        .withResultStatus(FeedProcessingResult.WARNING)
                        .withProcessingStats(new FeedProcessingStats.Builder()
                                .setNewOffers(1)
                                .setProcessedOffers(1)
                                .setDeclinedOffers(2)
                                .setTotalOffers(3)
                                .build())
                        .build());
    }

    @Nonnull
    private BlueAssortment.CheckResult getCheckResult(@Nonnull String name) {
        return ProtoTestUtil.getProtoMessageByJson(
                BlueAssortment.CheckResult.class,
                "BlueAssortmentParserImpl/proto/" + name + ".json",
                this.getClass()
        );
    }
}
