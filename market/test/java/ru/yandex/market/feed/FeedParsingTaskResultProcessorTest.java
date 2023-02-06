package ru.yandex.market.feed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import Market.DataCamp.API.GeneralizedMessageOuterClass;
import Market.DataCamp.API.UpdateTask;
import Market.DataCamp.DataCampExplanation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.StringUtils;
import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.shop.FunctionalTest;

class FeedParsingTaskResultProcessorTest extends FunctionalTest {

    @Autowired
    private FeedParsingTaskResultProcessor feedParsingTaskResultProcessor;

    @Test
    @DisplayName("У фида приходит успешный парсинг. Создаем статус")
    @DbUnitDataSet(
            after = "FeedParsingTaskResultProcessorTest.successStatus.after.csv"
    )
    void parsingStatus_successStatus_insertNew() {
        MessageBatch batch = new MessageBatchBuilder()
                .addItem(createReport(
                        999, 1,
                        1, UpdateTask.FeedParsingResult.FPR_SUCCESS,
                        null, UpdateTask.ParsingStatus.OK
                ))
                .build();

        feedParsingTaskResultProcessor.process(batch);
    }

    @Test
    @DisplayName("У фида приходит успешный парсинг с realFeedId. Создаем статус")
    @DbUnitDataSet(
            after = "FeedParsingTaskResultProcessorTest.successStatusRealFeedId.after.csv"
    )
    void parsingStatus_successStatusRealFeedId_insertNew() {
        MessageBatch batch = new MessageBatchBuilder()
                .addItem(createReport(
                        999, 777, 1,
                        1, UpdateTask.FeedParsingResult.FPR_SUCCESS,
                        null, UpdateTask.ParsingStatus.OK
                ))
                .build();

        feedParsingTaskResultProcessor.process(batch);
    }

    @Test
    @DisplayName("У фида приходит фатальный парсинг. Обновляем только флаг фатальности")
    @DbUnitDataSet(
            before = "FeedParsingTaskResultProcessorTest.failedStatus.before.csv",
            after = "FeedParsingTaskResultProcessorTest.failedStatus.after.csv"
    )
    void parsingStatus_successStatus_update() {
        MessageBatch batch = new MessageBatchBuilder()
                .addItem(createReport(
                        999, 1,
                        2, UpdateTask.FeedParsingResult.FPR_FAIL,
                        null, UpdateTask.ParsingStatus.FATAL
                ))
                .build();

        feedParsingTaskResultProcessor.process(batch);
    }

    @Test
    @DisplayName("Всегда обновляем статус парсинга")
    @DbUnitDataSet(
            before = "FeedParsingTaskResultProcessorTest.oldResult.before.csv",
            after = "FeedParsingTaskResultProcessorTest.oldResult.after.csv"
    )
    void parsingStatus_oldResult_updated() {
        MessageBatch batch = new MessageBatchBuilder()
                .addItem(createReport(
                        999, 1,
                        1, UpdateTask.FeedParsingResult.FPR_SUCCESS,
                        null, UpdateTask.ParsingStatus.WARN
                ))
                .build();

        feedParsingTaskResultProcessor.process(batch);
    }

    @Test
    @DisplayName("Сообщение с длинным сообщением об ошибке не провоцирует выброс исключения")
    void processMessageWithTooLongErrorText() {
        MessageBatch batch = new MessageBatchBuilder()
                .addItem(createReport(
                        777, 2,
                        1, UpdateTask.FeedParsingResult.FPR_FAIL,
                        "text".repeat(1000), UpdateTask.ParsingStatus.FATAL
                ))
                .build();
        feedParsingTaskResultProcessor.process(batch);
    }

    private static GeneralizedMessageOuterClass.GeneralizedMessage createReport(int feedId,
                                                                                int partnerId,
                                                                                long feedParsingTaskId,
                                                                                UpdateTask.FeedParsingResult result,
                                                                                String error,
                                                                                UpdateTask.ParsingStatus parsingStatus,
                                                                                DataCampExplanation.Explanation... errorMessages) {
        return createReport(feedId, 0, partnerId, feedParsingTaskId, result, error, parsingStatus, errorMessages);
    }

    private static GeneralizedMessageOuterClass.GeneralizedMessage createReport(int feedId, int realFeedId,
                                                                                int partnerId,
                                                                                long feedParsingTaskId,
                                                                                UpdateTask.FeedParsingResult result,
                                                                                String error,
                                                                                UpdateTask.ParsingStatus parsingStatus,
                                                                                DataCampExplanation.Explanation... errorMessages) {
        UpdateTask.FeedParsingTask task = UpdateTask.FeedParsingTask.newBuilder()
                .setFeedId(feedId)
                .setRealFeedId(realFeedId)
                .setShopId(partnerId)
                .setTimestamp(DateTimes.toTimestamp(DateTimes.toInstant(2020, 5, 26, 4, 27, 32)))
                .setFeedParsingTaskIdentifiers(UpdateTask.FeedParsingTaskIdentifiers.newBuilder()
                        .setTaskId(feedParsingTaskId))
                .setType(UpdateTask.FeedClass.FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_COMPLETE)
                .build();

        UpdateTask.FeedParsingTaskReport.Builder report = UpdateTask.FeedParsingTaskReport.newBuilder()
                .setFeedParsingTask(task)
                .setStatus(parsingStatus)
                .setFeedParsingResult(result);

        if (!StringUtils.isEmpty(error)) {
            report.setFeedParsingErrorText(error);
        }

        for (DataCampExplanation.Explanation explanation : errorMessages) {
            report.addFeedParsingErrorMessages(explanation);
        }

        return GeneralizedMessageOuterClass.GeneralizedMessage.newBuilder()
                .setFeedParsingTaskReport(report.build())
                .build();
    }

    static class MessageBatchBuilder {

        MessageMeta meta = new MessageMeta("test".getBytes(), 0, 0, 0, "::1", CompressionCodec.RAW,
                Collections.emptyMap());

        private List<GeneralizedMessageOuterClass.GeneralizedMessage> items = new ArrayList<>();

        public FeedParsingTaskResultProcessorTest.MessageBatchBuilder addItem(
                GeneralizedMessageOuterClass.GeneralizedMessage item
        ) {
            items.add(item);
            return this;
        }

        public FeedParsingTaskResultProcessorTest.MessageBatchBuilder setMeta(MessageMeta meta) {
            this.meta = meta;
            return this;
        }

        MessageBatch build() {
            return items.stream()
                    .map(item -> new MessageData(item.toByteArray(), 2, meta))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            msgs -> new MessageBatch("topic", 1, msgs)
                    ));
        }
    }
}
