package ru.yandex.autotests.innerpochta.tests.messagelist3pane;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Треды в 3pane")
@Description("На аккаунте заготовлен тред из 32 писем и еще одно отдельное письмо")
@Features(FeaturesConst.THREE_PANE)
@Tag(FeaturesConst.THREE_PANE)
@Stories(FeaturesConst.THREAD)
public class Threads3paneTest {

    private static final int MSG_COUNT = 29;

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
    @Title("Разворачиваем тред")
    @TestCaseId("344")
    public void shouldUnwrapThread() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().home().displayedMessages().list().get(0).expandThread())
            .waitInSeconds(3); // ждем разворачивания треда

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем верстку ссылок развернутого треда")
    @TestCaseId("3124")
    public void shouldSeeLinksInThread() {
        Consumer<InitStepsRule> actions = this::openThreadAndScrollToMoreLink;

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Подгружаем еще письма в тред")
    @TestCaseId("3125")
    public void shouldSeeAllMessagesInThread() {
        Consumer<InitStepsRule> actions = st -> {
            openThreadAndScrollToMoreLink(st);
            st.user().defaultSteps().clicksIfCanOn(st.pages().mail().home().displayedMessages().loadMoreLink())
                .clicksOn(st.pages().mail().home().displayedMessages().messagesInThread().get(MSG_COUNT + 2))
                .shouldNotSee(st.pages().mail().home().displayedMessages().loadMoreLink());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем тред на просмотр")
    @TestCaseId("3126")
    public void shouldOpenThread() {
        Consumer<InitStepsRule> actions = st ->
            st.user().messagesSteps().clicksOnMessageByNumber(1)
                .clicksOnMessageByNumber(0);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Разворачиваем письмо треда")
    @TestCaseId("3137")
    public void shouldUnwrapMessageInThread() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().msgInThread().get(0));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Разворачиваем письма в просмотре треда")
    @TestCaseId("3690")
    public void shouldSeeAllMessagesInView() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().loadMore());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Разворачиваем тред и скроллим до ссылки «Еще письма»")
    private void openThreadAndScrollToMoreLink(InitStepsRule st) {
        st.user().defaultSteps().clicksOn(
            st.pages().mail().home().displayedMessages().list().get(0).expandThread())
            .clicksOn(st.pages().mail().home().displayedMessages().messagesInThread().get(MSG_COUNT))
            .shouldSee(st.pages().mail().home().displayedMessages().loadMoreLink());
    }
}
