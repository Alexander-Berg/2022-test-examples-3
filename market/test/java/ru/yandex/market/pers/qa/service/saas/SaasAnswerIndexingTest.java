package ru.yandex.market.pers.qa.service.saas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.pers.qa.PersQATest;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.VoteValueType;
import ru.yandex.market.pers.qa.model.saas.IndexingMode;
import ru.yandex.market.pers.qa.model.saas.SaasIndexingResult;
import ru.yandex.market.pers.qa.model.saas.UploadMethod;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.VoteService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author vvolokh
 * 07.06.2019
 */
public class SaasAnswerIndexingTest extends PersQATest {
    private static final long UID = 1348719867L;
    public static final long ANSWER_UID = UID + 1;
    private static final int MODEL_ID = 1;
    
    List<QaEntityType> entityTypeAnswerList = Collections.singletonList(QaEntityType.ANSWER);

    @Autowired
    private SaasIndexingService indexingService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private VoteService voteService;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    @Test
    void testDiffQueueFillingOnCreation() {
        indexingService.markEntitiesForIndexing(entityTypeAnswerList);
        assertFalse(hasAnswersInIndexQueue());

        // check item added after creation
        Question question = createQuestion();
        Answer answer = createAnswer(question.getId());

        indexingService.markEntitiesForIndexing(entityTypeAnswerList);
        assertTrue(hasAnswersInIndexQueue());

        assertArrayEquals(new long[]{answer.getId()}, getAnswersQueue());

        indexingService.cleanQueue();
        assertFalse(hasAnswersInIndexQueue());
    }

    @Test
    void testRefreshQueueFilling() {
        indexingService.addAnswersToRefreshQueue();
        assertFalse(hasAnswersInRefreshQueue());

        // check item added after creation
        Question question = createQuestion();
        Answer answer = createAnswer(question.getId());

        indexingService.addAnswersToRefreshQueue();
        assertTrue(hasAnswersInRefreshQueue());

        assertArrayEquals(new long[]{answer.getId()}, getAnswersRefreshQueue());
    }

    @Test
    void testDiffQueueFillingAfterRemoving() {
        Question question = createQuestion();
        Answer answer = createAnswer(question.getId());

        resetQueue();
        assertFalse(hasAnswersInIndexQueue());

        answerService.deleteAnswer(answer.getId(), ANSWER_UID);

        indexingService.markEntitiesForIndexing(entityTypeAnswerList);
        assertTrue(hasAnswersInIndexQueue());

        assertArrayEquals(new long[]{answer.getId()}, getAnswersQueue());
    }

    private void resetQueue() {
        indexingService.cleanQueue();
        indexingService.markEntitiesForIndexing(entityTypeAnswerList);
    }

    @Test
    void testDiffQueueFillingAfterChangingModState() {
        Question question = createQuestion();
        Answer answer = createAnswer(question.getId());
        Answer answer2 = createAnswer(question.getId());

        resetQueue();
        assertFalse(hasAnswersInIndexQueue());

        answerService.forceUpdateModState(answer2.getId(), ModState.CONFIRMED);

        indexingService.markEntitiesForIndexing(entityTypeAnswerList);
        assertTrue(hasAnswersInIndexQueue());

        assertArrayEquals(new long[]{answer2.getId()}, getAnswersQueue());
    }

    @Test
    void testDiffQueueFillingAfterAddingVote() {
        Question question = createQuestion();
        Answer answer = createAnswer(question.getId());
        Answer answer2 = createAnswer(question.getId());

        resetQueue();
        assertFalse(hasAnswersInIndexQueue());

        voteService.createAnswerVote(answer.getId(), UID, VoteValueType.LIKE);
        voteService.createAnswerVote(answer2.getId(), UID, VoteValueType.DISLIKE);

        indexingService.markEntitiesForIndexing(entityTypeAnswerList);
        assertTrue(hasAnswersInIndexQueue());
        assertArrayEquals(new long[]{answer.getId(), answer2.getId()}, getAnswersQueue());

        resetQueue();
        assertFalse(hasAnswersInIndexQueue());

        voteService.deleteAnswerVote(answer.getId(), UID);

        indexingService.markEntitiesForIndexing(entityTypeAnswerList);
        assertTrue(hasAnswersInIndexQueue());
        assertArrayEquals(new long[]{answer.getId()}, getAnswersQueue());
    }

