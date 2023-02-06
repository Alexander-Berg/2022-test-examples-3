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
 * @author Ilya Kislitsyn / ilyakis@ / 25.12.2018
 */
public class SaasIndexingServiceTest extends PersQATest {
    private static final long UID = 1348719867L;
    private static final int MODEL_ID = 1;
    
    private List<QaEntityType> entityTypeQuestionList = Collections.singletonList(QaEntityType.QUESTION);

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
        indexingService.markEntitiesForIndexing(entityTypeQuestionList);
        assertFalse(hasQuestionsInIndexQueue());

        // check item added after creation
        Question question = createQuestion();
        Question question2 = createQuestion();

        indexingService.markEntitiesForIndexing(entityTypeQuestionList);
        assertTrue(hasQuestionsInIndexQueue());

        assertArrayEquals(new long[]{question.getId(), question2.getId()}, getQuestionsQueue());

        indexingService.cleanQueue(QaEntityType.QUESTION);
        assertFalse(hasQuestionsInIndexQueue());
    }

    @Test
    void testDiffQueueFillingAfterRemoving() {
        Question question = createQuestion();
        Question question2 = createQuestion();

        indexingService.cleanQueue(QaEntityType.QUESTION);
        indexingService.markEntitiesForIndexing(entityTypeQuestionList);
        assertFalse(hasQuestionsInIndexQueue());

        questionService.deleteQuestion(question.getId(), UID);

        indexingService.markEntitiesForIndexing(entityTypeQuestionList);
        assertTrue(hasQuestionsInIndexQueue());

        assertArrayEquals(new long[]{question.getId()}, getQuestionsQueue());
    }

    @Test
    void testDiffQueueFillingAfterChangingModState() {
        Question question = createQuestion();
        Question question2 = createQuestion();

        indexingService.cleanQueue(QaEntityType.QUESTION);
        indexingService.markEntitiesForIndexing(entityTypeQuestionList);
        assertFalse(hasQuestionsInIndexQueue());

        questionService.forceUpdateModState(question2.getId(), ModState.COMPLAINED);

        indexingService.markEntitiesForIndexing(entityTypeQuestionList);
        assertTrue(hasQuestionsInIndexQueue());

        assertArrayEquals(new long[]{question2.getId()}, getQuestionsQueue());
    }

    @Test
    void testDiffQueueFillingAfterAddingAnswer() {
        Question question = createQuestion();
        Question question2 = createQuestion();

        indexingService.cleanQueue(QaEntityType.QUESTION);
        indexingService.markEntitiesForIndexing(entityTypeQuestionList);
        assertFalse(hasQuestionsInIndexQueue());

        answerService.createAnswer(Answer.buildBasicAnswer(
            UID,
            UUID.randomUUID().toString(),
            question.getId()
        ), null);

        indexingService.markEntitiesForIndexing(entityTypeQuestionList);
        assertTrue(hasQuestionsInIndexQueue());

        assertArrayEquals(new long[]{question.getId()}, getQuestionsQueue());
    }

    @Test
    void testDiffQueueFillingAfterAddingVote() {
        Question question = createQuestion();
        Question question2 = createQuestion();

        indexingService.cleanQueue(QaEntityType.QUESTION);
        indexingService.markEntitiesForIndexing(entityTypeQuestionList);
        assertFalse(hasQuestionsInIndexQueue());

        voteService.createQuestionLike(question.getId(), UID);

        indexingService.markEntitiesForIndexing(entityTypeQuestionList);
        assertTrue(hasQuestionsInIndexQueue());

        assertArrayEquals(new long[]{question.getId()}, getQuestionsQueue());
    }

    @Test
    void testDiffQueueFillingAfterMultipleActions() {
        indexingService.cleanQueue(QaEntityType.QUESTION);

        Question question = createQuestion();
        Question question2 = createQuestion();

        answerService.createAnswer(Answer.buildBasicAnswer(
            UID,
            UUID.randomUUID().toString(),
            question.getId()
        ), null);

        indexingService.markEntitiesForIndexing(entityTypeQuestionList);
        assertTrue(hasQuestionsInIndexQueue());

        assertArrayEquals(new long[]{question.getId(), question2.getId()}, getQuestionsQueue());
    }

    @Test
    void checkBaseIndexation() {
        // create questions
        Question questionBase = createQuestion();

        Question questionRemoved = createQuestion();
        questionService.deleteQuestion(questionRemoved.getId(), UID);

        Question questionBanned = createQuestion();
        questionService.forceUpdateModState(questionBanned.getId(), ModState.TOLOKA_REJECTED);

        Question questionBase2 = createQuestion();

        // run indexation (items are not indexed)
        indexingService.buildSession(UploadMethod.LOGBROKER)
            .withMode(IndexingMode.DIFF)
            .forEntities(QaEntityType.QUESTION)
            .runIndexing(session -> {
                assertTrue(session.getMode() == IndexingMode.DIFF);
                return SaasIndexingResult.failed();
            });

        assertTrue(hasQuestionsInIndexQueue());

        // run indexation (items indexed ok)
        indexingService.buildSession(UploadMethod.LOGBROKER)
            .withMode(IndexingMode.DIFF)
            .forEntities(QaEntityType.QUESTION)
            .runIndexing(session -> {
                assertTrue(session.getMode() == IndexingMode.DIFF);
                return SaasIndexingResult.ok();
            });

        assertFalse(hasQuestionsInIndexQueue());
    }

    private boolean hasQuestionsInIndexQueue() {
        return indexingService.hasItemsToIndex(entityTypeQuestionList);
    }

    @Test
    void testIndexationTimeFill() {
        // create questions
        Question questionBase = createQuestion();

        Question questionRemoved = createQuestion();
        questionService.deleteQuestion(questionRemoved.getId(), UID);

        Question questionBanned = createQuestion();
        questionService.forceUpdateModState(questionBanned.getId(), ModState.TOLOKA_REJECTED);

        Question questionBase2 = createQuestion();

        // check no requires yet
        assertEquals(0, indexingService.getEntitiesWithoutIndexTime(QaEntityType.QUESTION, 100).size());

        // run indexation (items indexed ok)
        indexingService.buildSession(UploadMethod.LOGBROKER)
            .withMode(IndexingMode.DIFF)
            .forEntities(QaEntityType.QUESTION)
            .runIndexing(session -> {
                assertTrue(session.getMode() == IndexingMode.DIFF);
                return SaasIndexingResult.ok();
        });

        //check
        List<Long> idsNeedIndexTime = indexingService.getEntitiesWithoutIndexTime(QaEntityType.QUESTION, 100);
        assertEquals(2, idsNeedIndexTime.size());
        assertArrayEquals(
            new long[]{questionBase.getId(), questionBase2.getId()},
            idsNeedIndexTime.stream().sorted().mapToLong(x -> x).toArray()
        );

        // fill
        indexingService.updateIndexTime(QaEntityType.QUESTION, idsNeedIndexTime);

        // check no more requires
        assertEquals(0, indexingService.getEntitiesWithoutIndexTime(QaEntityType.QUESTION, 100).size());
    }

    private long[] getQuestionsQueue() {
        return jdbcTemplate.queryForList(
            "select entity_id from qa.v_saas_indexing_queue_ready where entity_type = ?",
            Long.class,
            QaEntityType.QUESTION.getValue()
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
}
