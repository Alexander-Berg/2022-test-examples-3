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
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;
import ru.yandex.market.crm.triggers.services.bpm.variables.QaCommentEvent;
import ru.yandex.market.crm.triggers.services.pers.PersInternalCommentInfo;
import ru.yandex.market.crm.triggers.services.pers.PersQaClient;
import ru.yandex.market.pers.author.client.PersAuthorClient;
import ru.yandex.market.pers.author.client.api.dto.AuthorIdDto;
import ru.yandex.market.pers.author.client.api.dto.VideoInfoDto;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoList;
import ru.yandex.market.pers.author.client.api.model.VideoUserType;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QaCommentEventConsumerTest {

    @Mock
    private MessageSender messageSender;

    @Mock
    private LogTypesResolver logTypes;

    private QaCommentsConsumer consumer;

    private PersQaClient persQaClient;

    private PersAuthorClient persAuthorClient;

    private static QaCommentEvent mockQaCommentEvent(String event, long project) {
        return new QaCommentEvent(
                event,
                project,
                1,
                (long) 2,
                3,
                4,
                5L,
                "text"
        );
    }

    @Before
    public void before() {
        when(logTypes.getLogIdentifier("pers.newJournalCommentLog"))
                .thenReturn(new LogIdentifier(null, null, LBInstallation.LOGBROKER));
        when(logTypes.getLogIdentifier("pers.newVideoCommentLog"))
            .thenReturn(new LogIdentifier(null, null, LBInstallation.LOGBROKER));

        persQaClient = mock(PersQaClient.class);
        persAuthorClient = mock(PersAuthorClient.class);

        NewQaCommentStrategy newQaCommentStrategy = new NewQaCommentStrategy(persQaClient);
        BanQaCommentStrategy banQaCommentStrategy = new BanQaCommentStrategy();
        NewVideoCommentStrategy newVideoCommentStrategy = new NewVideoCommentStrategy(persAuthorClient);

        consumer = new QaCommentsConsumer(messageSender, newQaCommentStrategy, banQaCommentStrategy,
            newVideoCommentStrategy, logTypes);
    }

    @Test
    public void checkMessageSent() {
        QaCommentEvent comment = mockQaCommentEvent("new_comment", 10);
        consumer.accept(Collections.singletonList(comment));

        verify(messageSender).send(argThat(messages -> !messages.isEmpty()));
    }

    @Test
    public void checkMessageIsNotSentIfIncorrectEventType() {
        QaCommentEvent comment = mockQaCommentEvent("new_comment1", 10);
        consumer.accept(Collections.singletonList(comment));

        verify(messageSender).send(argThat(List::isEmpty));
    }

    @Test
    public void checkMessageIsNotSentIfIncorrectProject() {
        QaCommentEvent comment = mockQaCommentEvent("new_comment", 11);
        consumer.accept(Collections.singletonList(comment));

        verify(messageSender).send(argThat(List::isEmpty));
    }

    @Test
    public void checkTriggerServiceInvokedForCorrectComments() {
        QaCommentEvent comment1 = mockQaCommentEvent("new_comment", 10);
        QaCommentEvent comment2 = mockQaCommentEvent("new_comment", 10);
        QaCommentEvent comment3 = mockQaCommentEvent("new_comment", 11);
        QaCommentEvent comment4 = mockQaCommentEvent("ban_comment", 10);
        QaCommentEvent comment5 = mockQaCommentEvent("new_comment", 13);

        consumer.accept(Arrays.asList(comment1, comment2, comment3, comment4, comment5));

        verify(messageSender).send(argThat(messages -> messages.size() == 4));
    }

    @Test
    public void newJournalCommentAsBpmMessage() {
        QaCommentEvent comment = mockQaCommentEvent("new_comment", 10);

        UidBpmMessage result = consumer.asBpmMessage(comment);

        Assert.assertNotNull(result);
        Assert.assertEquals(MessageTypes.NEW_ANSWER_ON_JOURNAL_COMMENT, result.getType());

        Uid uid = result.getUid();
        Assert.assertEquals(String.valueOf(comment.getParentAuthorId()), uid.getValue());
        Assert.assertEquals(UidType.PUID, uid.getType());

        Map<String, Object> variables = result.getVariables();
        Assert.assertEquals(comment, variables.get(ProcessVariablesNames.Event.JOURNAL_COMMENT));
    }

    @Test
    public void banJournalCommentAsBpmMessage() {
        QaCommentEvent comment = mockQaCommentEvent("ban_comment", 10);

        UidBpmMessage result = consumer.asBpmMessage(comment);

        Assert.assertNotNull(result);
        Assert.assertEquals(MessageTypes.BAN_ON_JOURNAL_COMMENT, result.getType());

        Uid uid = result.getUid();
        Assert.assertEquals(String.valueOf(comment.getAuthorId()), uid.getValue());
        Assert.assertEquals(UidType.PUID, uid.getType());

        Map<String, Object> variables = result.getVariables();
        Assert.assertEquals(comment, variables.get(ProcessVariablesNames.Event.JOURNAL_COMMENT));
    }

    @Test
    public void bpmMessageForIncorrectTypeAndProjectReturnsNull() {
        QaCommentEvent comment = mockQaCommentEvent("ban_comment1", 11);

        Assert.assertNull(consumer.asBpmMessage(comment));
    }

    @Test
    public void transformWithIncorrectTypeAndProjectReturnsEmptyList() {
        String line = "tskv\tevent_type=ba1n_comment\tcomment_id=100062597\tproject=11\tentity_id=51041\tauthor_id" +
                "=484377900\ttext=Vasily P., qwerty";

        List<QaCommentEvent> result = consumer.transform(line.getBytes());
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void transformBanJournalComment() {
        String line = "tskv\tevent_type=ban_comment\tcomment_id=100062597\tproject=10\tentity_id=51041\tauthor_id" +
                "=484377900\ttext=Vasily P., qwerty";

        List<QaCommentEvent> result = consumer.transform(line.getBytes());

        Assert.assertThat(
                "Должны получить только один объект т.к. исходное сообщение содержало одну строку",
                result, hasSize(1)
        );

        QaCommentEvent comment = Iterables.get(result, 0);
        Assert.assertEquals("ban_comment", comment.getEventType());
        Assert.assertEquals(100062597L, comment.getCommentId());
        Assert.assertEquals(10L, comment.getProject());
        Assert.assertEquals(51041L, comment.getEntityId());
        Assert.assertEquals(484377900L, comment.getAuthorId());
        Assert.assertEquals("Vasily P., qwerty", comment.getText());
        Assert.assertNull(comment.getParentAuthorId());
        Assert.assertNull(comment.getParentCommentId());
    }

    @Test
    public void transformNewJournalCommentWithoutParent() {
        String line = "tskv\tevent_type=new_comment\tcomment_id=100062597\tproject=10\tentity_id=51041\tauthor_id" +
                "=484377900\ttext=Vasily P., qwerty";

        List<QaCommentEvent> result = consumer.transform(line.getBytes());

        Assert.assertThat(
                "Должны получить только один объект т.к. исходное сообщение содержало одну строку",
                result,
                hasSize(1)
        );

        QaCommentEvent comment = Iterables.get(result, 0);
        Assert.assertEquals("new_comment", comment.getEventType());
        Assert.assertEquals(100062597L, comment.getCommentId());
        Assert.assertEquals(10L, comment.getProject());
        Assert.assertEquals(51041L, comment.getEntityId());
        Assert.assertEquals(484377900L, comment.getAuthorId());
        Assert.assertEquals("Vasily P., qwerty", comment.getText());
        Assert.assertNull(comment.getParentAuthorId());
        Assert.assertNull(comment.getParentCommentId());
    }

    @Test
    public void transformNewJournalCommentWithParent() {
        PersInternalCommentInfo.AuthorInfo authorInfo = new PersInternalCommentInfo.AuthorInfo();
        authorInfo.setId("1");

        PersInternalCommentInfo commentInfo = new PersInternalCommentInfo();
        commentInfo.setAuthor(authorInfo);

        when(persQaClient.getInternalCommentInfo(123)).thenReturn(commentInfo);
        String line = "tskv\tevent_type=new_comment\tparent_id=123\tcomment_id=100062597\tproject=10\tentity_id=51041" +
                "\tauthor_id=484377900\ttext=Vasily P., qwerty";

        List<QaCommentEvent> result = consumer.transform(line.getBytes());

        Assert.assertThat(
                "Должны получить только один объект т.к. исходное сообщение содержало одну строку",
                result, hasSize(1)
        );

        QaCommentEvent comment = Iterables.get(result, 0);
        Assert.assertEquals("new_comment", comment.getEventType());
        Assert.assertEquals(100062597L, comment.getCommentId());
        Assert.assertEquals(10L, comment.getProject());
        Assert.assertEquals(51041L, comment.getEntityId());
        Assert.assertEquals(484377900L, comment.getAuthorId());
        Assert.assertEquals(Long.valueOf(123), comment.getParentCommentId());
        Assert.assertEquals(Long.valueOf(1), comment.getParentAuthorId());
        Assert.assertEquals("Vasily P., qwerty", comment.getText());
    }

    @Test
    public void newVideoCommentAsBpmMessage() {
        QaCommentEvent comment = mockQaCommentEvent("new_comment", 13);

        UidBpmMessage result = consumer.asBpmMessage(comment);

        Assert.assertNotNull(result);
        Assert.assertEquals(MessageTypes.NEW_VIDEO_COMMENT, result.getType());

        Uid uid = result.getUid();
        Assert.assertEquals(String.valueOf(comment.getParentAuthorId()), uid.getValue());
        Assert.assertEquals(UidType.PUID, uid.getType());

        Map<String, Object> variables = result.getVariables();
        Assert.assertEquals(comment, variables.get(ProcessVariablesNames.Event.VIDEO_COMMENT));
    }

    @Test
    public void checkVideoCommentMessageSent() {
        QaCommentEvent comment = mockQaCommentEvent("new_comment", 13);
        consumer.accept(Collections.singletonList(comment));

        verify(messageSender).send(argThat(messages -> !messages.isEmpty()));
    }

    @Test
    public void checkVideoCommentMessageIsNotSentIfIncorrectEventType() {
        QaCommentEvent comment = mockQaCommentEvent("new_comment1", 13);
        consumer.accept(Collections.singletonList(comment));

        verify(messageSender).send(argThat(List::isEmpty));
    }

    @Test
    public void transformNewVideoComment() {
        VideoInfoDto videoInfoDto = new VideoInfoDto();
        videoInfoDto.setAuthorIdDto(new AuthorIdDto(VideoUserType.UID, "123"));
        when(persAuthorClient.getInternalVideoInfo(List.of(6216L))).thenReturn(new DtoList<>(List.of(videoInfoDto)));
        String line = "tskv\tevent_type=new_comment\tcomment_id=100062597\tproject=13\tentity_id=6216" +
            "\tauthor_id=484377900\ttext=Vasily P., qwerty";

        List<QaCommentEvent> result = consumer.transform(line.getBytes());

        Assert.assertThat(
            "Должны получить только один объект т.к. исходное сообщение содержало одну строку",
            result, hasSize(1)
        );

        QaCommentEvent comment = Iterables.get(result, 0);
        Assert.assertEquals("new_comment", comment.getEventType());
        Assert.assertEquals(100062597L, comment.getCommentId());
        Assert.assertEquals(13L, comment.getProject());
        Assert.assertEquals(6216L, comment.getEntityId());
        Assert.assertEquals(484377900L, comment.getAuthorId());
        Assert.assertNull(comment.getParentCommentId());
        Assert.assertEquals(Long.valueOf(123), comment.getParentAuthorId());
        Assert.assertEquals("Vasily P., qwerty", comment.getText());
    }

}
