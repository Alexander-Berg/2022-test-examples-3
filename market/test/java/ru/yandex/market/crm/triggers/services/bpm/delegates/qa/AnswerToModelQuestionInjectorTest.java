package ru.yandex.market.crm.triggers.services.bpm.delegates.qa;

import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.external.mbi.MbiCLient;
import ru.yandex.market.crm.core.services.external.mbi.PartnerName;
import ru.yandex.market.crm.core.services.mds.AvatarImageService;
import ru.yandex.market.crm.core.services.report.ReportService;
import ru.yandex.market.crm.domain.report.ShopInfo;
import ru.yandex.market.crm.external.blackbox.BlackBoxClient;
import ru.yandex.market.crm.external.blackbox.response.UserInfo;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.services.trigger.variables.AnswerOnModelQuestion;
import ru.yandex.market.crm.triggers.services.bpm.ProcessCancelReason;
import ru.yandex.market.crm.triggers.services.bpm.delegates.DelegateExecutionContext;
import ru.yandex.market.crm.triggers.services.bpm.variables.NewAnswerOnModel;
import ru.yandex.market.crm.triggers.services.cataloger.CatalogerClient;
import ru.yandex.market.crm.triggers.services.cataloger.CatalogerVendorInfo;
import ru.yandex.market.crm.triggers.services.pers.PersAnswerInfo;
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
public class AnswerToModelQuestionInjectorTest {

    private static final String FULL_QUESTION_TEXT = "longlonglonglonglonglonglonglonglonglonglonglonglonglongQuestion";
    private static final String SHORTEN_QUESTION_TEXT = "longlonglonglonglonglonglonglonglonglonglonglon...";

    private static final String FULL_ANSWER_TEXT = "longlonglonglonglonglonglonglonglonglonglonglonglonglongAnswer";
    private static final String SHORTEN_ANSWER_TEXT = "longlonglonglonglonglonglonglonglonglonglonglon...";

    private AnswerToModelQuestionInjector listener;

    @Mock
    private PersQaClient qaClient;

    @Mock
    private BlackBoxClient yandexBlackboxClient;

    @Mock
    private ReportService reportService;

    @Mock
    private MbiCLient mbiCLient;

    @Mock
    private CatalogerClient catalogerClient;

    private final NewAnswerOnModel newAnswerEvent = new NewAnswerOnModel(1, 2, 3, 4);

    private static DelegateExecutionContext mockCtxWithEvent(NewAnswerOnModel event) {
        DelegateExecutionContext ctx = mock(DelegateExecutionContext.class);
        when(ctx.getProcessVariable(ProcessVariablesNames.Event.NEW_ANSWER_ON_MODEL)).thenReturn(event);
        when(ctx.getComponentColor()).thenReturn(Color.GREEN);
        return ctx;
    }

    @Before
    public void before() {
        AvatarImageService avatarImageService = new AvatarImageService(
            new HttpAvatarWriteClient(null, null, "http://avatars-int.mds.yandex.net:13000", null),
            new HttpAvatarWriteClient(null, null, "http://avatars-int.mds.yandex.net:13001", null)
        );
        listener = new AnswerToModelQuestionInjector(qaClient, yandexBlackboxClient, catalogerClient, reportService, mbiCLient,
            avatarImageService);
    }

