package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.layer.Layer;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.AVAILABILITY_BUSY;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.AVAILABILITY_MAYBE_BUSY;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.AVAILABILITY_UNBUSY;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.PURPLE_COLOR;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.RED_COLOR;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.YELLOW_COLOR;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("[Тач] Поле «Статус»")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.NEW_EVENT_POPUP)
public class TouchStatusTest {

    private TouchRulesManager rules = touchRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().createNewLayer(
            steps.user().settingsCalSteps().formDefaultLayer()
                .withColor(PURPLE_COLOR)
                .withAffectsAvailability(TRUE)
                .withIsDefault(TRUE)
        );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Поле «Статус» при изменении календаря должно изменяться")
    @TestCaseId("1056")
    public void shouldChangeStatusAfterChangeCalendar() {
        Layer secondLayer = steps.user().settingsCalSteps().formDefaultLayer()
            .withColor(YELLOW_COLOR)
            .withAffectsAvailability(FALSE);
        Layer thirdLayer = steps.user().settingsCalSteps().formDefaultLayer()
            .withColor(RED_COLOR)
            .withAffectsAvailability(TRUE);

        steps.user().apiCalSettingsSteps().createNewLayer(secondLayer).createNewLayer(thirdLayer);
        steps.user().defaultSteps()
            .refreshPage()
            .clicksOn(steps.pages().cal().touchHome().addEventButton())
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().eventPage().editAvailability(),
                AVAILABILITY_BUSY
            )
            .clicksOn(
                steps.pages().cal().touchHome().eventPage().editAvailability(),
                steps.pages().cal().touchHome().editAvailabilityPage().menuItems().waitUntil(not(empty())).get(1)
            )
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().eventPage().editAvailability(),
                AVAILABILITY_MAYBE_BUSY
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().editLayer())
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().editLayerPage().menuItems().waitUntil(not(empty())),
                secondLayer.getName()
            )
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().eventPage().editAvailability(),
                AVAILABILITY_UNBUSY
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().editAvailability())
            .shouldNotSee(steps.pages().cal().touchHome().editAvailabilityPage())
            .clicksOn(steps.pages().cal().touchHome().eventPage().editLayer())
            .shouldSee(steps.pages().cal().touchHome().editLayerPage().menuItems().waitUntil(not(empty())))
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().editLayerPage().menuItems(),
                thirdLayer.getName()
            )
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().eventPage().editAvailability(),
                AVAILABILITY_MAYBE_BUSY
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().editAvailability())
            .shouldSee(steps.pages().cal().touchHome().editAvailabilityPage());
    }
}
