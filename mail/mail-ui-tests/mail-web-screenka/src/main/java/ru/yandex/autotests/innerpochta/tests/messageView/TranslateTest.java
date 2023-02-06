package ru.yandex.autotests.innerpochta.tests.messageView;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Переводчик в просмотре письма")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@RunWith(DataProviderRunner.class)
@Stories(FeaturesConst.GENERAL)
public class TranslateTest {

    private static final String MSG_BODY = "Hello, my name is panda";
    private static final String TRANSLATION_DONE_NOTIFICATION = "оригинал: английский, переведено на русский.";

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
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomString(), MSG_BODY);
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Должны видеть плашку перевода")
    @TestCaseId("2696")
    public void shouldSeeTranslateNotify() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().translateNotification());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть перевод")
    @TestCaseId("2698")
    public void shouldSeeTranslatedText() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().translateNotification())
                .clicksOn(st.pages().mail().msgView().translateNotification().translateButton())
                .shouldSee(st.pages().mail().msgView().translateNotification())
                .shouldHasText(
                    st.pages().mail().msgView().translateNotification().textTranslated(),
                    TRANSLATION_DONE_NOTIFICATION
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

}
