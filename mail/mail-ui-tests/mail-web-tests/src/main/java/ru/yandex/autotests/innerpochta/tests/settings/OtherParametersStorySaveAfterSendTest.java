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
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.SEND_JSON_PARAM_SAVE_SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Сохранение писем в папке отправленные")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
public class OtherParametersStorySaveAfterSendTest extends BaseTest {

    private String subject;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth).around(clearAcc(() -> user));

    @Before
    public void logIn() throws InterruptedException {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Сохраненяем письма в папке отправленные")
    @TestCaseId("1818")
    public void testEnableSaveAfterSend() {
        user.apiSettingsSteps().callWith(of(SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE));
        user.defaultSteps().refreshPage();
        sentMessageAndOpenSentFolder();
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Сохраненяем письма в папке отправленные (3pane)")
    @TestCaseId("1820")
    public void testEnableSaveAfterSendFor3Pane() {
        user.apiSettingsSteps().callWith(
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL, SEND_JSON_PARAM_SAVE_SENT, STATUS_ON)
        );
        user.defaultSteps().refreshPage();
        sentMessageAndOpenSentFolder();
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    private void sentMessageAndOpenSentFolder() {
        subject = Utils.getRandomName();
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE);
        user.composeSteps().inputsAndSendMail("sendtotestmail@yandex.ru", subject, "");
        user.defaultSteps().opensFragment(QuickFragments.SENT);
    }
}
