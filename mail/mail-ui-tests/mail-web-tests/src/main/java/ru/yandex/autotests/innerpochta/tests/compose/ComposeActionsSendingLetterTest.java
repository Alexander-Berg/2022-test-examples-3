package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_NO_REPLY_NOTIFY;


@Aqua.Test
@Title("Новый композ - Тест на отсылку письма с разными галочками")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.ADDITIONAL)
public class ComposeActionsSendingLetterTest extends BaseTest {

    private static final String SEND_NOTIFICATION = "успешно доставлено";

    private String subject;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            SETTINGS_PARAM_NO_REPLY_NOTIFY,
            of(SETTINGS_PARAM_NO_REPLY_NOTIFY, EMPTY_STR)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.COMPOSE);
        subject = Utils.getRandomString();
    }

    @Test
    @Title("Проверка галочки «Напомнить»")
    @TestCaseId("1196")
    public void composeActionsNoreplyCheckbox() {
        user.composeSteps().prepareDraftFor(DEV_NULL_EMAIL, subject, "");
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().notifyBtn())
            .turnTrue(onComposePopup().expandedPopup().notifyPopup().options().get(0))
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().clicksOn(onMessagePage().msgFiltersBlock().waitForAnswer());
        user.messagesSteps().clicksOnMessageWithSubject(subject);
    }

    @Test
    @Title("Проверка галочки «Уведомить»")
    @TestCaseId("1197")
    public void composeActionsNotifyCheckbox() {
        user.composeSteps().prepareDraftFor(lock.firstAcc().getSelfEmail(), subject, "");
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().notifyBtn())
            .turnTrue(onComposePopup().expandedPopup().notifyPopup().options().get(1))
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().opensFragment(QuickFragments.INBOX).refreshPage();
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
        assertTrue(
            "Нет письма с темой «Письмо доставлено»",
            onMessagePage().displayedMessages().list().stream()
                .anyMatch(msg -> msg.subject().getText().contains(SEND_NOTIFICATION))
        );
    }

    @Test
    @Title("Проверка галочки «Отправить позже», используем нижнюю кнопку отправить")
    @TestCaseId("1198")
    public void composeActionsSendTimeCheckBox() {
        user.composeSteps().prepareDraftFor(lock.firstAcc().getSelfEmail(), subject, "");
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().delaySendBtn())
            .shouldSee(onComposePopup().expandedPopup().delaySendPopup())
            .clicksOn(onComposePopup().expandedPopup().delaySendPopup().setTimeDate());
        user.composeSteps().selectDateFromComposeCalendar();
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().opensFragment(QuickFragments.OUTBOX).refreshPage();
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .shouldSeeMessageWithDate(subject, Utils.getTomorrowDate("d MMM"));
    }
}
