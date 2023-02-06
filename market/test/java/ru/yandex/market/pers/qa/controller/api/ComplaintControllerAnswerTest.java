package ru.yandex.market.pers.qa.controller.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.qa.controller.ControllerTest;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.service.AnswerService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.10.2018
 */
public class ComplaintControllerAnswerTest extends ComplaintControllerTest {

    @Autowired
    private AnswerService answerService;

    @Test
    void testCreateAnswerComplaintUidUnprepared() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long questionId = createQuestion();
        final long answerId = createAnswer(questionId);

        // prepare answer for complaining forbidden
        answerService.forceUpdateModState(answerId, ModState.TOLOKA_UPLOADED);

        boolean isOk = createComplaintByUid(QaEntityType.ANSWER, String.valueOf(answerId), REASON, text);

        // answer was not ready for complaining change in moderation state
        assertFalse(isOk);

        // but complaint was registered anyway
        checkComplaint(UserType.UID, ControllerTest.UID_STR, QaEntityType.ANSWER, answerId);
    }

    @Test
    void testCreateAnswerComplaintUid() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long questionId = createQuestion();
        final long answerId = createAnswer(questionId);

        // prepare answer for complaining
        answerService.forceUpdateModState(answerId, ModState.CONFIRMED);

        boolean isOk = createComplaintByUid(QaEntityType.ANSWER, String.valueOf(answerId), REASON, text);

        assertTrue(isOk);
        checkComplaint(UserType.UID, ControllerTest.UID_STR, QaEntityType.ANSWER, answerId);
    }

    @Test
    void testCreateAnswerComplaintYandexUid() throws Exception {
        final String text = UUID.randomUUID().toString();
        final long questionId = createQuestion();
        final long answerId = createAnswer(questionId);

        // prepare answer for complaining
        answerService.forceUpdateModState(answerId, ModState.CONFIRMED);

        boolean isOk = createComplaintByYandexUid(QaEntityType.ANSWER, String.valueOf(answerId), REASON, text);

        assertTrue(isOk);
        checkComplaint(UserType.YANDEXUID, ControllerTest.YANDEXUID, QaEntityType.ANSWER, answerId);
    }

}
