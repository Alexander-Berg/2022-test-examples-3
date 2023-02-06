package ru.yandex.autotests.innerpochta.tests.touch.screentests;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collections;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.INVITE_BUTTON_MAYBE;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.INVITE_BUTTON_NO;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.INVITE_BUTTON_YES;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("[Тач] Вёрстка элементов, связанных с приглашениями на события")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.VIEW_EVENT_POPUP)
@RunWith(DataProviderRunner.class)
public class TouchInvitesScreenTest {

    private static final int SIMPLE_EVENT_NUM = 0;
    private static final int REGULAR_EVENT_NUM = 1;

    private TouchScreenRulesManager rules = touchScreenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().usePreloadedTusAccounts(2);
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock.accNum(1));
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(auth2)
        .around(clearAcc(() -> stepsProd.user()));

    @Before
    public void setUp() {
        stepsProd.user().apiCalSettingsSteps().withAuth(auth2).deleteAllAndCreateNewLayer();
        Long layerID = stepsProd.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        stepsProd.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(
                stepsProd.user().settingsCalSteps().formDefaultEvent(layerID).withName(getRandomName()),
                Collections.singletonList(lock.accNum(1).getSelfEmail())
            )
            .createNewRepeatEventWithAttendees(
                stepsProd.user().settingsCalSteps().formDefaultRepeatingEvent(layerID).withName(getRandomName()),
                Collections.singletonList(lock.accNum(1).getSelfEmail())
            );
    }

    @Test
    @Title("Вёрстка кнопок «Пойду»/«Возможно» после нажатия (простое событие)")
    @TestCaseId("1185")
    @DataProvider({INVITE_BUTTON_YES, INVITE_BUTTON_MAYBE})
    public void shouldSeeColoredYesMaybeButtonsSimpleEvent(String buttonName) {
        Consumer<InitStepsRule> actions = st -> {
            openEventAndClickOnButton(st, buttonName, SIMPLE_EVENT_NUM);
            closeNotification(st);
        };
        parallelRun.withActions(actions).withAcc(lock.accNum(1)).run();
    }

    @Test
    @Title("Вёрстка модального окна после нажатия кнопки «Не пойду» (простое событие)")
    @TestCaseId("1188")
    public void shouldSeeRejectModalWindowSimpleEventInvite() {
        Consumer<InitStepsRule> actions = st -> openEventAndClickOnButton(st, INVITE_BUTTON_NO, SIMPLE_EVENT_NUM);
        parallelRun.withActions(actions).withAcc(lock.accNum(1)).run();
    }

    @Test
    @Title("Вёрстка кнопок «Пойду»/«Возможно» после нажатия (регулярное событие)")
    @TestCaseId("1186")
    @DataProvider({INVITE_BUTTON_YES, INVITE_BUTTON_MAYBE})
    public void shouldSeeColoredYesMaybeButtonsRegularEvent(String buttonName) {
        Consumer<InitStepsRule> actions = st -> {
            openEventAndClickOnButton(st, buttonName, REGULAR_EVENT_NUM);
            closeNotification(st);
        };
        parallelRun.withActions(actions).withAcc(lock.accNum(1)).run();
    }

    @Test
    @Title("Вёрстка модального окна после нажатия кнопки «Не пойду» (регулярное событие)")
    @TestCaseId("1190")
    public void shouldSeeRejectModalWindowRegularEventInvite() {
        Consumer<InitStepsRule> actions = st -> openEventAndClickOnButton(st, INVITE_BUTTON_NO, REGULAR_EVENT_NUM);
        parallelRun.withActions(actions).withAcc(lock.accNum(1)).run();
    }

    @Step("Закрываем нотификацию")
    public void closeNotification(InitStepsRule st) {
        st.user().defaultSteps()
            .clicksIfCanOn(st.pages().cal().touchHome().notificationDismiss())
            .shouldNotSee(st.pages().cal().touchHome().successNotify())
            .refreshPage();
    }

    @Step("Открываем событие приглашенным пользователем, нажимаем на кнопку {1}")
    public void openEventAndClickOnButton(InitStepsRule st, String buttonName, int eventNumber) {
        st.user().defaultSteps()
            .clicksOn(st.pages().cal().touchHome().events().waitUntil(not(empty())).get(eventNumber))
            .clicksOnElementWithText(
                st.user().pages().calTouch().eventPage().eventDecisionButtons().waitUntil(not(empty())),
                buttonName
            );
    }
}
