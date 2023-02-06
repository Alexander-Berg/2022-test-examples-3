package ru.yandex.autotests.innerpochta.tests.autotests.grid;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.TZ_POPUP;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Общие тесты")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.GENERAL)
public class GeneralTest {

    private static final String OLD_TZ = "UTC+06:00";
    private static final String TZ_OMSK = "Asia/Omsk";

    private CalendarRulesManager rules = calendarRulesManager().withLock(AccLockRule.use().annotation());
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void login() {
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Меняем таймзону из попапа")
    @TestCaseId("609")
    public void shouldChangeTimeZoneFromPopup() {
        steps.user().apiCalSettingsSteps()
            .updateUserSettings("Меняем таймзону на Омск", new Params().withTz(TZ_OMSK));
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(TZ_POPUP.fragment())
            .shouldSee(steps.pages().cal().home().warningPopup());
        String newTimezone = steps.pages().cal().home().warningPopup().getText().substring(32, 41);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().warningPopup().agreeBtn())
            .shouldNotSee(steps.pages().cal().home().warningPopup())
            .shouldHasText(steps.pages().cal().home().leftPanel().changeTime(), newTimezone);
    }

    @Test
    @Title("Не меняем таймзону из попапа")
    @TestCaseId("610")
    public void shouldCloseTimeZonePopup() {
        steps.user().apiCalSettingsSteps()
            .updateUserSettings("Меняем таймзону на Омск", new Params().withTz(TZ_OMSK));
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(TZ_POPUP.fragment())
            .shouldSee(steps.pages().cal().home().warningPopup())
            .clicksOn(steps.pages().cal().home().warningPopup().cancelBtn())
            .shouldNotSee(steps.pages().cal().home().warningPopup())
            .shouldSeeThatElementHasText(steps.pages().cal().home().leftPanel().changeTime(), OLD_TZ);
    }
}
