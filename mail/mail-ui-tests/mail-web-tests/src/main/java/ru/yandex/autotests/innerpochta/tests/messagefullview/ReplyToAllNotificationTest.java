package ru.yandex.autotests.innerpochta.tests.messagefullview;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableSortedMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL_2;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FORCE_REPLY;


/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тест всплывающее сообщение «Ответить всем»")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.TOOLBAR)
public class ReplyToAllNotificationTest extends BaseTest {

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final String subject = getRandomString();
    private Message msg;
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiMessagesSteps().sendMailToSeveralReceivers(subject, getRandomString(),
            lock.firstAcc().getSelfEmail(), DEV_NULL_EMAIL, DEV_NULL_EMAIL_2
        );
        msg = user.apiMessagesSteps().getMessageWithSubject(subject);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем показ попапа «Ваш ответ получат не все»",
            of(FORCE_REPLY, FALSE)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .clicksOn(onMessagePage().toolbar().replyButton());
        user.composeSteps().shouldSeeNotificationTab();
    }

    @Test
    @Title("Включаем чекбокс «больше не спрашивать» при ответе нескольким участникам")
    @TestCaseId("1655")
    public void testDoNotAskAgainCheckBoxInNotificationTab() {
        user.defaultSteps().turnTrue(onMessageView().replyNotification().getDoNotAskAgainCheckBox())
            .clicksOn(onMessageView().replyNotification().replyLink())
            .clicksOn(onComposePopup().expandedPopup().closeBtn())
            .opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .clicksOn(onMessagePage().toolbar().replyButton());
        user.composeSteps().shouldNotSeeNotificationTab();
    }

    @Test
    @Title("Тест на кнопку «ответить» и «ответить всем» из попапа при ответе нескольким участникам")
    @TestCaseId("1657")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-67610")
    public void testReplyButtonInNotificationTab() {
        user.defaultSteps().clicksOn(onMessageView().replyNotification().replyLink());
        user.composeSteps().shouldSeeNotificationAboutOtherAddresses();
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .clicksOn(onMessagePage().toolbar().replyButton());
        user.composeSteps().shouldSeeNotificationTab();
        user.defaultSteps().clicksOn(onMessageView().replyNotification().getReplyToAllLink());
        user.composeSteps().shouldNotSeeNotificationAboutOtherAddresses();
    }
}

