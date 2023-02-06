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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.TolokaModerationResult;
import ru.yandex.market.pers.qa.model.TolokaModerationStatus;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.yt.YtClusterType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

class TolokaReceiveResultsForQuestionExecutorTest extends TolokaReceiveResultsExecutorTest {

    @Autowired
    private TolokaReceiveResultsExecutor executor;

    @Autowired
    private QuestionService questionService;

    @Test
    void testNoResultToReceive() {
        executor.receiveResultsForQuestion();
        checkYtWasNotCalled();
        verify(yqJdbcTemplate, Mockito.never()).query(anyString(), (RowMapper<Pair<Long, TolokaModerationResult>>) any());
    }

    private Set<Long> createUploadedQuestions(int n, long modelId) {
        Set<Long> questions = new HashSet<>();
        for (int i = 0; i < n; i++) {
            Question question = questionService
                .createModelQuestion(i, "question" + UUID.randomUUID().toString(), modelId);
            questionService.forceUpdateModState(question.getId(), ModState.TOLOKA_UPLOADED);
            questions.add(question.getId());
        }
        return questions;
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

        //создаём таблички на модерации
        List<TableInfo> unreadyTables = Arrays.stream(YtClusterType.values())
            .flatMap(clusterType ->
                IntStream.range(0, 10)
                    .mapToObj(i -> {
                        UploadItems items = new UploadItems();
                        markEntities(createUploadedQuestions(n, 0), items.getUnprocessed());
                        return createTolokaTableForTest(clusterType, QaEntityType.QUESTION, items);
                    }))
            .collect(Collectors.toList());

        // мокируем работу с YT
        List<TableInfo> readyTables = clustersSet.stream()
            .map(clusterType -> {
                int modelId = 1;
                UploadItems items = new UploadItems();
                markEntities(createUploadedQuestions(n, modelId++), items.getOk());
                markEntities(createUploadedQuestions(n, modelId++), items.getUnknown());
                markEntities(createUploadedQuestions(n, modelId++), items.getRejected());
                markEntities(createUploadedQuestions(n, modelId++), items.getNotQuestions());
                markEntities(createUploadedQuestions(n, modelId++), items.getShopQuestions());

                //создаём табличку, которая есть с точки зрения ytClient в указанном кластере
                return createTolokaTableForTest(clusterType, QaEntityType.QUESTION, items);
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
            executor.receiveResultsForQuestion();
        } catch (Exception cause) {
            executorException = cause;
        }

        //Проверяем, что табличка помечена обработанной
        assertTablesState(readyTables, TolokaModerationStatus.PROCESSED);

        //Проверяем, что вопросам выставились нужные статусы на основании статусов модерации от Толоки
        assertModStates(QaEntityType.QUESTION,
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
            "select id, mod_state from qa.question",
            (rs, rowNum) -> Pair.of(
                rs.getLong("id"),
                ModState.valueOf(rs.getInt("mod_state"))
            ));
    }

}
