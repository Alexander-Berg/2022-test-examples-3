package ru.yandex.market.pers.qa.tms.saas;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
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
import ru.yandex.market.pers.qa.model.Photo;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.VoteValueType;
import ru.yandex.market.pers.qa.model.saas.IndexingMode;
import ru.yandex.market.pers.qa.model.saas.SaasIndexingState;
import ru.yandex.market.pers.qa.model.saas.UploadMethod;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.PhotoService;
import ru.yandex.market.pers.qa.service.QuestionProductService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.VoteService;
import ru.yandex.market.pers.qa.service.saas.SaasIndexingService;
import ru.yandex.market.pers.qa.service.saas.doc.SaasQuestionDocumentProvider;
import ru.yandex.market.pers.qa.utils.CommonUtils;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;
import ru.yandex.market.saas.indexer.ferryman.model.YtTableRef;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 24.12.2018
 */
public class SaasQuestionIndexingTest extends PersQaTmsTest {
    private static final long UID = 1348719867L;
    private static final int MODEL_ID = 1;
    private static final int HID = 2;
    public static final String TEST_CLIENT_URL = "localhost:123/push/market-pers-qa-test";

    private List<QaEntityType> entityTypeQuestionList = Collections.singletonList(QaEntityType.QUESTION);

    @Qualifier("pgJdbcTemplate")
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SaasIndexingService indexingService;

    @Autowired
    private SaasIndexingExecutor executor;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private QuestionProductService questionProductService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private VoteService voteService;

    @Autowired
    @Qualifier("saasHttpClient")
    protected HttpClient saasHttpClientMock;

