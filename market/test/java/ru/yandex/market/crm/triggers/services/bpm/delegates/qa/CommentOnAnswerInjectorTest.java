package ru.yandex.market.crm.triggers.services.bpm.delegates.qa;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.mds.AvatarImageService;
import ru.yandex.market.crm.core.services.report.ReportService;
import ru.yandex.market.crm.external.blackbox.BlackBoxClient;
import ru.yandex.market.crm.external.blackbox.response.UserInfo;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.services.trigger.variables.CommentOnQuestionAnswer;
import ru.yandex.market.crm.triggers.services.bpm.ProcessCancelReason;
import ru.yandex.market.crm.triggers.services.bpm.delegates.DelegateExecutionContext;
import ru.yandex.market.crm.triggers.services.bpm.variables.NewCommentOnAnswer;
import ru.yandex.market.crm.triggers.services.pers.PersAnswerInfo;
import ru.yandex.market.crm.triggers.services.pers.PersCommentInfo;
import ru.yandex.market.crm.triggers.services.pers.PersQaClient;
import ru.yandex.market.crm.triggers.services.pers.PersQuestionInfo;
import ru.yandex.market.mcrm.avatars.HttpAvatarWriteClient;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommentOnAnswerInjectorTest {
    private final NewCommentOnAnswer newCommentOnAnswer =
            new NewCommentOnAnswer(1, 2, 3, 4);
    private CommentOnAnswerInjector injector;
    @Mock
    private PersQaClient qaClient;
    @Mock
    private BlackBoxClient yandexBlackboxClient;
    @Mock
    private ReportService reportService;

    private static DelegateExecutionContext mockCtxWithEvent(NewCommentOnAnswer event) {
        DelegateExecutionContext ctx = mock(DelegateExecutionContext.class);
        when(ctx.getProcessVariable(ProcessVariablesNames.Event.NEW_COMMENT_ON_ANSWER)).thenReturn(event);
        when(ctx.getComponentColor()).thenReturn(Color.GREEN);
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

        injector = new CommentOnAnswerInjector(qaClient, yandexBlackboxClient, reportService, avatarImageService);
    }

    @Test
    public void testCommentIsSetToProcessVariablesIfCorrectData() {
        long modelId = 999;
        DelegateExecutionContext ctx = mockCtxWithEvent(newCommentOnAnswer);

        ModelInfo modelInfo = mockModelInfo(modelId);
        mockQuestionInfo(1, modelId, "testtesttesttestt" +
                "esttesttesttesttesttesttesttesttesttesttesttesttestt" +
                "esttesttesttesttesttesttesttesttesttesttesttesttestt" +
                "esttesttesttesttesttesttesttesttesttesttesttesttestt" +
                "esttesttesttesttesttesttesttesttesttesttesttesttestt" +
                "esttesttesttesttesttesttesttesttesttesttesttesttestt" +
                "esttesttesttesttesttest123456789012");

        mockAuthor("Some Name1", newCommentOnAnswer.getAnswerAuthorPuid(), "someSlug");
        mockAuthor("Some Name2", newCommentOnAnswer.getCommentAuthorPuid(), "someSlug");
        mockAnswerInfo(newCommentOnAnswer.getAnswerId(), 1, "testtesttesttesttesttesttest" +
                "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest" +
                "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest" +
                "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest" +
                "testtesttesttesttesttesttesttesttesttesttest123456789012");

        mockCommentInfo(newCommentOnAnswer.getCommentId(), "NEW");

        injector.doExecute(ctx);

        ArgumentCaptor<CommentOnQuestionAnswer> captor = ArgumentCaptor.forClass(CommentOnQuestionAnswer.class);
        verify(ctx).setProcessVariable(eq(ProcessVariablesNames.COMMENT), captor.capture(), eq(false));

        CommentOnQuestionAnswer actual = captor.getValue();

        assertEquals(modelInfo, actual.getModel());
        assertEquals("testtesttesttestt" +
                        "esttesttesttesttesttesttesttesttesttesttesttesttestt" +
                        "esttesttesttesttesttesttesttesttesttesttesttesttestt" +
                        "esttesttesttesttesttesttesttesttesttesttesttesttestt" +
                        "esttesttesttesttesttesttesttesttesttesttesttesttestt" +
                        "esttesttesttesttesttesttesttesttesttesttesttesttestt" +
                        "esttesttesttesttestt...",
                actual.getQuestionInfo().getText());

        assertEquals(
                "http://yandex.market.ru/product/" + modelId + "/question/1",
                actual.getQuestionInfo().getLink()
        );

        assertEquals("testtesttesttesttesttesttest" +
                        "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest" +
                        "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest" +
                        "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest" +
                        "testtesttesttesttesttesttesttesttesttestt..."
                , actual.getAnswerInfo().getText());
        assertEquals("Some Name1", actual.getAnswerInfo().getAuthorName());

        assertEquals("text2", actual.getCommentInfo().getText());
        assertEquals("Some Name2", actual.getCommentInfo().getAuthorName());
        assertEquals(
                "http://avatars-int.mds.yandex.net:13000/get-yapic/someSlug/islands-retina-50",
                actual.getCommentInfo().getAuthorAvatar()
        );
    }

    @Test
    public void testProcessIsCanceledIfNoNewComment() {

        long modelId = 999;
        DelegateExecutionContext ctx = mockCtxWithEvent(newCommentOnAnswer);

        mockModelInfo(modelId);
        mockQuestionInfo(1, modelId, "text");
        mockAuthor("Some Name", newCommentOnAnswer.getAnswerAuthorPuid(), "someSlug");
        mockAnswerInfo(newCommentOnAnswer.getAnswerId(), 1, "text1");
        mockCommentInfo(newCommentOnAnswer.getCommentId(), "REMOVED");

        injector.doExecute(ctx);

        verify(ctx).cancelProcess(ProcessCancelReason.NOT_NEW_COMMENT);
        verify(ctx, never()).setProcessVariable(eq(ProcessVariablesNames.COMMENT), anyObject(), eq(false));
    }

    @Test
    public void testProcessIsCanceledIfNoCommentInfo() {

        long modelId = 999;
        DelegateExecutionContext ctx = mockCtxWithEvent(newCommentOnAnswer);

        mockModelInfo(modelId);
        mockQuestionInfo(1, modelId, "text");
        mockAuthor("Some Name", newCommentOnAnswer.getAnswerAuthorPuid(), "someSlug");
        mockAnswerInfo(newCommentOnAnswer.getAnswerId(), 1, "text1");

        when(qaClient.getCommentInfo(newCommentOnAnswer.getCommentId())).thenReturn(null);

        injector.doExecute(ctx);

        verify(ctx).cancelProcess(ProcessCancelReason.NOT_NOTIFIABLE_COMMENT);
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

    private ModelInfo mockModelInfo(long modelId) {
        ModelInfo modelInfo = new ModelInfo(String.valueOf(modelId));
        modelInfo.setLink("http://yandex.market.ru/product/" + modelId);
        when(reportService.getProductInfo(modelId, Color.GREEN)).thenReturn(modelInfo);
        return modelInfo;
    }

    private void mockQuestionInfo(long questionId, long modelId, String text) {
        PersQuestionInfo questionInfoDto = new PersQuestionInfo(
                questionId,
                text,
                new PersQuestionInfo.ProductId(modelId)
        );
        when(qaClient.getQuestionInfo(questionId)).thenReturn(questionInfoDto);
    }

    private void mockAnswerInfo(long answerId, long questionId, String text) {
        PersAnswerInfo answerInfoDto = new PersAnswerInfo(answerId, text);
        PersAnswerInfo.QuestionInfo questionInfo = new PersAnswerInfo.QuestionInfo();
        questionInfo.setId(questionId);
        answerInfoDto.setQuestion(questionInfo);
        when(qaClient.getAnswerInfo(answerId)).thenReturn(answerInfoDto);
    }

    private void mockCommentInfo(long commentId, String state) {
        PersCommentInfo persCommentInfo = new PersCommentInfo();
        persCommentInfo.setId(commentId);
        persCommentInfo.setState(state);
        persCommentInfo.setText("text2");
        when(qaClient.getCommentInfo(commentId)).thenReturn(persCommentInfo);
    }
}
