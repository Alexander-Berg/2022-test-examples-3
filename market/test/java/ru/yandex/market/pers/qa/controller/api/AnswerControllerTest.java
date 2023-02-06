package ru.yandex.market.pers.qa.controller.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.author.client.PersAuthorClient;
import ru.yandex.market.pers.author.client.api.model.AgitationEntity;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.pers.author.client.api.model.AgitationUserType;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.client.utils.ControllerConstants;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.dto.AnswerDto;
import ru.yandex.market.pers.qa.mock.mvc.AnswerMvcMocks;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.Sort;
import ru.yandex.market.pers.qa.model.State;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Category;
import ru.yandex.market.report.model.Model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.model.SortField.ID;

public class AnswerControllerTest extends QAControllerTest {

    @Autowired
    private AnswerService answerService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PersAuthorClient authorClient;

    @Autowired
    private ReportService reportService;

    @Test
    public void testDeleteAnswerChangeState() throws Exception {
        long questionId = createQuestion();
        long answerId = createAnswer(questionId);
        checkAnswerState(answerId, State.NEW);
        deleteAnswer(answerId);
        checkAnswerState(answerId, State.DELETED);
    }

    @Test
    public void testDeleteQuestionBanned() throws Exception {
        long questionId = createQuestion();
        long answerId = createAnswer(questionId);
        // should be available fine
        AnswerDto answer = getAnswer(answerId, UID);
        assertTrue(answer.isCanDelete());

        // still should be accessible with cache after ban
        answerService.forceUpdateModState(answerId, ModState.TOLOKA_REJECTED);
        answer = getAnswer(answerId, UID);
        checkAnswerState(answerId, State.NEW);

        // should be deletable
        assertTrue(answer.isCanDelete());
        deleteAnswer(answerId);
        checkAnswerState(answerId, State.DELETED);

        // should fail in repeated delete
        deleteAnswer(answerId, UID, status().is4xxClientError());
    }

    @Test
    public void testCanDeleteField() throws Exception {
        long questionId = createQuestion();
        long answerId = createAnswer(questionId);
        final AnswerDto answer = getAnswer(answerId, UID);
        assertTrue(answer.isCanDelete());
    }

    @Test
    public void testCanDeleteFieldSomeoneElseAnswer() throws Exception {
        long questionId = createQuestion();
        long answerId = createAnswer(questionId);
        final AnswerDto answer = getAnswer(answerId, UID + 1);
        assertFalse(answer.isCanDelete());
    }

    @Test
    public void testCanDeleteFieldVeryOldAnswer() throws Exception {
        long questionId = createQuestion();
        long answerId = createAnswer(questionId);
        jdbcTemplate.update("UPDATE qa.answer SET cr_time = now() - interval '2' DAY WHERE id=?", answerId);
        // for correct update cr_time
        invalidateCache();
        final AnswerDto answer = getAnswer(answerId, UID);
        assertTrue(answer.isCanDelete());
    }

