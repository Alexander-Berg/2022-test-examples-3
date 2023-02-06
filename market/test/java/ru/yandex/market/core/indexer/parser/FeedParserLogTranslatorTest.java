package ru.yandex.market.core.indexer.parser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.API.UpdateTask.FeedParsingTaskReport;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitils.reflectionassert.ReflectionAssert;
import org.unitils.reflectionassert.ReflectionComparatorMode;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.assortment.model.FeedProcessingResult;
import ru.yandex.market.core.feed.validation.model.FeedParsingReportInfo;
import ru.yandex.market.core.feed.validation.model.FeedParsingResultInfo;
import ru.yandex.market.core.indexer.model.IndexerError;
import ru.yandex.market.mbi.core.feed.FeedProcessingStats;

/**
 * @author Vadim Lyalin
 */
@ParametersAreNonnullByDefault
class FeedParserLogTranslatorTest extends FunctionalTest {

    private static final String FEED_LOG_FILE_NAME = "feed.log";

    @Autowired
    private FeedParserLogTranslator feedParserLogTranslator;

    @Test
    @DbUnitDataSet(before = "FeedParserLogTranslatorTest.before.csv")
    void testFeedLogTranslate() throws IOException {

        final TranslatedFeedParserLog expectedTranslatedFeedParserLog = prepareExpectedParserLog(true);
        final TranslatedFeedParserLog parserLog = feedParserLogTranslator.translate(IOUtils.toString(
                this.getClass().getResourceAsStream(FEED_LOG_FILE_NAME)));

        ReflectionAssert.assertReflectionEquals(expectedTranslatedFeedParserLog, parserLog,
                ReflectionComparatorMode.LENIENT_ORDER);
    }

    @DisplayName("Перевод информации о валидации фида из лб")
    @Test
    @DbUnitDataSet(before = "FeedParserLogTranslatorTest.before.csv")
    void translate_correctOffer_success() {
        TranslatedFeedParserLog parserLog = feedParserLogTranslator.translate(FeedParsingResultInfo.builder()
                .withReportInfo(FeedParsingReportInfo.builder()
                        .withResultUrl("http://idx.ru")
                        .build())
                .withResultStatus(FeedProcessingResult.OK)
                .withIndexerErrors(List.of(
                        new IndexerError.Builder()
                                .setCode("45S")
                                .setText("This is the start")
                                .setPosition("1:8")
                                .build(),
                        new IndexerError.Builder()
                                .setCode("45S")
                                .setText("This is the end")
                                .setPosition("6:9")
                                .build(),
                        new IndexerError.Builder()
                                .setCode("367")
                                .setText("This is the new")
                                .setPosition("1:12")
                                .build(),
                        new IndexerError.Builder()
                                .setCode("45A")
                                .setText("Who I")
                                .setPosition("3:32")
                                .build()))
                .withProcessingStats(new FeedProcessingStats.Builder()
                        .setTotalOffers(13)
                        .setAcceptedOffers(10)
                        .setModerationOffers(123)
                        .setDeclinedOffers(3)
                        .build())
                .build());

        Assertions.assertThat(parserLog.getContent())
                .isEqualTo("Ошибка: В предложении нет картинки (строка 1, столбец 8)\n" +
                        "Ошибка: В предложении нет картинки (строка 6, столбец 9)\n" +
                        "Предупреждение: Интервал доставки больше трёх дней (строка 1, столбец 12)\n" +
                        "Ошибка: Неизвестная ошибка \"45A\" (строка 3, столбец 32)\n");

        List<FeedLogCodeStats> codeStats = parserLog.getParsedLog().getCodeStats();
        Assertions.assertThat(codeStats.size())
                .isEqualTo(3);

        List<FeedLogStats> feedStats = parserLog.getParsedLog().getFeedStats();
        Assertions.assertThat(feedStats.size())
                .isEqualTo(1);

        FeedLogStats feedLogStats = feedStats.get(0);
        Assertions.assertThat(feedLogStats.getCode())
                .isEqualTo("260");
        Assertions.assertThat(feedLogStats.getArguments().size())
                .isEqualTo(4);
    }

    @Test
    @DisplayName("Перевод ошибок парсинга из лб. Новый формат деталей")
    @DbUnitDataSet(before = "FeedParserLogTranslatorTest.before.csv")
    void testLogbrokerReportTranslateNew() {
        testLBReport("FeedParserLogTranslatorTest.logbrokerReport.json");
    }

    // todo batalin: del in MBI-50602
    @Test
    @DisplayName("Перевод ошибок парсинга из лб. Старый формат деталей")
    @DbUnitDataSet(before = "FeedParserLogTranslatorTest.before.csv")
    void testLogbrokerReportTranslateOld() {
        testLBReport("FeedParserLogTranslatorTest.logbrokerReport.old.json");
    }

