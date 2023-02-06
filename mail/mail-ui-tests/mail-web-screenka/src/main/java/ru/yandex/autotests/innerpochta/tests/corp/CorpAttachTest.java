package ru.yandex.autotests.innerpochta.tests.corp;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_AREAS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Инлайн аттач на корпе")
@Features(FeaturesConst.ATTACHES)
@Tag(FeaturesConst.ATTACHES)
@Stories(FeaturesConst.CORP)
@Description("У пользователя заранее подготовлено и запинено письмо с инлайн аттачем")
public class CorpAttachTest {
    
    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED_AREAS);

    private static final String SUBJECT = "inline";
    private static final String DELETE_EXP = "?experiments=12345,0,0";

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(removeAllMessages(() -> stepsProd.user(), INBOX, TRASH));

    @Before
    public void setUp() {
        stepsTest.user().loginSteps().forAcc(lock.firstAcc()).loginsToCorp();
        stepsTest.user().messagesSteps().selectMessageWithSubject(SUBJECT);
        stepsTest.user().defaultSteps().clicksOn(stepsTest.pages().mail().home().toolbar().forwardButton());
        stepsTest.user().composeSteps()
            .inputsAddressInFieldTo(lock.firstAcc().getSelfEmail())
            .clicksOnSendButtonInHeader()
            .waitForMessageToBeSend();
    }

    @Test
    @Title("Переслать письмо с инлайн аттачем")
    @TestCaseId("4645")
    public void shouldSeeInlineAttach() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject("Fwd: " + SUBJECT);
            st.user().defaultSteps().shouldSee(
                st.pages().mail().msgView().messageTextBlock().inlineAttaches().waitUntil(not(empty()))
            );
        };
        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).withUrlPath(DELETE_EXP).run();
    }
}
