package ru.yandex.market.pers.qa.controller.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.dto.ModelQuestionCountDto;
import ru.yandex.market.pers.qa.controller.dto.ProductIdDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionAgitationInfoDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.mock.ReportServiceMockUtils;
import ru.yandex.market.pers.qa.model.AgitationType;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.QuestionFilter;
import ru.yandex.market.pers.qa.model.SecurityData;
import ru.yandex.market.pers.qa.model.UserBanInfo;
import ru.yandex.market.pers.qa.service.SecurityService;
import ru.yandex.market.pers.qa.service.UserBanService;
import ru.yandex.market.pers.qa.utils.CommonUtils;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Category;
import ru.yandex.market.util.FormatUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class QuestionModelControllerTest extends AbstractQuestionControllerTest {

    private static final long MODERATOR_ID = 100500;

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserBanService userBanService;

    @Autowired
    private SecurityService securityService;

    @Override
    public void checkEntityId(QuestionDto question, long entityId) {
        assertEquals(QuestionDto.QUESTION, question.getEntity());
        assertNull(question.getCategoryIdDto());
        assertNull(question.getInterestIdDto());
        assertNotNull(question.getProductIdDto());
        assertEquals(entityId, question.getProductIdDto().getId());
        assertEquals(ProductIdDto.PRODUCT, question.getProductIdDto().getEntity());
    }

    @Test
    public void testGetPublicQuestions() throws Exception {
        List<Long> pubQuestions = createModelQuestionsReturnPublic();

        QAPager<QuestionDto> qaPager = getModelQuestions(MODEL_ID);
        List<Long> ids = qaPager.getData().stream().map(QuestionDto::getId).collect(Collectors.toList());
        assertEquals(pubQuestions.size(), qaPager.getData().size());
        pubQuestions.forEach(q -> {
            assertThat(ids, hasItem(q));
        });
    }

    @Test
    public void testGetPublicQuestionsWithAnswersCount() throws Exception {
        List<Long> pubQuestions = createModelQuestionsReturnPublic();
        Map<Long, Integer> questionsWithAnswersCount = new HashMap<>();
        for (Long questionId : pubQuestions) {
            List<Long> answers = createAnswersAndReturnPublic(questionId);
            questionsWithAnswersCount.put(questionId, answers.size());
        }
        QAPager<QuestionDto> qaPager = getModelQuestions(MODEL_ID);
        checkModelQuestions(pubQuestions, questionsWithAnswersCount, qaPager);
    }

    @Test
    public void testGetPublicQuestionsWithAnswersCountYandexUid() throws Exception {
        List<Long> pubQuestions = createModelQuestionsReturnPublic();
        Map<Long, Integer> questionsWithAnswersCount = new HashMap<>();
        for (Long questionId : pubQuestions) {
            List<Long> answers = createAnswersAndReturnPublic(questionId);
            questionsWithAnswersCount.put(questionId, answers.size());
        }
        QAPager<QuestionDto> qaPager = getModelQuestionsYandexUid(MODEL_ID, YANDEXUID);
        checkModelQuestions(pubQuestions, questionsWithAnswersCount, qaPager);
    }

    @Test
    public void testGetCountPublicQuestions() throws Exception {
        List<Long> pubQuestions = createModelQuestionsReturnPublic(UID);
        pubQuestions.addAll(createModelQuestionsReturnPublic(UID + 1));

        assertEquals(pubQuestions.size(), getModelQuestionsCount(MODEL_ID));
    }

    @Test
    public void testCreateQuestionCategoryFromReport() throws Exception {
        Long categoryId = 100500L;
        Category category = new Category(categoryId, "Мобильные телефоны");
        ReportServiceMockUtils.mockReportServiceCategoryInGetModelByModelId(reportService, MODEL_ID, category);
        final String response = invokeAndRetrieveResponse(
            post("/question/UID/" + UID + "/model/" + MODEL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\n" +
                    "  \"text\": \"%s\"\n" +
                    "}", UUID.randomUUID().toString()))
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());

        long questionId = this.objectMapper.readValue(response, QuestionDto.class).getId();
        checkQuestionHid(categoryId, questionId);
    }

    @Test
    public void testCreateQuestionCategoryFromRequest() throws Exception {
        long questionId = createQuestion();
        checkQuestionHid(CATEGORY_HID, questionId);
    }

    @Test
    public void testCreateQuestionIp() throws Exception {
        long questionId = questionMvc.createModelQuestionWithIp(MODEL_ID, UID, "123.123.123.123", null).getId();
        SecurityData securityData = securityService.getSecurityData(QaEntityType.QUESTION, questionId);
        assertEquals("123.123.123.123", securityData.getIp());
        assertNull(securityData.getPort());

        long questionId2 = questionMvc.createModelQuestionWithIp(MODEL_ID, UID, "123.123.123.222", 5322).getId();
        SecurityData securityData2 = securityService.getSecurityData(QaEntityType.QUESTION, questionId2);
        assertEquals("123.123.123.222", securityData2.getIp());
        assertEquals(5322, securityData2.getPort());
    }

    @Test
    public void testGetModelQuestionCount() throws Exception {
        long modelNoQuestions = 1;

        //model with questions, no answers
        long modelNoAnsweredQuestions = 22;
        long modelNoAnswQuestionCount = 13;
        for (int i = 0; i < modelNoAnswQuestionCount; i++) {
            createQuestion(modelNoAnsweredQuestions);
        }

        //model with questions, some answers
        long modelWithQuestions = 3330333;
        long modelWithQuestionsTotalCount = 15;
        long modelWithQuestionsNoAnsCount = 10;
        for (int i = 0; i < modelWithQuestionsNoAnsCount; i++) {
            createQuestion(modelWithQuestions);
        }
        for (int i = 0; i < modelWithQuestionsTotalCount - modelWithQuestionsNoAnsCount; i++) {
            long questionId = createQuestion(modelWithQuestions);
            createAnswer(questionId);
        }

        final List<Long> modelIds = Arrays.asList(modelNoQuestions, modelNoAnsweredQuestions, modelWithQuestions);

        List<ModelQuestionCountDto> modelsCounts = getModelQuestionCount(modelIds, true);
        assertEquals(modelIds.size(), modelsCounts.size());
        checkModelQuestionCount(modelsCounts, modelNoQuestions, 0, 0);
        checkModelQuestionCount(modelsCounts, modelNoAnsweredQuestions, modelNoAnswQuestionCount, modelNoAnswQuestionCount);
        checkModelQuestionCount(modelsCounts, modelWithQuestions, modelWithQuestionsTotalCount, modelWithQuestionsNoAnsCount);

        modelsCounts = getModelQuestionCount(modelIds, false);
        assertEquals(modelIds.size(), modelsCounts.size());
        checkModelQuestionCountNullNoAnswer(modelsCounts, modelNoQuestions, 0);
        checkModelQuestionCountNullNoAnswer(modelsCounts, modelNoAnsweredQuestions, modelNoAnswQuestionCount);
        checkModelQuestionCountNullNoAnswer(modelsCounts, modelWithQuestions, modelWithQuestionsTotalCount);

        modelsCounts = getModelQuestionCount(modelIds);
        assertEquals(modelIds.size(), modelsCounts.size());
        checkModelQuestionCountNullNoAnswer(modelsCounts, modelNoQuestions, 0);
        checkModelQuestionCountNullNoAnswer(modelsCounts, modelNoAnsweredQuestions, modelNoAnswQuestionCount);
        checkModelQuestionCountNullNoAnswer(modelsCounts, modelWithQuestions, modelWithQuestionsTotalCount);
    }

    @Test
    public void testGetModelQuestionCountToManyModels() throws Exception {
        final List<Long> modelIds = new ArrayList<>();
        long size = 31;
        for (long i = 0; i < size; i++) {
            modelIds.add(i);
        }

        final String response = invokeAndRetrieveResponse(
            get("/question/model/count")
                .param("userId", String.valueOf(UID))
                .param("modelId", modelIds.stream().map(String::valueOf).toArray(String[]::new))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is4xxClientError());
    }

    @Test
    public void testCountCorrectAfterCreateRemove() throws Exception {
        long modelId = 31415926;

        final long count = getModelQuestionsCount(modelId);
        final long questionId = createQuestion(modelId);
        assertEquals(count + 1, getModelQuestionsCount(modelId));

        deleteQuestion(questionId);
        assertEquals(count, getModelQuestionsCount(modelId));
    }

    @Test
    public void testQuestionLock() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long modelId = 3451;

        final long questionId = createQuestion(modelId, UID, text);

        String expectedLock = String.format("0_%s_0_%s_%s", UID, modelId, text.hashCode());
        String badLock = String.format("0_%s_0_%s_%s", UID, modelId, text);

        assertTrue(hasLock(expectedLock));
        assertFalse(hasLock(badLock));
    }

    @Test
    public void testQuestionCount() throws Exception {
        final long modelId = 3451;
        final long modelIdFictional = -1000;

        createQuestion(modelId);

        final List<Long> modelIds = Arrays.asList(modelId, modelIdFictional);

        final Map<Long, Long> counts = questionService.getPublicQuestionCountForModels(modelIds);

        assertEquals(1, counts.get(modelId).longValue());
        assertEquals(0, counts.get(modelIdFictional).longValue());
    }

    @Test
    public void testQuestionListModification() throws Exception {
        final long modelId = 2382752;

        final long pageSize = 3;
        final long firstPage = 1;
        final long secondPage = 2;

        // check no questions yet
        assertEquals(0, getModelQuestionsCount(modelId));
        Map<Long, QuestionDto> questions = map(getModelQuestions(modelId, firstPage, pageSize));
        assertEquals(0, questions.size());

        // two questions created, check everything ok
        final long questionId_1 = createQuestion(modelId);
        final long questionId_2 = createQuestion(modelId);

        questions = map(getModelQuestions(modelId, firstPage, pageSize));
        assertEquals(2, questions.size());
        assertTrue(questions.containsKey(questionId_1));
        assertTrue(questions.containsKey(questionId_2));

        questions = map(getModelQuestions(modelId, secondPage, pageSize));
        assertEquals(0, questions.size());

        // added two more - second page should not be empty
        final long questionId_3 = createQuestion(modelId);
        final long questionId_4 = createQuestion(modelId);

        questions = map(getModelQuestions(modelId, firstPage, pageSize));
        assertEquals(3, questions.size());
        assertTrue(questions.containsKey(questionId_2));
        assertTrue(questions.containsKey(questionId_3));
        assertTrue(questions.containsKey(questionId_4));

        questions = map(getModelQuestions(modelId, secondPage, pageSize));
        assertEquals(1, questions.size());
        assertTrue(questions.containsKey(questionId_1));

        // removed 2 questions
        deleteQuestion(questionId_1);
        deleteQuestion(questionId_3);

        questions = map(getModelQuestions(modelId, firstPage, pageSize));
        assertEquals(2, questions.size());
        assertTrue(questions.containsKey(questionId_2));
        assertTrue(questions.containsKey(questionId_4));

        questions = map(getModelQuestions(modelId, secondPage, pageSize));
        assertEquals(0, questions.size());
    }

    @Test
    public void testNoAgitationForShopQuestion() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long questionId = createQuestion(text);
        qaJdbcTemplate.update("update qa.question_info set shop_rx_fl = ? where question_id = ?", 1, questionId);

        QuestionAgitationInfoDto agitationInfo = getQuestionAgitationInfo(questionId);
        assertAgitation(AgitationType.SHOP, agitationInfo);
        assertEquals(text, agitationInfo.getText());
    }

    @Test
    public void testAgitationForNonShopQuestion() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long questionId = createQuestion(text);
        qaJdbcTemplate.update("update qa.question_info set shop_rx_fl = ? where question_id = ?", 0, questionId);

        QuestionAgitationInfoDto agitationInfo = getQuestionAgitationInfo(questionId);
        assertAgitation(AgitationType.DEFAULT, agitationInfo);
        assertEquals(text, agitationInfo.getText());
    }

    @Test
    public void testNoAgitationForUnknownShopQuestion() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long questionId = createQuestion(text);

        QuestionAgitationInfoDto agitationInfo = getQuestionAgitationInfo(questionId);
        assertAgitation(AgitationType.SHOP, agitationInfo);
        assertEquals(text, agitationInfo.getText());
    }

    @Test
    public void testNoAgitationForDeletedQuestion() throws Exception {
        final long questionId = -1;

        QuestionAgitationInfoDto agitationInfo = getQuestionAgitationInfo(questionId);
        assertAgitation(AgitationType.SHOP, agitationInfo);
        assertEquals("", agitationInfo.getText());
        assertEquals(questionId, agitationInfo.getId());
    }

    @Test
    public void testAgitationForNonShopQuestionStrict() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long questionId = createQuestion(text);
        qaJdbcTemplate.update("update qa.question_info set shop_rx_fl = ? where question_id = ?", 0, questionId);

        String agitationInfo = getQuestionAgitationInfoString(questionId);

        String expected = String.format("{\"id\":%s,\"text\":\"%s\",\"forAgitation\":1}", questionId, text);
        JSONAssert.assertEquals(expected, agitationInfo, true);
    }

    @Test
    public void testAgitationSerialization() throws Exception {
        QuestionAgitationInfoDto info = new QuestionAgitationInfoDto();

        int id = 123;
        String text = "test-text";

        info.setId(id);
        info.setText(text);
        info.setForAgitation(AgitationType.DEFAULT.getAgitatationFlag());

        String expected = "{\"id\":123,\"text\":\"test-text\",\"forAgitation\":1}";
        String result = FormatUtils.toJson(info);

        JSONAssert.assertEquals(expected, result, false);
    }

    @Test
    public void testAgitationDeserialization() {
        String source = "{\"id\":123,\"text\":\"test-text\",\"forAgitation\":1}";

        QuestionAgitationInfoDto info = FormatUtils.fromJson(source, QuestionAgitationInfoDto.class);

        assertEquals(123, info.getId());
        assertEquals("test-text", info.getText());
        assertEquals(AgitationType.DEFAULT.getAgitatationFlag(), info.getForAgitation());
    }

    @Test
    public void testCreateForBannedUser() throws Exception {
        long modelId = 1;
        long userId = -13498175;
        String reason = "no reason";

        userBanService.ban(UserBanInfo.forever(UserType.UID, String.valueOf(userId), reason, MODERATOR_ID));

        final String text = UUID.randomUUID().toString();

        // should create ok
        final long questionId = createQuestion(modelId, userId, text);

        // check exists and public - both ok
        assertTrue(questionService.isQuestionExists(questionId));
        assertEquals(1, questionService.getQuestionsCount(new QuestionFilter().id(questionId)));

        // no agitation instantly
        QuestionAgitationInfoDto agitationInfo = getQuestionAgitationInfo(questionId);
        assertAgitation(AgitationType.SHOP, agitationInfo);
        assertEquals(text, agitationInfo.getText());

        // banned after auto-filters
        questionService.autoFilterQuestions();

        // check banned
        // check exists and public - should exists, but would not be published (banned)
        assertTrue(questionService.isQuestionExists(questionId));
        assertEquals(0, questionService.getQuestionsCount(new QuestionFilter().id(questionId)));
    }

    @Test
    void testSimilar() throws Exception {
        long modelId = 934012;
        long pageSize = 10;
        long firstPage = 1;

        long baseQuestion = createQuestion(modelId, UID, "question 0");

        long[] questions = {
            createQuestion(modelId, UID, "question 1"),
            createQuestion(modelId, UID, "question 2"),
            createQuestion(modelId, UID + 1, "other question"),
        };

        saasMocks.mockModelCall(modelId, pageSize, CommonUtils.list(questions));

        // case: UID
        DtoList<QuestionDto> similarQuestions = getSimilarQuestionsUid(baseQuestion, UID, firstPage, pageSize);
        List<QuestionDto> data = similarQuestions.getData();
        assertEquals(3, data.size());
        checkQuestion(data.get(0), questions[0], modelId, UID, "question 1");
        checkQuestion(data.get(1), questions[1], modelId, UID, "question 2");
        checkQuestion(data.get(2), questions[2], modelId, UID + 1, "other question");

        // case: yandexuid, exclude requested question only
        similarQuestions = getSimilarQuestionsYandexUid(baseQuestion, YANDEXUID, firstPage, pageSize);
        data = similarQuestions.getData();
        assertEquals(3, data.size());
        checkQuestion(data.get(0), questions[0], modelId, UID, "question 1");
        checkQuestion(data.get(1), questions[1], modelId, UID, "question 2");
        checkQuestion(data.get(2), questions[2], modelId, UID + 1, "other question");
    }

    private void assertAgitation(AgitationType agitationType, QuestionAgitationInfoDto dto) {
        assertEquals(agitationType.getAgitatationFlag(), (int) dto.getForAgitation());
    }

    private void checkModelQuestions(List<Long> questions,
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

    private void checkModelQuestionCountNullNoAnswer(List<ModelQuestionCountDto> modelQuestionCount, long modelId, long totalCount) {
        final ModelQuestionCountDto counts = modelQuestionCount.stream().filter(it -> it.getModelId() == modelId).findFirst().get();
        assertNotNull(counts);
        assertEquals(totalCount, (long) counts.getTotalCount());
        assertNull(counts.getNoAnswerCount());
    }

    private void checkModelQuestionCount(List<ModelQuestionCountDto> modelQuestionCount, long modelId, long totalCount, long noAnsCount) {
        final ModelQuestionCountDto counts = modelQuestionCount.stream().filter(it -> it.getModelId() == modelId).findFirst().get();
        assertNotNull(counts);
        assertEquals(totalCount, (long) counts.getTotalCount());
        assertEquals(noAnsCount, (long) counts.getNoAnswerCount());
    }

    private void checkQuestionHid(Long categoryId, long questionId) {
        List<Long> hids = qaJdbcTemplate.queryForList("select entity_src from qa.question where id = ?",
            Long.class, questionId);

        assertEquals(1, hids.size());
        assertNotNull(hids.get(0));
        assertEquals(categoryId, hids.get(0));
    }
}
