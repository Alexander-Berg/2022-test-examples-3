package ru.yandex.market.pers.qa.tms.toloka.receive;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.TolokaModerationResult;
import ru.yandex.market.pers.qa.model.TolokaModerationStatus;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.yt.YtClusterType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TolokaReceiveResultsForAnswerExecutorTest extends TolokaReceiveResultsExecutorTest {

    @Autowired
    private AnswerService answerService;

    @Autowired
    private TolokaReceiveResultsExecutor executor;

    @Test
    void testNoResultToReceive() {
        executor.receiveResultsForAnswer();
        checkYtWasNotCalled();
        verify(yqJdbcTemplate, never()).query(anyString(), (RowMapper<Pair<Long, TolokaModerationResult>>) any());
    }

    private Set<Long> createUploadedAnswers(int n, long questionId) {
        Set<Long> answers = new HashSet<>();
        for (int userId = 0; userId < n; userId++) {
            Answer answer = answerService.createAnswer(userId, "answer" + UUID.randomUUID().toString(), questionId);
            answers.add(answer.getId());
            answerService.forceUpdateModState(answer.getId(), ModState.TOLOKA_UPLOADED);
        }
        return answers;
    }

    @Test
    void testReceiveResultsHahn() {
        Exception error = testReceiveResults(YtClusterType.HAHN);
        assertNull(error);
    }

    @Test
    void testReceiveResultsArnold() {
        Exception error = testReceiveResults(YtClusterType.ARNOLD);
        assertNull(error);
    }

    @Test
    void testReceiveResultsBoth() {
        Exception error = testReceiveResults(YtClusterType.HAHN, YtClusterType.ARNOLD);
        assertNull(error);
    }

    @Test
    void testReceiveResultsHahnWithArnoldFailed() {
        Exception error = testReceiveResults(
            () -> imitateYtFailed(YtClusterType.ARNOLD),
            YtClusterType.HAHN);
        assertNotNull(error);
        assertTrue(error.getMessage().contains("disabled: " + YtClusterType.ARNOLD.getCode()));
    }

    @Test
    void testReceiveResultsArnoldWithHahnFailed() {
        Exception error = testReceiveResults(
            () -> imitateYtFailed(YtClusterType.HAHN),
            YtClusterType.ARNOLD);
        assertNotNull(error);
        assertTrue(error.getMessage().contains("disabled: " + YtClusterType.HAHN.getCode()));
    }

    @Test
    void testReceiveResultsBothFailed() {
        Exception error = testReceiveResults(() -> {
            imitateYtFailed(YtClusterType.HAHN);
            imitateYtFailed(YtClusterType.ARNOLD);
        });
        assertNotNull(error);
        assertTrue(error.getMessage().contains("disabled: " + YtClusterType.ARNOLD.getCode()));
    }

    private Exception testReceiveResults(YtClusterType... clusterTypes) {
        return testReceiveResults(null, clusterTypes);
    }

    private Exception testReceiveResults(Runnable postMock, YtClusterType... clusterTypes) {
        int n = 10;
        Set<YtClusterType> clustersSet = Arrays.stream(clusterTypes).collect(Collectors.toSet());

        // создаём тестовые данные
        final Question question = questionService.createModelQuestion(1, "test question", 1);
        final Question otherQuestion = questionService.createModelQuestion(1, "test question", 2);

        //создаём таблички на модерации
        List<TableInfo> unreadyTables = Arrays.stream(YtClusterType.values())
            .flatMap(clusterType ->
                IntStream.range(0, 10)
                    .mapToObj(i -> {
                        UploadItems items = new UploadItems();
                        markEntities(createUploadedAnswers(n, otherQuestion.getId()), items.getUnprocessed());
                        return createTolokaTableForTest(clusterType, QaEntityType.ANSWER, items);
                    }))
            .collect(Collectors.toList());

        // мокируем работу с YT
        List<TableInfo> readyTables = clustersSet.stream()
            .map(clusterType -> {
                UploadItems items = new UploadItems();
                markEntities(createUploadedAnswers(n, question.getId()), items.getOk());
                markEntities(createUploadedAnswers(n, question.getId()), items.getUnknown());
                markEntities(createUploadedAnswers(n, question.getId()), items.getRejected());

                //создаём табличку, которая есть с точки зрения ytClient в указанном кластере
                return createTolokaTableForTest(clusterType, QaEntityType.ANSWER, items);
            })
            .collect(Collectors.toList());

        // мокируем ответ YT
        // обозначим существование таблицы в искомом кластере
        // в других кластерах по умолчанию ничего нет, так что их отдельно не нужно упоминать
        readyTables.forEach(table -> {
            mockTableExists(table);
            mockYql(table);
        });

        // дополнительные моки, если нужно
        if (postMock != null) {
            postMock.run();
        }

        //Проверяем, что до обработки табличка в состоянии NEW
        assertTablesState(readyTables, TolokaModerationStatus.NEW);

        Exception executorException = null;
        try {
            executor.receiveResultsForAnswer();
        } catch (Exception cause) {
            executorException = cause;
        }

        //Проверяем, что табличка помечена обработанной
        assertTablesState(readyTables, TolokaModerationStatus.PROCESSED);

        //Проверяем, что ответам выставились нужные статусы на основании статусов модерации от Толоки
        assertModStates(QaEntityType.ANSWER,
            getModStates(),
            mergeItems(getItems(unreadyTables), getItems(readyTables))
        );

        //А остальные так и остались необработанными
        assertTablesState(unreadyTables, TolokaModerationStatus.NEW);

        return executorException;
    }

    @NotNull
    private List<Pair<Long, ModState>> getModStates() {
        return qaJdbcTemplate.query(
            "select id, mod_state from qa.answer",
            (rs, rowNum) -> Pair.of(
                rs.getLong("id"),
                ModState.valueOf(rs.getInt("mod_state"))
            ));
    }
}
