package ru.yandex.market.pers.qa.tms.toloka.upload;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.client.model.QuestionType;
import ru.yandex.market.pers.qa.mock.ReportServiceMockUtils;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.State;
import ru.yandex.market.pers.qa.model.TolokaModerationStatus;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.toloka.TolokaModerationService;
import ru.yandex.market.pers.qa.service.toloka.model.QuestionForToloka;
import ru.yandex.market.pers.qa.service.toloka.model.TolokaEntity;
import ru.yandex.market.pers.qa.service.toloka.upload.TolokaUploadQuestionService;
import ru.yandex.market.pers.qa.service.toloka.upload.UnnecessaryUploadException;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClusterType;
import ru.yandex.market.report.model.Category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TolokaUploadTasksForQuestionExecutorTest extends TolokaUploadTasksExecutorTest {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private TolokaUploadQuestionService tolokaUploadQuestionService;

    @Autowired
    private TolokaUploadTasksExecutor executor;

    @Autowired
    private TolokaModerationService tolokaModerationService;

    /**
     * Создаём по passed вопросов в статусах AUTO_FILTER_PASSED и unknown в TOLOKA_UNKNOWN
     * А также по одному вопросу в каждом из оставшихся статусов
     */
    private void prepareQuestions(int passed, int unknown) {
        for (int i = 0; i < passed; i++) {
            Question question = questionService.createModelQuestion(i, "text" + i, passed - i);
            questionService.forceUpdateModState(question.getId(), ModState.AUTO_FILTER_PASSED);
        }

        // создаётся удалённый ответ, он не должен попадать в выгрузку в толоку
        final Question deletedQuestion = questionService.createModelQuestion(1, "deleted question", 1);
        questionService.forceUpdateModState(deletedQuestion.getId(), ModState.AUTO_FILTER_PASSED);
        questionService.forceUpdateState(deletedQuestion.getId(), State.DELETED);

        for (int i = 0; i < unknown; i++) {
            Question question = questionService.createModelQuestion(i, i + "text", unknown - i);
            questionService.forceUpdateModState(question.getId(), ModState.TOLOKA_UNKNOWN);
        }

        Stream.of(ModState.values())
            .filter(modState -> modState != ModState.AUTO_FILTER_PASSED)
            .filter(modState -> modState != ModState.TOLOKA_UNKNOWN)
            .filter(modState -> modState != ModState.NEW)
            .filter(modState -> modState != ModState.TOLOKA_UPLOADED)
            .filter(modState -> modState != ModState.COMPLAINED)
            .forEach(modState -> {
                Question question = questionService.createModelQuestion(1, modState.name(), 1);
                questionService.forceUpdateModState(question.getId(), modState);
            });

        questionService.createModelQuestion(1, ModState.NEW.name(), 1);
    }

    @Test
    void testNotEnoughEntitiesUnnecessaryUpload() {
        Exception e = Assertions.assertThrows(UnnecessaryUploadException.class,
            () -> tolokaUploadQuestionService.upload());
        assertTrue(e.getMessage().contains("Not enough entities for upload"));

        checkYtWasNotCalled();
    }

    @Test
    void testTooManyUploadsAlready() {
        ReportServiceMockUtils.mockReportServiceByModelIdModules(reportService, MOD1, MOD2);
        long uploadLimit = tolokaUploadQuestionService.getUploadLimit();
        for (int i = 0; i < uploadLimit; i++) {
            String tableName = UUID.randomUUID().toString();
            tolokaModerationService.markTablesUploaded(
                QaEntityType.QUESTION,
                YtClusterType.HAHN,
                tableName,
                Collections.emptyList()
            );
        }
        long threshold = tolokaUploadQuestionService.getThresholdForUpload() - 1;
        for (int i = 0; i < threshold; i++) {
            Question question = questionService.createModelQuestion(i, "text" + i, 1);
            questionService.forceUpdateModState(question.getId(), ModState.AUTO_FILTER_PASSED);
        }

        Exception e = Assertions.assertThrows(UnnecessaryUploadException.class,
            () -> tolokaUploadQuestionService.upload());
        assertTrue(e.getMessage().contains("Too many tables for type"));

        checkYtWasNotCalled();
        assertEquals((Integer) 0, getUploadedCount());
    }

    @Test
    void testUploadLimit() {
        ReportServiceMockUtils.mockReportServiceByModelIdModules(reportService, MOD1, MOD2);
        long uploadLimit = tolokaUploadQuestionService.getUploadLimit();
        for (int i = 0; i < uploadLimit; i++) {
            String tableName = UUID.randomUUID().toString();
            tolokaModerationService.markTablesUploaded(
                QaEntityType.QUESTION,
                YtClusterType.HAHN,
                tableName,
                Collections.emptyList()
            );

            tolokaModerationService.updateTableState(
                QaEntityType.QUESTION,
                tableName,
                TolokaModerationStatus.PROCESSED
            );
        }

        long threshold = tolokaUploadQuestionService.getThresholdForUpload() - 1;
        for (int i = 0; i < threshold; i++) {
            Question question = questionService.createModelQuestion(i, "text" + i, 1);
            questionService.forceUpdateModState(question.getId(), ModState.AUTO_FILTER_PASSED);
        }

        Exception e = Assertions.assertThrows(UnnecessaryUploadException.class,
            () -> tolokaUploadQuestionService.upload());
        assertTrue(e.getMessage().contains("Not enough entities for upload"));

        checkYtWasNotCalled();
        assertEquals((Integer) 0, getUploadedCount());
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

        int questionPassedCount = 100;
        int questionUnknownCount = 50;
        prepareQuestions(questionPassedCount, questionUnknownCount);

        executor.uploadQuestions();

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
        assertEquals(tableSchemaArgumentCaptor.getValue(), QuestionForToloka.tableSchema());
        List<?> entities = listArgumentCaptor.getValue();

        //expectedListSize = число вопросов в статусе AUTO_FILTER_PASSED, среди которых каждый MOD1 и каждый MOD2 без валидной модельки от репорта
        // + число вопросов в статусе TOLOKA_UNKNOWN, среди которых каждый MOD1 и каждый MOD2 без валидной модельки от репорта
        int expectedListSize = questionPassedCount - (questionPassedCount / MOD1) - (questionPassedCount / MOD2)
            + questionUnknownCount - (questionUnknownCount / MOD1) - (questionUnknownCount / MOD2);
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
            questionService.forceUpdateModState(question.getId(), ModState.AUTO_FILTER_PASSED);
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
        configurationService.mergeValue(TolokaUploadQuestionService.THRESHOLD_FOR_UPLOAD_QUESTIONS_KEY, "1");

        executor.uploadQuestions();

        // check result
        YtClient ytClient = getYtClient(clusterType);
        verify(ytClient).append(any(), any(), listArgumentCaptor.capture());

        Map<Long, ? extends TolokaEntity> items = ((List<? extends TolokaEntity>) listArgumentCaptor.getValue())
            .stream()
            .collect(Collectors.toMap(TolokaEntity::getId, x -> x));

        assertEquals(3, items.size());

        checkEntity(items.get(questions[0].getId()), QuestionType.MODEL, 1, "text m1", "https:yandex.ru");
        checkEntity(items.get(questions[2].getId()), QuestionType.CATEGORY, 3, "text cat1", "https://cat-picture");
        checkEntity(items.get(questions[4].getId()), QuestionType.CATEGORY, 5, "text cat3", null);
    }

    @Test
    void testYtMaintenance() {
        ReportServiceMockUtils.mockReportServiceByModelIdModules(reportService, MOD1, MOD2);

        prepareQuestions(100, 50);

        imitateYtFailed();

        Assertions.assertThrows(YtDisabledException.class, () -> executor.uploadQuestions());

        int uploaded = getUploadedCount();
        assertEquals(0, uploaded);
    }

    private Integer getUploadedCount() {
        return jdbcTemplate.queryForObject("select count(*) from qa.question where mod_state = ?",
            Integer.class,
            ModState.TOLOKA_UPLOADED.getValue());
    }

}
