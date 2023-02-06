package ru.yandex.autotests.innerpochta.tests.screentests.MessageList;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TEMPLATE;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на свайпы")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.POPUPS)
@RunWith(DataProviderRunner.class)
public class SwipeScreenTest {

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

    @Test
    @Title("Отсутствует левый свайп у черновиков и шаблонов")
    @TestCaseId("709")
    @DataProvider({DRAFT, TEMPLATE})
    public void shouldNotSwipeDraftLeft(String folder) {
        Consumer<InitStepsRule> act = st -> {
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().messageBlock());
            st.user().touchSteps().leftSwipeMsgAndKeepHolding();
        };
        stepsProd.user().apiMessagesSteps().createDraftMessage();
        stepsProd.user().apiMessagesSteps().createTemplateMessage(accLock.firstAcc());
        parallelRun.withActions(act).withAcc(accLock.firstAcc())
            .withUrlPath(
                FOLDER_ID.makeTouchUrlPart(stepsProd.user().apiFoldersSteps().getFolderBySymbol(folder).getFid())
            ).run();
    }
}
