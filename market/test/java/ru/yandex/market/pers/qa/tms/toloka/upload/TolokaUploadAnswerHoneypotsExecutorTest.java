package ru.yandex.market.pers.qa.tms.toloka.upload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.mock.ReportServiceMockUtils;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.TolokaModerationResult;
import ru.yandex.market.pers.qa.service.toloka.model.TolokaHoneypot;
import ru.yandex.market.pers.qa.service.toloka.upload.UnnecessaryUploadException;
import ru.yandex.market.pers.qa.service.toloka.upload.honeypots.TolokaUploadAnswerHoneypotsService;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClusterType;
import ru.yandex.market.report.model.Category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.qa.client.model.QuestionType.CATEGORY;
import static ru.yandex.market.pers.qa.client.model.QuestionType.MODEL;

class TolokaUploadAnswerHoneypotsExecutorTest extends TolokaUploadHoneypotsExecutorTest {

    @Autowired
    private TolokaUploadHoneypotsExecutor executor;

    @Autowired
    private TolokaUploadAnswerHoneypotsService tolokaUploadHoneypotsService;

    @Test
    void testUploadDisabled() {
        executor.uploadHoneypotsForAnswers();
        verifyZeroInteractions(reportService);
        checkYtWasNotCalled();
    }

    @Test
    void testUploadNotEnoughEntities() {
        tolokaUploadHoneypotsService.enableHoneypotUpload();
        Exception e = Assertions.assertThrows(UnnecessaryUploadException.class,
            () -> executor.uploadHoneypotsForAnswers());
        Assertions.assertTrue(e.getMessage().contains("Not enough entities for upload. Actual=0."));
    }

    @Test
    void testUploadHahn() {
        changeUploadYtCluster(YtClusterType.HAHN);

        tryUploadHoneypots();

        checkYtClientUploadOk(YtClusterType.HAHN);
        checkYtWasNotCalled(YtClusterType.ARNOLD);

        // disabled since successfully uploaded
        assertFalse(tolokaUploadHoneypotsService.isHoneypotUploadEnabled());
    }

    @Test
    void testUploadHahnWithArnoldDown() {
        changeUploadYtCluster(YtClusterType.HAHN);
        imitateYtFailed(YtClusterType.ARNOLD);

        tryUploadHoneypots();

        checkYtClientUploadOk(YtClusterType.HAHN);
        checkYtWasNotCalled(YtClusterType.ARNOLD);

        // disabled since successfully uploaded
        assertFalse(tolokaUploadHoneypotsService.isHoneypotUploadEnabled());
    }

    @Test
    void testUploadHahnFailed() {
        changeUploadYtCluster(YtClusterType.HAHN);
        imitateYtFailed(YtClusterType.HAHN);

        assertThrows(YtDisabledException.class, this::tryUploadHoneypots);

        checkYtWasOnlyTriedToCall(YtClusterType.HAHN);
        checkYtWasNotCalled(YtClusterType.ARNOLD);

        // still enabled since did not uploaded to all clusters
        assertTrue(tolokaUploadHoneypotsService.isHoneypotUploadEnabled());
    }

    @Test
    void testUploadArnoldFailed() {
        changeUploadYtCluster(YtClusterType.ARNOLD);
        imitateYtFailed(YtClusterType.ARNOLD);

        assertThrows(YtDisabledException.class, this::tryUploadHoneypots);

        checkYtWasNotCalled(YtClusterType.HAHN);
        checkYtWasOnlyTriedToCall(YtClusterType.ARNOLD);

        // still enabled since did not uploaded to all clusters
        assertTrue(tolokaUploadHoneypotsService.isHoneypotUploadEnabled());
    }

    private void tryUploadHoneypots() {
        ReportServiceMockUtils.mockReportService(reportService);
        createHoneypots(tolokaUploadHoneypotsService.getThresholdForUpload());

        tolokaUploadHoneypotsService.enableHoneypotUpload();
        executor.uploadHoneypotsForAnswers();
    }