    @Autowired
    @Qualifier("saasPushRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SaasQuestionDocumentProvider saasQuestionDocumentProvider;

    private String qModelDocTemplate;
    private String qModelDocTemplateWithPhoto;
    private String qModelDocTemplateWithProductIds;
    private String qCategoryDocTemplate;
    private String qDocTemplateRemoved;

    @BeforeEach
    void init() throws Exception {
        qModelDocTemplate = IOUtils.toString(
            getClass().getResourceAsStream("/data/saas_question_doc_template.json"),
            StandardCharsets.UTF_8
        );

        qModelDocTemplateWithPhoto = IOUtils.toString(
            getClass().getResourceAsStream("/data/saas_question_with_photo_doc_template.json"),
            StandardCharsets.UTF_8
        );

        qModelDocTemplateWithProductIds = IOUtils.toString(
                getClass().getResourceAsStream("/data/saas_question_with_product_ids_doc_template.json"),
                StandardCharsets.UTF_8
        );

        qCategoryDocTemplate = IOUtils.toString(
            getClass().getResourceAsStream("/data/saas_question_cat_doc_template.json"),
            StandardCharsets.UTF_8
        );

        qDocTemplateRemoved = IOUtils.toString(
            getClass().getResourceAsStream("/data/saas_question_doc_template_removed.json"),
            StandardCharsets.UTF_8
        );
    }

    @Test
    void testIndexingExportDiff() throws Exception {
        assertFalse(hasQuestionsInIndexQueue());

        // create questions
        Question questionBase = createQuestion();

        Question questionRemoved = createQuestion();
        questionService.deleteQuestion(questionRemoved.getId(), UID);

        Question questionBanned = createQuestion();
        questionService.forceUpdateModState(questionBanned.getId(), ModState.TOLOKA_REJECTED);

        Question questionBase2 = createQuestion();

        assertTrue(hasQuestionsInIndexQueue());

        // export diff
        executor.sendEntitiesToSaas();

        assertFalse(hasQuestionsInIndexQueue());

        // check exported to SaaS
        ArgumentCaptor<String> docCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(restTemplate, times(4))
            .postForEntity(endsWith(TEST_CLIENT_URL), docCaptor.capture(), eq(String.class));

        // check entities sent
        List<String> documents = docCaptor.getAllValues();

        assertEquals(4, documents.size());
        assertDoc(getExpectedDoc(questionBase), questionBase.getId(), documents);
        assertDoc(getExpectedDoc(questionBase2), questionBase2.getId(), documents);
        assertDoc(getRemovedDoc(questionRemoved), questionRemoved.getId(), documents);
        assertDoc(getRemovedDoc(questionBanned), questionBanned.getId(), documents);

        // check indexing history
        String generationId = getLastCompletedGenerationId(IndexingMode.DIFF);

        // check archived items
        assertEquals(4, getArchiveSize(generationId).longValue());
    }

    @Test
    void testIndexingExportRefresh() throws Exception {
        configurationService.mergeValue(saasQuestionDocumentProvider.getRefreshBatchSizeKey(), 2L);

        // create questions
        Question questionBase = createQuestion();

        Question questionRemoved = createQuestion();
        questionService.deleteQuestion(questionRemoved.getId(), UID);

        Question questionBanned = createQuestion();
        questionService.forceUpdateModState(questionBanned.getId(), ModState.TOLOKA_REJECTED);

        Question questionBase2 = createQuestion();

        // start test
        assertEquals(0, countQuestionsInRefreshQueue());

        executor.addQuestionsToRefreshQueue();
        assertEquals(4, countQuestionsInRefreshQueue());

        // export refresh
        executor.sendEntitiesToSaasRefresh();

        ArgumentCaptor<String> docCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(restTemplate, times(2))
            .postForEntity(endsWith(TEST_CLIENT_URL), docCaptor.capture(), eq(String.class));

        List<String> documents = new ArrayList<>(docCaptor.getAllValues());

        assertNull(getLastCompletedGenerationId(IndexingMode.REFRESH), "check refresh is not completed");
        String generationId = getLastGenerationId(IndexingMode.REFRESH, SaasIndexingState.INDEXING);
        assertNotNull(generationId);

        // refresh rest elements
        PersQaTmsMockFactory.resetMocks();

        assertEquals(2, countQuestionsInRefreshQueue());
        executor.sendEntitiesToSaasRefresh();
        assertEquals(0, countQuestionsInRefreshQueue());

        docCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(restTemplate, times(2))
            .postForEntity(endsWith(TEST_CLIENT_URL), docCaptor.capture(), eq(String.class));

        documents.addAll(docCaptor.getAllValues());

        // check documents
        assertEquals(4, documents.size());
        assertDoc(getExpectedDoc(questionBase), questionBase.getId(), documents);
        assertDoc(getExpectedDoc(questionBase2), questionBase2.getId(), documents);
        assertDoc(getRemovedDoc(questionRemoved), questionRemoved.getId(), documents);
        assertDoc(getRemovedDoc(questionBanned), questionBanned.getId(), documents);

        // check indexing history and generationId is same
        assertNotNull(getLastCompletedGenerationId(IndexingMode.REFRESH));
        assertEquals(generationId, getLastCompletedGenerationId(IndexingMode.REFRESH));
    }

    private boolean hasQuestionsInIndexQueue() {
        return indexingService.hasItemsToIndex(entityTypeQuestionList);
    }

    private int countQuestionsInRefreshQueue() {
        return jdbcTemplate.queryForObject(
            "SELECT count(entity_id) " +
                "FROM qa.saas_refresh_indexing_queue " +
                "WHERE entity_type = " + QaEntityType.QUESTION.getValue(),
            Integer.class);
    }

    @Test
    void testIndexingExportDiffWithCategory() throws Exception {
        // publish snapshot, prepare to create diff
        executor.sendEntitiesToSaas();
        PersQaTmsMockFactory.resetMocks();

        assertFalse(hasQuestionsInIndexQueue());

        // create questions
        Question modelQuestion = createQuestion();
        Question categoryQuestion = createCategoryQuestion();

        assertTrue(hasQuestionsInIndexQueue());

        // export diff
        executor.sendEntitiesToSaas();

        assertFalse(hasQuestionsInIndexQueue());

        // check exported to SaaS
        ArgumentCaptor<String> docCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(restTemplate, times(2))
            .postForEntity(endsWith(TEST_CLIENT_URL), docCaptor.capture(), eq(String.class));

        // check entities sent
        List<String> documents = docCaptor.getAllValues();

        assertEquals(2, documents.size());
        assertDoc(getExpectedDoc(modelQuestion), modelQuestion.getId(), documents);
        assertDoc(getExpectedCategoryDoc(categoryQuestion), categoryQuestion.getId(), documents);

        // check indexing history
        String generationId = getLastCompletedGenerationId(IndexingMode.DIFF);

        // check archived items
        assertEquals(2, getArchiveSize(generationId).longValue());
    }

    @Test
    void testIndexingWithAnswersAndComplexData() throws Exception {
        // create questions
        Question questionBase = createQuestion();

        Answer answer = answerService.createAnswer(Answer.buildBasicAnswer(
            UID,
            UUID.randomUUID().toString(),
            questionBase.getId()
        ), null);

        Answer answer2 = answerService.createAnswer(Answer.buildBasicAnswer(
            UID,
            UUID.randomUUID().toString(),
            questionBase.getId()
        ), null);

        voteService.createQuestionLike(questionBase.getId(), UID);
        voteService.createQuestionLike(questionBase.getId(), UID + 1);
        voteService.createQuestionLike(questionBase.getId(), UID + 2);

        voteService.createAnswerVote(answer2.getId(), UID, VoteValueType.LIKE);

        assertTrue(hasQuestionsInIndexQueue());

        // export
        executor.sendEntitiesToSaas();

        assertFalse(hasQuestionsInIndexQueue());

        // check exported to SaaS
        ArgumentCaptor<String> docCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(restTemplate, times(3))
            .postForEntity(endsWith(TEST_CLIENT_URL), docCaptor.capture(), eq(String.class));

        // check entities sent
        List<String> documents = docCaptor.getAllValues();

        assertEquals(3, documents.size());
        assertDoc(
            getExpectedDocWithAnswers(questionBase, 2, 3),
            questionBase.getId(),
            documents);
    }

    @Test
    void testFailedIndexingDaemon() {
        // fail table creation
        doThrow(new RuntimeException()).when(restTemplate)
            .postForEntity(endsWith(TEST_CLIENT_URL), any(), eq(String.class));

        // create questions
        Question questionBase = createQuestion();

        // export
        executor.sendEntitiesToSaas();

        // check history is written
        Long historyItems = getHistoryFailedItems();

        assertEquals(1, historyItems.longValue());

        // check question is still in queue
        assertTrue(hasQuestionsInIndexQueue());
    }

    @Test
    void testFillingIndexingTime() {
        // create questions
        Question questionBase = createQuestion();

        Question questionRemoved = createQuestion();
        questionService.deleteQuestion(questionRemoved.getId(), UID);

        Question questionBanned = createQuestion();
        questionService.forceUpdateModState(questionBanned.getId(), ModState.TOLOKA_REJECTED);

        Question questionBase2 = createQuestion();

        // export snapshot
        executor.sendEntitiesToSaas();

        // check they are not in saas yet
        assertArrayEquals(
            new long[]{questionBase.getId(), questionBase2.getId()},
            indexingService.getEntitiesWithoutIndexTime(QaEntityType.QUESTION, 100).stream()
                .sorted()
                .mapToLong(x -> x).toArray()
        );

        HttpClientMockUtils.mockResponseWithFile(saasHttpClientMock, "/data/saas_response_questions_by_url_empty.json");
        executor.checkEntities();

        assertEquals(2, getIndexedWithoutTime().longValue());
        assertEquals(0, getIndexedWithTimeSize().longValue());

        // check they are now in saas
        assertArrayEquals(
            new long[]{questionBase.getId(), questionBase2.getId()},
            indexingService.getEntitiesWithoutIndexTime(QaEntityType.QUESTION, 100).stream()
                .sorted()
                .mapToLong(x -> x).toArray()
        );

        HttpClientMockUtils.mockResponse(
            saasHttpClientMock,
            req -> getStreamWithFixes(
                "/data/saas_response_questions_by_url_for_index_time.json",
                source -> source.replace("Q_1_ID", questionBase.getId().toString())
            ));
        executor.checkEntities();

        assertEquals(1, getIndexedWithoutTime().longValue());
        assertEquals(1, getIndexedWithTimeSize().longValue());
    }


    @Test
    public void testQuestionWithProductIds() throws Exception {
        Question questionBase = createQuestion();
        List<Long> expectedProductIds = Arrays.asList(432L, 546L, 23L, 43L, 8L);
        questionProductService.saveProductsForQuestion(questionBase.getId(), expectedProductIds);

        assertTrue(hasQuestionsInIndexQueue());
        executor.sendEntitiesToSaas();
        assertFalse(hasQuestionsInIndexQueue());

        executor.sendEntitiesToSaas();

        ArgumentCaptor<String> docCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(restTemplate, times(1))
                .postForEntity(endsWith(TEST_CLIENT_URL), docCaptor.capture(), eq(String.class));

        // check entities sent
        List<String> documents = docCaptor.getAllValues();

        assertEquals(1, documents.size());
        assertDoc(getExpectedDocWithProductIds(questionBase, expectedProductIds), questionBase.getId(), documents);

        // check indexing history
        String generationId = getLastCompletedGenerationId(IndexingMode.DIFF);

        // check archived items
        assertEquals(1, getArchiveSize(generationId).longValue());
    }

    @Test
    public void testQuestionWithPhoto() throws Exception {
        Question questionBase = createQuestion();

        Photo photo1 = new Photo(QaEntityType.QUESTION, String.valueOf(questionBase.getId()), "ns", "group", "name0", 0);
        Photo photo2 = new Photo(QaEntityType.QUESTION, String.valueOf(questionBase.getId()), "ns", "group", "name1", 1);
        Photo id1 = photoService.getPhotoById(photoService.createPhoto(photo1));
        Photo id2 = photoService.getPhotoById(photoService.createPhoto(photo2));
        photoService.updatePhotosModState(ModState.AUTO_FILTER_PASSED, List.of(id1, id2));
        questionService.forceUpdateModState(questionBase.getId(), ModState.NEW);


        assertTrue(hasQuestionsInIndexQueue());
        executor.sendEntitiesToSaas();
        assertFalse(hasQuestionsInIndexQueue());


        executor.sendEntitiesToSaas();

        ArgumentCaptor<String> docCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(restTemplate, times(1))
            .postForEntity(endsWith(TEST_CLIENT_URL), docCaptor.capture(), eq(String.class));

        // check entities sent
        List<String> documents = docCaptor.getAllValues();

        assertEquals(1, documents.size());
        assertDoc(getExpectedDocWithPhotos(questionBase, List.of(photo1, photo2)), questionBase.getId(), documents);
        assertDoc(getExpectedDocWithPhotos(questionBase, List.of(photo2, photo1)), questionBase.getId(), documents);

        // check indexing history
        String generationId = getLastCompletedGenerationId(IndexingMode.DIFF);

        // check archived items
        assertEquals(1, getArchiveSize(generationId).longValue());
    }

    @Test
    public void testQuestionWithPhotoUnmoderated() throws Exception {
        Question question = createQuestion();

        Photo photo3 = new Photo(QaEntityType.QUESTION, String.valueOf(question.getId()), "ns", "group", "name3", 2);
        Photo id3 = photoService.getPhotoById(photoService.createPhoto(photo3));

        questionService.deleteQuestion(question.getId(), question.getUser().getIdAsLong());
        Question questionBase = questionService.getQuestionByIdInternal(question.getId());

        assertTrue(hasQuestionsInIndexQueue());
        executor.sendEntitiesToSaas();
        assertFalse(hasQuestionsInIndexQueue());

        ArgumentCaptor<String> docCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(restTemplate, times(1))
            .postForEntity(endsWith(TEST_CLIENT_URL), docCaptor.capture(), eq(String.class));

        // check entities sent
        List<String> documents = docCaptor.getAllValues();

        assertEquals(1, documents.size());
        assertDoc(getRemovedDoc(questionBase), questionBase.getId(), documents);

        // check indexing history
        String generationId = getLastCompletedGenerationId(IndexingMode.DIFF);

        // check archived items
        assertEquals(1, getArchiveSize(generationId).longValue());
    }

    private Long getHistoryFailedItems() {
        return jdbcTemplate.queryForObject(
            "select count(*) " +
                "from qa.saas_indexing_history " +
                "where state = ?",
            Long.class,
            SaasIndexingState.FAILED.value()
        );
    }

    private String getLastCompletedGenerationId(IndexingMode indexingMode) {
        return getLastGenerationId(indexingMode, SaasIndexingState.COMPLETED);
    }

    private String getLastGenerationId(IndexingMode indexingMode, SaasIndexingState indexingState) {
        AtomicReference<String> generationId = new AtomicReference<>();
        jdbcTemplate.query(
            "select generation_id, external_id " +
                "from qa.saas_indexing_history " +
                "where state = ? and diff_fl = ?",
            (rs) -> {
                generationId.set(rs.getString("generation_id"));
            },
            indexingState.value(),
            indexingMode.getValue()
        );
        return generationId.get();
    }

    @NotNull
    private String getGenerationIdWithTimestamp(ArgumentCaptor<YtTableRef> ferrymanTableRef, IndexingMode mode) {
        String timestampPart = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date(
            TimeUnit.MICROSECONDS.toMillis(ferrymanTableRef.getValue().getTimestamp())
        ));
        return String.format("%s-%d-%d", timestampPart, mode.getValue(), UploadMethod.FERRYMAN.getValue());
    }

