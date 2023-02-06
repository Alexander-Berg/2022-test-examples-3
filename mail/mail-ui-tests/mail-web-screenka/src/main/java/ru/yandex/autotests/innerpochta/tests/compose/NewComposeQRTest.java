package ru.yandex.autotests.innerpochta.tests.compose;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.KeysOwn;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.HEAVY_IMAGE;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MessageHTMLBodyBuilder.messageHTMLBodyBuilder;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Открытие из QR")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class NewComposeQRTest {
    private static String HEAVY_MSG_SUBJECT = "heavy";
    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".js-folders"),
        cssSelector(".qa-MessageViewer-RightColumn")
    );

    private String msgBody = getRandomString();

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withAdditionalIgnoredElements(IGNORE_THIS);

    private String msgSubject = Utils.getRandomString();
    private String bodyText = Utils.getRandomString();

    private String IMAGE_LINK =
        "https://upload.wikimedia.org/wikipedia/commons/thumb/5/54/Sun_white.jpg/1280px-Sun_white.jpg";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, FALSE)
        );
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            lock.firstAcc().getSelfEmail(),
            HEAVY_MSG_SUBJECT,
            messageHTMLBodyBuilder(stepsProd.user()).addTextLine(getRandomString()).addInlineAttach(HEAVY_IMAGE).build()
        );
        stepsProd.user().apiMessagesSteps().sendMail(lock.firstAcc().getSelfEmail(), msgSubject, bodyText);
    }

    @Test
    @Title("Отвечаем на письмо инлайн картинкой в полной форме ответа")
    @TestCaseId("4724")
    public void shouldReplyToMessageInFullFormWithInlineImage() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(msgSubject);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().quickReplyPlaceholder())
                .clicksOn(st.pages().mail().msgView().quickReply().openCompose())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().toolbarBlock().addImage())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().addImageLink())
                .inputsTextInElement(
                    st.pages().mail().composePopup().expandedPopup().addImagePopup().hrefInput(),
                    IMAGE_LINK
                )
                .clicksOn(st.pages().mail().composePopup().expandedPopup().addImagePopup().addLinkBtn())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().sendBtn())
                .opensFragment(QuickFragments.INBOX);
            st.user().messagesSteps().clicksOnMessageWithSubject("Re: " + msgSubject);
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Переносим данные из QR в композ")
    @TestCaseId("3359")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-69602")
    public void shouldTransferAllDataFromQrToCompose() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(msgSubject);
            st.user().defaultSteps().copyScreenInTransferBuffer()
                .clicksOn(st.pages().mail().msgView().qrInFullViewReplyBtn())
                .inputsTextInElement(st.pages().mail().msgView().quickReply().replyText(), msgBody);
            st.user().hotkeySteps().pressCombinationOfHotKeys(
                st.pages().mail().msgView().quickReply().replyText(),
                KeysOwn.key(Keys.CONTROL),
                "v"
            );
            st.user().defaultSteps().waitInSeconds(2);
            st.user().composeSteps().uploadLocalFile(
                    st.pages().mail().msgView().quickReply().localAttachInput(),
                    IMAGE_ATTACHMENT
                )
                .shouldSeeAttachmentName(
                    st.pages().mail().composePopup().expandedPopup().attachPanel().linkedAttach(),
                    0,
                    IMAGE_ATTACHMENT
                );
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Отвечаем на очень большое письмо")
    @TestCaseId("4893")
    public void shouldAnswerOnAVeryBigLetter() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(HEAVY_MSG_SUBJECT);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().quickReplyPlaceholder())
                .clicksOn(st.pages().mail().msgView().quickReply().openCompose())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), bodyText)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().sendBtn())
                .shouldSee(st.pages().mail().composePopup().doneScreen())
                .shouldNotSee(st.pages().mail().composePopup().expandedPopup());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
