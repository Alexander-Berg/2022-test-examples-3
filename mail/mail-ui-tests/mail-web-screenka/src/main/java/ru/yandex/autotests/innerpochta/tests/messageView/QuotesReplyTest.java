package ru.yandex.autotests.innerpochta.tests.messageView;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.KeysOwn;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.SENT_RU;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.REMOVE_FIXED_QR;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;

/**
 * @author vasily-k
 */

@Aqua.Test
@Title("Просмотр письма с цитированием")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.FULL_VIEW)
public class QuotesReplyTest {

    private static final String SUBJECT = "Re: Переписка с цитатами";
    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private ScreenRulesManager rules = screenRulesManager();
    @Rule
    public RuleChain chain = rules.createRuleChain();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);
    private AccLockRule lock = rules.getLock();

    @Test
    @Title("Цвета ответов должны чередоваться")
    @TestCaseId("934")
    public void shouldSeeAlternateRepliesColors() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(SUBJECT);
            st.user().defaultSteps().clicksOn(
                st.pages().mail().msgView().messageViewSideBar().messagesBySubjList().relatedMsgList().get(0)
            );
            st.user().leftColumnSteps().shouldBeInFolder(SENT_RU);
            st.user().defaultSteps().executesJavaScript(REMOVE_FIXED_QR);
            st.user().defaultSteps().scrollAndClicksOn(
                st.pages().mail().msgView().messageTextBlock().quotes().get(0).showFullQuote()
            );
            st.user().hotkeySteps().pressSimpleHotKey(KeysOwn.key(Keys.END));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withIgnoredElements(IGNORED_ELEMENTS).run();
    }

    @Test
    @Title("Отвечаем автору цитаты")
    @TestCaseId("934")
    @DoTestOnlyForEnvironment("Not FF")
    public void quoteAuthorReply() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(SUBJECT);
            st.user().defaultSteps().clicksOn(
                st.pages().mail().msgView().messageViewSideBar().messagesBySubjList().relatedMsgList().get(0)
            );
            st.user().leftColumnSteps().shouldBeInFolder(SENT_RU);
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().msgView().messageTextBlock().quotes().get(0).quotesAuthors().get(0))
                .switchOnJustOpenedWindow()
                .shouldSee(st.pages().mail().composePopup().expandedPopup());
            st.user().composeSteps().shouldSeeSendToAreaHas("test-for-t@yandex.ru");
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
