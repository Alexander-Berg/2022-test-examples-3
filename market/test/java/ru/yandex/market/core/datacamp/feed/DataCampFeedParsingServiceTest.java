package ru.yandex.market.core.datacamp.feed;

import java.util.Map;
import java.util.Optional;

import Market.DataCamp.API.UpdateTask;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.datacamp.feed.status.DataCampFeedParsingError;
import ru.yandex.market.core.datacamp.feed.status.DataCampFeedParsingFeedIdentifiers;
import ru.yandex.market.core.datacamp.feed.status.DataCampFeedParsingInfo;
import ru.yandex.market.core.datacamp.feed.status.DataCampFeedParsingReturnCode;
import ru.yandex.market.core.datacamp.feed.status.DataCampFeedParsingStatus;
import ru.yandex.market.core.datacamp.feed.status.DataCampFeedParsingStatusIdentifiers;
import ru.yandex.market.core.datacamp.feed.status.DataCampFeedParsingTime;
import ru.yandex.market.core.feed.datacamp.FeedParsingType;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.indexer.model.ReturnCode;
import ru.yandex.market.core.util.DateTimes;

/**
 * Тесты для {@link DataCampFeedParsingService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DataCampFeedParsingServiceTest extends FunctionalTest {

    @Autowired
    private DataCampFeedParsingService dataCampFeedParsingService;

    @Test
    @DisplayName("Сохранить новый пустой статус парсинга фида")
    @DbUnitDataSet(after = "insertEmptyStatus.after.csv")
    void insertEmpty_notExists_insertNew() {
        saveStatus(getEmptyStatus());
    }

    @Test
    @DisplayName("Сохранить новый статус парсинга фида")
    @DbUnitDataSet(before = "mergeStatus.before.csv", after = "insertStatus.after.csv")
    void mergeStatus_notExists_insertNew() {
        saveStatus(getFullStatus());
    }

    @Test
    @DisplayName("Сохранить новый статус парсинга фида")
    @DbUnitDataSet(before = "updateStatus.before.csv", after = "insertStatus.after.csv")
    void mergeStatus_exists_update() {
        saveStatus(getFullStatus());
    }

    @Test
    @DisplayName("Получить полный статус парсинга фида")
    @DbUnitDataSet(before = "insertStatus.after.csv")
    void getStatus_full_success() {
        Optional<DataCampFeedParsingStatus> actual = dataCampFeedParsingService.getStatus(1001L, FeedType.ASSORTMENT);
        Assertions.assertThat(actual)
                .hasValue(getFullStatus());
    }

    @Test
    @DisplayName("Получить пустой статус парсинга фида")
    @DbUnitDataSet(before = "insertStatus.after.csv")
    void getStatus_empty_success() {
        Optional<DataCampFeedParsingStatus> actual = dataCampFeedParsingService.getStatus(1001L, FeedType.STOCKS);
        Assertions.assertThat(actual)
                .hasValue(getEmptyStatus());
    }

    @Test
    @DisplayName("Создание новой модели. Заполняются только обязательные поля")
    void parsingStatusModel_new() {
        DataCampFeedParsingStatus actual = DataCampFeedParsingStatus.createNew(1001L, FeedType.STOCKS, 1002L);
        Assertions.assertThat(actual)
                .isEqualTo(getEmptyStatus());
    }

    @Test
    @DisplayName("Обновление статуса парсинга. Успешный парсинг. Обновляются все поля")
    void updateStatusModel_nonFatal_updateAllFields() {
        UpdateTask.FeedParsingTask task = UpdateTask.FeedParsingTask.newBuilder()
                .setTimestamp(DateTimes.toTimestamp(DateTimes.toInstant(2022, 1, 1)))
                .setType(UpdateTask.FeedClass.FEED_CLASS_UPDATE)
                .build();

        DataCampFeedParsingResult parsingResult = DataCampFeedParsingResult.builder()
                .setTaskId(2001L)
                .setFeedId(1001L)
                .setPartnerId(1002L)
                .setFeedParsingResult(UpdateTask.FeedParsingResult.FPR_SUCCESS)
                .setParserReturnCode(ReturnCode.OK)
                .setReceivedTime(DateTimes.toInstant(2022, 1, 2))
                .build();

        DataCampFeedParsingStatus updatedStatus = getFullStatus().update(task, parsingResult);

        DataCampFeedParsingStatus expected = DataCampFeedParsingStatus.builder()
                .setFeedIdentifiers(DataCampFeedParsingFeedIdentifiers.builder()
                        .setFeedId(1001L)
                        .setFeedType(FeedType.ASSORTMENT)
                        .setPartnerId(1002L)
                        .build())
                .setIsFatalInLastParsing(false)
                .setParsingIdentifiers(DataCampFeedParsingStatusIdentifiers.builder()
                        .setLastParsingId(2001L)
                        .setLastNonFatalParsingId(2001L)
                        .setLastSuccessParsingId(2001L)
                        .build())
                .setLastNonFatalParsing(DataCampFeedParsingInfo.builder()
                        .setReturnCode(DataCampFeedParsingReturnCode.builder()
                                .setCode(ReturnCode.OK)
                                .setNumber(1007L)
                                .build())
                        .setParsingType(FeedParsingType.UPDATE_FEED)
                        .setTime(DataCampFeedParsingTime.builder()
                                .setLastParsingSentTime(DateTimes.toInstant(2022, 1, 1))
                                .setLastParsingReceivedTime(DateTimes.toInstant(2022, 1, 2))
                                .build())
                        .build())
                .build();

        Assertions.assertThat(updatedStatus)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Обновление статуса парсинга. Фатальный парсинг. Обновляется только последний парсинг и флаг фатальности")
    void updateStatusModel_nonFatal_updateFatalFields() {
        UpdateTask.FeedParsingTask task = UpdateTask.FeedParsingTask.newBuilder()
                .setTimestamp(DateTimes.toTimestamp(DateTimes.toInstant(2022, 1, 1)))
                .setType(UpdateTask.FeedClass.FEED_CLASS_UPDATE)
                .build();

        DataCampFeedParsingResult parsingResult = DataCampFeedParsingResult.builder()
                .setTaskId(2001L)
                .setFeedId(1001L)
                .setPartnerId(1002L)
                .setFeedParsingResult(UpdateTask.FeedParsingResult.FPR_RETRY)
                .setParserReturnCode(ReturnCode.FATAL)
                .setReceivedTime(DateTimes.toInstant(2022, 1, 2))
                .build();

        DataCampFeedParsingStatus updatedStatus = getFullStatus().update(task, parsingResult);

        DataCampFeedParsingStatus expected = DataCampFeedParsingStatus.builder()
                .of(getFullStatus())
                .setParsingIdentifiers(DataCampFeedParsingStatusIdentifiers.builder()
                        .of(getFullStatus().getParsingIdentifiers().orElse(null))
                        .setLastParsingId(2001L)
                        .build())
                .setIsFatalInLastParsing(true)
                .build();

        Assertions.assertThat(updatedStatus)
                .isEqualTo(expected);
    }

    private void saveStatus(DataCampFeedParsingStatus status) {
        dataCampFeedParsingService.saveParsingStatus(status);
    }

    private DataCampFeedParsingStatus getFullStatus() {
        return DataCampFeedParsingStatus.builder()
                .setFeedIdentifiers(DataCampFeedParsingFeedIdentifiers.builder()
                        .setFeedId(1001L)
                        .setFeedType(FeedType.ASSORTMENT)
                        .setPartnerId(1002L)
                        .build())
                .setIsFatalInLastParsing(false)
                .setParsingIdentifiers(DataCampFeedParsingStatusIdentifiers.builder()
                        .setLastParsingId(1003L)
                        .setLastNonFatalParsingId(1004L)
                        .setLastSuccessParsingId(1005L)
                        .build())
                .setLastNonFatalParsing(DataCampFeedParsingInfo.builder()
                        .setReturnCode(DataCampFeedParsingReturnCode.builder()
                                .setCode(ReturnCode.OK)
                                .setNumber(1006L)
                                .build())
                        .setParsingType(FeedParsingType.COMPLETE_FEED)
                        .setError(DataCampFeedParsingError.builder()
                                .setCode("code")
                                .setErrorArgs(Map.of("args", "vals"))
                                .build())
                        .setTime(DataCampFeedParsingTime.builder()
                                .setLastParsingSentTime(DateTimes.toInstant(2020, 1, 1))
                                .setLastParsingReceivedTime(DateTimes.toInstant(2020, 1, 2))
                                .build())
                        .build())
                .build();
    }

    private DataCampFeedParsingStatus getEmptyStatus() {
        return DataCampFeedParsingStatus.builder()
                .setFeedIdentifiers(DataCampFeedParsingFeedIdentifiers.builder()
                        .setFeedId(1001L)
                        .setFeedType(FeedType.STOCKS)
                        .setPartnerId(1002L)
                        .build())
                .build();
    }
}