    private Question createQuestion() {
        Question question = Question.buildModelQuestion(UID, UUID.randomUUID().toString(), MODEL_ID);
        return questionService.createQuestion(question, null);
    }

    private Question createCategoryQuestion() {
        Question question = Question.buildCategoryQuestion(UID, UUID.randomUUID().toString(), HID);
        return questionService.createQuestion(question, null);
    }

    private String getExpectedDoc(Question question) {
        return getExpectedDocWithAnswers(question, 0, 0);
    }

    private String getExpectedDocWithPhotos(Question question, List<Photo> photos) {
        String PHOTO_JSON_TEMPLATE =
            "{\"value\":\"%s:%s:%s:%d\",\"type\":\"#p\"}";

        String photosString = photos.stream()
                .map(photo -> String.format(PHOTO_JSON_TEMPLATE, photo.getNamespace(), photo.getGroupId(),
                    photo.getImageName(), photo.getOrderNumber()))
                .collect(Collectors.joining(","));
        return getExpectedDocBase(qModelDocTemplateWithPhoto.replace("QUESTION_PHOTOS", photosString), question, 0, 0);
    }

    private String getExpectedDocWithProductIds(Question question, List<Long> productIds) {
        String PRODUCT_IDS_JSON_TEMPLATE = "{\"value\":\"%s:%d\",\"type\":\"#p\"}";

        String productIdsString = IntStream.range(0, productIds.size()).boxed()
                .map(i -> String.format(PRODUCT_IDS_JSON_TEMPLATE, productIds.get(i), i))
                .collect(Collectors.joining(","));

        return getExpectedDocBase(qModelDocTemplateWithProductIds
                .replace("QUESTION_PRODUCT_IDS", productIdsString), question, 0, 0);
    }

