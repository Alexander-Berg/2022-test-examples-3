package ru.yandex.autotests.innerpochta.tests.screentests.LeftPanel;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
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
import ru.yandex.qatools.ashot.coordinates.Coords;

import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_DEFAULT_EMAIL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TOUCH_ONBOARDING;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на левую колонку")
@Features(FeaturesConst.LEFT_PANEL)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class GeneralLeftPanelScreenTest {

    private static final Set<Coords> IGNORED_AREA = Sets.newHashSet(
        new Coords(500, 1950, 460, 60)
    );

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount(2));
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private AccLockRule lock2 = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock2);
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED_AREA);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(acc)
        .around(lock2)
        .around(auth2);

    @Before
    public void prepare() {
        stepsProd.user().apiSettingsSteps().withAuth(auth2).callWithListAndParams(
            "Сбрасываем показ онбординга табов",
            of(TOUCH_ONBOARDING, STATUS_ON)
        );
    }

    @Test
    @Title("В левой колонке по нажатию на метелку рядом с «Удаленные» и «Спам» появляется попап")
    @TestCaseId("221")
    @DataProvider({SPAM, TRASH})
    public void shouldSeeClearFolderPopupFromLeftPanel(String folder) {
        Consumer<InitStepsRule> actions = steps ->
            steps.user().defaultSteps().refreshPage()
                .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
                .clicksOn(steps.pages().touch().sidebar().clearToggler())
                .shouldSee(steps.pages().touch().sidebar().clearFolderPopup());

        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(acc.accNum(0), Utils.getRandomString(), "");
        stepsProd.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, folder);
        parallelRun.withActions(actions).withAcc(acc.accNum(0)).run();
    }

    @Test
    @Title("Должны увидеть прыщик в карусели пользователей у неактивного юзера с новым письмом")
    @TestCaseId("313")
    public void shouldSeeUserToggler() {
        Consumer<InitStepsRule> act = st -> {
            st.user().loginSteps().multiLoginWith(lock2.firstAcc());
            st.user().apiMessagesSteps().sendMailWithNoSave(acc.firstAcc(), Utils.getRandomString(), "");
            st.user().defaultSteps().refreshPage()
                .shouldSee(st.pages().touch().messageList().headerBlock())
                .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
                .shouldSee(st.pages().touch().sidebar().userToggler());
        };
        parallelRun.withActions(act).withAcc(acc.accNum(0)).runSequentially();
    }

    @Test
    @Title("Не должен меняться порядок юзеров в карусели при переключении между ними")
    @TestCaseId("995")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldNotChangeOrderOfAccounts() {
        String email = stepsProd.user().apiSettingsSteps().withAuth(auth2)
            .getUserSettings(SETTINGS_PARAM_DEFAULT_EMAIL);
        Consumer<InitStepsRule> act = st -> {
            st.user().loginSteps().multiLoginWith(acc.accNum(0), lock2.firstAcc());
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
                .shouldSee(st.pages().touch().sidebar().sidebarAvatar())
                .shouldContainText(st.pages().touch().sidebar().userEmail(), email)
                .clicksOn(st.pages().touch().sidebar().inactiveMAAccount().get(1))
                .shouldSee(st.pages().touch().sidebar().sidebarAvatar())
                .shouldNotContainText(st.pages().touch().sidebar().userEmail(), email);
        };
        parallelRun.withActions(act).withAcc(acc.accNum(1)).run();
    }

    @Test
    @Title("Не должен меняться порядок юзеров в карусели при переключении между ними")
    @TestCaseId("995")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldNotChangeOrderOfAccountsTablet() {
        String email = stepsProd.user().apiSettingsSteps().withAuth(auth2)
            .getUserSettings(SETTINGS_PARAM_DEFAULT_EMAIL);
        Consumer<InitStepsRule> act = st -> {
            st.user().loginSteps().multiLoginWith(acc.accNum(1), acc.accNum(0), lock2.firstAcc());
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
                .shouldSee(st.pages().touch().sidebar().sidebarAvatar())
                .shouldContainText(st.pages().touch().sidebar().userEmail(), email)
                .clicksOn(st.pages().touch().sidebar().inactiveMAAccount().get(1))
                .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
                .shouldNotContainText(st.pages().touch().sidebar().userEmail(), email);
        };
        parallelRun.withActions(act).run();
    }
}
