package ru.yandex.autotests.innerpochta.tests.screentests.Attachments;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
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

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MESSAGES;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SPAM_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.TRASH_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.Scrips.SCRIPT_FOR_SCROLL_ATTACHMENTS;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.MARKLABEL;
import static ru.yandex.autotests.innerpochta.util.MailConst.EXCEL_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.LONG_NAME;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.SPECIFIC_FORM;
import static ru.yandex.autotests.innerpochta.util.MailConst.TXT_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WORD_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WRONG_EXTENSION;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_DISABLE_INBOXATTACHS;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Cкриночные тесты на аттачи в списке писем")
@Features(FeaturesConst.ATTACHES)
@Stories(FeaturesConst.MESSAGE_LIST)
@RunWith(DataProviderRunner.class)
public class AttachmentsInMsgListScreenTest {

    private static final String WITH_ATTACHMENTS_LABEL_POSTFIX = "all/only_atta";

    private String subj;

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        subj = getRandomString();
        stepsProd.user().apiLabelsSteps()
            .addNewLabel(getRandomString() + getRandomString(), LABELS_PARAM_GREEN_COLOR);
        stepsProd.user().apiLabelsSteps().addNewLabel(getRandomString(), LABELS_PARAM_GREEN_COLOR);
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(
            acc.firstAcc().getSelfEmail(),
            subj,
            Utils.getRandomString(),
            IMAGE_ATTACHMENT,
            PDF_ATTACHMENT,
            IMAGE_ATTACHMENT,
            PDF_ATTACHMENT
        );
    }

    @Test
    @Title("Стрелочки намекающие на скрол аттачей в списке писем")
    @TestCaseId("737")
    public void shouldSeeArrow() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().messageBlock().arrorNext())
                .shouldNotSee(st.pages().touch().messageList().messageBlock().arrorBack())
                .executesJavaScript(SCRIPT_FOR_SCROLL_ATTACHMENTS)
                .shouldSee(
                    st.pages().touch().messageList().messageBlock().arrorNext(),
                    st.pages().touch().messageList().messageBlock().arrorBack()
                );

        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Выключение настройки «Показывать вложения в списке писем»")
    @TestCaseId("137")
    public void shouldDisableAttachments() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps()
                .shouldSee(st.pages().touch().messageList().messageBlock().clipOnMsg())
                .shouldNotSee(st.pages().touch().messageList().messageBlock().attachmentsInMessageList());

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем показ аттачей в инбоксе",
            of(SETTINGS_DISABLE_INBOXATTACHS, TRUE)
        );
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Вместо аттачей должны видеть иконку скрепки в папке Спам")
    @TestCaseId("182")
    public void shouldNotSeeAttachInSpam() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().messageBlock().clipOnMsg());

        stepsProd.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, SPAM);
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart(SPAM_FOLDER)).run();
    }

    @Test
    @Title("Вместо аттачей должны видеть иконку скрепки в папке Удаленные")
    @TestCaseId("736")
    public void shouldNotSeeAttachInTrash() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().messageBlock().clipOnMsg());

        stepsProd.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, TRASH);
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart(TRASH_FOLDER))
            .run();
    }

    @Test
    @Title("Карусель аттачей смещается под метки")
    @TestCaseId("233")
    public void shouldSeeAttachesUnderLabels() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps()
                .rightSwipe(st.pages().touch().messageList().messages().waitUntil(not(empty())).get(0).subject());
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().messageList().messageBlock().swipeFirstBtn())
                .shouldSee(st.pages().touch().messageList().popup())
                .clicksOnElementWithText(st.pages().touch().messageView().btnsList(), MARKLABEL.btn())
                .clicksOn(
                    st.pages().touch().messageList().popup().labels().waitUntil(not(empty())).get(2),
                    st.pages().touch().messageList().popup().labels().get(1)
                )
                .shouldSeeElementsCount(st.pages().touch().messageList().popup().tick(), 2)
                .clicksOn(st.pages().touch().messageList().popup().done())
                .shouldNotSee(st.pages().touch().messageList().popup())
                .clicksOn(st.pages().touch().messageList().messageBlock().labelMore())
                .shouldSee(st.pages().touch().messageList().messageBlock().label());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(MESSAGES.makeTouchUrlPart(WITH_ATTACHMENTS_LABEL_POSTFIX)).run();
    }

    @Test
    @Title("Отображение аттачей в инбоксе у тредов")
    @TestCaseId("28")
    public void shouldSeeAttachmentsOnThread() {
        String subj2 = getRandomString();
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps()
                .shouldSee(st.pages().touch().messageList().messageBlock().attachmentsInMessageList());

        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsToThreadWithSubject(
            acc.firstAcc().getSelfEmail(),
            subj,
            Utils.getRandomString(),
            IMAGE_ATTACHMENT
        );
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(
            acc.firstAcc().getSelfEmail(),
            subj2,
            Utils.getRandomString(),
            IMAGE_ATTACHMENT
        );
        stepsProd.user().apiMessagesSteps().sendMessageToThreadWithSubject(subj2, acc.firstAcc(), getRandomString());
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Отображение превью аттачей с длинным названием, нестандартного размера, разных форматов")
    @TestCaseId("28")
    public void shouldSeeSpecificAttachmentsInMsgList() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(
                st.pages().touch().messageList().messageBlock().attachmentsInMessageList().waitUntil(not(empty())).get(0)
            );

        stepsProd.user().apiMessagesSteps().deleteAllMessagesInFolder(
            stepsProd.user().apiFoldersSteps().getFolderBySymbol(INBOX)
        );
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(
            acc.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            Utils.getRandomString(),
            WRONG_EXTENSION,
            PDF_ATTACHMENT,
            EXCEL_ATTACHMENT,
            WORD_ATTACHMENT,
            TXT_ATTACHMENT
        );
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(
            acc.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            Utils.getRandomString(),
            SPECIFIC_FORM,
            LONG_NAME
        );
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }
}
