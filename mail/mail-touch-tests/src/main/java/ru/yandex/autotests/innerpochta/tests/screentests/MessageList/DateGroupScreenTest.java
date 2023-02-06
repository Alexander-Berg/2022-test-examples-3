package ru.yandex.autotests.innerpochta.tests.screentests.MessageList;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Скриночные тесты на группировку писем по дате")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class DateGroupScreenTest {

    private static final String TODAY = "Сегодня";

    LocalDateTime date = LocalDateTime.now();

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule accLock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        stepsProd.user().imapSteps().addMessage(
            date.minusDays(1).getMonthValue(), date.minusDays(1).getYear(), date.minusDays(1).getDayOfMonth()
        )
            .addMessage(
                date.minusDays(2).getMonthValue(), date.minusDays(2).getYear(), date.minusDays(2).getDayOfMonth()
            )
            .addMessage(
                date.minusWeeks(1).getMonthValue(), date.minusWeeks(1).getYear(), date.minusWeeks(1).getDayOfMonth()
            )
            .addMessage(
                date.minusMonths(1).getMonthValue(), date.minusMonths(1).getYear(), date.minusMonths(1).getDayOfMonth()
            )
            .addMessage(
                date.minusYears(2).getMonthValue(), date.minusYears(2).getYear(), date.minusYears(2).getDayOfMonth()
            );
        stepsProd.user().apiMessagesSteps().sendMail(accLock.firstAcc(), Utils.getRandomString(), "");
    }

    @Test
    @Title("Должны видеть группировку списка писем по дате")
    @TestCaseId("1092")
    public void shouldSeeGroupByDateMsgs() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps()
                .shouldSeeElementInList(st.pages().touch().messageList().dateGroup().waitUntil(not(empty())), TODAY);

        parallelRun.withActions(act).withAcc(accLock.firstAcc()).run();
    }
}
