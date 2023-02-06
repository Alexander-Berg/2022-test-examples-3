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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_COLLECT_ADRESSES;

@Aqua.Test
@Title("Тест на включение/отключение автоматического сбора контактов")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SETTINGS_ABOOK)
public class AbookSettingsStoryCollectContactsTest extends BaseTest {

    private static final String NAME = "testbot2";
    private static final String EMAIL = "testbot2@yandex.ru";
    private static final int TIMEOUT = 15;

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
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_ABOOK);
    }

    @Test
    @Title("Включаем автоматический сбор контактов")
    @TestCaseId("1710")
    public void testEnableAutomaticallyCollectContacts() {
        user.defaultSteps().turnTrue(onAbookSettingsPage().blockSetupAbook().importExportView().autoCollectContacts());
        user.settingsSteps().saveSettingsIfCan();
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE);
        user.composeSteps().inputsAndSendMail(EMAIL, getRandomString(), "");
        user.defaultSteps().opensFragment(QuickFragments.CONTACTS)
            .shouldSee(onAbookPage().toolbarBlock());
        user.abookSteps().shouldSeeContactWithWaiting(NAME, TIMEOUT);
    }

    @Test
    @Title("Отключаем автоматический сбор контактов")
    @TestCaseId("1711")
    public void testDisableAutomaticallyCollectContacts() {
        user.apiSettingsSteps().callWithListAndParams(
            SETTINGS_PARAM_COLLECT_ADRESSES,
            of(SETTINGS_PARAM_COLLECT_ADRESSES, EMPTY_STR)
        );
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE);
        user.composeSteps().inputsAndSendMail(EMAIL, getRandomString(), "");
        user.defaultSteps().opensFragment(QuickFragments.CONTACTS)
            .shouldSee(onAbookPage().toolbarBlock())
            .waitInSeconds(TIMEOUT)
            .refreshPage()
            .shouldSeeElementsCount(user.pages().AbookPage().contacts(), 0);
    }
}
