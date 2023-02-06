package ru.yandex.market.pers.qa.controller.api.question;

import java.util.List;

import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.dto.CategoryIdDto;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.03.2020
 */
public class QuestionByCategoryCommonControllerTest extends AbstractQuestionCommonControllerTest {

    @Override
    public String doCreateQuestion(long entityId,
                                   long userId,
                                   String text,
                                   ResultMatcher resultMatcher) throws Exception {
        return createCategoryQuestion(entityId, userId, text, resultMatcher);
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
        assertNull(question.getProductIdDto());
        assertNull(question.getInterestIdDto());
        assertNotNull(question.getCategoryIdDto());
        assertEquals(entityId, question.getCategoryIdDto().getId());
        assertEquals(CategoryIdDto.CATEGORY, question.getCategoryIdDto().getEntity());
    }
}
