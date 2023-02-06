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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.WITH_BIGGER_TEXT;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Включение и выключение крупного шрифта")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.LARGE_FONT)
public class LargeFontTest extends BaseTest {

    public AccLockRule lock = AccLockRule.use().useTusAccount();
    public RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void LogIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Включение крупного шрифта")
    @TestCaseId("2159")
    public void shouldSwitchToBiggerTextMode() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем крупный шрифт",
            of(WITH_BIGGER_TEXT, EMPTY_STR)
        );
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().mail360HeaderBlock().settingsMenu())
            .shouldSee(onMessagePage().mainSettingsPopupNew())
            .turnTrue(onMessagePage().mainSettingsPopupNew().settingsCheckboxes().get(0))
            .shouldBeSelected(onMessagePage().mainSettingsPopupNew().settingsCheckboxes().get(0));
        assertEquals(
            "Настройка «Крупный шрифт» должна быть включена",
            user.apiSettingsSteps().getUserSettings(WITH_BIGGER_TEXT),
            STATUS_ON
        );
    }

    @Test
    @Title("Выключение крупного шрифта")
    @TestCaseId("2159")
    public void shouldSwitchToNormalTextMode() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем крупный шрифт",
            of(WITH_BIGGER_TEXT, STATUS_ON)
        );
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().mail360HeaderBlock().settingsMenu())
            .shouldSee(onMessagePage().mainSettingsPopupNew())
            .deselects(onMessagePage().mainSettingsPopupNew().settingsCheckboxes().get(0))
            .shouldBeDeselected(onMessagePage().mainSettingsPopupNew().settingsCheckboxes().get(0));
        assertEquals(
            "Настройка «Крупный шрифт» должна быть выключена",
            user.apiSettingsSteps().getUserSettings(WITH_BIGGER_TEXT),
            EMPTY_STR
        );
    }
}