    private void checkYtClientUploadOk(YtClusterType clusterType) {
        YtClient ytClient = getYtClient(clusterType);
        verify(ytClient).createTable(
            txArgumentCaptor1.capture(),
            objectYPathArgumentCaptor1.capture());
        verify(ytClient).append(
            txArgumentCaptor2.capture(),
            objectYPathArgumentCaptor2.capture(),
            listArgumentCaptor.capture()
        );
        verify(ytClient).doInTransaction(any());
        verifyNoMoreInteractions(ytClient);

        assertEquals(txArgumentCaptor1.getValue(), txArgumentCaptor2.getValue());
        assertEquals(objectYPathArgumentCaptor1.getValue(), objectYPathArgumentCaptor2.getValue());
        List<?> entities = listArgumentCaptor.getValue();
        assertEquals(tolokaUploadHoneypotsService.getThresholdForUpload(), entities.size());
    }

    @Test
    public void testUploadWithDataCheck() {
        YtClusterType clusterType = YtClusterType.HAHN;

        Question[] questions = {
            questionService.createModelQuestion(1, "text m1", 1),
            questionService.createCategoryQuestion(3, "text cat1", 3)
        };

        for (Question question : questions) {
            questionService.forceUpdateModState(question.getId(), ModState.CONFIRMED);
        }

        Answer[] answers = {
            answerService.createAnswer(1, "ans1", questions[0].getId()),
            answerService.createAnswer(2, "ans2", questions[1].getId()),
        };

        for (Answer answer : answers) {
            answerService.forceUpdateModState(answer.getId(), ModState.COMPLAINED);
        }

        addAnswerHoneypot(answers[0].getId(), TolokaModerationResult.APPROVE);
        addAnswerHoneypot(answers[1].getId(), TolokaModerationResult.REJECTED);

        // mock model 1, do not mock model 2 (does not exists)
        ReportServiceMockUtils.mockReportServiceByModelIdCategories(reportService, 1, 3, new Category(3, ""));

        // mock category 3
        when(catalogerClient.getTree(3, 0)).thenReturn(Optional.of(mockCategory(3, "Some Name")));

        // mock pictures for 3 category
        HashMap<Long, String> pictures = new HashMap<>();
        pictures.put(3L, "//cat-picture");
        when(reportService.getMainPictureByHid(any())).thenReturn(pictures);

        // lower thesholds
        configurationService.mergeValue(TolokaUploadAnswerHoneypotsService.HONEYPOTS_MIN_COUNT_KEY, "1");

        tolokaUploadHoneypotsService.enableHoneypotUpload();
        executor.uploadHoneypotsForAnswers();

        // check result
        YtClient ytClient = getYtClient(clusterType);
        verify(ytClient).append(any(), any(), listArgumentCaptor.capture());

        Map<Long, TolokaHoneypot> items = ((List<TolokaHoneypot>) listArgumentCaptor.getValue())
            .stream()
            .collect(Collectors.toMap(x -> x.getTolokaEntity().getId(), x -> x));

        assertEquals(2, items.size());

        checkEntity(items.get(answers[0].getId()).getTolokaEntity(), MODEL, 1, "ans1", "https:yandex.ru");
        assertEquals(TolokaModerationResult.APPROVE.getName(),
            items.get(answers[0].getId()).getTolokaResult().getResult());

        checkEntity(items.get(answers[1].getId()).getTolokaEntity(), CATEGORY, 3, "ans2", "https://cat-picture");
        assertEquals(TolokaModerationResult.REJECTED.getName(),
            items.get(answers[1].getId()).getTolokaResult().getResult());
    }

    private void addAnswerHoneypot(long answerId, TolokaModerationResult result) {
        jdbcTemplate.update("insert into qa.answer_honeypots(id, result) values (?, ?)",
            answerId,
            result.getValue());
    }

    private void createHoneypots(long n) {
        List<Long> answers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Question question = questionService.createModelQuestion(i, UUID.randomUUID().toString(), n - i);
            answers.add(answerService.createAnswer(i, UUID.randomUUID().toString(), question.getId()).getId());
        }
        answers.forEach(answerId -> {
            addAnswerHoneypot(
                answerId,
                rnd.nextBoolean() ? TolokaModerationResult.APPROVE : TolokaModerationResult.REJECTED);
        });
    }

}