    @Test
    public void testShopAnswer() {
        long modelId = 999;
        DelegateExecutionContext ctx = mockCtxWithEvent(newAnswerEvent);

        ModelInfo modelInfo = mockModelInfo(modelId);
        mockQuestionInfo(newAnswerEvent.getQuestionId(), modelId, FULL_QUESTION_TEXT);
        mockAnswerAuthorAsShop("Vasily", newAnswerEvent.getAnswerAuthorPuid(), "//avatars.mds.yandex" +
            ".net/get-market-shop-logo/1539910/2a0000016a264e9d62ac02599e28d3e6bd33/orig");
        mockAnswerInfo(newAnswerEvent.getAnswerId(), FULL_ANSWER_TEXT, "shop",
            newAnswerEvent.getAnswerAuthorPuid() + "");

        listener.doExecute(ctx);

        ArgumentCaptor<AnswerOnModelQuestion> captor = ArgumentCaptor.forClass(AnswerOnModelQuestion.class);
        verify(ctx).setProcessVariable(eq(ProcessVariablesNames.ANSWER), captor.capture(), eq(false));

        AnswerOnModelQuestion actual = captor.getValue();

        assertEquals(modelInfo, actual.getModel());
        assertEquals(FULL_QUESTION_TEXT, actual.getQuestionInfo().getText());
        assertEquals(SHORTEN_QUESTION_TEXT, actual.getQuestionInfo().getTextShort());
        assertEquals(
            "http://yandex.market.ru/product/" + modelId + "/question/" + newAnswerEvent.getQuestionId(),
            actual.getQuestionInfo().getLink()
        );

        assertEquals(SHORTEN_ANSWER_TEXT, actual.getAnswerInfo().getText());
        assertEquals("Vasily", actual.getAnswerInfo().getAuthorName());
        assertEquals(
            AnswerToModelQuestionInjector.DUMMY_LOGO_URL,
            actual.getAnswerInfo().getAuthorAvatar()
        );
        assertEquals("shop", actual.getAnswerInfo().getAuthorType());
    }

    @Test
    public void testSupplierAnswer() {
        long modelId = 999;
        DelegateExecutionContext ctx = mockCtxWithEvent(newAnswerEvent);

        ModelInfo modelInfo = mockModelInfo(modelId);
        mockQuestionInfo(newAnswerEvent.getQuestionId(), modelId, FULL_QUESTION_TEXT);
        mockShopInfoAnswer("Ne vasiliy", newAnswerEvent.getAnswerAuthorPuid());
        mockAnswerInfo(newAnswerEvent.getAnswerId(), FULL_ANSWER_TEXT, "shop",
            newAnswerEvent.getAnswerAuthorPuid() + "");

        listener.doExecute(ctx);

        ArgumentCaptor<AnswerOnModelQuestion> captor = ArgumentCaptor.forClass(AnswerOnModelQuestion.class);
        verify(ctx).setProcessVariable(eq(ProcessVariablesNames.ANSWER), captor.capture(), eq(false));

        AnswerOnModelQuestion actual = captor.getValue();

        assertEquals(modelInfo, actual.getModel());
        assertEquals(FULL_QUESTION_TEXT, actual.getQuestionInfo().getText());
        assertEquals(SHORTEN_QUESTION_TEXT, actual.getQuestionInfo().getTextShort());
        assertEquals(
            "http://yandex.market.ru/product/" + modelId + "/question/" + newAnswerEvent.getQuestionId(),
            actual.getQuestionInfo().getLink()
        );

        assertEquals(SHORTEN_ANSWER_TEXT, actual.getAnswerInfo().getText());
        assertEquals("Ne vasiliy", actual.getAnswerInfo().getAuthorName());
        assertEquals(
            AnswerToModelQuestionInjector.DUMMY_LOGO_URL,
            actual.getAnswerInfo().getAuthorAvatar()
        );
        assertEquals("shop", actual.getAnswerInfo().getAuthorType());
    }

    @Test
    public void testVendorAnswerWithoutSquareLogo() {
        long modelId = 999;
        DelegateExecutionContext ctx = mockCtxWithEvent(newAnswerEvent);

        ModelInfo modelInfo = mockModelInfo(modelId);
        mockQuestionInfo(newAnswerEvent.getQuestionId(), modelId, FULL_QUESTION_TEXT);
        mockAnswerAuthorAsVendor("Vasily", newAnswerEvent.getAnswerAuthorPuid(), "beautifulVasilyAvatarSlug", false);
        mockAnswerInfo(newAnswerEvent.getAnswerId(), FULL_ANSWER_TEXT, "vendor",
            newAnswerEvent.getAnswerAuthorPuid() + "");

        listener.doExecute(ctx);

        ArgumentCaptor<AnswerOnModelQuestion> captor = ArgumentCaptor.forClass(AnswerOnModelQuestion.class);
        verify(ctx).setProcessVariable(eq(ProcessVariablesNames.ANSWER), captor.capture(), eq(false));

        AnswerOnModelQuestion actual = captor.getValue();

        assertEquals(modelInfo, actual.getModel());
        assertEquals(FULL_QUESTION_TEXT, actual.getQuestionInfo().getText());
        assertEquals(SHORTEN_QUESTION_TEXT, actual.getQuestionInfo().getTextShort());
        assertEquals(
            "http://yandex.market.ru/product/" + modelId + "/question/" + newAnswerEvent.getQuestionId(),
            actual.getQuestionInfo().getLink()
        );

        assertEquals(SHORTEN_ANSWER_TEXT, actual.getAnswerInfo().getText());
        assertEquals("Vasily", actual.getAnswerInfo().getAuthorName());
        assertEquals(
            AnswerToModelQuestionInjector.DUMMY_LOGO_URL,
            actual.getAnswerInfo().getAuthorAvatar()
        );
        assertEquals("vendor", actual.getAnswerInfo().getAuthorType());
    }

