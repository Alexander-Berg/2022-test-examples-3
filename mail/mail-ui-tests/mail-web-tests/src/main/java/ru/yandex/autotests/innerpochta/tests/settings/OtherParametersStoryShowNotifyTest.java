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
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.NOTIFY_MESSAGE;

/**
 * Created by cosmopanda
 */
@Aqua.Test
@Title("Показывать уведомления о новых письмах")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
public class OtherParametersStoryShowNotifyTest extends BaseTest {

    private String SENDER_CREDS = "SenderBot";
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private AccLockRule lock_sender = AccLockRule.use().names(SENDER_CREDS);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private RestAssuredAuthRule auth_sender = RestAssuredAuthRule.auth(lock_sender);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(lock_sender)
        .around(auth_sender)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_OTHER);
    }

    @Test
    @Title("Включаем показ нотифаек")
    @TestCaseId("2303")
    public void shouldSeeMessageNotify() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем показ нотификаций",
            of(NOTIFY_MESSAGE, EMPTY_STR)
        );
        user.defaultSteps().refreshPage()
            .turnTrue(onOtherSettings().blockSetupOther().bottomPanel().notifyMessage())
            .clicksOn(onOtherSettings().blockSetupOther().bottomPanel().save());
        user.apiMessagesSteps().withAuth(auth_sender).sendMailWithNoSaveWithoutCheck(
            lock.firstAcc().getSelfEmail(),
            Utils.getRandomName(),
            Utils.getRandomName()
        );
        user.defaultSteps().shouldSee(onMessagePage().notificationEventBlock());
    }

    @Test
    @Title("Выключаем показ нотифаек")
    @TestCaseId("2205")
    public void shouldNotSeeMessageNotify() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем показ нотификаций",
            of(NOTIFY_MESSAGE, STATUS_ON)
        );
        user.defaultSteps().refreshPage()
            .deselects(onOtherSettings().blockSetupOther().bottomPanel().notifyMessage())
            .clicksOn(onOtherSettings().blockSetupOther().bottomPanel().save());
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), Utils.getRandomName());
        user.defaultSteps().shouldNotSee(onMessagePage().notificationEventBlock());
    }
}
