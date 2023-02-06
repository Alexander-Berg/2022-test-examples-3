package ru.yandex.autotests.innerpochta.tests.autotests.event;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.cal.rules.RemoveAllOldAndCreateNewLayer.removeAllOldAndCreateNewLayer;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Общие тесты")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.COMMON)
@RunWith(DataProviderRunner.class)
public class CommonTest {

    private static final String BAD_USER_LOGIN = "yandex-team-298745206";
    private static final String BAD_USER_PASSWORD = "Y0Usha11N0Tpass";
    private static final String IMPORT_CAL = "testcalgoog@gmail.com.ics";
    private static final String HELP_CANT_CREATE_URL = "https://yandex.ru/support/calendar/common/" +
            "troubleshooting.html#cant-create-event";
    private static final String NOTIFICATION_MSG = "События скоро загрузятся";

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
            .around(removeAllOldAndCreateNewLayer(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Включаем показ выходных в сетке",
            new Params().withShowWeekends(true)
        );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Сообщение для пользователя с плохой кармой")
    @TestCaseId("977")
    public void shouldShowMessageForBadUser() {
        steps.user().loginSteps().forAcc(new Account(BAD_USER_LOGIN, BAD_USER_PASSWORD)).logins();
        steps.user().defaultSteps().opensDefaultUrl()
                .clicksOn(steps.pages().cal().home().leftPanel().createEvent())
                .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), DEV_NULL_EMAIL);
        steps.user().hotkeySteps()
                .pressSimpleHotKey(steps.pages().cal().home().newEventPage().membersInput(), key(Keys.ENTER));
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().createFromPageBtn())
                .shouldSee(steps.pages().cal().home().notificationError())
                .clicksOn(steps.pages().cal().home().helpLink())
                .switchOnJustOpenedWindow()
                .shouldBeOnUrl(HELP_CANT_CREATE_URL);
    }

    @Test
    @Title("Импорт из файла в новый календарь")
    @TestCaseId("456")
    public void shouldImportFromFileToNewCal() {
        String name = getRandomName();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().addCal())
                .clicksOn(steps.pages().cal().home().importLink())
                .clicksOn(steps.pages().cal().home().addCalSideBar().fromFileBtn());
        steps.pages().cal().home().addCalSideBar().selectFileBtn()
                .sendKeys(steps.user().defaultSteps().getAttachPath(IMPORT_CAL));
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().addCalSideBar().layerSelect())
                .clicksOn(steps.pages().cal().home().newCalImport())
                .inputsTextInElement(steps.pages().cal().home().addCalSideBar().nameInput(), name)
                .clicksOn(steps.pages().cal().home().addCalSideBar().importBtn())
                .shouldSeeThatElementHasText(steps.pages().cal().home().notificationMessage(), NOTIFICATION_MSG)
                .shouldSeeElementInList(steps.pages().cal().home().leftPanel().layersNames(), name);
    }
}
