package ru.yandex.autotests.innerpochta.tests.main;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
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

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_OTHER;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TIMELINE_COLLAPSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TIMELINE_ENABLE;

/**
 * @author yaroslavna
 */
@Aqua.Test
@Title("Тесты на таймлайн")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.TIMELINE)
public class TimelineViewTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    private static final String DATE = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    private static final String CALENDAR_CURRENT_WEEK_URL = "https://calendar.yandex.ru/week?show_date=" + DATE;
    private static final String NEW_EVENT_URL = "https://calendar.yandex.ru/event?uid=%s";

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем и разворачиваем таймлайн",
            of(TIMELINE_ENABLE, TRUE, TIMELINE_COLLAPSE, TRUE)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Проверка настройки выключения таймлайна")
    @TestCaseId("2418")
    public void shouldTurnOffTimelineSetting() {
        user.defaultSteps().shouldSee(onMessagePage().timelineBlock())
            .opensFragment(SETTINGS_OTHER)
            .deselects(onOtherSettings().blockSetupOther().topPanel().timeline());
        user.settingsSteps().saveOtherSettingsSetup();
        user.defaultSteps().opensDefaultUrl()
            .shouldNotSee(onMessagePage().timelineBlock());
    }

    @Test
    @Title("Проверка настройки включения таймлайна")
    @TestCaseId("2417")
    public void shouldTurnOnTimelineSetting() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем таймлайн",
            of(TIMELINE_ENABLE, FALSE)
        );
        user.defaultSteps().refreshPage()
            .shouldNotSee(onMessagePage().timelineBlock())
            .opensFragment(SETTINGS_OTHER)
            .turnTrue(onOtherSettings().blockSetupOther().topPanel().timeline());
        user.settingsSteps().saveOtherSettingsSetup();
        user.defaultSteps().opensDefaultUrl()
            .shouldSee(onMessagePage().timelineBlock());
    }

    @Test
    @Title("Переходим в календарь текущей недели по клику на таймлайн")
    @TestCaseId("2434")
    public void shouldSeeCalendar() {
        user.defaultSteps().shouldSee(onMessagePage().timelineBlock())
            .forceClickOn(onMessagePage().timelineBlock().currentTime())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(CALENDAR_CURRENT_WEEK_URL));
    }

    @Test
    @Title("Переходим на страницу создания события в календаре по клику на `Создать событие`")
    @TestCaseId("2437")
    public void shouldSeeCalendarNewEvent() {
        user.defaultSteps().shouldSee(onMessagePage().timelineBlock())
            .clicksOn(onMessagePage().timelineBlock().newEvent())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(String.format(NEW_EVENT_URL, Utils.getUserUid(lock.firstAcc().getLogin())));
    }
}