    @Test
    public void testDeleteOldAnswer() throws Exception {
        long questionId = createQuestion();
        long answerId = createAnswer(questionId);
        jdbcTemplate.update("UPDATE qa.answer SET cr_time = now() - interval '2' DAY WHERE id=?", answerId);
        String response = invokeAndRetrieveResponse(
            delete("/answer/" + answerId).param("userId", String.valueOf(UID))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        assertTrue(response.isEmpty());
    }

    @Test
    public void testDeleteSomeoneElseAnswer() throws Exception {
        long questionId = createQuestion();
        long answerId = createAnswer(questionId);
        String response = invokeAndRetrieveResponse(
            delete("/answer/" + answerId).param("userId", String.valueOf(UID + 1))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is4xxClientError());
        assertTrue(response.contains(getNotEnoughRightsErrorField("answer", UID + 1, answerId)));
    }

    @Test
    public void testCreateDuplicateAnswer() throws Exception {
        long questionId = createQuestion();
        String text = "Окончательный Ответ на величайший вопрос Жизни, Вселенной и Всего Такого - 42!";
        long answerId = createAnswer(questionId, text);
        final String response = invokeAndRetrieveResponse(
            post("/answer/UID/" + UID + "/question/" + questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(AnswerMvcMocks.ANSWER_BODY, text))
                .accept(MediaType.APPLICATION_JSON),
            status().is4xxClientError());
        assertTrue(response.contains("\"error\":\"Answer already exists\""));
    }

    @Test
    public void testGetPublicAnswers() throws Exception {
        long questionId = createQuestion();
        List<Long> pubAnswers = createAnswersAndReturnPublic(questionId);

        QAPager<AnswerDto> qaPager = getAnswers(questionId);
        checkAnswers(pubAnswers, qaPager);
    }

    @Test
    public void testGetPublicAnswersYandexUid() throws Exception {
        long questionId = createQuestion();
        List<Long> pubAnswers = createAnswersAndReturnPublic(questionId);

        QAPager<AnswerDto> qaPager = getAnswersYandexUid(questionId, YANDEXUID);
        checkAnswers(pubAnswers, qaPager);
    }

    @Test
    public void testAnswerCanDeleteIsCorrect() throws Exception {
        final long brandId = 12345;
        final long brandIdOther = 12345;

        final long questionId = createQuestion();
        final long uid = UID;
        final long uid2 = UID + 1;

        final long answerId = createAnswer(questionId, uid, "Some text");
        final long answerIdFromOtherUser = createAnswer(questionId, uid2, "Some text from other");

        final long vendorAnswerId = createVendorAnswer(brandId, uid, questionId, "Some text 1");
        final long vendorAnswerIdFromOtherUser = createVendorAnswer(brandId, uid2, questionId, "Some text 2");
        final long vendorAnswerIdFromOtherBrand = createVendorAnswer(brandIdOther, uid, questionId, "Some text 3");

        //
        // check delete rights when called from regular method
        //
        final Map<Long, AnswerDto> answerMap = getAnswers(questionId).getData().stream()
            .collect(Collectors.toMap(AnswerDto::getAnswerId, x -> x));

        // delete/edit is ok only when user is author
        assertTrue(answerMap.get(answerId).isCanDelete());
        assertFalse(answerMap.get(answerIdFromOtherUser).isCanDelete());

        // can't delete/edit vendor answers from regular user
        assertFalse(answerMap.get(vendorAnswerId).isCanDelete());
        assertFalse(answerMap.get(vendorAnswerIdFromOtherUser).isCanDelete());
        assertFalse(answerMap.get(vendorAnswerIdFromOtherBrand).isCanDelete());

        //
        // check delete rights from vendor api
        //
        final Map<Long, AnswerDto> vendorAnswerMap = getAnswersForVendor(questionId, brandId, Sort.asc(ID))
            .getData().stream()
            .collect(Collectors.toMap(AnswerDto::getAnswerId, x -> x));

        // can't delete/edit regular answers from vendor
        assertFalse(vendorAnswerMap.get(answerId).isCanDelete());
        assertFalse(vendorAnswerMap.get(answerIdFromOtherUser).isCanDelete());

        // can edit with matching brand, no matter which user is used to call answers
        assertTrue(vendorAnswerMap.get(vendorAnswerId).isCanDelete());
        assertTrue(vendorAnswerMap.get(vendorAnswerIdFromOtherUser).isCanDelete());
        assertFalse(answerMap.get(vendorAnswerIdFromOtherBrand).isCanDelete());
    }

    @Test
    public void testEditAnswer() throws Exception {
        final int brandId = 24525;
        final long questionId = createQuestion();

        final long answerId = createAnswer(questionId, "Test from user");

        // can't edit vendor answer from user controller (even with same UID)
        // can't edit user answer at all (for now)
        assertThrows(AssertionError.class, () -> editVendorAnswer(answerId, brandId, "New updated text"));
    }

    @Test
    public void testDeleteAnswer() throws Exception {
        final int brandId = 24525;
        final long questionId = createQuestion();
        final long answerId = createAnswer(questionId, "Test from user");

        // should fail - can't delete user answers with vendor rights (even with same UID)
        assertThrows(AssertionError.class, () -> deleteVendorAnswer(answerId, brandId));

        // check delete is ok for regular user
        assertEquals(State.NEW, answerService.getAnswerByIdInternal(answerId).getState());
        deleteAnswer(answerId);
        assertEquals(State.DELETED, answerService.getAnswerByIdInternal(answerId).getState());
    }

    @Test
    public void testVendorAnswerBrandId() throws Exception {
        final int brandId = 24525;
        final long questionId = createQuestion();

        final long answerId = createAnswer(questionId);
        final long vendorAnswerId = createVendorAnswer(brandId, questionId, "Vendor answer");

        final Map<Long, AnswerDto> answerMap = getAnswers(questionId).getData().stream()
            .collect(Collectors.toMap(AnswerDto::getAnswerId, x -> x));

        // brand is not filled for regular answer
        assertNull(answerMap.get(answerId).getBrandId());

        // brand is filled for vendor answer
        assertEquals(Long.valueOf(brandId), answerMap.get(vendorAnswerId).getBrandId());
    }

    @Test
    public void testCountCorrectAfterCreateRemove() throws Exception {
        long modelId = 31415926;
        final long questionId = createQuestion(modelId);

        final long count = questionMvc.getQuestion(questionId, UID).getAnswersCount();

        final long answerId = createAnswer(questionId);
        assertEquals(count + 1, questionMvc.getQuestion(questionId, UID).getAnswersCount());

        deleteAnswer(answerId);
        assertEquals(count, questionMvc.getQuestion(questionId, UID).getAnswersCount());
    }

    @Test
    public void testAnswerLock() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long modelId = 3451;

        final long questionId = createQuestion(modelId);
        final long answerId = createAnswer(questionId, text);

        String expectedLock = String.format("0_%s_%s_%s", UID, questionId, text.hashCode());
        String badLock = String.format("0_%s_%s_%s", UID, questionId, text);

        assertTrue(hasLock(expectedLock));
        assertFalse(hasLock(badLock));
    }