    private void testLBReport(final String messageFile) {
        final var feedParsingTaskReport = ProtoTestUtil.getProtoMessageByJson(
                FeedParsingTaskReport.class,
                messageFile,
                getClass()
        );
        final TranslatedFeedParserLog expectedTranslatedFeedParserLog = prepareExpectedParserLog(false);
        final TranslatedFeedParserLog parserLog = feedParserLogTranslator.translate(feedParsingTaskReport);

        ReflectionAssert.assertReflectionEquals(
                expectedTranslatedFeedParserLog,
                parserLog,
                ReflectionComparatorMode.LENIENT_ORDER
        );
    }

    /**
     * В фидлоге для всех сообщений пишется {@code [posLine:posNumber]}.
     * В protobuf-сообщении {@link FeedParsingTaskReport} позиция
     * известна только при наличии таких именованных параметров.
     *
     * @param withAllPositions номера строки и столбцы известны независимо от наличия именованных параметров
     */
    private static TranslatedFeedParserLog prepareExpectedParserLog(final boolean withAllPositions) {
        FeedLogCodeStats stats110 = createFeedCode("110", new FeedLogLine(
                "110",
                null,
                null,
                null,
                "YT: commit transaction 74e6-54e657-3fe0001-82d829bb",
                withAllPositions ? 9337 : null,
                withAllPositions ? 14 : null,
                null
        ));

        FeedLogCodeStats stats261 = createFeedCode("261", new FeedLogLine(
                "261",
                null,
                null,
                null,
                "Cpa candidate offers : 608, Valid cpa offers: 608, Real cpa offers: 608, Offers with discount: 33",
                0,
                0,
                ImmutableMap.<String, String>builder()
                        .put("code", "261")
                        .put("posColumn", "0")
                        .put("posLine", "0")
                        .build()
        ));

        FeedLogCodeStats stats367 = createFeedCode("367", new FeedLogLine(
                "367",
                null,
                null,
                null,
                "Too long delivery-option days period",
                148,
                12,
                ImmutableMap.<String, String>builder()
                        .put("code", "367")
                        .put("posColumn", "12")
                        .put("posLine", "148")
                        .build()
        ));

        FeedLogCodeStats stats45S = createFeedCode("45S", new FeedLogLine(
                "45S",
                null,
                null,
                null,
                "No valid picture URLs",
                withAllPositions ? 564 : null,
                15,
                withAllPositions ? null : ImmutableMap.<String, String>builder()
                        .put("posColumn", "15")
                        .build()
        ));

        FeedLogCodeStats stats4YY = createFeedCode("4YY", new FeedLogLine(
                "4YY",
                null,
                null,
                null,
                "test message",
                9337,
                withAllPositions ? 12 : null,
                withAllPositions ? null : ImmutableMap.<String, String>builder()
                        .put("posLine", "9337")
                        .build()
        ));

        FeedLogCodeStats statsXYZ = createFeedCode("XYZ", new FeedLogLine(
                "XYZ",
                null,
                null,
                null,
                "msg",
                withAllPositions ? 9337 : null,
                withAllPositions ? 101 : null,
                null
        ));

        FeedLogCodeStats stats1XX = createFeedCode("1XX", new FeedLogLine(
                "1XX",
                null,
                null,
                null,
                "message for 1XX",
                withAllPositions ? 9337 : null,
                withAllPositions ? 11 : null,
                null
        ));

        final String content = String.format(
                "" +
                        "Предупреждение: Интервал доставки больше трёх дней (строка 148, столбец 12)\n" +
                        "Ошибка: В предложении нет картинки " +
                        (withAllPositions ? "(строка 564, столбец 15)" : "(столбец 15)") +
                        "\n" +
                        "Неизвестная ошибка \"XYZ\"" +
                        (withAllPositions ? " (строка 9337, столбец 101)" : "") +
                        "\n" +
                        "Ошибка: Неизвестная ошибка \"4YY\" " +
                        (withAllPositions ? "(строка 9337, столбец 12)" : "(строка 9337)") +
                        "\n",
                withAllPositions ? 564 : null,
                withAllPositions ? 15 : null,
                withAllPositions ? 9337 : null,
                withAllPositions ? 101 : null,
                withAllPositions ? 9337 : null,
                withAllPositions ? 12 : null
        );
        final ParseLogParsed parseLogParsed = new ParseLogParsed(
                Arrays.asList(stats110, stats261, stats367, stats45S, stats4YY, statsXYZ, stats1XX),
                Arrays.asList(
                        new FeedLogStats("161", ImmutableMap.<String, String>builder()
                                .put("totalOffers", "618")
                                .build()),
                        new FeedLogStats("162", ImmutableMap.<String, String>builder()
                                .put("ymldate", "2017-09-05 10:06")
                                .build())
                ));

        return new TranslatedFeedParserLog(content, parseLogParsed);
    }

    private static FeedLogCodeStats createFeedCode(String code, FeedLogLine example) {
        return FeedLogCodeStats.builder()
                .setCode(code)
                .addExample(example)
                .incrementErrorsCount()
                .build();
    }
}
