package ru.yandex.autotests.innerpochta.tests.messagelist;

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
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGES_PER_PAGE;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Раскрытие треда в списке писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class OpenThreadTest {

    private static final int CUSTOM_COUNT_MSG = 5;
    private static final int MSG_COUNT = 7;

    private Message thread;

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
    public void setUp() {
        thread = stepsProd.user().apiMessagesSteps()
            .sendThread(lock.firstAcc(), Utils.getRandomName(), MSG_COUNT);
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams(
                "Включаем показ 5 писем на странице",
                of(SETTINGS_PARAM_MESSAGES_PER_PAGE, CUSTOM_COUNT_MSG)
            );
    }

    @Test
    @Title("Раскрываем тред кликом в тему")
    @TestCaseId("3033")
    public void shouldExpandThread() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().turnTrue(st.pages().mail().home()
                .displayedMessages().list().get(0).checkBox())
            .clicksOn(st.pages().mail().home().displayedMessages().list().get(0).subject())
            .shouldSee(st.pages().mail().home().displayedMessages().loadMoreLink());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Раскрываем тред кликом в количество писем")
    @TestCaseId("3034")
    public void shouldExpandThreadUseCountBtn() {
        Consumer<InitStepsRule> actions = st ->
            st.user().messagesSteps().expandsMessagesThread(thread.getSubject());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на «Еще письма» в треде")
    @TestCaseId("3035")
    public void shouldSeeAllMsgInThread() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().expandsMessagesThread(thread.getSubject());
            st.user().defaultSteps().clicksOn(st.pages().mail().home()
                .displayedMessages().loadMoreLink());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выделение тредов в списке писем")
    @TestCaseId("3995")
    public void shouldSeeSelectedThread() {
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomString(), "");
        Consumer<InitStepsRule> actions = st ->
            st.user().messagesSteps().selectsAllDisplayedMessagesInFolder()
                .expandsMessagesThread(thread.getSubject());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

}
