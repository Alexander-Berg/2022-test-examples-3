package ru.yandex.market.pers.qa.controller.api.question;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.api.AbstractQuestionControllerTest;
import ru.yandex.market.pers.qa.controller.dto.PhotoDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.Photo;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.State;
import ru.yandex.market.pers.qa.service.PhotoService;
import ru.yandex.market.pers.qa.service.QuestionProductService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.utils.Slugifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * General cases for questions and posts.
 *
 * @author Ilya Kislitsyn / ilyakis@ / 12.03.2020
 */
public abstract class AbstractQuestionCommonControllerTest extends AbstractQuestionControllerTest {

    @Autowired
    protected PhotoService photoService;

    @Autowired
    protected QuestionProductService questionProductService;

    @Test
    public void testCreateQuestionSlugLengthCheck() throws Exception {
        String text = String.join("", Collections.nCopies(Slugifier.DEFAULT_SLUG_SIZE + 1, "я"));
        long questionId = doCreateQuestion(MODEL_ID, UID, text);
        String slug = qaJdbcTemplate.queryForObject(
                "select human_url from qa.question where id = ?",
                String.class,
                questionId
        );
        Assertions.assertTrue(slug != null && slug.length() <= Slugifier.DEFAULT_SLUG_SIZE);
    }

    @Test
    public void testCreateDuplicateQuestion() throws Exception {
        final String text = "Величайший вопрос Жизни, Вселенной и Всего Такого";
        long questionId = doCreateQuestion(MODEL_ID, UID, text);

        final String response = doCreateQuestion(MODEL_ID, UID, text, status().is4xxClientError());
        assertTrue(response.contains("\"error\":\"Question is already exist\""));
    }

    @Test
    public final void testSingleQuestion() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long entityId = 3923370598L;
        final long questionId = doCreateQuestion(entityId, UID, text);

        // check something for uid
        QuestionDto question = doGetQuestion(questionId, UID);
        checkQuestion(question, questionId, entityId, text);

        // check something for yandexuid
        question = doGetQuestionYandexUid(questionId, YANDEXUID, status().is2xxSuccessful());
        checkQuestion(question, questionId, entityId, text);

        // remove, check that removed
        doDeleteQuestion(questionId, UID, status().is2xxSuccessful());

