package ru.yandex.autotests.innerpochta.tests.setting;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
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

import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_COLLECTORS;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_OTHER;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.BE;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.EN;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.RU;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.TR;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.UK;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Настройки - Прочие")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
@RunWith(DataProviderRunner.class)
public class SettingsOtherTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @DataProvider
    public static Object[][] langs() {
        return new Object[][]{
            {RU}, {EN}, {TR}, {UK}, {BE}
        };
    }

    @Before
    public void setUp() {
        stepsProd.user().apiFoldersSteps().deleteAllCustomFolders()
            .createNewFolder(Utils.getRandomName());
    }

    @Test
    @Title("Проверяем хелп для хоткеев")
    @TestCaseId("2466")
    public void shouldSeeHelpForHotkeys() {
        Consumer<InitStepsRule> actions = st -> st.user().settingsSteps().clicksOnHotKeysInfo();

        parallelRun.withActions(actions).withUrlPath(SETTINGS_OTHER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем выпадушку действий после отправки письма")
    @TestCaseId("2467")
    public void shouldSeeDropdownAfterSend() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsOther()
                    .blockSetupOther().bottomPanel().pageAfterOptionsList().get(0))
                .shouldSee(st.pages().mail().settingsCommon().selectConditionDropdown());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_OTHER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем выпадушку действий после удаления письма")
    @TestCaseId("2468")
    public void shouldSeeDropdownAfterDelete() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsOther()
                    .blockSetupOther().bottomPanel().pageAfterOptionsList().get(1))
                .shouldSee(st.pages().mail().settingsCommon().selectConditionDropdown());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_OTHER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем выпадушку выбора папок для пушей")
    @TestCaseId("3765")
    public void shouldSeePushFoldersDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().settingsOther().blockSetupOther().bottomPanel().folderPushFoldedDropdown())
                .shouldSee(st.pages().mail().settingsCommon().folderListPushDropdown());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_OTHER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка нотифайки о временном отключении рекламы")
    @TestCaseId("4938")
    public void shouldSeeMuteAdsNotify() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSee(st.pages().mail().settingsOther().blockSetupOther().topPanel().showAdvertisementInfo());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_OTHER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Переводим сообщение о включении рекламы через месяц на разные языки")
    @TestCaseId("4940")
    @UseDataProvider("langs")
    public void shouldSeeAdsOffWarning(String lang) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .opensDefaultUrlWithPostFix("/?lang=" + lang)
            .opensFragment(QuickFragments.SETTINGS_OTHER)
            .shouldSee(st.pages().mail().settingsOther().blockSetupOther().topPanel().showAdvertisementInfo());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_OTHER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка страницы «Сбор писем» без подключенных сборщиков")
    @TestCaseId("2874")
    public void shouldNotSeeCollectors() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldNotSee(st.pages().mail().settingsCollectors().blockMain().blockConnected().collectors());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_COLLECTORS).withAcc(lock.firstAcc()).run();
    }
}
