package ru.yandex.autotests.innerpochta.tests.screentests;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
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

import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.USER_FOLDER_FID_7;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.experiments.ExperimentsBuilder.experimentsBuilder;
import static ru.yandex.autotests.innerpochta.util.experiments.ExperimentsConstants.CLASSDIALOG_EXP;

/**
 * @author sshelgunova
 */
@Aqua.Test
@Title("Скриночные тесты на плашку классификаций")
@Description("У пользователя подготовлены письма с разными типами в отдельной папке")
@Features({FeaturesConst.MESSAGE_FULL_VIEW})
@Stories(FeaturesConst.CLASS_DIALOG)
@RunWith(DataProviderRunner.class)
public class ClassDialogScreenTest {

    private static final String PEOPLE = "mail_from_people";
    private static final String SOCIAL = "mail_from_social";
    private static final String NOTIFY = "mail_from_notify";
    private static final String NEWS = "mail_from_new";

    private TouchScreenRulesManager rules = touchScreenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("QUINN-6763")
    @Test
    @Title("Плашка на письмах с разными типами")
    @TestCaseId("889")
    @DataProvider({PEOPLE, SOCIAL, NOTIFY, NEWS})
    public void ShouldSeeClassDialogFromPeople(String subject) {
        Consumer<InitStepsRule> act = steps -> {
            switchExpFromFolder(steps);
            steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messageBlock())
                .clicksOnElementWithText(steps.pages().touch().messageList().subjectList(), subject)
                .shouldSee(steps.pages().touch().messageView().classDialog());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).run();
    }

    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("QUINN-6763")
    @Test
    @Title("Плашка не пропадает после разворачивания деталей письма")
    @TestCaseId("969")
    public void ShouldSeeClassDialogAfterViewDetailsMail() {
        Consumer<InitStepsRule> act = steps -> {
            switchExpFromFolder(steps);
            steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messageBlock().subject())
                .clicksOn(steps.pages().touch().messageView().toolbar())
                .shouldSee(steps.pages().touch().messageView().msgDetails());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).run();
    }

    @Step("Включаем эксперимент плашки классификаций с частотой показа = 1 и переходим в подготовленную папку")
    private void switchExpFromFolder(InitStepsRule st) {
        st.user().defaultSteps().addExperimentsWithJson(experimentsBuilder().withDefaultConditions(CLASSDIALOG_EXP))
            .opensCurrentUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(USER_FOLDER_FID_7));
    }
}