    @Test
    public void testVendorAnswerWithSquareLogo() {
        long modelId = 999;
        DelegateExecutionContext ctx = mockCtxWithEvent(newAnswerEvent);

        ModelInfo modelInfo = mockModelInfo(modelId);
        mockQuestionInfo(newAnswerEvent.getQuestionId(), modelId, FULL_QUESTION_TEXT);
        mockAnswerAuthorAsVendor("Vasily", newAnswerEvent.getAnswerAuthorPuid(), "beautifulVasilyAvatarSlug", true);
        mockAnswerInfo(newAnswerEvent.getAnswerId(), FULL_ANSWER_TEXT, "vendor",
            newAnswerEvent.getAnswerAuthorPuid() + "");

        listener.doExecute(ctx);

        ArgumentCaptor<AnswerOnModelQuestion> captor = ArgumentCaptor.forClass(AnswerOnModelQuestion.class);
        verify(ctx).setProcessVariable(eq(ProcessVariablesNames.ANSWER), captor.capture(), eq(false));

        AnswerOnModelQuestion actual = captor.getValue();

        assertEquals(modelInfo, actual.getModel());
        assertEquals(FULL_QUESTION_TEXT, actual.getQuestionInfo().getText());
        assertEquals(SHORTEN_QUESTION_TEXT, actual.getQuestionInfo().getTextShort());
        assertEquals(
            "http://yandex.market.ru/product/" + modelId + "/question/" + newAnswerEvent.getQuestionId(),
            actual.getQuestionInfo().getLink()
        );

        assertEquals(SHORTEN_ANSWER_TEXT, actual.getAnswerInfo().getText());
        assertEquals("Vasily", actual.getAnswerInfo().getAuthorName());
        assertEquals(
            "https://beautifulVasilyAvatarSlug",
            actual.getAnswerInfo().getAuthorAvatar()
        );
        assertEquals("vendor", actual.getAnswerInfo().getAuthorType());
    }

    @Test
    public void testUserAnswer() {
        long modelId = 999;
        DelegateExecutionContext ctx = mockCtxWithEvent(newAnswerEvent);

        ModelInfo modelInfo = mockModelInfo(modelId);
        mockQuestionInfo(newAnswerEvent.getQuestionId(), modelId, FULL_QUESTION_TEXT);
        mockAnswerAuthorAsUser("Vasily", newAnswerEvent.getAnswerAuthorPuid(), "beautifulVasilyAvatarSlug");
        mockAnswerInfo(newAnswerEvent.getAnswerId(), FULL_ANSWER_TEXT, "user",
            newAnswerEvent.getAnswerAuthorPuid() + "");

        listener.doExecute(ctx);

        ArgumentCaptor<AnswerOnModelQuestion> captor = ArgumentCaptor.forClass(AnswerOnModelQuestion.class);
        verify(ctx).setProcessVariable(eq(ProcessVariablesNames.ANSWER), captor.capture(), eq(false));

        AnswerOnModelQuestion actual = captor.getValue();

        assertEquals(modelInfo, actual.getModel());
        assertEquals(FULL_QUESTION_TEXT, actual.getQuestionInfo().getText());
        assertEquals(SHORTEN_QUESTION_TEXT, actual.getQuestionInfo().getTextShort());
        assertEquals(
            "http://yandex.market.ru/product/" + modelId + "/question/" + newAnswerEvent.getQuestionId(),
            actual.getQuestionInfo().getLink()
        );

        assertEquals(SHORTEN_ANSWER_TEXT, actual.getAnswerInfo().getText());
        assertEquals("Vasily", actual.getAnswerInfo().getAuthorName());
        assertEquals(
            "http://avatars-int.mds.yandex.net:13000/get-yapic/beautifulVasilyAvatarSlug/islands-retina-50",
            actual.getAnswerInfo().getAuthorAvatar()
        );
        assertEquals("user", actual.getAnswerInfo().getAuthorType());
    }

