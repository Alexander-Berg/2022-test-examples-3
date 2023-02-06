package ru.yandex.market.crm.triggers.services.bpm.delegates.qa;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.cms.CmsApiClient;
import ru.yandex.market.crm.core.services.cms.CmsArticle;
import ru.yandex.market.crm.core.services.mds.AvatarImageService;
import ru.yandex.market.crm.external.blackbox.BlackBoxClient;
import ru.yandex.market.crm.external.blackbox.response.UserInfo;
import ru.yandex.market.crm.services.trigger.variables.JournalCommentInfo;
import ru.yandex.market.crm.triggers.services.bpm.ProcessCancelReason;
import ru.yandex.market.crm.triggers.services.bpm.delegates.DelegateExecutionContext;
import ru.yandex.market.crm.triggers.services.bpm.variables.QaCommentEvent;
import ru.yandex.market.crm.triggers.services.pers.PersInternalCommentInfo;
import ru.yandex.market.crm.triggers.services.pers.PersQaClient;
import ru.yandex.market.mcrm.avatars.HttpAvatarWriteClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JournalCommentInjectorTest {
    private final QaCommentEvent comment = new QaCommentEvent(
            "new_comment",
            10,
            1,
            (long) 2,
            3,
            4,
            4L,
            "text"
    );

    private final QaCommentEvent commentWithoutParent = new QaCommentEvent(
            "new_comment",
            10,
            1,
            null,
            3,
            4,
            null,
            "text"
    );

    private JournalCommentInjector injector;
    @Mock
    private PersQaClient qaClient;
    @Mock
    private BlackBoxClient yandexBlackboxClient;
    @Mock
    private CmsApiClient cmsApiClient;

    private static DelegateExecutionContext mockCtxWithEvent(QaCommentEvent event, String state) {
        DelegateExecutionContext ctx = mock(DelegateExecutionContext.class);
        when(ctx.getProcessVariable(ProcessVariablesNames.Event.JOURNAL_COMMENT)).thenReturn(event);
        when(ctx.getProcessVariable(ProcessVariablesNames.JOURNAL_COMMENT_STATE)).thenReturn(state);
        return ctx;
    }

    @Before
    public void before() {
        AvatarImageService avatarImageService = new AvatarImageService(
                new HttpAvatarWriteClient(null,
                        null,
                        "http://avatars-int.mds.yandex.net:13000",
                        null
                ),
                new HttpAvatarWriteClient(null,
                        null,
                        "http://avatars-int.mds.yandex.net:13001",
                        null
                )
        );

        injector = new JournalCommentInjector(qaClient, yandexBlackboxClient, avatarImageService, cmsApiClient);
    }


    @Test
    public void testCommentIsSetToProcessVariablesIfCorrectDataWithParent() {
        DelegateExecutionContext ctx = mockCtxWithEvent(comment, "NEW");

        mockArticle(3l, "text", "type", "id");
        mockAuthor("test", 4l, "slug");
        mockCommentInfo(1, "text1", 1557146510000l, "4", "NEW");
        mockCommentInfo(2, "text2", 1557146510000l, "4", "NEW");

        injector.doExecute(ctx);

        ArgumentCaptor<JournalCommentInfo> captor = ArgumentCaptor.forClass(JournalCommentInfo.class);
        verify(ctx).setProcessVariable(eq(ProcessVariablesNames.JOURNAL_COMMENT), captor.capture(), eq(false));

        JournalCommentInfo actual = captor.getValue();
        assertEquals("text1", actual.getCommentInfo().getText());
        assertEquals("6 мая 2019", actual.getCommentInfo().getDate());
        assertEquals("test", actual.getCommentInfo().getAuthorInfo().getName());
        assertEquals("test", actual.getCommentInfo().getAuthorInfo().getDisplayName());
        assertEquals("http://avatars-int.mds.yandex.net:13000/get-yapic/slug/islands-retina-50",
                actual.getCommentInfo().getAuthorInfo().getAvatar());

        assertEquals("text2", actual.getParentCommentInfo().getText());
        assertEquals("6 мая 2019", actual.getParentCommentInfo().getDate());
        assertEquals("test", actual.getParentCommentInfo().getAuthorInfo().getName());
        assertEquals("test", actual.getParentCommentInfo().getAuthorInfo().getDisplayName());
        assertEquals("http://avatars-int.mds.yandex.net:13000/get-yapic/slug/islands-retina-50",
                actual.getParentCommentInfo().getAuthorInfo().getAvatar());

        assertEquals("text", actual.getArticleInfo().getName());
        assertEquals("https://market.yandex.ru/journal/type/id", actual.getArticleInfo().getLink());
    }

    @Test
    public void testCommentIsSetToProcessVariablesIfCorrectDataWithoutParent() throws Exception {
        DelegateExecutionContext ctx = mockCtxWithEvent(commentWithoutParent, "BANNED");

        mockArticle(3l, "text", "type", "id");
        mockAuthor("test", 4l, "slug");
        mockCommentInfo(1, "text1", 1557146510000l, "4", "BANNED");
        injector.doExecute(ctx);

        ArgumentCaptor<JournalCommentInfo> captor = ArgumentCaptor.forClass(JournalCommentInfo.class);
        verify(ctx).setProcessVariable(eq(ProcessVariablesNames.JOURNAL_COMMENT), captor.capture(), eq(false));

        JournalCommentInfo actual = captor.getValue();
        assertEquals("text1", actual.getCommentInfo().getText());
        assertEquals("6 мая 2019", actual.getCommentInfo().getDate());
        assertEquals("test", actual.getCommentInfo().getAuthorInfo().getName());
        assertEquals("test", actual.getCommentInfo().getAuthorInfo().getDisplayName());
        assertEquals("http://avatars-int.mds.yandex.net:13000/get-yapic/slug/islands-retina-50",
                actual.getCommentInfo().getAuthorInfo().getAvatar());

        assertNull(actual.getParentCommentInfo());

        assertEquals("text", actual.getArticleInfo().getName());
        assertEquals("https://market.yandex.ru/journal/type/id", actual.getArticleInfo().getLink());
    }

    @Test
    public void testProcessIsCanceledIfNoCommentInfo() {
        DelegateExecutionContext ctx = mockCtxWithEvent(comment, "NEW");

        injector.doExecute(ctx);

        verify(ctx).cancelProcess(ProcessCancelReason.NOT_NOTIFIABLE_JOURNAL_COMMENT);
        verify(ctx, never()).setProcessVariable(eq(ProcessVariablesNames.COMMENT), anyObject(), eq(false));
    }

    @Test
    public void testProcessIsCanceledIfNoCommentInfoBecauseOfStatus() {
        DelegateExecutionContext ctx = mockCtxWithEvent(comment, "NEW");
        mockCommentInfo(comment.getCommentId(), "", 1, "1", "BANNED");
        injector.doExecute(ctx);

        verify(ctx).cancelProcess(ProcessCancelReason.NOT_NOTIFIABLE_JOURNAL_COMMENT);
        verify(ctx, never()).setProcessVariable(eq(ProcessVariablesNames.COMMENT), anyObject(), eq(false));
    }

    private void mockAuthor(String name, long puid, String avatarSlug) {
        UserInfo answerAuthor = new UserInfo();
        answerAuthor.setDisplayName(name);
        answerAuthor.setUid(puid);
        answerAuthor.setAvatarSlug(avatarSlug);
        when(yandexBlackboxClient.getUserInfoByUid(eq(puid), anyObject(), anyObject(), anyObject()))
                .thenReturn(answerAuthor);
    }

    private void mockArticle(long id, String name, String type, String semanticId) {
        CmsArticle cmsArticle = new CmsArticle();
        cmsArticle.setName(name);
        cmsArticle.setType(type);
        cmsArticle.setSemanticId(semanticId);

        when(cmsApiClient.getArticleById(id, true)).thenReturn(cmsArticle);
    }

    private void mockCommentInfo(long commentId, String text, long update, String authorId, String state) {
        PersInternalCommentInfo.AuthorInfo authorInfo = new PersInternalCommentInfo.AuthorInfo();
        authorInfo.setId(authorId);

        PersInternalCommentInfo commentInfo = new PersInternalCommentInfo();
        commentInfo.setText(text);
        commentInfo.setAuthor(authorInfo);
        commentInfo.setUpdateTime(update);
        commentInfo.setState(state);
        when(qaClient.getInternalCommentInfo(commentId)).thenReturn(commentInfo);
    }
}
