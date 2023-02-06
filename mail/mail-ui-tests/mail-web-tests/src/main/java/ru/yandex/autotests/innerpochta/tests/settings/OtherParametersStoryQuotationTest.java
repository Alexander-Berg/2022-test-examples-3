package ru.yandex.autotests.innerpochta.tests.settings;

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
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_ENABLE_QUOTING;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Цитирование при ответе")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
public class OtherParametersStoryQuotationTest extends BaseTest {

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
    public void logIn() throws InterruptedException {
        user.apiSettingsSteps().callWithListAndParams(
                "Включаем открытие письма в списке писем",
                of(SETTINGS_OPEN_MSG_LIST, TRUE)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_OTHER);
    }

    @Test
    @Title("Выключаем цитирование в 2pane интерфейсе")
    @TestCaseId("1815")
    public void testQuotationIsOff() {
        String subject = Utils.getRandomName();
        String text = Utils.getRandomName();
        user.defaultSteps().deselects(onOtherSettings().blockSetupOther().bottomPanel().enableQuoting());
        user.settingsSteps().saveSettingsIfCanAndClicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(0));
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, text);
        user.defaultSteps().opensFragment(QuickFragments.INBOX).refreshPage();
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().replyBtn());
        user.composeSteps().clicksOnTurnOffHtmlView();
        user.composeSteps().shouldNotSeeTextAreaContains(text);
    }

    @Test
    @Title("Включаем цитирование в 2pane интерфейсе")
    @TestCaseId("1816")
    public void testQuotationIsOn() {
        String subject = Utils.getRandomName();
        String text = Utils.getRandomName();
        user.apiSettingsSteps().callWith(of(SETTINGS_ENABLE_QUOTING, STATUS_ON));
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, text);
        user.defaultSteps().opensFragment(QuickFragments.INBOX).refreshPage();
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().replyBtn());
        user.composeSteps().revealQuotes()
            .shouldSeeTextAreaContains(text);
    }
}