    private String getExpectedCategoryDoc(Question question) {
        return getExpectedCategoryDocWithAnswers(question, 0, 0);
    }

    private String getRemovedDoc(Question question) {
        return qDocTemplateRemoved.replace("QUESTION_ID", question.getId().toString());
    }

    private String getExpectedDocWithAnswers(Question question, long answerCount, long voteCount) {
        return getExpectedDocBase(qModelDocTemplate, question, answerCount, voteCount);
    }

    private String getExpectedCategoryDocWithAnswers(Question question, long answerCount, long voteCount) {
        return getExpectedDocBase(qCategoryDocTemplate, question, answerCount, voteCount);
    }

    private String getExpectedDocBase(String template, Question question, long answerCount, long voteCount) {
        long timestamp = question.getTimestamp().toEpochMilli();
        return template.replace("QUESTION_ID", question.getId().toString())
            .replace("AUTHOR_ID", question.getUserId())
            .replace("CREATE_DT", String.valueOf(timestamp))
            .replace("CREATE_TIME_RATED", String.valueOf(CommonUtils.getRatedTimestamp(timestamp)))
            .replace("QUESTION_TEXT", question.getText())
            .replace("QUESTION_ANSW", String.valueOf(answerCount))
            .replace("QUESTION_ANS_HAS", String.valueOf(answerCount > 0 ? 1 : 0))
            .replace("QUESTION_VOTE", String.valueOf(voteCount));
    }

