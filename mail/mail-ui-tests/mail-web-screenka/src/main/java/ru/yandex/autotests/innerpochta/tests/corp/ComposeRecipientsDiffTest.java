package ru.yandex.autotests.innerpochta.tests.corp;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Проверяем полоску «+ кукуц» при ответе на письмо")
@Features(FeaturesConst.PLUS_KUKUTZ)
@Tag(FeaturesConst.PLUS_KUKUTZ)
@Stories(FeaturesConst.CORP)
@Description("У пользователя подготовлено письмо с несколькими получателями")
@UseCreds(ComposeRecipientsDiffTest.CREDS)
public class ComposeRecipientsDiffTest {
    public static final String CREDS = "CorpAttachTest";
    private static final String EMAIL = "robot-mailcorp-3@yandex-team.ru";
    private static final String EMAIL_BCC = "robot-mailcorp-7@yandex-team.ru";
    private static final String SUBJ = "kukutz";
    private static final String DELETE_EXP = "?experiments=12345,0,0";

    private ScreenRulesManager rules = screenRulesManager().withLock(AccLockRule.use().annotation());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(removeAllMessages(() -> stepsProd.user(), INBOX, SENT, TRASH));

    @Test
    @Title("Полоска «+ кукуц» при удалении и добавлении получателей")
    @TestCaseId("3764")
    public void shouldSeePlusKukutzStripe() {
        String text = getRandomString();
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(SUBJ);
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().msgView().toolbar().replyToAllButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup());
            st.user().composeSteps()
                .clearAddressFieldTo()
                .inputsAddressInFieldCc(" ");
            st.user().composeSteps().inputsAddressInFieldTo(EMAIL);
            st.user().hotkeySteps().pressHotKeys(Keys.ENTER.toString());
            st.user().composeSteps().inputsAddressInFieldBcc(EMAIL_BCC);
            st.user().hotkeySteps().pressHotKeys(Keys.ENTER.toString());
            st.user().composeSteps().inputsSendText(text);
        };
        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).withUrlPath(DELETE_EXP).run();
    }
}
