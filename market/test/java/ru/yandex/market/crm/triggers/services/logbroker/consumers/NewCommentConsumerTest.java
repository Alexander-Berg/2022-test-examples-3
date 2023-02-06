package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.logbroker.LogTypesResolver;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.triggers.services.bpm.TriggerService;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;
import ru.yandex.market.crm.triggers.services.bpm.variables.NewCommentOnAnswer;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NewCommentConsumerTest {

    @Mock
    private MessageSender messageSender;

    @Mock
    private LogTypesResolver logTypes;

    private NewCommentConsumer consumer;

    @Before
    public void before() {
        when(logTypes.getLogIdentifier("pers.newCommentLog"))
                .thenReturn(new LogIdentifier(null, null, LBInstallation.LOGBROKER));
        consumer = new NewCommentConsumer(messageSender, logTypes);
    }

    /**
     * Проверяем, что событие будет отправлено в движок
     */
    @Test
    public void checkMessageIsSent() {
        NewCommentOnAnswer comment = new NewCommentOnAnswer(1, 2, 3, 4);

        // вызов системы
        consumer.accept(Collections.singletonList(comment));

        verify(messageSender).send(argThat(messages -> messages.size() == 1));
    }

    /**
     * Проверяем, что событие не будет перенаправлено в {@link TriggerService}, если автор ответа и автор комментария
     * совпадают
     */
    @Test
    public void checkMesseageIsNotSentIfAuthorAndCommentatorEqual() {
        NewCommentOnAnswer comment = new NewCommentOnAnswer(1, 2, 3, 2);

        // вызов системы
        consumer.accept(Collections.singletonList(comment));

        verify(messageSender).send(argThat(List::isEmpty));
    }


    /**
     * Проверяем, что событие не будет перенаправлено в {@link TriggerService}
     */
    @Test
    public void checkMessagsIsSentForCorrectComments() {
        NewCommentOnAnswer comment1 = new NewCommentOnAnswer(1, 2, 3, 4);
        NewCommentOnAnswer comment2 = new NewCommentOnAnswer(11, 22, 3, 22);
        NewCommentOnAnswer comment3 = new NewCommentOnAnswer(5, 6, 7, 8);
        // вызов системы
        consumer.accept(Arrays.asList(comment1, comment2, comment3));

        verify(messageSender).send(argThat(messages -> messages.size() == 2));
    }

    @Test
    public void bpmMessage() {
        NewCommentOnAnswer comment = new NewCommentOnAnswer(1, 2, 3, 4);

        UidBpmMessage result = consumer.asBpmMessage(comment);

        Assert.assertNotNull(result);
        Assert.assertEquals(MessageTypes.NEW_COMMENT_ON_ANSWER_QUESTION, result.getType());

        Uid uid = result.getUid();
        Assert.assertEquals(String.valueOf(comment.getAnswerAuthorPuid()), uid.getValue());
        Assert.assertEquals(UidType.PUID, uid.getType());

        Map<String, Object> variables = result.getVariables();
        Assert.assertEquals(comment, variables.get(ProcessVariablesNames.Event.NEW_COMMENT_ON_ANSWER));
    }

    @Test
    public void transform() {
        String line = "tskv\tcomment_id=284759\tcomment_uid=13245928475\tanswer_id=2458729457\tanswer_uid=457298475";

        List<NewCommentOnAnswer> result = consumer.transform(line.getBytes());

        Assert.assertThat(
                "Должны получить только один объект т.к. исходное сообщение содержало одну строку",
                result, hasSize(1)
        );

        NewCommentOnAnswer answer = Iterables.get(result, 0);
        Assert.assertEquals(284759L, answer.getCommentId());
        Assert.assertEquals(13245928475L, answer.getCommentAuthorPuid());
        Assert.assertEquals(2458729457L, answer.getAnswerId());
        Assert.assertEquals(457298475L, answer.getAnswerAuthorPuid());
    }
}
