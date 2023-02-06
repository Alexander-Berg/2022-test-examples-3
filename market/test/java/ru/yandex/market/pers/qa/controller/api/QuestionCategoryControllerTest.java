package ru.yandex.market.pers.qa.controller.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.qa.PersQaServiceMockFactory;
import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.controller.dto.CategoryIdDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.utils.CommonUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.mock.SaasMocks.getCategoryEntityFilter;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 21.02.2019
 */
public class QuestionCategoryControllerTest extends AbstractQuestionControllerTest {

    @Override
    public void checkEntityId(QuestionDto question, long entityId) {
        assertEquals(QuestionDto.QUESTION, question.getEntity());
        assertNull(question.getProductIdDto());
        assertNull(question.getInterestIdDto());
        assertNotNull(question.getCategoryIdDto());
        assertEquals(entityId, question.getCategoryIdDto().getId());
        assertEquals(CategoryIdDto.CATEGORY, question.getCategoryIdDto().getEntity());
    }

    @Test
    public void testGetPublicQuestionsUid() throws Exception {
        List<Long> pubQuestions = createCategoryQuestionsReturnPublic(UID);
        Map<Long, Integer> questionsWithAnswersCount = new HashMap<>();
        for (Long questionId : pubQuestions) {
            List<Long> answers = createAnswersAndReturnPublic(questionId);
            questionsWithAnswersCount.put(questionId, answers.size());
        }

        saasMocks.mockCategoryCallLite(CATEGORY_HID, DEF_PAGE_SIZE, pubQuestions);
        QAPager<QuestionDto> qaPager = getCategoryQuestionsUidLite(CATEGORY_HID);
        checkQuestions(pubQuestions, questionsWithAnswersCount, qaPager);
    }

    @Test
    public void testGetQuestionsSimple() throws Exception {
        long uid = UID;
        long otherUid = uid + 1;

        List<Long> allQuestions = Arrays.asList(
            createCategoryQuestion(CATEGORY_HID, uid),
            createCategoryQuestion(CATEGORY_HID, uid),
            createCategoryQuestion(CATEGORY_HID, otherUid),
            createCategoryQuestion(CATEGORY_HID, otherUid),
            createCategoryQuestion(CATEGORY_HID, uid),
            createCategoryQuestion(CATEGORY_HID, otherUid),
            createCategoryQuestion(CATEGORY_HID, uid)
        );

        List<Long> saasQuestions = Arrays.asList(
            allQuestions.get(1),
            allQuestions.get(5),
            allQuestions.get(3),
            allQuestions.get(2),
            allQuestions.get(4)
        );

        saasMocks.mockCategoryCallLite(CATEGORY_HID, DEF_PAGE_SIZE, saasQuestions);
        QAPager<QuestionDto> qaPager = getCategoryQuestionsUidLite(CATEGORY_HID);
        assertQuestions(saasQuestions, qaPager);
        assertEquals(saasQuestions.size(), getCategoryQuestionsCountUidLite(CATEGORY_HID, UID, false));

        qaPager = getCategoryQuestionsYandexUidLite(CATEGORY_HID, YANDEXUID);
        assertQuestions(saasQuestions, qaPager);
        assertEquals(saasQuestions.size(), getCategoryQuestionsCountYandexUidLite(CATEGORY_HID, YANDEXUID));

        PersQaServiceMockFactory.resetMocks();
        saasMocks.mockCategoryCallLite(CATEGORY_HID, DEF_PAGE_SIZE, saasQuestions);
        qaPager = getCategoryQuestionsYandexUid(CATEGORY_HID, YANDEXUID);
        assertQuestions(saasQuestions, qaPager);
        assertEquals(saasQuestions.size(), getCategoryQuestionsCountYandexUid(CATEGORY_HID, YANDEXUID));
    }

