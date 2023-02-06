package ru.yandex.autotests.innerpochta.tests.setting;

import com.google.common.collect.Sets;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.By;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.webcommon.rules.AccountsRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Set;
import java.util.function.Consumer;

import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_SENDER;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_AREAS;

/**
 * @author cosmopanda
 **/
@Aqua.Test
@Title("Настройки - Информация об отправителе")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SettingsSenderTest {

    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".mail-LoadingBar-Container")
    );

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED_AREAS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static AccountsRule account = new AccountsRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Должен быть хелп возле аватарки")
    @TestCaseId("2331")
    public void shouldSeeAvatarHelp() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().settingsSender().blockSetupSender().blockAvatar().helpButton())
            .shouldSee(st.pages().mail().settingsCommon().popup());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должен быть дропдаун выбора языка подписи")
    @TestCaseId("2335")
    public void shouldSeeDropdownChangeLangSignature() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().clicksOnEditSignature();
            st.user().defaultSteps()
                .scrollAndClicksOn(st.pages().mail().settingsSender().blockSetupSender().signatures().editLang())
                .shouldSee(st.pages().mail().settingsSender().languagesDropdown());
        };
        stepsProd.user().apiSettingsSteps().changeSignsAmountTo(1);
        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @TestCaseId("2337")
    @Title("Должна появиться нотификация сохранения подписи")
    public void shouldSeeSaveSignatureNotification() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().clicksOnEditSignature();
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().settingsSender().blockSetupSender().signatures().saveSignature())
                .shouldSee(st.pages().mail().settingsSender().statusLineBlock());
        };
        stepsProd.user().apiSettingsSteps().changeSignsAmountTo(1);
        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должна быть выпадушка адресов в подписи")
    @TestCaseId("2333")
    public void shouldSeeAliasesForNewSignDropdown() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().settingsSender().blockSetupSender().signatures().aliasesList());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должна быть выпадушка доменов")
    @TestCaseId("2334")
    public void shouldSeeDomainDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(
                st.pages().mail().settingsSender().blockSetupSender().blockAliases().domainsList().get(0)
            );

        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @TestCaseId("2656")
    @Title("Должен появиться попап сохранения изменений адреса отправителя")
    public void shouldSeeSaveSignatureChangesPopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .onMouseHoverAndClick(st.pages().mail().settingsSender().blockSetupSender().blockAliases().logins().get(0))
            .clicksOn(st.pages().mail().home().mail360HeaderBlock().serviceIcons().get(0))
            .shouldSee(st.pages().mail().settingsCommon().popup());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc())
            .withIgnoredElements(IGNORE_THIS).run();
    }

    @Test
    @TestCaseId("3580")
    @Title("Должен появиться попап добавления картинки")
    public void shouldSeeAddImagePopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().settingsSender().blockSetupSender().signatures().addImage())
            .shouldSee(st.pages().mail().settingsCommon().addImagePopup());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @TestCaseId("3581")
    @Title("Должен появиться информер «Некорректная ссылка»")
    public void shouldSeeErrorAddImagePopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(
                st.pages().mail().settingsSender().blockSetupSender().signatures().addImage(),
                st.pages().mail().settingsCommon().addImagePopup().addImageButton()
            )
            .shouldSee(st.pages().mail().settingsCommon().addImageError());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @TestCaseId("4096")
    @Title("Настройка выбора подписи должна быть задизейблена")
    public void shouldNotChooseSign() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSee(st.pages().mail().settingsSender().blockSetupSender().signatures());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

    @Test
    @TestCaseId("4154")
    @Title("Должна быть монограмма, если нет аватарки")
    public void shouldSeeMonogram() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSee(st.pages().mail().settingsSender().blockSetupSender().blockAvatar().avatarImg());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_SENDER).withAcc(lock.firstAcc()).run();
    }

}