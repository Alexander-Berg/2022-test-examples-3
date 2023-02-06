package ru.yandex.autotests.innerpochta.tests.messageView;

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

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Виджет адресов в письме")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.WIDGET)
public class AddressWidgetTest {

    private static final String ADDRESS_TO_HIGHLIGHT = "Колпино, ул. Механическая, 20";
    private String subject;

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
        subject = getRandomString();
        stepsProd.user().apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc(), subject, ADDRESS_TO_HIGHLIGHT);
    }

    @Test
    @Title("Должны видеть подсвеченный адрес")
    @TestCaseId("3393")
    public void shouldSeeAddressHighlighted() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().shouldSeeMessageWithSubject(subject)
                .clicksOnMessageByNumber(0);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageTextBlock().highlightedAddress());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть карту")
    @TestCaseId("3394")
    public void shouldSeeMapWidget() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().shouldSeeMessageWithSubject(subject)
                .clicksOnMessageByNumber(0);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().messageTextBlock().highlightedAddress())
                .shouldSee(st.pages().mail().home().mapPopup());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
