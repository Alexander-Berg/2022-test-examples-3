package ru.yandex.autotests.innerpochta.tests.autotests;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.experiments.ExperimentsBuilder.experimentsBuilder;
import static ru.yandex.autotests.innerpochta.util.experiments.ExperimentsConstants.CLASSDIALOG_EXP;

/**
 * @author sshelgunova
 */
@Aqua.Test
@Title("Тесты на плашку классификаций")
@Features({FeaturesConst.MESSAGE_FULL_VIEW})
@Stories(FeaturesConst.CLASS_DIALOG)
public class ClassDialogTest {

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule acc = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().apiMessagesSteps().sendMailWithNoSave(
            acc.firstAcc().getSelfEmail(), Utils.getRandomString(), Utils.getRandomString()
        );
        steps.user().loginSteps().forAcc(acc.firstAcc()).logins();
        steps.user().defaultSteps().addExperimentsWithJson(experimentsBuilder().withDefaultConditions(CLASSDIALOG_EXP))
            .refreshPage();
    }

    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("QUINN-6763")
    @Test
    @Title("После клика на кнопку Да плашка исчезла")
    @TestCaseId("894")
    public void ShouldCloseClassDialogAfterClickYes() {
        showClassDialog();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().classDlgYesBtn())
            .shouldNotSee(steps.pages().touch().messageView().classDialog());
    }

    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("QUINN-6763")
    @Test
    @Title("После клика на кнопку Нет плашка исчезла")
    @TestCaseId("895")
    public void ShouldCloseClassDialogAfterClickNo() {
        showClassDialog();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().classDlgNoBtn())
            .shouldNotSee(steps.pages().touch().messageView().classDialog());
    }

    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("QUINN-6763")
    @Test
    @Title("Плашка не пропадает после сворачивания деталей письма обратно")
    @TestCaseId("888")
    public void ShouldSeeClassDialogAfterCloseDetailsMail() {
        showClassDialog();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().toolbar())
            .shouldSee(steps.pages().touch().messageView().msgDetails())
            .clicksOn(steps.pages().touch().messageView().toolbar())
            .shouldSee(steps.pages().touch().messageView().classDialog());
    }

    @Step("Показываем плашку классфикации на письме")
    private void showClassDialog() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messageBlock().subject())
            .shouldSee(steps.pages().touch().messageView().classDialog());
    }
}