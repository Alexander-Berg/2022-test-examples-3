package ru.yandex.market.adv.content.manager.database.repository.moderation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.market.adv.content.manager.database.entity.moderation.ModerationTaskEntity;
import ru.yandex.market.adv.content.manager.database.entity.moderation.ModerationTaskStatus;
import ru.yandex.market.adv.content.manager.model.moderation.ModerationContentModel;
import ru.yandex.market.adv.content.manager.model.moderation.ModerationContentResultModel;
import ru.yandex.market.adv.content.manager.model.moderation.ModerationContentRuleModel;
import ru.yandex.market.adv.content.manager.model.moderation.ModerationTaskDataModel;
import ru.yandex.market.adv.service.time.TimeService;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Date: 05.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
class ModerationTaskRepositoryTest extends AbstractContentManagerTest {

    @Autowired
    private ModerationTaskRepository moderationTaskRepository;
    @Autowired
    private TimeService timeService;

    @DisplayName("По идентификатору задания нашли данные.")
    @DbUnitDataSet(
            before = "ModerationTaskRepository/csv/findById_correctId_presentData.before.csv"
    )
    @Test
    void findById_correctId_presentData() {
        Assertions.assertThat(moderationTaskRepository.findById(423L, "423_4123_5132_BUSINESS"))
                .isPresent()
                .get()
                .isEqualTo(
                        getModerationTaskEntity(
                                423L,
                                "423_4123_5132_BUSINESS",
                                LocalDateTime.of(2021, 10, 3, 22, 49, 50),
                                getResultModerationTaskDataModel(),
                                true,
                                ModerationTaskStatus.ENDED,
                                "1636704060998/ee3f7813653cab9e19796ad57bff47e7"
                        )
                );
    }

    @DisplayName("По идентификатору задания не нашли данные.")
    @Test
    void findById_wrongId_emptyData() {
        Assertions.assertThat(moderationTaskRepository.findById(423L, "423_4123_5132_BUSINESS"))
                .isEmpty();
    }

    @DisplayName("Нашли все задания на модерацию в статусе SENT.")
    @DbUnitDataSet(
            before = "ModerationTaskRepository/csv/findByStatus_sentStatus_threeResult.before.csv"
    )
    @Test
    void findByStatus_sentStatus_threeResult() {
        Assertions.assertThat(moderationTaskRepository.findByStatus(ModerationTaskStatus.SENT.name()))
                .containsExactlyInAnyOrder(
                        getModerationTaskEntity(
                                523L,
                                "523_3123_5132_BUSINESS",
                                LocalDateTime.of(2021, 10, 3, 20, 49, 50),
                                getModerationTaskDataModel(
                                        Map.of(
                                                "1",
                                                getModerationContentModel(
                                                        "https://s3.yandex.net/car.jpeg",
                                                        "IMAGE",
                                                        Map.of(
                                                                "vendor", "market",
                                                                "category", "phone"
                                                        ),
                                                        null)
                                        )
                                ),
                                null,
                                ModerationTaskStatus.SENT,
                                null
                        ),
                        getModerationTaskEntity(
                                423L,
                                "423_4123_5232_EXPRESS",
                                LocalDateTime.of(2021, 10, 6, 21, 49, 50),
                                getModerationTaskDataModel(
                                        Map.of(
                                                "1",
                                                getModerationContentModel(
                                                        "https://s3.yandex.net/car.jpeg",
                                                        "IMAGE",
                                                        Map.of(
                                                                "vendor", "market",
                                                                "category", "phone"
                                                        ),
                                                        null),
                                                "2",
                                                getModerationContentModel(
                                                        "Привет",
                                                        "TEXT",
                                                        Map.of(),
                                                        null)

                                        )
                                ),
                                null,
                                ModerationTaskStatus.SENT,
                                null
                        ),
                        getModerationTaskEntity(
                                423L,
                                "423_4123_5632_BUSINESS",
                                LocalDateTime.of(2021, 10, 3, 23, 49, 50),
                                getModerationTaskDataModel(Map.of()),
                                null,
                                ModerationTaskStatus.SENT,
                                "1636704060998/ee3f7813653cab9e19796ad57bff47e7"
                        )
                );
    }

    @DisplayName("Не нашли записей в статусе отличном от ENDED или старше 36 часов от текущей даты.")
    @DbUnitDataSet(
            before = "ModerationTaskRepository/csv/findByStatusEndedAndCreated_allEnded_emptyList.before.csv"
    )
    @Test
    void findByStatusEndedAndCreated_allEnded_emptyList() {
        Instant limitTime = timeService.get()
                .minus(36, ChronoUnit.HOURS);
        Assertions.assertThat(moderationTaskRepository.findByStatusEndedAndCreated(limitTime))
                .isEmpty();
    }

    @DisplayName("Нашли записи в статусе отличном от ENDED и старше 36 часов от текущей даты.")
    @DbUnitDataSet(
            before = "ModerationTaskRepository/csv/" +
                    "findByStatusEndedAndCreated_expectStickModeration_emptyList.before.csv"
    )
    @Test
    void findByStatusEndedAndCreated_expectStickModeration_emptyList() {
        Instant limitTime = timeService.get()
                .minus(36, ChronoUnit.HOURS);
        Assertions.assertThat(moderationTaskRepository.findByStatusEndedAndCreated(limitTime)
                        .stream()
                        .map(ModerationTaskEntity::getId)
                )
                .containsExactlyInAnyOrder(
                        "524_3123_5132_BUSINESS",
                        "423_4423_5232_EXPRESS",
                        "524_5123_5332_BUSINESS",
                        "427_4123_5439_BUSINESS"
                );
    }

