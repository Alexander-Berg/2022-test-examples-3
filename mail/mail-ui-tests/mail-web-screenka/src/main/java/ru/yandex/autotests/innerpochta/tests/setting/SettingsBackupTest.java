package ru.yandex.autotests.innerpochta.tests.setting;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
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
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL360_PAID;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Почта 360 - Резервная копия")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SETTINGS)
@RunWith(DataProviderRunner.class)
public class SettingsBackupTest {

    private static final String FOLDER_NAME = getRandomName();
    private static final String SUBFOLDER_NAME = getRandomName();

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount(MAIL360_PAID);
    private RunAndCompare parallelRun = runAndCompare()
        .withProdSteps(stepsProd)
        .withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Верстка страницы настроек резервной копии у пользователя с подпиской")
    @TestCaseId("6056")
    public void shouldSeeBackupSettingsPageWithSubscription() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().opensFragment(QuickFragments.SETTINGS_BACKUP)
                .shouldSee(
                    st.pages().mail().settingsBackup().foldersToggler(),
                    st.pages().mail().settingsBackup().enotBlock()
                );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка страницы настроек резервной копии у пользователя с подпиской c развернутым тогглером")
    @TestCaseId("6056")
    public void shouldSeeBackupSettingsPageWithSubscriptionAndExpandedToggler() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().opensFragment(QuickFragments.SETTINGS_BACKUP)
                .clicksOn(st.pages().mail().settingsBackup().foldersToggler())
                .shouldSee(
                    st.pages().mail().settingsBackup().enotBlock(),
                    st.pages().mail().settingsBackup().foldersBlock(),
                    st.pages().mail().settingsBackup().createBtn(),
                    st.pages().mail().settingsBackup().lastCopyInfo()
                );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выбираем пользовательские папки для резервной копии")
    @TestCaseId("6065")
    public void shouldSeeAllUserFolders() {
        Folder parentFolder = stepsProd.user().apiFoldersSteps().createNewFolder(FOLDER_NAME);
        stepsProd.user().apiFoldersSteps().createNewSubFolder(SUBFOLDER_NAME, parentFolder);
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 3);

        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().opensFragment(QuickFragments.SETTINGS_BACKUP)
                .refreshPage()
                .clicksOn(st.pages().mail().settingsBackup().foldersToggler())
                .shouldContainText(st.pages().mail().settingsBackup().folders().get(1), FOLDER_NAME)
                .shouldContainText(st.pages().mail().settingsBackup().folders().get(2), SUBFOLDER_NAME)
                .turnTrue(st.pages().mail().settingsBackup().foldersCheckBoxes().get(1))
                .turnTrue(st.pages().mail().settingsBackup().foldersCheckBoxes().get(2))
                .clicksOn(st.pages().mail().settingsBackup().foldersApplyBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

}
