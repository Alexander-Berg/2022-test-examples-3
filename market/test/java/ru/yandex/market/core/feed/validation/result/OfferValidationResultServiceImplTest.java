package ru.yandex.market.core.feed.validation.result;

import java.util.List;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.assortment.model.FeedProcessingResult;
import ru.yandex.market.core.feed.model.FeedFileType;
import ru.yandex.market.core.feed.validation.model.FeedParsingReportInfo;
import ru.yandex.market.core.feed.validation.model.FeedParsingResultInfo;
import ru.yandex.market.core.indexer.model.IndexerError;
import ru.yandex.market.core.indexer.model.IndexerErrorLevel;
import ru.yandex.market.core.indexer.model.OfferPosition;
import ru.yandex.market.mbi.core.feed.FeedProcessingStats;

import static ru.yandex.common.util.collections.CollectionFactory.list;
import static ru.yandex.market.core.feed.validation.result.ValidationResultTestUtil.buildIndexerError;
import static ru.yandex.market.core.feed.validation.result.ValidationResultTestUtil.buildIndexerErrorInfo;
import static ru.yandex.market.core.feed.validation.result.ValidationResultTestUtil.buildValidationResult;
import static ru.yandex.market.core.feed.validation.result.ValidationResultTestUtil.getValidationResultOnlyErrorTestCollection;
import static ru.yandex.market.core.feed.validation.result.ValidationResultTestUtil.getValidationResultTestCollection;
import static ru.yandex.market.core.feed.validation.result.ValidationResultTestUtil.getValidationResultWithoutErrorTestCollection;
import static ru.yandex.market.core.offer.model.united.UnitedOfferTestUtil.buildRequiredUnitedOffer;
import static ru.yandex.market.core.offer.model.united.UnitedOfferTestUtil.buildUnitedOffer;

