package ru.yandex.market.pers.qa.tms.questions;

import org.junit.jupiter.api.BeforeEach;
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
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.SecurityData;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.SubscriptionService;
import ru.yandex.market.pers.qa.tms.subs.AutoSubscriptionExecutor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.pers.notify.model.NotificationType.QA_NEW_ANSWERS;
import static ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam.PARAM_QUESTION_ID;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 23.08.2018
 */
public class SubscribeQuestionTest extends PersQaTmsTest {

    private static final long MODEL_ID = 112358;
    private static final long USER_ID = 325581000;

    @Autowired
    private PersNotifyClient persNotifyClient;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private AutoSubscriptionExecutor autoSubscriptionExecutor;

    @Autowired
    protected void resetMocks() {
        PersQaServiceMockFactory.resetMocks();
    }

    @Test
    void testQuestionSubsFail() {
        NotifyServiceMockUtils.mockFailToSubscribe(persNotifyClient);
        final Question question = createQuestion();

        // subs was not processed due to errors in notify -> queue is not empty
        assertEquals(1, getSubsInQueue(question), "subs should not be processed");

        // still not empty even after executor processing
        assertThrows(RuntimeException.class, () -> autoSubscriptionExecutor.subscribeAll());
        assertEquals(1, getSubsInQueue(question), "subs should not be processed");

        // now update mock to be ok
        NotifyServiceMockUtils.mockSubscribeOk(persNotifyClient);

        // should become processed -> queue is empty
        autoSubscriptionExecutor.subscribeAll();
        assertEquals(0, getSubsInQueue(question), "all subs should be processed");
    }

    @Test
    void testQuestionSubsOk() throws Exception {
        NotifyServiceMockUtils.mockSubscribeOk(persNotifyClient);

        final Question question = createQuestion();

        assertSubsOk(testSecData(), question);
    }

    @Test
    void testQuestionSubsOkNoUserAgent() throws Exception {
        // should work as OK
        NotifyServiceMockUtils.mockSubscribeOk(persNotifyClient);

        final SecurityData securityData = testSecDataNoUserAgent();
        final Question question = createQuestion(securityData);

        assertSubsOk(securityData, question);
    }

    private void assertSubsOk(SecurityData sec, Question question) throws PersNotifyClientException {
        final String userAgentExpected = sec.getUserAgent() != null ? sec.getUserAgent() : PersNotifyTag.NO_LIMIT_AGENT;

        final ArgumentCaptor<EmailSubscriptionWriteRequest> arg = ArgumentCaptor
            .forClass(EmailSubscriptionWriteRequest.class);
        Mockito.verify(persNotifyClient).createSubscriptions(arg.capture());

        // processing is ok -> queue is empty
        assertEquals(0, getSubsInQueue(question), "all subs should be processed");

        // check arguments
        final List<EmailSubscription> subsList = (List<EmailSubscription>) arg.getValue().getBody();
        assertEquals(sec.getIp(), arg.getValue().getUserIp());
        assertEquals(userAgentExpected, arg.getValue().getUserAgent());
        assertEquals("a@a.a", arg.getValue().getEmail());
        assertEquals(USER_ID, arg.getValue().getUid().longValue());
        assertEquals(1, subsList.size());
        assertEquals(QA_NEW_ANSWERS, subsList.get(0).getSubscriptionType());
        assertEquals(1, subsList.get(0).getParameters().size());
        assertEquals(question.getId().toString(), subsList.get(0).getParameters().get(PARAM_QUESTION_ID));
    }

    @Test
    void testQuestionSubsWithNoProcessing() {
        NotifyServiceMockUtils.mockFailToSubscribe(persNotifyClient);
        final Question question = createQuestion(testSecDataNoIP());

        // no email is set up -> ok
        assertEquals(0, getSubsInQueue(question), "there should be no subs");
    }

    @Test
    void testQuestionSubsWithNoIp() {
        // should be ok
        autoSubscriptionExecutor.checkForInvalidSubs();

        NotifyServiceMockUtils.mockFailToSubscribe(persNotifyClient);
        final Question question = createQuestion(testSecDataNoIP());

        // no email is set up -> ok
        assertEquals(0, getSubsInQueue(question), "there should be no subs");

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

    private int getSubsInQueue(Question question) {
        return subscriptionService.loadSubsForEntity(QaEntityType.QUESTION, question.getId()).size();
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