    @DisplayName("Задание на модерацию успешно сохранилось в БД.")
    @DbUnitDataSet(
            after = "ModerationTaskRepository/csv/insert_correctData_oneRow.after.csv"
    )
    @Test
    void insert_correctData_oneRow() {
        moderationTaskRepository.insert(
                423L,
                "423_4123_5132_BUSINESS",
                LocalDateTime.of(2021, 10, 3, 22, 49, 50)
                        .atZone(ZoneOffset.systemDefault())
                        .toInstant(),
                ModerationTaskStatus.CREATED.name(),
                getModerationTaskDataModel(
                        Map.of(
                                "https://s3.yandex.net/car.jpeg", getModerationContentModel(null)
                        )
                ),
                null,
                "1636704060998/ee3f7813653cab9e19796ad57bff47e7"
        );
    }

    @DisplayName("Результат по заданию на модерацию успешно обновился в БД.")
    @DbUnitDataSet(
            before = "ModerationTaskRepository/csv/" +
                    "updateDataAndResultById_notNullDataAndResult_updateOneRow.before.csv",
            after = "ModerationTaskRepository/csv/" +
                    "updateDataAndResultById_notNullDataAndResult_updateOneRow.after.csv"
    )
    @Test
    void updateDataAndResultById_notNullDataAndResult_updateOneRow() {
        Assertions.assertThat(
                        moderationTaskRepository.updateStatusAndDataAndResultById(
                                423L,
                                "423_4123_5132_BUSINESS",
                                ModerationTaskStatus.MODERATED.name(),
                                getResultModerationTaskDataModel(),
                                true
                        )
                )
                .isEqualTo(1L);
    }

    @DisplayName("Результат по заданию на модерацию не сохранился в БД из-за неверного id.")
    @DbUnitDataSet(
            before = "ModerationTaskRepository/csv/" +
                    "updateDataAndResultById_notNullDataAndResultAndWrongId_updateZeroRow.csv",
            after = "ModerationTaskRepository/csv/" +
                    "updateDataAndResultById_notNullDataAndResultAndWrongId_updateZeroRow.csv"
    )
    @Test
    void updateDataAndResultById_notNullDataAndResultAndWrongId_updateZeroRow() {
        Assertions.assertThat(
                        moderationTaskRepository.updateStatusAndDataAndResultById(
                                424L,
                                "424_4123_5132_BUSINESS",
                                ModerationTaskStatus.ENDED.name(),
                                getResultModerationTaskDataModel(),
                                true
                        )
                )
                .isEqualTo(0L);
    }

    @DisplayName("Статус результата по заданию на модерацию успешно обновился в БД.")
    @DbUnitDataSet(
            before = "ModerationTaskRepository/csv/" +
                    "updateStatusById_correctData_updateOneRow.before.csv",
            after = "ModerationTaskRepository/csv/" +
                    "updateStatusById_correctData_updateOneRow.after.csv"
    )
    @Test
    void updateStatusById_correctData_updateOneRow() {
        Assertions.assertThat(
                        moderationTaskRepository.updateStatusById(
                                423L,
                                "423_4123_5132_BUSINESS",
                                ModerationTaskStatus.ENDED.name()
                        )
                )
                .isEqualTo(1L);
    }

    @Nonnull
    private ModerationTaskEntity getModerationTaskEntity(long businessId,
                                                         String id,
                                                         @Nonnull LocalDateTime localDateTime,
                                                         ModerationTaskDataModel data,
                                                         Boolean result,
                                                         ModerationTaskStatus status,
                                                         String traceId) {
        ModerationTaskEntity moderationTaskEntity = new ModerationTaskEntity();
        moderationTaskEntity.setBusinessId(businessId);
        moderationTaskEntity.setId(id);
        moderationTaskEntity.setCreated(
                localDateTime.atZone(ZoneOffset.systemDefault())
                        .toInstant()
        );
        moderationTaskEntity.setData(data);
        moderationTaskEntity.setResult(result);
        moderationTaskEntity.setStatus(status);
        moderationTaskEntity.setTraceId(traceId);

        return moderationTaskEntity;
    }

    @Nonnull
    private ModerationTaskDataModel getResultModerationTaskDataModel() {
        ModerationContentResultModel resultModel = new ModerationContentResultModel(
                false,
                Map.of(
                        "WRONG_CATEGORY", "Некорректная категория."
                )
        );

        return getModerationTaskDataModel(
                Map.of(
                        "https://s3.yandex.net/car.jpeg", getModerationContentModel(resultModel)
                )
        );
    }

    @Nonnull
    private ModerationTaskDataModel getModerationTaskDataModel(Map<String, ModerationContentModel> map) {
        ModerationTaskDataModel taskDataModel = new ModerationTaskDataModel();
        taskDataModel.setModerationContentModelMap(map);
        return taskDataModel;
    }

    @Nonnull
    private ModerationContentModel getModerationContentModel(ModerationContentResultModel resultModel) {
        return getModerationContentModel("https://s3.yandex.net/car.jpeg", "IMAGE",
                Map.of(
                        "vendor", "market",
                        "category", "phone"
                ),
                resultModel
        );
    }

    @Nonnull
    private ModerationContentModel getModerationContentModel(String content,
                                                             String contentRuleId,
                                                             Map<String, String> params,
                                                             ModerationContentResultModel resultModel) {
        ModerationContentModel contentModel = new ModerationContentModel();
        contentModel.setContent(content);
        contentModel.setRuleModel(new ModerationContentRuleModel(contentRuleId, params));
        contentModel.setResultModel(resultModel);

        return contentModel;
    }
}
