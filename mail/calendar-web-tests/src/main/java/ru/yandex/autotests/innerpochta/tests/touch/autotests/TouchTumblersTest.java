package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("[Тач] Тесты на тумблеры")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.NEW_EVENT_POPUP)
public class TouchTumblersTest {

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
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Поле доступ: тумблеры зависят друг от друга, если сначала включить «могут редактировать событие»")
    @TestCaseId("1244")
    public void shouldToggleAccessFieldTumblersTogether() {
        steps.user().defaultSteps()
            .clicksOn(steps.user().pages().calTouch().addEventButton())
            .deselects(canEditEventTumbler(), canInviteOthersTumbler())
            .shouldSeeTumblersInState(STATUS_FALSE, canEditEventTumbler(), canInviteOthersTumbler())
            .turnTrue(canEditEventTumbler())
            .shouldSeeTumblersInState(STATUS_TRUE, canInviteOthersTumbler(), canEditEventTumbler())
            .shouldBeDisabled(canInviteOthersTumbler())
            .deselects(canEditEventTumbler())
            .shouldSeeTumblersInState(STATUS_FALSE, canEditEventTumbler(), canInviteOthersTumbler());
    }

    @Test
    @Title("Поле доступ: тумблеры не зависят друг от друга, если сначала включить «могут приглашать других»")
    @TestCaseId("1244")
    public void shouldToggleAccessFieldTumblersIndependently() {
        steps.user().defaultSteps()
            .clicksOn(steps.user().pages().calTouch().addEventButton())
            .deselects(canEditEventTumbler(), canInviteOthersTumbler())
            .turnTrue(canInviteOthersTumbler())
            .shouldBeSelectedTumbler(canInviteOthersTumbler()).shouldBeDeselectedTumbler(canEditEventTumbler())
            .turnTrue(canEditEventTumbler())
            .shouldSeeTumblersInState(STATUS_TRUE, canEditEventTumbler(), canInviteOthersTumbler())
            .deselects(canEditEventTumbler())
            .shouldBeDeselectedTumbler(canEditEventTumbler())
            .shouldBeSelectedTumbler(canInviteOthersTumbler());
    }

    private MailElement canEditEventTumbler() {
        return steps.user().pages().calTouch().eventPage().canEditEventTumbler();
    }

    private MailElement canInviteOthersTumbler() {
        return steps.user().pages().calTouch().eventPage().canInviteOthersTumbler();
    }
}