    private InputStream getStreamWithFixes(String file, Function<String, String> function) throws Exception {
        InputStream stream = getClass().getResourceAsStream(file);
        String data = IOUtils.toString(stream, StandardCharsets.UTF_8);
        String result = function.apply(data);
        return new BufferedInputStream(new ByteArrayInputStream(result.getBytes()));
    }

    private void assertDoc(String expectedDoc, long questionId, List<String> docs) throws Exception {
        List<String> foundDocs = docs.stream()
            .filter(x -> x.contains(String.format("\"url\":\"question-%s\"", questionId)))
            .collect(Collectors.toList());

        assertEquals(1, foundDocs.size());
        JSONAssert.assertEquals(expectedDoc, foundDocs.get(0), false);
    }

    private Long getArchiveSize(String generationId) {
        return jdbcTemplate.queryForObject(
            "select count(*) " +
                "from qa.saas_indexing_queue_archive " +
                "where entity_type = ? and generation_id = ?",
            Long.class,
            QaEntityType.QUESTION.getValue(),
            generationId
        );
    }

    private Long getIndexedWithTimeSize() {
        return jdbcTemplate.queryForObject(
            "select count(*) " +
                "from qa.saas_indexing_time " +
                "where entity_type = ? and index_time is not null ",
            Long.class,
            QaEntityType.QUESTION.getValue()
        );
    }

    private Long getIndexedWithoutTime() {
        return jdbcTemplate.queryForObject(
            "select count(*) " +
                "from qa.saas_indexing_time " +
                "where entity_type = ? and index_time is null",
            Long.class,
            QaEntityType.QUESTION.getValue()
        );
    }

}
