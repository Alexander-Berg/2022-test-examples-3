package ru.yandex.market.crm.triggers.services.bpm.delegates.qa;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.report.ReportService;
import ru.yandex.market.crm.external.blackbox.YandexBlackboxClient;
import ru.yandex.market.crm.external.blackbox.response.UserInfo;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.triggers.services.bpm.ProcessCancelReason;
import ru.yandex.market.crm.triggers.services.bpm.delegates.DelegateExecutionContext;
import ru.yandex.market.crm.triggers.services.bpm.variables.NewQuestionOnModel;
import ru.yandex.market.crm.triggers.services.bpm.variables.QuestionOnModel;
import ru.yandex.market.crm.triggers.services.pers.PersQaClient;
import ru.yandex.market.crm.triggers.services.pers.PersQuestionAgitationInfo;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QuestionOnModelInjectorTest {

    private final NewQuestionOnModel newQuestionOnModel = new NewQuestionOnModel(1, "2", 3);
    private QuestionOnModelInjector injector;
    @Mock
    private PersQaClient persQaClient;
    @Mock
    private YandexBlackboxClient blackboxClient;
    @Mock
    private ReportService reportService;

    @Before
    public void before() {
        injector = new QuestionOnModelInjector(persQaClient, blackboxClient, reportService);
    }

    @Test
    public void test() {
        DelegateExecutionContext ctx = mockExecutionContextWithVar(newQuestionOnModel);

        mockQuestionInfo(
                newQuestionOnModel.getQuestionId(),
                "question text",
                1);

        mockQuestionAuthor(
                newQuestionOnModel.getAuthorPuid(),
                "Vasily");

        mockModelInfo(
                newQuestionOnModel.getModelId(),
                "http://yandex.market.ru/product/" + newQuestionOnModel.getModelId());

        injector.doExecute(ctx);

        ArgumentCaptor<QuestionOnModel> questionCaptor = ArgumentCaptor.forClass(QuestionOnModel.class);
        verify(ctx).setProcessVariable(eq(ProcessVariablesNames.QUESTION), questionCaptor.capture(), eq(false));

        QuestionOnModel actual = questionCaptor.getValue();
        assertEquals("2", actual.getModel().getId());

        assertEquals("Vasily", actual.getAuthorName());
        assertEquals("question text", actual.getText());
        assertEquals(
                "http://yandex.market.ru/product/" + newQuestionOnModel.getModelId() + "/question/" + newQuestionOnModel.getQuestionId(),
                actual.getLink()
        );
    }

    @Test
    public void testNotNotifiableQuestion() throws Exception {
        DelegateExecutionContext ctx = mockExecutionContextWithVar(newQuestionOnModel);

        mockQuestionAuthor(
                newQuestionOnModel.getAuthorPuid(),
                "Ivan");

        mockModelInfo(
                newQuestionOnModel.getModelId(),
                "http://yandex.market.ru/product/" + newQuestionOnModel.getModelId());

        injector.doExecute(ctx);

        verify(ctx).cancelProcess(ProcessCancelReason.NOT_NOTIFIABLE_QUESTION);
        verify(ctx, never()).setProcessVariable(eq(ProcessVariablesNames.QUESTION), anyObject(), eq(false));

        // если по вопросу не нужно отправлять письмо пользователю
        mockQuestionInfo(
                newQuestionOnModel.getQuestionId(),
                "Valid text",
                0);

        verify(ctx).cancelProcess(ProcessCancelReason.NOT_NOTIFIABLE_QUESTION);
        verify(ctx, never()).setProcessVariable(eq(ProcessVariablesNames.QUESTION), anyObject(), eq(false));

        mockQuestionInfo(
                newQuestionOnModel.getQuestionId(),
                "Valid text",
                null);

        verify(ctx).cancelProcess(ProcessCancelReason.NOT_NOTIFIABLE_QUESTION);
        verify(ctx, never()).setProcessVariable(eq(ProcessVariablesNames.QUESTION), anyObject(), eq(false));
    }

    private DelegateExecutionContext mockExecutionContextWithVar(NewQuestionOnModel newQuestionOnModel) {
        DelegateExecutionContext mock = mock(DelegateExecutionContext.class);
        when(mock.getProcessVariable(ProcessVariablesNames.Event.NEW_QUESTION_ON_MODEL)).thenReturn(newQuestionOnModel);
        when(mock.getComponentColor()).thenReturn(Color.GREEN);
        return mock;
    }

    private void mockQuestionInfo(long id, String text, Integer forAgitation) {
        PersQuestionAgitationInfo qInfo = new PersQuestionAgitationInfo(id, text, forAgitation);
        when(persQaClient.getQuestionAgitationInfo(qInfo.getId())).thenReturn(qInfo);
    }

    private void mockQuestionAuthor(long puid, String name) {
        UserInfo questionAuthor = new UserInfo();
        questionAuthor.setDisplayName(name);
        when(blackboxClient.getUserInfoByUid(eq(puid), anyObject(), anyObject(), anyObject()))
                .thenReturn(questionAuthor);
    }

    private void mockModelInfo(String id, String link) {
        ModelInfo modelInfo = new ModelInfo(id);
        modelInfo.setLink(link);
        when(reportService.getProductInfo(newQuestionOnModel.getModelId(), Color.GREEN)).thenReturn(modelInfo);
    }
}