    @Test
    public void testProcessIsCanceledIfNoAnswerInfo() {
        long modelId = 999;
        DelegateExecutionContext ctx = mockCtxWithEvent(newAnswerEvent);

        mockModelInfo(modelId);
        mockQuestionInfo(newAnswerEvent.getQuestionId(), modelId, FULL_ANSWER_TEXT);
        mockAnswerAuthorAsUser("Some Name", newAnswerEvent.getAnswerAuthorPuid(), "someSlug");
        when(qaClient.getAnswerInfo(newAnswerEvent.getAnswerId())).thenReturn(null);

        listener.doExecute(ctx);

        verify(ctx).cancelProcess(ProcessCancelReason.NOT_NOTIFIABLE_ANSWER);
        verify(ctx, never()).setProcessVariable(eq(ProcessVariablesNames.ANSWER), anyObject(), eq(false));
    }

    private void mockAnswerAuthorAsUser(String name, long puid, String avatarSlug) {
        UserInfo answerAuthor = new UserInfo();
        answerAuthor.setPublicName(name);
        answerAuthor.setUid(puid);
        answerAuthor.setAvatarSlug(avatarSlug);
        when(yandexBlackboxClient.getUserInfoByUid(
            eq(puid),
            anyObject(),
            anyObject(),
            eq(ImmutableSet.of(BlackBoxClient.Parameter.PUBLIC_NAME))))
            .thenReturn(answerAuthor);
    }

    private void mockShopInfoAnswer(String name, long puid) {
        PartnerName partnerName = new PartnerName();
        partnerName.setId(puid);
        partnerName.setName(name);
        when(mbiCLient.getPartnerName(
            eq(newAnswerEvent.getAnswerAuthorPuid()))).thenReturn(partnerName);
    }

    private void mockAnswerAuthorAsShop(String name, long puid, String avatar) {
        ShopInfo shopInfo = new ShopInfo();
        shopInfo.setLogo(avatar);
        shopInfo.setShopName(name);
        shopInfo.setId(puid);

        when(reportService.getShopInfo(
            eq(puid + ""), eq(Color.GREEN))).thenReturn(shopInfo);
    }

    private void mockAnswerAuthorAsVendor(String name, long puid, String avatar, boolean withCorrectImg) {
        CatalogerVendorInfo vendorInfo = new CatalogerVendorInfo();
        CatalogerVendorInfo.Logo logo = new CatalogerVendorInfo.Logo();
        logo.setUrl(avatar);

        CatalogerVendorInfo.Thumbnail thumbnail1 = new CatalogerVendorInfo.Thumbnail();
        thumbnail1.setUrl(avatar);
        thumbnail1.setContainerWidth(50);
        thumbnail1.setContainerHeight(50);

        CatalogerVendorInfo.Thumbnail thumbnail2 = new CatalogerVendorInfo.Thumbnail();
        thumbnail2.setUrl(avatar);
        thumbnail2.setContainerWidth(50);
        thumbnail2.setContainerHeight(60);

        logo.setThumbnails(withCorrectImg ? Arrays.asList(thumbnail2, thumbnail1) : Collections.singletonList(thumbnail2));
        CatalogerVendorInfo.Result result = new CatalogerVendorInfo.Result();
        result.setId(puid + "");
        result.setLogo(logo);
        result.setName(name);

        vendorInfo.setResult(result);

        when(catalogerClient.getVendorInfo(
            eq(puid + ""))).thenReturn(vendorInfo);
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

    private void mockAnswerInfo(long answerId, String text, String type, String authorId) {
        PersAnswerInfo answerInfoDto = new PersAnswerInfo(answerId, text);
        PersAnswerInfo.AuthorInfo authorInfo = new PersAnswerInfo.AuthorInfo(type, authorId);
        answerInfoDto.setAuthor(authorInfo);
        when(qaClient.getAnswerInfo(answerId)).thenReturn(answerInfoDto);
    }
}
