package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.popup.MaxCollectorCountPopUp;
import ru.yandex.autotests.innerpochta.rules.RetryRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;

@Aqua.Test
@Title("Тест на добавление 11го сборщика почты")
@Description("Тесты на сборщики. У юзера создано 11 коллекторов. Оин из них привязан к папке.")
@Features({FeaturesConst.SETTINGS, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.COLLECTORS)
public class CollectorSettingsMaxCollectorCountTest extends BaseTest {

    private static final int ATTEMPTS = 3;
    private static final String MESSAGE = "Вы действительно хотите удалить сборщик";
    private static final String LOGIN_FOR_COLLECTOR = "testoviy-test109@yandex.ru";
    private static final String PASSWORD = "testoviy";
    private static final String COLLECTOR_NOTIFICATION = "В папке «test» 6 писем. Письма будут удалены" +
        " вместе с папкой.\nТакже папка используется сборщиком. После удаления папки все письма, " +
        "собираемые сборщиком, начнут приходить во «Входящие». Отредактировать сборщик можно в настройках.";

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @Rule
    public AccLockRule lock = AccLockRule.use().className();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RetryRule retry = RetryRule.retry().ifException(Exception.class).every(3, TimeUnit.SECONDS).times(ATTEMPTS);

    @Before
    public void logIn() throws InterruptedException {
        user.defaultSteps().logsOut();
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_COLLECTORS);
    }

    @Test
    @Title("Добавление 11го сборщика")
    @TestCaseId("1718")
    public void testMaxCollectorCountFromSettings() throws Exception {
        user.settingsSteps().inputsTextInPassInputBox(PASSWORD)
            .inputsTextInEmailInputBox(LOGIN_FOR_COLLECTOR);
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockMain().turnOnCollector());
        restartTestIfConditionFailed(
            user.wizardSteps().isPresentPopUpAboutMaxCollectors(),
            "Сообщение о максимальном количестве сборщиков не появилось"
        );
        user.defaultSteps().shouldContainText(
            onCollectorSettingsPage().maxCollectorCountPopUp(),
            MaxCollectorCountPopUp.TEXT
        );
        user.defaultSteps().clicksOn(onCollectorSettingsPage().maxCollectorCountPopUp().closePopUpButton())
            .shouldNotSee(onCollectorSettingsPage().maxCollectorCountPopUp().closePopUpButton());
        user.settingsSteps().shouldSeeCollectorCount(10);
    }

    @Test
    @Title("Удаление папки используемой сборщиком")
    @TestCaseId("1719")
    public void testDeleteFolderConnectedToCollectorPopUp() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS);
        user.settingsSteps().clicksOnFolder("test");
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().deleteCustomFolder())
            .shouldSee(onFoldersAndLabelsSetup().deleteFolderPopUp())
            .shouldSeeThatElementTextEquals(
                onFoldersAndLabelsSetup().deleteFolderPopUp().notification(),
                COLLECTOR_NOTIFICATION
            )
            .clicksOn(onFoldersAndLabelsSetup().deleteFolderPopUp().collectorSettingsLink())
            .shouldBeOnUrl(containsString("#setup/collectors"));
    }

    @Test
    @Title("Настройка существующего сборщика")
    @TestCaseId("1720")
    public void testConfigureCollectorLink() {
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockSetup().toggleServerSettings().get(1))
            .clicksOn(onCollectorSettingsPage().blockSetup().delete().get(1))
            .shouldSee(onCollectorSettingsPage().deleteCollectorPopUp());
    }

    @Test
    @Title("Отключение и подключение сборщика")
    @TestCaseId("1722")
    public void testSwitchOnAndOffCollector() {
        user.defaultSteps()
            .shouldSee(onCollectorSettingsPage().blockMain().blockConnected().collectors().get(0).switcher());
        user.settingsSteps()
            .turnSwitchOn(onCollectorSettingsPage().blockMain().blockConnected().collectors().get(0).switcher())
            .shouldSeeSwitchOn(onCollectorSettingsPage().blockMain().blockConnected().collectors().get(0).switcher())
            .turnSwitchOff(onCollectorSettingsPage().blockMain().blockConnected().collectors().get(0).switcher())
            .shouldSeeSwitchOff(onCollectorSettingsPage().blockMain().blockConnected().collectors().get(0).switcher());
    }

    @Test
    @Title("Попап удаления сборщика")
    @TestCaseId("1723")
    public void testDeleteCollectorPopUp() {
        user.settingsSteps().clicksOnCollector(0);
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockMain().blockConnected().collectors().get(0)
            .deleteMailboxBtn())
            .shouldSee(onCollectorSettingsPage().deleteCollectorPopUp())
            .shouldContainText(onCollectorSettingsPage().deleteCollectorPopUp().message(), MESSAGE)
            .clicksOn(onCollectorSettingsPage().deleteCollectorPopUp().cancelBtn())
            .shouldNotSee(onCollectorSettingsPage().deleteCollectorPopUp());
    }

    private void restartTestIfConditionFailed(boolean condition, String reason) throws Exception {
        if (!condition) {
            if (retry.getCurrentCount() < ATTEMPTS) {
                throw new Exception(reason);
            } else {
                throw new RuntimeException(reason);
            }
        }
    }
}
