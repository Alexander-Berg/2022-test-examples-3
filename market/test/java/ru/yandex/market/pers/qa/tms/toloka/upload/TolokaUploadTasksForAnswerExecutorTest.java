package ru.yandex.market.pers.qa.tms.toloka.upload;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.client.model.QuestionType;
import ru.yandex.market.pers.qa.mock.ReportServiceMockUtils;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.State;
import ru.yandex.market.pers.qa.model.TolokaModerationStatus;
import ru.yandex.market.pers.qa.service.toloka.TolokaModerationService;
import ru.yandex.market.pers.qa.service.toloka.model.AnswerForToloka;
import ru.yandex.market.pers.qa.service.toloka.model.TolokaEntity;
import ru.yandex.market.pers.qa.service.toloka.upload.TolokaUploadAnswerService;
import ru.yandex.market.pers.qa.service.toloka.upload.UnnecessaryUploadException;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClusterType;
import ru.yandex.market.report.model.Category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TolokaUploadTasksForAnswerExecutorTest extends TolokaUploadTasksExecutorTest {

    @Autowired
    private TolokaUploadAnswerService tolokaUploadAnswerService;

    @Autowired
    private TolokaUploadTasksExecutor executor;

    @Autowired
    private TolokaModerationService tolokaModerationService;

    @Test
    void testNotEnoughEntitiesUnnecessaryUpload() {
        Exception e = Assertions.assertThrows(UnnecessaryUploadException.class,
            () -> tolokaUploadAnswerService.upload());
        assertTrue(e.getMessage().contains("Not enough entities for upload"));

        checkYtWasNotCalled();
    }

    @Test
    void testTooManyUploadsAlready() {
        long uploadLimit = tolokaUploadAnswerService.getUploadLimit();
        for (int i = 0; i < uploadLimit; i++) {
            String tableName = UUID.randomUUID().toString();
            tolokaModerationService.markTablesUploaded(
                QaEntityType.ANSWER,
                YtClusterType.HAHN,
                tableName,
                Collections.emptyList()
            );
        }
        Exception e = Assertions.assertThrows(UnnecessaryUploadException.class,
            () -> tolokaUploadAnswerService.upload());
        assertTrue(e.getMessage().contains("Too many tables for type"));

        checkYtWasNotCalled();
    }

    @Test
    void testUploadLimit() {
        long uploadLimit = tolokaUploadAnswerService.getUploadLimit();
        for (int i = 0; i < uploadLimit; i++) {
            String tableName = UUID.randomUUID().toString();
            tolokaModerationService.markTablesUploaded(
                QaEntityType.ANSWER,
                YtClusterType.HAHN,
                tableName,
                Collections.emptyList()
            );

            tolokaModerationService.updateTableState(
                QaEntityType.ANSWER,
                tableName,
                TolokaModerationStatus.PROCESSED
            );
        }

        Exception e = Assertions.assertThrows(UnnecessaryUploadException.class,
            () -> tolokaUploadAnswerService.upload());
        assertTrue(e.getMessage().contains("Not enough entities for upload"));

        checkYtWasNotCalled();
    }

    @Test
    void testUploadToHahnOnly() {
        changeUploadYtCluster(YtClusterType.HAHN);
        tryUploadAndCheck(YtClusterType.HAHN);
    }

    @Test
    void testUploadToArnoldOnly() {
        changeUploadYtCluster(YtClusterType.ARNOLD);
        tryUploadAndCheck(YtClusterType.ARNOLD);
    }

    private void tryUploadAndCheck(YtClusterType clusterType) {
        ReportServiceMockUtils.mockReportServiceByModelIdModules(reportService, MOD1, MOD2);

        int expectedListSize = prepareAnswers();

        executor.uploadAnswers();

        // check that expected server was called
        YtClient ytClient = getYtClient(clusterType);
        verify(ytClient).createTable(
            txArgumentCaptor1.capture(),
            objectYPathArgumentCaptor1.capture(),
            tableSchemaArgumentCaptor.capture());
        verify(ytClient).append(
            txArgumentCaptor2.capture(),
            objectYPathArgumentCaptor2.capture(),
            listArgumentCaptor.capture()
        );
        verify(ytClient).doInTransaction(any());
        verifyNoMoreInteractions(ytClient);

        // check that no more yt clusters were called
        Arrays.stream(YtClusterType.values())
            .filter(x -> x != clusterType)
            .forEach(this::checkYtWasNotCalled);

        assertEquals(txArgumentCaptor1.getValue(), txArgumentCaptor2.getValue());
        assertEquals(objectYPathArgumentCaptor1.getValue(), objectYPathArgumentCaptor2.getValue());
        assertEquals(AnswerForToloka.tableSchema(), tableSchemaArgumentCaptor.getValue());
        List<?> entities = listArgumentCaptor.getValue();

        int uploaded = getUploadedCount();
        assertEquals(expectedListSize, entities.size());
        assertEquals(expectedListSize, uploaded);
    }

    @Test
    public void testUploadWithDataCheck() {
        YtClusterType clusterType = YtClusterType.HAHN;

        Question[] questions = {
            questionService.createModelQuestion(1, "text m1", 1),
            questionService.createModelQuestion(2, "text m2", 2),
            questionService.createCategoryQuestion(3, "text cat1", 3),
            questionService.createCategoryQuestion(4, "text cat2", 4),
            questionService.createCategoryQuestion(5, "text cat3", 5),
        };

        for (Question question : questions) {
            questionService.forceUpdateModState(question.getId(), ModState.CONFIRMED);
        }

        Answer[] answers = {
            answerService.createAnswer(1, "ans1", questions[0].getId()),
            answerService.createAnswer(2, "ans2", questions[1].getId()),
            answerService.createAnswer(3, "ans3", questions[2].getId()),
            answerService.createAnswer(4, "ans4", questions[3].getId()),
            answerService.createAnswer(5, "ans5", questions[4].getId()),
        };

        for (Answer answer : answers) {
            answerService.forceUpdateModState(answer.getId(), ModState.COMPLAINED);
        }

        // mock model 1, do not mock model 2 (does not exists)
        ReportServiceMockUtils.mockReportServiceByModelIdCategories(reportService, 1, 3, new Category(3, ""));

        // mock categories 3 and 5
        when(catalogerClient.getTree(3, 0)).thenReturn(Optional.of(mockCategory(3, "Some Name")));
        when(catalogerClient.getTree(4, 0)).thenReturn(Optional.empty());
        when(catalogerClient.getTree(5, 0)).thenReturn(Optional.of(mockCategory(5, "Some another name")));

        // mock pictures for 3 category
        HashMap<Long, String> pictures = new HashMap<>();
        pictures.put(3L, "//cat-picture");
        when(reportService.getMainPictureByHid(any())).thenReturn(pictures);

        // lower thesholds
        configurationService.mergeValue(TolokaUploadAnswerService.THRESHOLD_FOR_UPLOAD_ANSWERS_KEY, "1");

        executor.uploadAnswers();

        // check result
        YtClient ytClient = getYtClient(clusterType);
        verify(ytClient).append(any(), any(), listArgumentCaptor.capture());

        Map<Long, ? extends TolokaEntity> items = ((List<? extends TolokaEntity>) listArgumentCaptor.getValue())
            .stream()
            .collect(Collectors.toMap(TolokaEntity::getId, x -> x));

        assertEquals(3, items.size());

        checkEntity(items.get(answers[0].getId()), QuestionType.MODEL, 1, "ans1", "https:yandex.ru");
        checkEntity(items.get(answers[2].getId()), QuestionType.CATEGORY, 3, "ans3", "https://cat-picture");
        checkEntity(items.get(answers[4].getId()), QuestionType.CATEGORY, 5, "ans5", null);
    }

    @Test
    void testYtMaintenance() {
        ReportServiceMockUtils.mockReportServiceByModelIdModules(reportService, MOD1, MOD2);

        prepareAnswers();

        imitateYtFailed();

        Assertions.assertThrows(YtDisabledException.class, () -> executor.uploadAnswers());

        int uploaded = getUploadedCount();
        assertEquals(0, uploaded);
    }

    /**
     * Создаём все комбинации статусов в вопросах и ответах (TOLOKA_UPLOADED для ответов), а также ещё 50 ответов в статусе TOLOKA_UNKNOWN
     * Возвращает ожидаемое число ответов для отправки на модерацию
     */
    private int prepareAnswers() {
        int answerIdx = 1;
        for (ModState questionModState : ModState.values()) {
            Question question = questionService.createModelQuestion(answerIdx, questionModState.name(), answerIdx);
            for (ModState answerModState : ModState.values()) {
                if (answerModState != ModState.TOLOKA_UPLOADED) {
                    Answer answer = answerService.createAnswer(answerIdx, answerModState.name(), question.getId());
                    answerService.forceUpdateModState(answer.getId(), answerModState);
                    answerIdx++;
                }
            }
            questionService.forceUpdateModState(question.getId(), questionModState);
        }

        // отправляем ответы в статусе TOLOKA_UNKNOWN и COMPLAINED и когда статус вопроса не TOLOKA_UNKNOWN или CONFIRMED
        // количество комбинаций легко подсчитать
        final int expected = 2 * 2;

        // создаём заранее известное количество ответов в статусе TOLOKA UNKNOWN на произвольный вопрос
        final int tolokaUnknownAnswerCount = 50;

        final Question question = questionService.createModelQuestion(1, "text", 1);
        questionService.forceUpdateModState(question.getId(), ModState.CONFIRMED);

        for (int i = 0; i < tolokaUnknownAnswerCount; i++) {
            Answer answer = answerService.createAnswer(i, "text" + i, question.getId());
            answerService.forceUpdateModState(answer.getId(), ModState.TOLOKA_UNKNOWN);
        }

        // создаём один удалённый ответ - он не должен попадать в выгрузку в толоку
        Answer deletedAnswer = answerService.createAnswer(1, "deleted answer", question.getId());
        answerService.forceUpdateModState(deletedAnswer.getId(), ModState.TOLOKA_UNKNOWN);
        answerService.forceUpdateState(deletedAnswer.getId(), State.DELETED);

        // создаём вендорские ответы в различных ModState - они не должен попадать в выгрузку в толоку
        for (ModState modState : ModState.values()) {
            if (modState != ModState.TOLOKA_UPLOADED) {
                Answer vendorAnswer = answerService.createVendorAnswer(1, UUID.randomUUID().toString(), question.getId(), 2);
                answerService.forceUpdateModState(vendorAnswer.getId(), modState);
            }
        }

        return expected + tolokaUnknownAnswerCount;
    }

    private Integer getUploadedCount() {
        return jdbcTemplate.queryForObject("select count(*) from qa.answer where mod_state = ?",
            Integer.class,
            ModState.TOLOKA_UPLOADED.getValue());
    }

}
