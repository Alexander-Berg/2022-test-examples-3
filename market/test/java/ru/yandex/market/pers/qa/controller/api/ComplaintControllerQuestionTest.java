package ru.yandex.market.pers.qa.controller.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.qa.controller.ControllerTest;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.service.QuestionService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.10.2018
 */
public class ComplaintControllerQuestionTest extends ComplaintControllerTest {

    @Autowired
    private QuestionService questionService;

    @Test
    void testCreateQuestionComplaintUidUnprepared() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long questionId = createQuestion();

        boolean isOk = createComplaintByUid(QaEntityType.QUESTION, String.valueOf(questionId), REASON, text);

        // question was not ready for complaining change in moderation state
        assertFalse(isOk);

        // but complaint was registered anyway
        checkComplaint(UserType.UID, ControllerTest.UID_STR, QaEntityType.QUESTION, questionId);
    }

    @Test
    void testCreateQuestionComplaintUid() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long questionId = createQuestion();

        // prepare question for complaining
        questionService.forceUpdateModState(questionId, ModState.CONFIRMED);

        boolean isOk = createComplaintByUid(QaEntityType.QUESTION, String.valueOf(questionId), REASON, text);

        assertTrue(isOk);
        checkComplaint(UserType.UID, ControllerTest.UID_STR, QaEntityType.QUESTION, questionId);
    }

    @Test
    void testCreateQuestionComplaintYandexUid() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long questionId = createQuestion();

        // prepare question for complaining
        questionService.forceUpdateModState(questionId, ModState.CONFIRMED);

        boolean isOk = createComplaintByYandexUid(QaEntityType.QUESTION, String.valueOf(questionId), REASON, text);

        assertTrue(isOk);
        checkComplaint(UserType.YANDEXUID, ControllerTest.YANDEXUID, QaEntityType.QUESTION, questionId);
    }
}