        doGetQuestion(questionId, UID, status().isNotFound());
        doGetQuestionYandexUid(questionId, YANDEXUID, status().isNotFound());
    }

    @Test
    public final void testDeleteQuestion() throws Exception {
        long questionId = doCreateQuestion();
        checkQuestionState(questionId, State.NEW);

        assertTrue(doGetQuestion(questionId, UID).isCanDelete());

        doDeleteQuestion(questionId, UID, status().is2xxSuccessful());
        checkQuestionState(questionId, State.DELETED);
    }

    @Test
    public void testCanDeleteSomeoneElseQuestion() throws Exception {
        long questionId = doCreateQuestion();

        assertFalse(doGetQuestion(questionId, UID + 1).isCanDelete());

        final String response = doDeleteQuestion(questionId, UID + 1, status().is4xxClientError());
        assertTrue(response.contains(getQuestionRightsNotEnough(UID + 1, questionId)));
    }

    @Test
    public void testCanDeleteFieldQuestionWithAnswers() throws Exception {
        long questionId = createCategoryQuestion(ModState.CONFIRMED, State.NEW);
        createAnswer(questionId, ModState.CONFIRMED, State.NEW);

        assertTrue(doGetQuestion(questionId, UID).isCanDelete());

        doDeleteQuestion(questionId, UID, status().is2xxSuccessful());
    }

    @Test
    public void testCanDeleteVeryOldQuestion() throws Exception {
        long questionId = doCreateQuestion();
        jdbcTemplate.update("UPDATE qa.question SET cr_time=now() - interval '2' DAY WHERE id=?", questionId);

        // for correct update cr_time
        invalidateCache();

        assertTrue(doGetQuestion(questionId, UID).isCanDelete());

        doDeleteQuestion(questionId, UID, status().is2xxSuccessful());
    }

    @Test
    public void testDeleteQuestionBanned() throws Exception {
        long questionId = doCreateQuestion();
        // should be available fine
        QuestionDto question = doGetQuestion(questionId, UID);
        assertTrue(question.isCanDelete());

        // still should be accessible with cache after ban
        questionService.forceUpdateModState(questionId, ModState.TOLOKA_REJECTED);
        question = doGetQuestion(questionId, UID);
        checkQuestionState(questionId, State.NEW);

        // should be deletable
        assertTrue(question.isCanDelete());
        doDeleteQuestion(questionId, UID, status().is2xxSuccessful());
        checkQuestionState(questionId, State.DELETED);

        // should fail in repeated delete
        doDeleteQuestion(questionId, UID, status().is4xxClientError());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    public void testBatchQuestion(int userTypeId) throws Exception {
        UserType userType = UserType.valueOf(userTypeId);

        final String text = UUID.randomUUID().toString();
        final long modelId = 3923370598L;

        long[] questionIds = {
                doCreateQuestion(modelId, UID, text),
                doCreateQuestion(modelId + 1, UID, text + 1)
        };
        List<Long> questionIdsList = Arrays.asList(questionIds[0], questionIds[1]);

        // create answers only when they are supported
        long[] answerIds = !supportsAnswers() ? new long[0] : new long[]{
                createAnswer(questionIds[1])
        };

        // check something for uid
        resetMocks();
        if (supportsAnswers()) {
            saasMocks.mockBestAnswersSaasCall(DEF_PAGE_SIZE, questionIdsList, 1);
        }
        saasMocks.mockQuestionBulkCall(questionIdsList);
        List<QuestionDto> questions = doGetQuestionsBulk(
                questionIdsList,
                userType,
                status().is2xxSuccessful());
        assertEquals(2, questions.size());
        questions.sort(Comparator.comparing(QuestionDto::getId));
        checkQuestion(questions.get(0), questionIds[0], modelId, text);
        checkQuestion(questions.get(1), questionIds[1], modelId + 1, text + 1);
        if (supportsAnswers()) {
            assertEquals(1, questions.get(1).getAnswers().size());
            assertEquals(answerIds[0], questions.get(1).getAnswers().get(0).getAnswerId());
        } else {
            assertEquals(0, questions.get(1).getAnswers().size());
        }

        // remove, check that removed
        doDeleteQuestion(questionIds[0], UID, status().is2xxSuccessful());

        resetMocks();
        if (supportsAnswers()) {
            saasMocks.mockBestAnswersSaasCall(DEF_PAGE_SIZE, Collections.singletonList(questionIds[1]), 1);
        }
        saasMocks.mockQuestionBulkCall(questionIdsList);
        questions = doGetQuestionsBulk(
                questionIdsList,
                userType,
                status().is2xxSuccessful());
        assertEquals(1, questions.size());
        checkQuestion(questions.get(0), questionIds[1], modelId + 1, text + 1);

        // remove second, check that removed
        if (supportsAnswers()) {
            deleteAnswer(answerIds[0]);
        }
        doDeleteQuestion(questionIds[1], UID, status().is2xxSuccessful());

        resetMocks();
        if (supportsAnswers()) {
            saasMocks.mockBestAnswersSaasCall(DEF_PAGE_SIZE, Collections.emptyList(), 1);
        }
        saasMocks.mockQuestionBulkCall(questionIdsList);
        questions = doGetQuestionsBulk(
                questionIdsList,
                userType,
                status().is2xxSuccessful());

        assertTrue(questions.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    public void testBatchQuestionWithPhoto(int userTypeId) throws Exception {
        UserType userType = UserType.valueOf(userTypeId);
        final String text = UUID.randomUUID().toString();
        final long modelId = 3923370598L;

        long[] questionIds = {
                doCreateQuestion(modelId, UID, text),
                doCreateQuestion(modelId + 1, UID, text + 1)
        };
        List<Long> questionIdsList = Arrays.asList(questionIds[0], questionIds[1]);

        // create photos
        Photo photo1 = new Photo(QaEntityType.QUESTION,
            String.valueOf(questionIds[1]), "ns1", "gr1", "image1", 0);
        Photo photo2 = new Photo(QaEntityType.QUESTION,
            String.valueOf(questionIds[1]), "ns2", "gr2", "image2", 1);
        // order is reversed intentionally
        photoService.createPhotos(QaEntityType.QUESTION, String.valueOf(questionIds[1]), List.of(photo2, photo1));
        photoService.updatePhotosModState(ModState.AUTO_FILTER_PASSED, List.of(photo2, photo1));
        questionService.forceUpdateModState(questionIds[1], ModState.NEW);

        // check something for uid
        resetMocks();
        if (supportsAnswers()) {
            saasMocks.mockBestAnswersSaasCall(DEF_PAGE_SIZE, questionIdsList, 1);
        }
        saasMocks.mockQuestionBulkCall(questionIdsList);
        List<QuestionDto> questions = doGetQuestionsBulk(
                questionIdsList,
                userType,
                status().is2xxSuccessful());
        assertEquals(2, questions.size());
        questions.sort(Comparator.comparing(QuestionDto::getId));
        checkQuestion(questions.get(0), questionIds[0], modelId, text);
        checkQuestion(questions.get(1), questionIds[1], modelId + 1, text + 1);

        assertEquals(2, questions.get(1).getPhotos().size());
        assertPhoto(photo1, questions.get(1).getPhotos().get(0));
        assertPhoto(photo2, questions.get(1).getPhotos().get(1));
    }

    private void assertPhoto(Photo expected, PhotoDto actual) {
        assertEquals(expected.getNamespace(), actual.getNamespace());
        assertEquals(expected.getGroupId(), actual.getGroupId());
        assertEquals(expected.getImageName(), actual.getImageName());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    public void testBatchQuestionWithProductIds(int userTypeId) throws Exception {
        UserType userType = UserType.valueOf(userTypeId);
        final String text = UUID.randomUUID().toString();
        final long modelId = 3923370598L;

        long[] questionIds = {
                doCreateQuestion(modelId, UID, text),
                doCreateQuestion(modelId + 1, UID, text + 1)
        };
        List<Long> questionIdsList = Arrays.asList(questionIds[0], questionIds[1]);

        // create product ids
        List<Long> expectedProductIds = Arrays.asList(42L, 596L, 23L, 3L, 8L);
        // order is reversed intentionally
        questionProductService.saveProductsForQuestion(questionIds[1], expectedProductIds);

        // check something for uid
        resetMocks();
        if (supportsAnswers()) {
            saasMocks.mockBestAnswersSaasCall(DEF_PAGE_SIZE, questionIdsList, 1);
        }
        saasMocks.mockQuestionBulkCall(questionIdsList);
        List<QuestionDto> questions = doGetQuestionsBulk(questionIdsList, userType, status().is2xxSuccessful());
        assertEquals(2, questions.size());
        questions.sort(Comparator.comparing(QuestionDto::getId));
        checkQuestion(questions.get(0), questionIds[0], modelId, text);
        checkQuestion(questions.get(1), questionIds[1], modelId + 1, text + 1);

        assertEquals(Collections.emptyList(), questions.get(0).getProductIds());
        assertEquals(expectedProductIds, questions.get(1).getProductIds());
    }

    protected String getQuestionRightsNotEnough(long uid, long questionId) {
        return String.format("User=%s hasn't rights to remove question with id=%s", uid, questionId);
    }

    public final long doCreateQuestion() throws Exception {
        return doCreateQuestion(MODEL_ID, UID, UUID.randomUUID().toString());
    }

    public final long doCreateQuestion(long entityId, long userId, String text) throws Exception {
        String response = doCreateQuestion(entityId, userId, text, status().is2xxSuccessful());
        return objectMapper.readValue(response, QuestionDto.class).getId();
    }

    public final QuestionDto doGetQuestion(long questionId, long userId) throws Exception {
        return doGetQuestion(questionId, userId, status().is2xxSuccessful());
    }

    public abstract String doCreateQuestion(long entityId,
                                            long userId,
                                            String text,
                                            ResultMatcher resultMatcher) throws Exception;

    public abstract QuestionDto doGetQuestion(long questionId,
                                              long userId,
                                              ResultMatcher resultMatcher) throws Exception;

    public abstract QuestionDto doGetQuestionYandexUid(long questionId,
                                                       String yandexUid,
                                                       ResultMatcher resultMatcher) throws Exception;

    public abstract String doDeleteQuestion(long questionId, long userId, ResultMatcher resultMatcher) throws Exception;

    public abstract List<QuestionDto> doGetQuestionsBulk(List<Long> questionIds,
                                                         UserType type,
                                                         ResultMatcher resultMatcher) throws Exception;

    public boolean supportsAnswers() {
        return true;
    }
}