    @Test
    public void testGetQuestionsUidComplex() throws Exception {
        long uid = UID;
        long otherUid = uid + 1;

        List<Long> allQuestions = Arrays.asList(
            createCategoryQuestion(CATEGORY_HID, uid),
            createCategoryQuestion(CATEGORY_HID, uid),
            createCategoryQuestion(CATEGORY_HID, otherUid),
            createCategoryQuestion(CATEGORY_HID, otherUid),
            createCategoryQuestion(CATEGORY_HID, uid),
            createCategoryQuestion(CATEGORY_HID, otherUid),
            createCategoryQuestion(CATEGORY_HID, uid)
        );

        List<Long> saasQuestions = Arrays.asList(
            allQuestions.get(5),
            allQuestions.get(3),
            allQuestions.get(2)
        );

        List<Long> ownQuestions = Arrays.asList(
            allQuestions.get(6),
            allQuestions.get(4),
            allQuestions.get(1),
            allQuestions.get(0),
            allQuestions.get(5),
            allQuestions.get(3),
            allQuestions.get(2)
        );

        // with models call to saas
        saasMocks.mockCategoryCallWithModel(CATEGORY_HID, DEF_PAGE_SIZE, saasQuestions);
        QAPager<QuestionDto> qaPager = getCategoryQuestionsUid(CATEGORY_HID, true);
        assertQuestions(ownQuestions, qaPager);
        assertEquals(ownQuestions.size(), getCategoryQuestionsCountUid(CATEGORY_HID, uid, true));
        assertEquals(saasQuestions.size(), getCategoryQuestionsCountUidLite(CATEGORY_HID, uid, true));

        // lite call to saas
        PersQaServiceMockFactory.resetMocks();
        saasMocks.mockCategoryCallLite(CATEGORY_HID, DEF_PAGE_SIZE, saasQuestions);

        qaPager = getCategoryQuestionsUid(CATEGORY_HID, false);
        assertQuestions(ownQuestions, qaPager);
        assertEquals(ownQuestions.size(), getCategoryQuestionsCountUid(CATEGORY_HID, uid, false));

        qaPager = getCategoryQuestionsUidLite(CATEGORY_HID);
        assertQuestions(saasQuestions, qaPager);
        assertEquals(saasQuestions.size(), getCategoryQuestionsCountUidLite(CATEGORY_HID, uid, false));
    }

    private void assertQuestions(List<Long> expected, QAPager<QuestionDto> actual) {
        assertArrayEquals(
            expected.toArray(),
            actual.getData().stream().map(QuestionDto::getId).toArray()
        );
    }

    @Test
    public void testGetPublicQuestionsYandexUid() throws Exception {
        List<Long> pubQuestions = createCategoryQuestionsReturnPublic(UID);
        Map<Long, Integer> questionsWithAnswersCount = new HashMap<>();
        for (Long questionId : pubQuestions) {
            List<Long> answers = createAnswersAndReturnPublic(questionId);
            questionsWithAnswersCount.put(questionId, answers.size());
        }

        saasMocks.mockCategoryCallLite(CATEGORY_HID, DEF_PAGE_SIZE, pubQuestions);
        QAPager<QuestionDto> qaPager = getCategoryQuestionsYandexUid(CATEGORY_HID, YANDEXUID);
        checkQuestions(pubQuestions, questionsWithAnswersCount, qaPager);
    }

    @Test
    public void testCountCorrectAfterCreateRemove() throws Exception {
        long hid = 31415926;

        PersQaServiceMockFactory.resetMocks();
        saasMocks.mockCategoryCallWithModel(hid, DEF_PAGE_SIZE, Collections.emptyList());
        final long count = getCategoryQuestionsCountUid(hid, UID, true);

        final long questionId = createCategoryQuestion(hid, UID);

        assertEquals(count + 1, getCategoryQuestionsCountUid(hid, UID, true));

        deleteQuestion(questionId);
        assertEquals(count, getCategoryQuestionsCountUid(hid, UID, true));
    }

    @Test
    public void testCountCorrectAfterCreateRemoveYandexUid() throws Exception {
        long hid = 31415926;

        PersQaServiceMockFactory.resetMocks();
        saasMocks.mockCategoryCallLite(hid, DEF_PAGE_SIZE, Collections.emptyList());
        final long count = getCategoryQuestionsCountYandexUid(hid, YANDEXUID);

        final long questionId = createCategoryQuestion(hid, UID);

        PersQaServiceMockFactory.resetMocks();
        saasMocks.mockCategoryCallLite(hid, DEF_PAGE_SIZE, Collections.singletonList(questionId));
        assertEquals(count + 1, getCategoryQuestionsCountYandexUid(hid, YANDEXUID));

        deleteQuestion(questionId);
        assertEquals(count, getModelQuestionsCount(hid));
    }

    @Test
    public void testQuestionLock() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long hid = 3451;

        String expectedLock = String.format("0_%s_1_%s_%s", UID, hid, text.hashCode());
        assertFalse(hasLock(expectedLock));

