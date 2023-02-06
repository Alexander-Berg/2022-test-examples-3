package ru.yandex.market.pers.qa.tms.saas;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.mock.PersQaTmsMockFactory;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.saas.IndexingMode;
import ru.yandex.market.pers.qa.model.saas.SaasIndexingState;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.saas.SaasIndexingService;
import ru.yandex.market.pers.qa.service.saas.doc.SaasAnswerDocumentProvider;
import ru.yandex.market.pers.qa.utils.CommonUtils;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

/**
 * @author vvolokh
 * 26.06.2019
 */
public class SaasAnswerIndexingTest extends PersQaTmsTest {
    private static final long UID = 1348719867L;
    private static final long ANSWER_UID = UID + 1;
    private static final int MODEL_ID = 1;
    public static final String TEST_CLIENT_URL = "localhost:123/push/market-pers-qa-test";
    private List<QaEntityType> entityTypeAnswerList = Collections.singletonList(QaEntityType.ANSWER);

    @Autowired
    private SaasIndexingService indexingService;

    @Autowired
    @Qualifier("saasPushRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private SaasIndexingExecutor executor;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SaasAnswerDocumentProvider saasAnswerDocumentProvider;

    private String answerDocTemplate;

    @BeforeEach
    void init() throws Exception {
        answerDocTemplate = IOUtils.toString(
            getClass().getResourceAsStream("/data/saas_answer_doc_template.json"),
            StandardCharsets.UTF_8
        );
    }

    @Test
    void testIndexingPublicQuestionWithAnswerExportDiff() throws Exception {
        assertFalse(hasQuestionsInIndexQueue());
        assertFalse(hasAnswersInIndexQueue());

        Question question = createQuestion();
        Answer answer = createAnswer(question.getId());

        assertTrue(hasQuestionsInIndexQueue());
        assertTrue(hasAnswersInIndexQueue());

        // export diff
        executor.sendEntitiesToSaas();

        assertFalse(hasQuestionsInIndexQueue());
        assertFalse(hasAnswersInIndexQueue());

        // ban question
        questionService.forceUpdateModState(question.getId(), ModState.TOLOKA_REJECTED);

        assertTrue(hasQuestionsInIndexQueue());
        assertFalse(hasAnswersInIndexQueue());

        // refresh rest elements
        PersQaTmsMockFactory.resetMocks();

        // export diff
        executor.sendEntitiesToSaas();

        assertFalse(hasQuestionsInIndexQueue());
        assertFalse(hasAnswersInIndexQueue());

        // check exported to SaaS
        ArgumentCaptor<String> docCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(restTemplate, times(2))
                .postForEntity(endsWith(TEST_CLIENT_URL), docCaptor.capture(), eq(String.class));

        // check entities sent
        List<String> documents = docCaptor.getAllValues();

        assertEquals(2, documents.size());
        assertEntityDeleteDoc(QaEntityType.QUESTION.getSimpleName(), question.getId(), "delete", documents);
        assertEntityDeleteDoc(QaEntityType.ANSWER.getSimpleName(), answer.getId(), "delete", documents);
    }

    @Test
    void testIndexingBannedPublicQuestionExportDiff() throws Exception {
        assertFalse(hasQuestionsInIndexQueue());
        assertFalse(hasAnswersInIndexQueue());

        Question question = createQuestion();
        Answer answer = createAnswer(question.getId());

        // export diff
        executor.sendEntitiesToSaas();
        // ban question
        questionService.forceUpdateModState(question.getId(), ModState.TOLOKA_REJECTED);

        assertTrue(hasQuestionsInIndexQueue());
        assertFalse(hasAnswersInIndexQueue());

        // export diff
        executor.sendEntitiesToSaas();

        assertFalse(hasQuestionsInIndexQueue());
        assertFalse(hasAnswersInIndexQueue());

        // unban question
        questionService.forceUpdateModState(question.getId(), ModState.CONFIRMED);

        // refresh rest elements
        PersQaTmsMockFactory.resetMocks();

        executor.sendEntitiesToSaas();

        // check exported to SaaS
        ArgumentCaptor<String> docCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(restTemplate, times(2))
                .postForEntity(endsWith(TEST_CLIENT_URL), docCaptor.capture(), eq(String.class));

        // check entities sent
        List<String> documents = docCaptor.getAllValues();

        assertEquals(2, documents.size());
        assertEntityDeleteDoc(QaEntityType.QUESTION.getSimpleName(), question.getId(), "modify", documents);
        assertEntityDeleteDoc(QaEntityType.ANSWER.getSimpleName(), answer.getId(), "modify", documents);
    }

    private void assertEntityDeleteDoc(String expectedEntityType,
                                       long entityId,
                                       String expectedAction,
                                       List<String> docs) throws Exception {
        long count = docs.stream()
                .filter(x -> x.contains(String.format("\"url\":\"%s-%s\"", expectedEntityType, entityId)))
                .filter(x -> x.contains(String.format("\"action\":\"%s\"", expectedAction)))
                .count();

        assertEquals(1, count);
    }

    private boolean hasQuestionsInIndexQueue() {
        return indexingService.hasItemsToIndex(Collections.singletonList(QaEntityType.QUESTION));
    }

    @Test
    public void testDiff() throws Exception {
        assertFalse(hasAnswersInIndexQueue());

        // check item added after creation
        Question question = createQuestion();
        Answer answer = createAnswer(question.getId());

        // ensure only answers are enqueued
        indexingService.cleanQueue(QaEntityType.QUESTION);
        assertTrue(hasAnswersInIndexQueue());

        assertIndexingHistoryEmpty();

        executor.sendEntitiesToSaas();

        assertFalse(hasAnswersInIndexQueue());
        ArgumentCaptor<String> doc = ArgumentCaptor.forClass(String.class);
        Mockito.verify(restTemplate).postForEntity(endsWith(TEST_CLIENT_URL), doc.capture(), eq(String.class));

        JSONAssert.assertEquals(getExpectedAnswerDoc(answerDocTemplate, answer, 0, 0, 0), doc.getValue(), false);

        // check indexing history
        String generationId = getLastGenerationId(IndexingMode.DIFF, SaasIndexingState.COMPLETED);

        // check archived items
        assertEquals(1, getArchiveSize(generationId).longValue());
    }

    @Test
    public void testRefreshPart() throws Exception {
        configurationService.mergeValue(saasAnswerDocumentProvider.getRefreshBatchSizeKey(), 1L);
        assertEquals(Integer.valueOf(0), countAnswersInRefreshQueue());

        // check item added after creation
        Question question = createQuestion();
        Answer answer = createAnswer(question.getId());
        Question question2 = createQuestion();
        Answer answer2 = createAnswer(question2.getId());

        assertEquals(Integer.valueOf(0), countAnswersInRefreshQueue());
        executor.addAnswersToRefreshQueue();
        assertEquals(Integer.valueOf(2), countAnswersInRefreshQueue());

        assertIndexingHistoryEmpty();

        List<String> sentDocs = new ArrayList<>();
        executor.sendEntitiesToSaasRefresh();

        assertEquals(Integer.valueOf(1), countAnswersInRefreshQueue());
        ArgumentCaptor<String> doc = ArgumentCaptor.forClass(String.class);
        Mockito.verify(restTemplate).postForEntity(endsWith(TEST_CLIENT_URL), doc.capture(), eq(String.class));
        Mockito.clearInvocations(restTemplate);
        sentDocs.add(doc.getValue());
        String generationId = getLastGenerationId(IndexingMode.REFRESH, SaasIndexingState.INDEXING);
        assertNotNull(generationId);

        executor.sendEntitiesToSaasRefresh();

        assertEquals(Integer.valueOf(0), countAnswersInRefreshQueue());
        Mockito.verify(restTemplate).postForEntity(endsWith(TEST_CLIENT_URL), doc.capture(), eq(String.class));

        sentDocs.add(doc.getValue());

        assertDoc(getExpectedAnswerDoc(answerDocTemplate, answer, 0, 0, 0), answer.getId(), sentDocs);
        assertDoc(getExpectedAnswerDoc(answerDocTemplate, answer2, 0, 0, 0), answer2.getId(), sentDocs);
        assertEquals(generationId, getLastGenerationId(IndexingMode.REFRESH, SaasIndexingState.COMPLETED));
    }

    @Test
    public void testNoRunOnEmptyQueue() throws Exception {
        assertEquals(Integer.valueOf(0), countAnswersInRefreshQueue());
        assertEquals(Integer.valueOf(0), getIndexingHistoryEntriesCount());

        executor.sendEntitiesToSaasRefresh();

        assertEquals(Integer.valueOf(0), getIndexingHistoryEntriesCount());

        Question question = createQuestion();
        Answer answer = createAnswer(question.getId());

        assertEquals(Integer.valueOf(0), countAnswersInRefreshQueue());
        executor.addAnswersToRefreshQueue();
        assertEquals(Integer.valueOf(1), countAnswersInRefreshQueue());

        executor.sendEntitiesToSaasRefresh();

        assertEquals(Integer.valueOf(0), countAnswersInRefreshQueue());
        assertEquals(Integer.valueOf(1), getIndexingHistoryEntriesCount());

        executor.sendEntitiesToSaasRefresh();

        assertEquals(Integer.valueOf(1), getIndexingHistoryEntriesCount());
    }

    private void assertIndexingHistoryEmpty() {
        assertEquals(Long.valueOf(0), jdbcTemplate.queryForObject("select count(generation_id) from qa.saas_indexing_history", Long.class));
    }

    private Question createQuestion() {
        Question question = Question.buildModelQuestion(UID, UUID.randomUUID().toString(), MODEL_ID);
        return questionService.createQuestion(question, null);
    }

    private Answer createAnswer(long questionId) {
        Answer answer = Answer.buildBasicAnswer(ANSWER_UID, UUID.randomUUID().toString(), questionId);
        return answerService.createAnswer(answer, null);
    }

    private boolean hasAnswersInIndexQueue() {
        return indexingService.hasItemsToIndex(entityTypeAnswerList);
    }

    private Integer countAnswersInRefreshQueue() {
        return jdbcTemplate.queryForObject("SELECT count(entity_id) FROM qa.saas_refresh_indexing_queue WHERE entity_type = " + QaEntityType.ANSWER.getValue(), Integer.class);
    }

    private String getExpectedAnswerDoc(String template,
                                        Answer answer,
                                        long likeCount,
                                        long dislikeCount,
                                        long commentCount) {
        long timestamp = answer.getTimestamp().toEpochMilli();
        return template.replace("ANSWER_ID", String.valueOf(answer.getId()))
            .replace("AUTHOR_ID", answer.getUserId())
            .replace("ANSWER_TEXT", answer.getText())
            .replace("CREATE_DT", String.valueOf(timestamp))
            .replace("CREATE_TIME_RATED", String.valueOf(CommonUtils.getRatedTimestamp(timestamp)))
            .replace("QUESTION_ID", String.valueOf(answer.getQuestionId()))
            .replace("ANSWER_VOTES", String.valueOf(likeCount + dislikeCount))
            .replace("ANSWER_LIKES", String.valueOf(likeCount))
            .replace("ANSWER_DISLIKES", String.valueOf(dislikeCount))
            .replace("ANSWER_RATED_LIKES", String.valueOf(CommonUtils.getRatedValue(likeCount, likeCount + dislikeCount)))
            .replace("ANSWER_RATED_DISLIKES", String.valueOf(CommonUtils.getRatedValue(dislikeCount, likeCount + dislikeCount)))
            .replace("ANSWER_COMMENTS", String.valueOf(commentCount));
    }

    @NotNull
    private String getLastGenerationId(IndexingMode mode, SaasIndexingState state) {
        AtomicReference<String> generationId = new AtomicReference<>();
        jdbcTemplate.query(
            "select generation_id \n" +
                "from qa.saas_indexing_history \n" +
                "where state = ? and diff_fl = ? \n" +
                "order by cr_time desc \n" +
                "limit 1",
            (rs) -> {
                generationId.set(rs.getString("generation_id"));
            },
            state.value(),
            mode.getValue()
        );
        return generationId.get();
    }

    private Integer getIndexingHistoryEntriesCount() {
        return jdbcTemplate.queryForObject("SELECT count(cr_time) FROM qa.saas_indexing_history", Integer.class);
    }

    private Long getArchiveSize(String generationId) {
        return jdbcTemplate.queryForObject(
            "select count(*) " +
                "from qa.saas_indexing_queue_archive " +
                "where entity_type = ? and generation_id = ?",
            Long.class,
            QaEntityType.ANSWER.getValue(),
            generationId
        );
    }

    private void assertDoc(String expectedDoc, long answerId, List<String> docs) throws Exception {
        List<String> foundDocs = docs.stream()
            .filter(x -> x.contains(String.format("\"url\":\"answer-%s\"", answerId)))
            .collect(Collectors.toList());

        assertEquals(1, foundDocs.size());
        JSONAssert.assertEquals(expectedDoc, foundDocs.get(0), false);
    }
}
