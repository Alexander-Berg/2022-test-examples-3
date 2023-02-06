package ru.yandex.market.pers.qa.tms.questions;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.EmailSubscriptionWriteRequest;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.web.PersNotifyTag;
import ru.yandex.market.pers.qa.PersQaServiceMockFactory;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.exception.QaRuntimeException;
import ru.yandex.market.pers.qa.mock.NotifyServiceMockUtils;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.SecurityData;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.SubscriptionService;
import ru.yandex.market.pers.qa.tms.subs.AutoSubscriptionExecutor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.pers.notify.model.NotificationType.QA_NEW_COMMENTS;
import static ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam.PARAM_ANSWER_ID;

/**
 * @author vvolokh
 */
public class SubscribeAnswerTest extends PersQaTmsTest {

    private static final long MODEL_ID = 112358;
    private static final long USER_ID = 325581000;

    @Autowired
    private PersNotifyClient persNotifyClient;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private AutoSubscriptionExecutor autoSubscriptionExecutor;

    @Override
    protected void resetMocks() {
        PersQaServiceMockFactory.resetMocks();
    }

    @Test
    void testAnswerSubsFail() {
        NotifyServiceMockUtils.mockFailToSubscribe(persNotifyClient);
        final Question question = createQuestion();
        final Answer answer = createAnswer(question.getId());

        // subs was not processed due to errors in notify -> queue is not empty
        assertEquals(1, getSubsInQueue(answer), "subs should not be processed");

        // still not empty even after executor processing
        assertThrows(RuntimeException.class, () -> autoSubscriptionExecutor.subscribeAll());
        assertEquals(1, getSubsInQueue(answer), "subs should not be processed");

        // now update mock to be ok
        NotifyServiceMockUtils.mockSubscribeOk(persNotifyClient);

        // should become processed -> queue is empty
        autoSubscriptionExecutor.subscribeAll();
        assertEquals(0, getSubsInQueue(answer), "all subs should be processed");
    }

    @Test
    void testAnswerSubsOk() throws Exception {
        NotifyServiceMockUtils.mockSubscribeOk(persNotifyClient);

        final Question question = createQuestion();
        Mockito.clearInvocations(persNotifyClient);
        final Answer answer = createAnswer(question.getId());

        assertSubsOk(testSecData(), answer);
    }

    @Test
    void testAnswerSubsOkNoUserAgent() throws Exception {
        // should work as OK
        NotifyServiceMockUtils.mockSubscribeOk(persNotifyClient);

        final SecurityData securityData = testSecDataNoUserAgent();
        final Question question = createQuestion(securityData);
        Mockito.clearInvocations(persNotifyClient);
        final Answer answer = createAnswer(question.getId(), securityData);

        assertSubsOk(securityData, answer);
    }

    private void assertSubsOk(SecurityData sec, Answer answer) throws PersNotifyClientException {
        final String userAgentExpected = sec.getUserAgent() != null ? sec.getUserAgent() : PersNotifyTag.NO_LIMIT_AGENT;

        final ArgumentCaptor<EmailSubscriptionWriteRequest> arg = ArgumentCaptor
            .forClass(EmailSubscriptionWriteRequest.class);
        Mockito.verify(persNotifyClient).createSubscriptions(arg.capture());

        // processing is ok -> queue is empty
        assertEquals(0, getSubsInQueue(answer), "all subs should be processed");

        // check arguments
        final List<EmailSubscription> subsList = (List<EmailSubscription>) arg.getValue().getBody();
        assertEquals(sec.getIp(), arg.getValue().getUserIp());
        assertEquals(userAgentExpected, arg.getValue().getUserAgent());
        assertEquals("a@a.a", arg.getValue().getEmail());
        assertEquals(USER_ID, arg.getValue().getUid().longValue());
        assertEquals(1, subsList.size());
        assertEquals(QA_NEW_COMMENTS, subsList.get(0).getSubscriptionType());
        assertEquals(1, subsList.get(0).getParameters().size());
        assertEquals(String.valueOf(answer.getId()), subsList.get(0).getParameters().get(PARAM_ANSWER_ID));
    }

    @Test
    void testAnswerSubsWithNoProcessing() {
        NotifyServiceMockUtils.mockFailToSubscribe(persNotifyClient);
        final Question question = createQuestion(testSecDataNoIP());
        final Answer answer = createAnswer(question.getId(), testSecDataNoIP());

        // no email is set up -> ok
        assertEquals(0, getSubsInQueue(answer), "there should be no subs");
    }

    @Test
    void testAnswerSubsWithNoIp() {
        // should be ok
        autoSubscriptionExecutor.checkForInvalidSubs();

        NotifyServiceMockUtils.mockFailToSubscribe(persNotifyClient);
        final Question question = createQuestion(testSecDataNoIP());
        final Answer answer = createAnswer(question.getId(), testSecDataNoIP());

        // no email is set up -> ok
        assertEquals(0, getSubsInQueue(answer), "there should be no subs");

        assertThrows(QaRuntimeException.class, () -> autoSubscriptionExecutor.checkForInvalidSubs());
    }

    private Question createQuestion() {
        return createQuestion(testSecData());
    }

    private Question createQuestion(SecurityData sec) {
        return questionService.createQuestion(
            Question.buildModelQuestion(USER_ID, UUID.randomUUID().toString(), MODEL_ID),
            sec
        );
    }

    private Answer createAnswer(long questionId) {
        return createAnswer(questionId, testSecData());
    }

    private Answer createAnswer(long questionId, SecurityData sec) {
        return answerService.createAnswer(Answer.buildBasicAnswer(USER_ID, UUID.randomUUID().toString(), questionId), sec);
    }

    private int getSubsInQueue(Answer answer) {
        return subscriptionService.loadSubsForEntity(QaEntityType.ANSWER, answer.getId()).size();
    }

    private SecurityData testSecData() {
        final SecurityData sec = new SecurityData();
        sec.setIp("127.0.0.1");
        sec.setUserAgent("Some user agent");
        return sec;
    }

    private SecurityData testSecDataNoIP() {
        final SecurityData sec = new SecurityData();
        return sec;
    }

    private SecurityData testSecDataNoUserAgent() {
        final SecurityData sec = new SecurityData();
        sec.setIp("127.0.0.1");
        return sec;
    }

}