    @Test
    public void testAnswerCount() throws Exception {
        final long modelId = 3451;

        final long questionId = createQuestion(modelId);
        final long questionIdFictional = -1000;

        final List<Long> questionIds = Arrays.asList(questionId, questionIdFictional);

        final long answerId = createAnswer(questionId);

        final Map<Long, Long> counts = answerService.getPublicAnswersCountByQuestionIds(questionIds);

        assertEquals(1, counts.get(questionId).longValue());
        assertEquals(0, counts.get(questionIdFictional).longValue());
    }

    @Test
    public void testAnswerListModification() throws Exception {
        final long modelId = 2382752;

        final long pageSize = 3;
        final long firstPage = 1;
        final long secondPage = 2;

        final long questionId = createQuestion(modelId);

        // check no answers yet
        Map<Long, AnswerDto> answers = getAnswersMap(questionId, firstPage, pageSize);
        assertEquals(0, answers.size());

        // two answers created, check everything ok
        final long answerId_1 = createAnswer(questionId);
        final long answerId_2 = createAnswer(questionId);

        answers = getAnswersMap(questionId, firstPage, pageSize);
        assertEquals(2, answers.size());
        assertTrue(answers.containsKey(answerId_1));
        assertTrue(answers.containsKey(answerId_2));

        answers = getAnswersMap(questionId, secondPage, pageSize);
        assertEquals(0, answers.size());

        // added two more - second page should not be empty
        final long answerId_3 = createAnswer(questionId);
        final long answerId_4 = createAnswer(questionId);

        answers = getAnswersMap(questionId, firstPage, pageSize);
        assertEquals(3, answers.size());
        assertTrue(answers.containsKey(answerId_1));
        assertTrue(answers.containsKey(answerId_2));
        assertTrue(answers.containsKey(answerId_3));

        answers = getAnswersMap(questionId, secondPage, pageSize);
        assertEquals(1, answers.size());
        assertTrue(answers.containsKey(answerId_4));

        // removed 2 answers
        deleteAnswer(answerId_1);
        deleteAnswer(answerId_3);

        answers = getAnswersMap(questionId, firstPage, pageSize);
        assertEquals(2, answers.size());
        assertTrue(answers.containsKey(answerId_2));
        assertTrue(answers.containsKey(answerId_4));

        answers = getAnswersMap(questionId, secondPage, pageSize);
        assertEquals(0, answers.size());
    }

