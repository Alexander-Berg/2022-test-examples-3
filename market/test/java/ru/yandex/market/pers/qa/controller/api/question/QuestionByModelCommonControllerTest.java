package ru.yandex.market.pers.qa.controller.api.question;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.dto.ProductIdDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.03.2020
 */
public class QuestionByModelCommonControllerTest extends AbstractQuestionCommonControllerTest {

    @Override
    public String doCreateQuestion(long entityId,
                                   long userId,
                                   String text,
                                   ResultMatcher resultMatcher) throws Exception {
        return questionMvc.createModelQuestion(entityId, userId, text, null, resultMatcher);
    }

    @Override
    public QuestionDto doGetQuestion(long questionId, long userId, ResultMatcher resultMatcher) throws Exception {
        return questionMvc.getQuestion(questionId, userId, resultMatcher);
    }

    @Override
    public QuestionDto doGetQuestionYandexUid(long questionId,
                                              String yandexUid,
                                              ResultMatcher resultMatcher) throws Exception {
        return questionMvc.getQuestionYandexUid(questionId, yandexUid, resultMatcher);
    }

    @Override
    public String doDeleteQuestion(long questionId,
                                   long userId,
                                   ResultMatcher resultMatcher) throws Exception {
        return questionMvc.deleteQuestion(questionId, userId, resultMatcher);
    }

    @Override
    public List<QuestionDto> doGetQuestionsBulk(List<Long> questionIds,
                                                UserType type,
                                                ResultMatcher resultMatcher) throws Exception {
        return questionMvc.getQuestionsBulk(questionIds, type, resultMatcher);
    }

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
    public final void testSingleQuestionWithSku() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long entityId = 3923370598L;
        final long sku = 987654321L;
        final long questionId = objectMapper.readValue(
            questionMvc.createModelQuestion(entityId, UID, text, null, sku, status().is2xxSuccessful()),
            QuestionDto.class).getId();

        // check something for uid
        QuestionDto question = doGetQuestion(questionId, UID);
        checkQuestion(question, questionId, entityId, text);
        assertEquals(sku, question.getSku());

        // check something for yandexuid
        question = doGetQuestionYandexUid(questionId, YANDEXUID, status().is2xxSuccessful());
        checkQuestion(question, questionId, entityId, text);
        assertEquals(sku, question.getSku());

        // remove, check that removed
        doDeleteQuestion(questionId, UID, status().is2xxSuccessful());

        doGetQuestion(questionId, UID, status().isNotFound());
        doGetQuestionYandexUid(questionId, YANDEXUID, status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    public void testBatchQuestionWithSku(int userTypeId) throws Exception {
        UserType userType = UserType.valueOf(userTypeId);
        final String text = UUID.randomUUID().toString();
        final long modelId = 3923370598L;
        final long sku = 987654321L;

        long[] questionIds = {
            objectMapper.readValue(
                questionMvc.createModelQuestion(modelId, UID, text, null, sku, status().is2xxSuccessful()),
                QuestionDto.class).getId(),
            objectMapper.readValue(
                questionMvc.createModelQuestion(modelId + 1, UID, text + 1, null, sku, status().is2xxSuccessful()),
                QuestionDto.class).getId()
        };
        List<Long> questionIdsList = Arrays.asList(questionIds[0], questionIds[1]);

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
        assertEquals(0, questions.get(1).getAnswers().size());
    }
}