        createCategoryQuestion(hid, UID, text);
        assertTrue(hasLock(expectedLock));
    }

    @Test
    void testSimilar() throws Exception {
        long hid = 934012;
        long pageSize = 10;
        long firstPage = 1;

        // create model question with modelID = hid to check that nothing is messed up inside of service
        createQuestion(hid, UID, "question 1 for model");

        long baseQuestion = createCategoryQuestion(hid, UID, "question 0");

        long[] questions = {
            createCategoryQuestion(hid, UID, "question 1"),
            createCategoryQuestion(hid, UID, "question 2"),
            createCategoryQuestion(hid, UID + 1, "other question"),
        };

        saasMocks.mockCategoryCallLite(hid, pageSize, CommonUtils.list(questions));

        // case: UID
        DtoList<QuestionDto> similarQuestions = getSimilarQuestionsUid(baseQuestion, UID, firstPage, pageSize);
        List<QuestionDto> data = similarQuestions.getData();
        assertEquals(3, data.size());
        checkQuestion(data.get(0), questions[0], hid, UID, "question 1");
        checkQuestion(data.get(1), questions[1], hid, UID, "question 2");
        checkQuestion(data.get(2), questions[2], hid, UID + 1, "other question");

        // case: yandexuid
        similarQuestions = getSimilarQuestionsYandexUid(baseQuestion, YANDEXUID, firstPage, pageSize);
        data = similarQuestions.getData();
        assertEquals(3, data.size());
        checkQuestion(data.get(0), questions[0], hid, UID, "question 1");
        checkQuestion(data.get(1), questions[1], hid, UID, "question 2");
        checkQuestion(data.get(2), questions[2], hid, UID + 1, "other question");
    }

    @Test
    void testQuestionsLoadedFields() throws Exception {
        long hid = 934012;
        String text = "Some text";

        long questionId = createCategoryQuestion(hid, UID, text);

        saasMocks.mockCategoryCallWithModel(hid, DEF_PAGE_SIZE, Collections.emptyList());

        // from SAAS
        saasMocks.mockCategoryCallLite(hid, DEF_PAGE_SIZE, Collections.singletonList(questionId));
        QAPager<QuestionDto> liteQuestions = getCategoryQuestionsUidLite(hid);
        assertEquals(1, liteQuestions.getData().size());
        checkQuestion(liteQuestions.getData().get(0), questionId, hid, UID, text);

        // from DB (through push own to top) - mix models
        PersQaServiceMockFactory.resetMocks();
        saasMocks.mockCategoryCallWithModel(hid, DEF_PAGE_SIZE, Collections.emptyList());
        QAPager<QuestionDto> questions = getCategoryQuestionsUid(hid, true);
        assertEquals(1, questions.getData().size());
        checkQuestion(questions.getData().get(0), questionId, hid, UID, text);

        // from DB (through push own to top) - do not mix models
        PersQaServiceMockFactory.resetMocks();
        saasMocks.mockCategoryCallLite(hid, DEF_PAGE_SIZE, Collections.emptyList());
        questions = getCategoryQuestionsUid(hid, false);
        assertEquals(1, questions.getData().size());
        checkQuestion(questions.getData().get(0), questionId, hid, UID, text);
    }

    @Test
    public void testGetQuestionsOverloaded() throws Exception {
        // imitate saas timed out
        saasMocks.mockCategoryPageCall(1, DEF_PAGE_SIZE,
            Collections.emptyList(),
            HttpStatus.SC_SERVICE_UNAVAILABLE,
            getCategoryEntityFilter(CATEGORY_HID, true));

        String response = invokeAndRetrieveResponse(
            get("/question/category/" + CATEGORY_HID + "/UID/" + UID + "/lite"),
            status().is5xxServerError()
        );

        assertTrue(response.contains("SaaS failed to handle request within proper timeout"));
        assertTrue(response.contains("\"http_status\":" + HttpStatus.SC_SERVICE_UNAVAILABLE));
    }

    private void checkQuestions(List<Long> questions,
                                Map<Long, Integer> answersCount,
                                QAPager<QuestionDto> result) {
        List<QuestionDto> ids = result.getData();
        assertEquals(questions.size(), ids.size());
        ids.forEach(q -> {
            assertTrue(answersCount.containsKey(q.getId()));
            final int count = answersCount.get(q.getId());
            assertEquals(q.getAnswersCount(), count);
        });
    }


}
