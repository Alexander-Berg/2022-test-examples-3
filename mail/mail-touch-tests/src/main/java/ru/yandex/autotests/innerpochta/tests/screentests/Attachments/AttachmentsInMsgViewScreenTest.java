package ru.yandex.autotests.innerpochta.tests.screentests.Attachments;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DISK_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.MailConst.EXCEL_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.LONG_NAME;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.SPECIFIC_FORM;
import static ru.yandex.autotests.innerpochta.util.MailConst.TXT_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WORD_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WRONG_EXTENSION;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Общие скриночные тесты на аттачи в просмотре письма")
@Features(FeaturesConst.ATTACHES)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AttachmentsInMsgViewScreenTest {

    private static final String SUBJ_DISK = "Аттачи с диска";

    private TouchScreenRulesManager rules = touchScreenRulesManager()
        .withLock(AccLockRule.use().useTusAccount(DISK_USER_TAG));
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

    @Test
    @Title("Аттачи в просмотре письма: разных форматов, с длинными названиями")
    @TestCaseId("741")
    @DataProvider({"0", "1"})
    public void shouldSeeFileIconsInMsgView(int num) {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().messageList().messages().waitUntil(not(empty())).get(num).subject())
                .shouldSee(st.pages().touch().messageView().attachmentsBlock());

        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(
            acc.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            Utils.getRandomString(),
            LONG_NAME,
            SPECIFIC_FORM
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
        parallelRun.withActions(act).withAcc(acc.firstAcc()).runSequentially();
    }

    @Test
    @Title("Аттачи c диска в просмотре письма")
    @TestCaseId("741")
    public void shouldSeeDiskAttachInMsgView() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().refreshPage()
                .clicksOn(st.pages().touch().messageList().messages().waitUntil(not(empty())).get(0))
                .shouldSee(st.pages().touch().messageView().attachmentsBlock());

        stepsTest.user().loginSteps().forAcc(acc.firstAcc()).logins();
        stepsTest.user().touchSteps().sendMsgWithDiskAttaches(acc.firstAcc().getSelfEmail(), SUBJ_DISK, 2);
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Карусель аттачей в просмотре письма можно скролить")
    @TestCaseId("29")
    public void shouldScrollAttachmentsInMsgView() {
        Consumer<InitStepsRule> act = st -> {
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().messageList().messages().waitUntil(not(empty())).get(0).subject())
                .shouldSeeElementsCount(st.pages().touch().messageView().attachments().waitUntil(not(empty())), 9);
            st.user().touchSteps().rightSwipe(st.pages().touch().messageView().attachmentsBlock());
        };
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(
            acc.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            Utils.getRandomString(),
            IMAGE_ATTACHMENT,
            PDF_ATTACHMENT,
            IMAGE_ATTACHMENT,
            PDF_ATTACHMENT,
            IMAGE_ATTACHMENT,
            PDF_ATTACHMENT,
            IMAGE_ATTACHMENT,
            PDF_ATTACHMENT,
            IMAGE_ATTACHMENT
        );
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }
}