    private boolean hasAnswersInIndexQueue() {
        return indexingService.hasItemsToIndex(entityTypeAnswerList);
    }

    private boolean hasAnswersInRefreshQueue() {
        return jdbcTemplate.queryForObject("SELECT count(entity_id) FROM qa.saas_refresh_indexing_queue WHERE entity_type = " + QaEntityType.ANSWER.getValue(), Integer.class) > 0;
    }

    @Test
    void checkBaseIndexation() {
        // create questions
        Question questionBase = createQuestion();
        Answer answer = createAnswer(questionBase.getId());

        Answer answerDeleted = createAnswer(questionBase.getId());
        answerService.deleteAnswer(answerDeleted.getId(), ANSWER_UID);

        Answer answerBanned = createAnswer(questionBase.getId());
        answerService.forceUpdateModState(answerBanned.getId(), ModState.TOLOKA_REJECTED);

        Answer answer2 = createAnswer(questionBase.getId());

        // run indexation (items are not indexed)
        indexingService.buildSession(UploadMethod.LOGBROKER)
            .withMode(IndexingMode.DIFF)
            .forEntities(QaEntityType.ANSWER)
            .runIndexing(session -> {
                //remember - no snapshots
                assertTrue(session.getMode() == IndexingMode.DIFF);
                return SaasIndexingResult.ok();
            });

        assertFalse(hasAnswersInIndexQueue());
    }

    @Test
    void testIndexationTimeFill() {
        // create question and answers
        Question questionBase = createQuestion();
        Answer answer = createAnswer(questionBase.getId());

        Answer answerDeleted = createAnswer(questionBase.getId());
        answerService.deleteAnswer(answerDeleted.getId(), ANSWER_UID);

        Answer answerBanned = createAnswer(questionBase.getId());
        answerService.forceUpdateModState(answerBanned.getId(), ModState.TOLOKA_REJECTED);

        Answer answer2 = createAnswer(questionBase.getId());

        // check no requires yet
        assertEquals(0, indexingService.getEntitiesWithoutIndexTime(QaEntityType.ANSWER, 100).size());

        // run indexation (items indexed ok)
        indexingService.buildSession(UploadMethod.LOGBROKER)
            .withMode(IndexingMode.DIFF)
            .forEntities(QaEntityType.ANSWER)
            .runIndexing(session -> {
                assertTrue(session.getMode() == IndexingMode.DIFF);
                return SaasIndexingResult.ok();
            });

        //check
        List<Long> idsNeedIndexTime = indexingService.getEntitiesWithoutIndexTime(QaEntityType.ANSWER, 100);
        assertEquals(2, idsNeedIndexTime.size());
        assertArrayEquals(
            new long[]{answer.getId(), answer2.getId()},
            idsNeedIndexTime.stream().sorted().mapToLong(x -> x).toArray()
        );

        // fill
        indexingService.updateIndexTime(QaEntityType.ANSWER, idsNeedIndexTime);

        // check no more requires
        assertEquals(0, indexingService.getEntitiesWithoutIndexTime(QaEntityType.ANSWER, 100).size());
    }

    private long[] getAnswersQueue() {
        return jdbcTemplate.queryForList(
            "select entity_id from qa.v_saas_indexing_queue_ready where entity_type = ?",
            Long.class,
            QaEntityType.ANSWER.getValue()
        )
            .stream()
            .mapToLong(x -> x)
            .sorted()
            .toArray();
    }

    private long[] getAnswersRefreshQueue() {
        return jdbcTemplate.queryForList(
            "select entity_id from qa.saas_refresh_indexing_queue where entity_type = ?",
            Long.class,
            QaEntityType.ANSWER.getValue()
        )
            .stream()
            .mapToLong(x -> x)
            .sorted()
            .toArray();
    }

    private Question createQuestion() {
        Question question = Question.buildModelQuestion(UID, UUID.randomUUID().toString(), MODEL_ID);
        return questionService.createQuestion(question, null);
    }

    private Answer createAnswer(long questionId) {
        Answer answer = Answer.buildBasicAnswer(ANSWER_UID, UUID.randomUUID().toString(), questionId);
        return answerService.createAnswer(answer, null);
    }
}
