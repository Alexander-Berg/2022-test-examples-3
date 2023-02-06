package ru.yandex.autotests.innerpochta.tests.messagelist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.experiments.ExperimentsConstants.REPLY_LATER_EXP;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на фичу «Напомнить позже»")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.REPLY_LATER)
public class ReplyLaterTest {

    private static final String REPLY_LATER_FOLDER_SYMBOL = "#reply_later";
    private static final String PROMO_REPLY_LATER_PARAM = "?promo=promo-reply-later#";

    Message msg;

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void logIn() {
        msg = stepsProd.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomString(), "");
    }

    @Test
    @Title("Вёрстка выпадушки «Напомнить позже»")
    @TestCaseId("6373")
    public void shouldSeeReplyLaterMenu() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().addExperimentsWithYexp(REPLY_LATER_EXP);
            st.user().messagesSteps().openReplyLaterDropdown(0);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка выпадушки «Напомнить позже» у письма с напоминанием")
    @TestCaseId("6375")
    public void shouldSeeReplyLaterMenuInRLFolder() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().addExperimentsWithYexp(REPLY_LATER_EXP);
            st.user().messagesSteps().openReplyLaterDropdown(0);
        };
        stepsProd.user().apiMessagesSteps().doReplyLaterForTomorrow(msg, 1);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(REPLY_LATER_FOLDER_SYMBOL).run();
    }

    @Test
    @Title("Вёрстка плашки в просмотре письма до закрепа")
    @TestCaseId("6381")
    public void shouldSeeReplyLaterBar() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().addExperimentsWithYexp(REPLY_LATER_EXP)
                .shouldSee(st.pages().mail().msgView().deleteReminderBtn());

        stepsProd.user().apiMessagesSteps().doReplyLaterForTomorrow(msg, 1);
        parallelRun.withActions(actions).withAcc(lock.firstAcc())
            .withUrlPath(QuickFragments.MSG_FRAGMENT.makeUrlPart(msg.getMid())).run();
    }

    @Test
    @Title("Должны увидеть промо «Напомнить позже»")
    @TestCaseId("6383")
    public void shouldSeeReplyLaterPromo() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().addExperimentsWithYexp(REPLY_LATER_EXP)
                .shouldSee(st.pages().mail().msgView().replyLaterPromo());

        parallelRun.withActions(actions).withAcc(lock.firstAcc())
            .withUrlPath(PROMO_REPLY_LATER_PARAM + QuickFragments.MSG_FRAGMENT.fragment(msg.getMid())).run();
    }

    @Test
    @Title("Для писем с напоминанием не доступна часть действий в контекстном меню")
    @TestCaseId("6379")
    public void shouldNotSeeActionsInContextMenu() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().addExperimentsWithYexp(REPLY_LATER_EXP);
            st.user().messagesSteps().rightClickOnMessageWithSubject(msg.getSubject())
                .shouldSeeContextMenu();
        };
        stepsProd.user().apiMessagesSteps().doReplyLaterForTomorrow(msg, 1);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(REPLY_LATER_FOLDER_SYMBOL).run();
    }

    @Test
    @Title("Для писем с напоминанием не доступна часть действий в тулбаре")
    @TestCaseId("6379")
    public void shouldNotSeeActionsInToolbar() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().addExperimentsWithYexp(REPLY_LATER_EXP);
            st.user().messagesSteps().clicksOnMessageCheckBoxByNumber(0);
            st.user().defaultSteps().shouldSee(st.pages().mail().home().toolbar().replyLaterBtn())
                .clicksIfCanOn(st.pages().mail().home().toolbar().moreBtn());
        };
        stepsProd.user().apiMessagesSteps().doReplyLaterForTomorrow(msg, 1);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(REPLY_LATER_FOLDER_SYMBOL).run();
    }
}
