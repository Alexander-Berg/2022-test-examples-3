package ru.yandex.autotests.innerpochta.tests.messagefullview;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;


/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тест на кнопку «Ответить Всем» в просмотре письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.TOOLBAR)
public class ReplyCcButtonTest extends BaseTest {

    private static final String ADDRESS = "xivatesting@yandex.ru";
    private static final String ADDRESS2 = "testingxiva@yandex.ru";
    private static final String ADDRESS3 = "handlerstest@yandex.ru";
    private static final String AVA = "\nTE\n";
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private Message msg;
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем треды у пользователя",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Тест на кнопку «Ответить всем» для одного получателя c СС")
    @Description("Жмём ответить, смотрим, что заполнились поля кому, тема, СС и текст.")
    @TestCaseId("1651")
    public void messageViewReplyToSenderCc() {
        msg = user.apiMessagesSteps().addCcEmails(ADDRESS).addBccEmails(lock.firstAcc().getSelfEmail())
            .sendMailWithCcAndBcc(ADDRESS2, Utils.getRandomName(), Utils.getRandomString());
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
        clicksOnReplyAllButtonAndChecksComposePage(ADDRESS2, ADDRESS);
    }

    @Test
    @Title("Тест на кнопку «Ответить всем» для двух получателей c СС")
    @Description("Жмём ответить, смотрим, что заполнились поля кому, тема, СС и текст.")
    @TestCaseId("1652")
    public void messageViewReplyToSenderRecipientCc() {
        msg = user.apiMessagesSteps().addCcEmails(ADDRESS).addBccEmails(lock.firstAcc().getSelfEmail())
            .sendMailWithCcAndBcc(ADDRESS3 + ", " + ADDRESS2, Utils.getRandomName(), Utils.getRandomString());
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
        clicksOnReplyAllButtonAndChecksComposePage(ADDRESS3 + AVA + ADDRESS2, ADDRESS);
    }

    @Test
    @Title("Тест на кнопку «Ответить всем» для одного получателя и два в СС")
    @Description("Жмём ответить, смотрим, что заполнились поля кому, тема, СС и текст.")
    @TestCaseId("1653")
    public void testCcMessageReplyToSenderAndRecipientAndCC() {
        msg = user.apiMessagesSteps().addCcEmails(ADDRESS, ADDRESS2)
            .addBccEmails(lock.firstAcc().getSelfEmail())
            .sendMailWithCcAndBcc(ADDRESS3, Utils.getRandomName(), Utils.getRandomString());
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
        clicksOnReplyAllButtonAndChecksComposePage(ADDRESS3, ADDRESS + AVA + ADDRESS2);
    }

    @Test
    @Title("Тест на кнопку «Ответить всем» для одного получателя, СС и BCC")
    @Description("Жмём ответить, смотрим, что заполнились поля кому, тема, СС и текст.")
    @TestCaseId("1654")
    public void testBccMessageReplyToSenderAndRecipientAndCC() {
        msg = user.apiMessagesSteps().addCcEmails(ADDRESS2).addBccEmails(lock.firstAcc().getSelfEmail())
            .sendMailWithCcAndBcc(ADDRESS, Utils.getRandomName(), Utils.getRandomString());
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
        clicksOnReplyAllButtonAndChecksComposePage(ADDRESS, ADDRESS2);
    }

    private void clicksOnReplyAllButtonAndChecksComposePage(String sendTo, String cc) {
        user.defaultSteps().clicksOn(onMessageView().toolbar().replyToAllButton());
        user.composeSteps().shouldSeeTextAreaContains(msg.getFirstline())
            .shouldSeeSubject("Re: " + msg.getSubject());
        user.defaultSteps().shouldContainText(onComposePopup().expandedPopup().popupTo(), sendTo)
            .shouldContainText(onComposePopup().expandedPopup().popupCc(), cc)
            .clicksOn(onComposePopup().expandedPopup().sendBtn())
            .opensFragment(QuickFragments.SENT);
        user.messagesSteps().shouldSeeMessageWithSubject("Re: " + msg.getSubject());
    }
}