/**
 * Date: 18.11.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@DbUnitDataSet(before = "OfferValidationResultService/db/tanker.csv")
class OfferValidationResultServiceImplTest extends FunctionalTest {

    private static final List<IndexerError> INDEXER_ERRORS = list(
            buildIndexerError("300", "35B", "55:9", IndexerErrorLevel.WARNING,
                    "{\"attrName\":\"unit\",\"attrValue\":\"\",\"tagName\":\"age\"," +
                            "\"context\":\"[line 55, col 9]\",\"offer\":\"300\",\"code\":\"35B\"}"),
            buildIndexerError("300", "35B", "66:9", IndexerErrorLevel.WARNING,
                    "{\"attrName\":\"unit\",\"attrValue\":\"\",\"tagName\":\"price\"," +
                            "\"context\":\"[line 66, col 9]\",\"offer\":\"300\",\"code\":\"35B\"}"),
            buildIndexerError("301", "35B", "66:18", null,
                    "{\"tagValue\":\"6+\",\"tagName\":\"age\"," +
                            "\"context\":\"[line 66, col 18]\",\"offer\":\"301\",\"code\":\"35B\"}"),
            buildIndexerError(null, "451", "", IndexerErrorLevel.ERROR,
                    "{\"reason\":\"is empty\",\"tagName\":\"price\"," +
                            "\"context\":\"[line 71, col 32]\",\"code\":\"451\"}"),
            buildIndexerError(null, "393", "77:9", IndexerErrorLevel.FATAL,
                    "{\"attrName\":\"unit\",\"attrValue\":\"\",\"tagName\":\"age\"," +
                            "\"context\":\"[line 77, col 9]\",\"code\":\"393\"}")
    );

    @Autowired
    private OfferValidationResultService offerValidationResultService;

    @DisplayName("Проверяет корректность слияния офферов и ошибок по shop sku, а также тексты из танкера")
    @Test
    void transformResult_allData_successful() {
        FeedParsingResultInfo feedParsingResultInfo = getOfferListProcessingResultBuilder()
                .withUnitedOffer(buildUnitedOffer(0, "300"))
                .withUnitedOffer(buildRequiredUnitedOffer(0, "301"))
                .withUnitedOffer(buildUnitedOffer(0, "303"))
                .withIndexerErrors(INDEXER_ERRORS)
                .build();

        Assertions.assertThat(offerValidationResultService.transformResult(feedParsingResultInfo))
                .containsExactlyElementsOf(getValidationResultTestCollection(false, true));
    }

    @DisplayName("Проверяет корректность слияния офферов и ошибок по shop sku, а также тексты из танкера. Нет офферов")
    @Test
    void transformResult_onlyErrors_successful() {
        FeedParsingResultInfo feedParsingResultInfo = getOfferListProcessingResultBuilder()
                .withIndexerErrors(INDEXER_ERRORS)
                .build();

        Assertions.assertThat(offerValidationResultService.transformResult(feedParsingResultInfo))
                .containsExactlyElementsOf(getValidationResultOnlyErrorTestCollection());
    }

    @DisplayName("Проверяет корректность слияния офферов и ошибок по shop sku, а также тексты из танкера. Нет ошибок")
    @Test
    void transformResult_onlyOffers_successful() {
        FeedParsingResultInfo feedParsingResultInfo = getOfferListProcessingResultBuilder()
                .withUnitedOffer(buildUnitedOffer(null, "offer1"))
                .withUnitedOffer(buildRequiredUnitedOffer(null, "offer4"))
                .withUnitedOffer(buildUnitedOffer(null, "offer5"))
                .build();

        Assertions.assertThat(offerValidationResultService.transformResult(feedParsingResultInfo))
                .containsExactlyInAnyOrderElementsOf(getValidationResultWithoutErrorTestCollection());
    }

    @DisplayName("Проверяет корректность слияния офферов и ошибок по shop sku, если shop_sku пустой")
    @Test
    void transformResult_emptyShopSku_onlyError() {
        FeedParsingResultInfo feedParsingResultInfo = getOfferListProcessingResultBuilder()
                .withUnitedOffer(buildUnitedOffer(0, ""))
                .withIndexerErrors(INDEXER_ERRORS)
                .build();

        Assertions.assertThat(offerValidationResultService.transformResult(feedParsingResultInfo))
                .containsExactlyElementsOf(getValidationResultOnlyErrorTestCollection());
    }

    @DisplayName("Проверяет корректность заполнения кода ошибки, если в танкере нет данных")
    @Test
    void transformResult_unknownCode_code() {
        FeedParsingResultInfo feedParsingResultInfo = getOfferListProcessingResultBuilder()
                .withUnitedOffer(buildUnitedOffer(0, ""))
                .withIndexerErrors(list(
                        buildIndexerError("300",  "500", "55:9", IndexerErrorLevel.WARNING,
                                "{\"attrName\":\"unit\",\"attrValue\":\"\",\"tagName\":\"age\"," +
                                        "\"context\":\"[line 55, col 9]\",\"offer\":\"300\",\"code\":\"35B\"}"))
                )
                .build();

        Assertions.assertThat(offerValidationResultService.transformResult(feedParsingResultInfo))
                .containsExactlyElementsOf(List.of(
                        buildValidationResult("300",
                                null,
                                list(
                                        buildIndexerErrorInfo(
                                                "300",
                                                "500",
                                                "500",
                                                null,
                                                null,
                                                IndexerErrorLevel.WARNING,
                                                OfferPosition.of(55, 9)
                                        )
                                ))));
    }

    @Nonnull
    private FeedParsingResultInfo.Builder getOfferListProcessingResultBuilder() {
        return FeedParsingResultInfo.builder()
                .withReportInfo(FeedParsingReportInfo.builder()
                        .withResultUrl("http://indexer.com/")
                        .build())
                .withFileType(FeedFileType.YML)
                .withProcessingStats(FeedProcessingStats.ZERO)
                .withResultStatus(FeedProcessingResult.WARNING);
    }
}
