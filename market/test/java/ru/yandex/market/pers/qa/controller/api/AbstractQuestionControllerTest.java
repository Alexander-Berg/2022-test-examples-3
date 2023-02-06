package ru.yandex.market.pers.qa.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.dto.QuestionDto;
import ru.yandex.market.pers.qa.mock.SaasMocks;
import ru.yandex.market.pers.qa.model.State;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.utils.Slugifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Common question controller tests logic.
 *
 * @author Ilya Kislitsyn / ilyakis@ / 12.03.2020
 */
public abstract class AbstractQuestionControllerTest extends QAControllerTest {

    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected QuestionService questionService;

    @Autowired
    protected SaasMocks saasMocks;

    protected void checkQuestion(QuestionDto question, long questionId, long entityId, String text) {
        checkQuestion(question, questionId, entityId, UID, text);
    }

    protected void checkQuestion(QuestionDto question, long questionId, long entityId, long userId, String text) {
        assertEquals(questionId, question.getId());
        assertEquals(userId, question.getUserDto().getUserId());
        assertEquals(text, question.getText());
        assertEquals(Slugifier.slugify(question.getText()), question.getHumanReadableUrl());
        checkEntityId(question, entityId);
    }

    public abstract void checkEntityId(QuestionDto question, long entityId);

    protected void checkQuestionState(long id, State expectedState) {
        assertEquals(expectedState, questionService.getQuestionByIdInternal(id).getState());
    }

    protected boolean hasLock(String lockId) {
        final Long count = jdbcTemplate.queryForObject(
                "select count(*) from qa.question_lock where id = ?",
                Long.class,
                lockId
        );
        return count != null && count > 0;
    }

}
