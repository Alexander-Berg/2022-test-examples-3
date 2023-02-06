package ru.yandex.autotests.innerpochta.tests.messagelist;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на виджеты")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.WIDGET)
@Description("Юзеру каждое утро присылаются письма")
public class WidgetTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Должны видеть обычные авиабилеты")
    @TestCaseId("2571")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldSeeFlyWidget() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().leftColumnSteps().opensCustomFolder(1);
            st.user().defaultSteps()
                .shouldSee(st.pages().mail().home().displayedMessages().list().get(0).widgetTicket().widgetDecoration())
                .shouldSeeElementsCount(
                    st.pages().mail().home().displayedMessages().list().get(0).widgetTicket().widgetBtns(),
                    1
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны быть авиабилеты «tomorrow»")
    @TestCaseId("2581")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldSeeTomorrowFlyWidget() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().leftColumnSteps().opensCustomFolder(1);
            shouldSeeWidget(st);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны быть авиабилеты без фактов")
    @TestCaseId("2738")
    public void shouldSeeInvalidFlyWidget() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().leftColumnSteps().opensCustomFolder(3);
            shouldSeeOnlyDecoration(st);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны быть обычные отели")
    @TestCaseId("2556")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldSeeHotelWidget() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().leftColumnSteps().opensCustomFolder(5);
            shouldSeeWidgetWithPrintBtn(st);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны быть отели «tomorrow»")
    @TestCaseId("2557")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldSeeTomorrowHotelWidget() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().leftColumnSteps().opensCustomFolder(5);
            shouldSeeWidgetWithPrintBtn(st);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны быть отели без фактов")
    @TestCaseId("2558")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldSeeInvalidHotelWidget() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().leftColumnSteps().opensCustomFolder(6);
            shouldSeeOnlyDecoration(st);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны быть билеты в кино")
    @TestCaseId("2737")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldSeeCinemaWidget() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().leftColumnSteps().opensCustomFolder(8);
            st.user().defaultSteps().shouldSee(
                st.pages().mail().home().displayedMessages().list().get(0).widgetTicket().widgetDecoration(),
                st.pages().mail().home().displayedMessages().list().get(0).widgetTicket().printButton()
            )
                .shouldSeeElementsCount(
                    st.pages().mail().home().displayedMessages().list().get(0).widgetTicket().widgetClickBtns(),
                    0
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны быть виджеты регистрации/смены пароля")
    @TestCaseId("3471")
    public void shouldSeeAuthWidget() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().leftColumnSteps().opensCustomFolder(9);
            st.user().defaultSteps().shouldSee(
                st.pages().mail().home().displayedMessages().list().get(0).widget().widgetClickBtns().get(0)
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть виджеты отлупов")
    @TestCaseId("3445")
    public void shouldSeeBounceWidget() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().leftColumnSteps().opensCustomFolder(10);
            st.user().defaultSteps().shouldSee(
                st.pages().mail().home().displayedMessages().list().get(0).widget().deleteButton()
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть виджеты покупок")
    @TestCaseId("3767")
    public void shouldSeeShopWidget() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().leftColumnSteps().opensCustomFolder(11);
            st.user().defaultSteps().shouldSee(
                st.pages().mail().home().displayedMessages().list().get(0).widget().widgetDecoration(),
                st.pages().mail().home().displayedMessages().list().get(0).widget().widgetClickBtns().get(0)
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Должны видеть виджет с кнопкой «Распечатать» на первом билете")
    private void shouldSeeWidgetWithPrintBtn(InitStepsRule st) {
        st.user().defaultSteps().shouldSee(
            st.pages().mail().home().displayedMessages().list().get(0).widgetTicket().widgetDecoration(),
            st.pages().mail().home().displayedMessages().list().get(0).widgetTicket().printButton()
        );
    }

    @Step("Должны видеть виджет кнопкой «Аэроэкспресс» на первом билете")
    private void shouldSeeWidget(InitStepsRule st) {
        st.user().defaultSteps().shouldSee(
            st.pages().mail().home().displayedMessages().list().get(0).widgetTicket().widgetDecoration(),
            st.pages().mail().home().displayedMessages().list().get(0).widgetTicket().widgetBtns().get(0)
        );
    }

    @Step("Должны виджет без фактов на первом билете")
    private void shouldSeeOnlyDecoration(InitStepsRule st) {
        st.user().defaultSteps().shouldSee(
            st.pages().mail().home().displayedMessages().list().get(0).widgetTicket().widgetDecoration())
            .shouldNotSee(st.pages().mail().home().displayedMessages().list().get(0).widgetTicket().printButton())
            .shouldSeeElementsCount(
                st.pages().mail().home().displayedMessages().list().get(0).widgetTicket().widgetClickBtns(),
                0
            );
    }
}