    @Test
    public void testSingleAnswer() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long questionId = createQuestion();
        final long answerId = createAnswer(questionId, UID, text);

        // check something for uid
        AnswerDto answer = getAnswer(answerId, UID);
        checkAnswer(answer, answerId, questionId, text);

        // check something for yandexuid
        answer = getAnswerYandexUid(answerId, YANDEXUID);
        checkAnswer(answer, answerId, questionId, text);

        // remove, check that removed
        deleteAnswer(answerId);

        getAnswer(answerId, UID, status().isNotFound());
        getAnswerYandexUid(answerId, YANDEXUID, status().isNotFound());
    }

    @Test
    public void testAnswerAuthorSignal() throws Exception {
        long hid = 1;
        long modelId = 1;
        final long questionId = questionMvc.createModelQuestionHid(modelId, hid);
        final long answerId = createAnswer(questionId, UID);
        verify(authorClient, times(1)).markContentUpdated(
            anyLong(),
            eq(AgitationEntity.MODEL_QUESTION),
            eq(String.valueOf(questionId)),
            eq(hid),
            argThat(argument -> argument != null && argument.equals(List.of(AgitationType.MODEL_QUESTION_ANSWER))),
            argThat(argument -> argument != null && argument.isEmpty())
        );
    }

    @Test
    public void testAnswerAuthorSignalMultiple() throws Exception {
        long hid = 1;
        long modelId = 1;
        final long questionId = questionMvc.createModelQuestionHid(modelId, hid);
        final long answerId = createAnswer(questionId, UID);
        final long answerId2 = createAnswer(questionId, UID);

        // only one signal, would be sent
        verify(authorClient, times(1)).markContentUpdated(
            anyLong(),
            eq(AgitationEntity.MODEL_QUESTION),
            eq(String.valueOf(questionId)),
            eq(hid),
            argThat(argument -> argument != null && argument.equals(List.of(AgitationType.MODEL_QUESTION_ANSWER))),
            argThat(argument -> argument != null && argument.isEmpty())
        );

        verify(authorClient, times(1)).completeAgitation(
            eq(AgitationUserType.UID),
            eq(UID_STR),
            eq(AgitationType.MODEL_QUESTION_ANSWER),
            eq(String.valueOf(questionId))
        );
    }

    @Test
    public void testAnswerAuthorSignalMultiple2() throws Exception {
        long hid = 1;
        long modelId = 1;
        final long questionId = questionMvc.createModelQuestionHid(modelId, hid);
        final long answerId = createAnswer(questionId, UID);
        deleteAnswer(answerId);
        final long answerId2 = createAnswer(questionId, UID);

        // both signals would be sent
        verify(authorClient, times(2)).markContentUpdated(
            anyLong(),
            eq(AgitationEntity.MODEL_QUESTION),
            eq(String.valueOf(questionId)),
            eq(hid),
            argThat(argument -> argument != null && argument.equals(List.of(AgitationType.MODEL_QUESTION_ANSWER))),
            argThat(argument -> argument != null && argument.isEmpty())
        );
    }

    @Test
    public void testAnswerAuthorSignalNoHid() throws Exception {
        long modelId = 1;
        final long questionId = questionMvc.createModelQuestion(modelId);
        final long answerId = createAnswer(questionId, UID);
        verify(authorClient, times(1)).markContentUpdated(
            anyLong(),
            eq(AgitationEntity.MODEL_QUESTION),
            eq(String.valueOf(questionId)),
            isNull(),
            argThat(argument -> argument != null && argument.equals(List.of(AgitationType.MODEL_QUESTION_ANSWER))),
            argThat(argument -> argument != null && argument.isEmpty())
        );
    }

    @Test
    public void testAnswerAuthorSignalNoHidReport() throws Exception {
        long hid = 1231431L;
        long modelId = 1L;

        Model model = mock(Model.class);
        when(model.getCategory()).thenReturn(new Category(hid, "name"));
        when(reportService.getModelsByIds(anyList())).thenReturn(Map.of(modelId, model));

        final long questionId = questionMvc.createModelQuestion(modelId);
        final long answerId = createAnswer(questionId, UID);
        verify(authorClient, times(1)).markContentUpdated(
            anyLong(),
            eq(AgitationEntity.MODEL_QUESTION),
            eq(String.valueOf(questionId)),
            eq(hid),
            argThat(argument -> argument != null && argument.equals(List.of(AgitationType.MODEL_QUESTION_ANSWER))),
            argThat(argument -> argument != null && argument.isEmpty())
        );
    }

    @Test
    public void testAnswerAuthorSignalByShop() throws Exception {
        long modelId = 1;
        int hid = 1;
        int shopId = 123;
        final long questionId = questionMvc.createModelQuestionHid(modelId, hid);
        final long answerId = createPartnerShopAnswer(shopId, questionId, "text");
        verify(authorClient, times(0)).markContentUpdated(
            anyLong(),
            any(),
            any(),
            any(),
            any(),
            any()
        );
    }

    @Test
    public void testAnswerAuthorSignalByVendor() throws Exception {
        long modelId = 1;
        int hid = 1;
        int vendorId = 123;
        final long questionId = questionMvc.createModelQuestionHid(modelId, hid);
        final long answerId = createVendorAnswer(vendorId, questionId, "text");
        verify(authorClient, times(0)).markContentUpdated(
            anyLong(),
            any(),
            any(),
            any(),
            any(),
            any()
        );
    }

    @Test
    public void testAnswerRemovedQuestion() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long questionId = createQuestion();

        // need to delete with internal tools, cache should be filled
        questionService.deleteQuestion(questionId, UID);

        final long answerId = createAnswer(questionId, UID, text);

        // should be ok
        AnswerDto answer = getAnswer(answerId, UID);
        checkAnswer(answer, answerId, questionId, text);
    }

    @Test
    public void testAnswerBannedQuestion() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long questionId = createQuestion();

        // need to delete with internal tools, cache should be filled
        questionService.forceUpdateModState(questionId, ModState.TOLOKA_REJECTED);

        final long answerId = createAnswer(questionId, UID, text);

        // should be ok
        AnswerDto answer = getAnswer(answerId, UID);
        checkAnswer(answer, answerId, questionId, text);
    }

    @Test
    public void testEmptyAnswer() throws Exception {
        String ANSWER_REQUEST_BODY = "{\n" +
            "  \"text\": null" +
            "}";
        String response = invokeAndRetrieveResponse(
            post("/answer/UID/1/question/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ANSWER_REQUEST_BODY)
                .accept(MediaType.APPLICATION_JSON),
            status().is4xxClientError());
        JSONObject jsonObject = new JSONObject(response);
        assertEquals("VALIDATION_ERROR", jsonObject.getString("code"));
        assertEquals("text must not be empty",
            jsonObject.getJSONArray("error").getString(0));
    }

    private void checkAnswers(List<Long> answers, QAPager<AnswerDto> result) {
        result.getData().size();
        List<Long> ids = result.getData().stream().map(AnswerDto::getAnswerId).collect(Collectors.toList());
        assertEquals(answers.size(), result.getData().size());
        answers.forEach(q -> {
            assertThat(ids, hasItem(q));
        });
    }

    private void checkAnswer(AnswerDto answer, long answerId, long questionId, String text) {
        assertEquals(ControllerConstants.ENTITY_ANSWER, answer.getEntity());
        assertEquals(answerId, answer.getAnswerId());
        assertEquals(questionId, answer.getQuestionId().getId());
        assertEquals(UID, answer.getUserDto().getUserId());
        assertEquals(text, answer.getText());
    }

    private void checkAnswerState(long id, State expectedState) {
        final Answer answer = answerService.getAnswerByIdInternal(id);
        assertEquals(expectedState, answer.getState());
    }

    private boolean hasLock(String lockId) {
        final Long count = jdbcTemplate.queryForObject(
            "select count(*) from qa.answer_lock where id = ?",
            Long.class,
            lockId
        );
        return count != null && count > 0;
    }

}
