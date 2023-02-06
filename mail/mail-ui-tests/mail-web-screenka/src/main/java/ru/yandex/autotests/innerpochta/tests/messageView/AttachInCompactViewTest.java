package ru.yandex.autotests.innerpochta.tests.messageView;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableBiMap.of;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.EXCEL_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.TXT_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WORD_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MessageHTMLBodyBuilder.messageHTMLBodyBuilder;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.HTML;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_OFF;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_TRANSLATE;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Просмотр аттачей в письме")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.ATTACHES)
@RunWith(DataProviderRunner.class)
public class AttachInCompactViewTest {

    private static final String LONG_BODY = StringUtils.repeat("hi ", 3000);
    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private String msgWithAttaches = Utils.getRandomString();
    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    @Rule
    public RuleChain chain = rules.createRuleChain();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);
    private AccLockRule lock = rules.getLock().useTusAccount();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем и выключаем бажный переводчик",
            of(
                SETTINGS_OPEN_MSG_LIST, STATUS_ON,
                SETTINGS_PARAM_TRANSLATE, STATUS_OFF
            )
        );
    }

    @Test
    @Title("Разворачиваем все аттачи")
    @TestCaseId("2702")
    public void shouldSeeAllAttachments() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(msgWithAttaches)
                .shouldSeeAllAttachmentInMsgView();
        };
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(lock.firstAcc().getSelfEmail(),
            msgWithAttaches, "", IMAGE_ATTACHMENT, PDF_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT,
            TXT_ATTACHMENT, IMAGE_ATTACHMENT, IMAGE_ATTACHMENT, IMAGE_ATTACHMENT, IMAGE_ATTACHMENT, IMAGE_ATTACHMENT,
            IMAGE_ATTACHMENT, IMAGE_ATTACHMENT, IMAGE_ATTACHMENT, IMAGE_ATTACHMENT,IMAGE_ATTACHMENT, IMAGE_ATTACHMENT
        );
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Смотрим превью картинки")
    @TestCaseId("2703")
    public void shouldSeeImagePreview() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(msgWithAttaches);
            st.user().defaultSteps()
                .onMouseHover(st.pages().mail().msgView().attachments().list().get(0).imgPreview())
                .clicksOn(st.pages().mail().msgView().attachments().list().get(0).show())
                .shouldSee(st.pages().mail().msgView().imageViewer())
                .shouldNotSee(st.pages().mail().msgView().imageViewerLoader());
        };
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(lock.firstAcc().getSelfEmail(),
            msgWithAttaches, "", IMAGE_ATTACHMENT
        );
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("При просмотре спамного письма картинок нет")
    @TestCaseId("2705")
    public void shouldNotSeeImagesAndLinks() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(msgWithAttaches);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().showHiddenPicturesButton());
        };
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(
            lock.firstAcc().getSelfEmail(),
            msgWithAttaches,
            messageHTMLBodyBuilder(stepsProd.user()).makeBodyWithInlineAttachAndText()
        );
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
        stepsProd.user().apiMessagesSteps()
            .moveMessagesToSpam(stepsProd.user().apiMessagesSteps().getMessageWithSubject(msgWithAttaches));
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(QuickFragments.SPAM).run();
    }

    @Test
    @Title("Включаем картинки в спамном письме")
    @TestCaseId("2706")
    public void shouldSeeImagesAndLinks() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(msgWithAttaches);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().showHiddenPicturesButton())
                .clicksOn(st.pages().mail().msgView().showHiddenPicturesButton())
                .shouldSee(st.pages().mail().msgView().messageTextBlock());
        };
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(
            lock.firstAcc().getSelfEmail(),
            msgWithAttaches,
            messageHTMLBodyBuilder(stepsProd.user()).makeBodyWithInlineAttachAndText()
        );
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
        stepsProd.user().apiMessagesSteps()
            .moveMessagesToSpam(stepsProd.user().apiMessagesSteps().getMessageWithSubject(msgWithAttaches));
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(QuickFragments.SPAM).run();
    }

    @Test
    @Title("Смотрим html письмо")
    @TestCaseId("1078")
    @DataProvider({STATUS_ON, EMPTY_STR})
    public void shouldSeeHtmlMessage(String status) {
        Consumer<InitStepsRule> actions = st ->
            st.user().messagesSteps().clicksOnMessageWithSubject(msgWithAttaches);

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Переключаем просмотр письма на отдельной странице",
            of(SETTINGS_OPEN_MSG_LIST, status)
        );
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(
            lock.firstAcc().getSelfEmail(),
            msgWithAttaches,
            messageHTMLBodyBuilder(stepsProd.user()).addTextLine(HTML).build()
        );
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Смотрим скролбары в треде с широкими письмами в 3pane")
    @TestCaseId("352")
    public void shouldSeeScrollbarsInWideMessages3pane() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(msgWithAttaches);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().msgInThread().get(0));
            st.pages().mail().msgView().msgInThread().waitUntil(empty());
            st.user().defaultSteps().shouldSee(
                st.pages().mail().msgView().shownPictures().toArray(new MailElement[0])
            );
        };

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            lock.firstAcc().getSelfEmail(),
            msgWithAttaches,
            messageHTMLBodyBuilder(stepsProd.user()).makeBodyWithWideInlineAttachAndText()
        );
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Смотрим горизонтальный скролл в письме с широким инлайном в 3pane")
    @TestCaseId("5829")
    public void shouldSeeHorizontalScrollBarInMessageWithWideInline3pane() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(msgWithAttaches);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().shownPictures());
        };
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            lock.firstAcc().getSelfEmail(),
            msgWithAttaches,
            messageHTMLBodyBuilder(stepsProd.user()).makeBodyWithWideInlineAttachAndText()
        );
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
