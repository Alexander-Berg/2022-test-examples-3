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
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Запиненные письма")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.Pin)
public class PinnedLettersTest {

    private ScreenRulesManager rules = screenRulesManager();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        Message msgFromThread = stepsProd.user().apiMessagesSteps().sendThread(
            lock.firstAcc(),
            Utils.getRandomName(),
            3
        );
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
        stepsProd.user().apiMessagesSteps().getAllMessages()
            .forEach(msg -> stepsProd.user().apiLabelsSteps().pinLetter(msg));
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(
            lock.firstAcc(),
            msgFromThread.getSubject(),
            Utils.getRandomString()
        );
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
    }

    @Test
    @Title("Открываем письмо в закрепленном треде")
    @TestCaseId("4761")
    public void shouldSeeMessageInPinnedThread() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageTextBlock().text());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
