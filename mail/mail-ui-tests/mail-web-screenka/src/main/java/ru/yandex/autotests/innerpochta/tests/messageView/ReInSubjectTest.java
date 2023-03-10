package ru.yandex.autotests.innerpochta.tests.messageView;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("?????????????????? ?????????????? ?? ???????????????? ?????? ????????????????????/?????????????????????? ??????????")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.FULL_VIEW)
@RunWith(DataProviderRunner.class)
public class ReInSubjectTest {

    private static String prefixRe = "Re";
    private String subjRe;

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);


    @DataProvider
    public static Object[][] extendedTestData() {
        return new Object[][]{
            {LAYOUT_2PANE, "colorful", EMPTY_STR},
            {LAYOUT_2PANE, "colorful", STATUS_TRUE},
            {LAYOUT_2PANE, "lamp", EMPTY_STR},
            {LAYOUT_2PANE, "lamp", STATUS_TRUE},
            {LAYOUT_3PANE_VERTICAL, "lamp", EMPTY_STR},
            {LAYOUT_3PANE_VERTICAL, "colorful", EMPTY_STR}
        };
    }

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();


    @Before
    public void setUp() {
        stepsTest.user().apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 1);
        subjRe = stepsTest.user().apiMessagesSteps().getAllMessages().get(0).getSubject();
        stepsTest.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("?????????????????? ?????????????? Re ?? ?????????????????? ????????????")
    @TestCaseId("5350")
    @UseDataProvider("extendedTestData")
    public void shouldSeeReInMsg(String layout, String theme, String settingOpenMsg) {
        stepsTest.user().messagesSteps().replyToMessage(subjRe);
        stepsTest.user().apiMessagesSteps().deleteMessages(
                stepsTest.user().apiMessagesSteps().getMessageWithSubjectInFolder(subjRe, SENT)
            )
            .deleteMessages(
                stepsTest.user().apiMessagesSteps().getMessageWithSubjectInFolder(subjRe, TRASH)
            )
            .markAllMsgRead();

        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().expandsMessagesThread(subjRe)
                .clicksOnMessageByNumber(1);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageHead())
                .shouldContainText(st.pages().mail().msgView().messageSubjectInFullView(), prefixRe);
        };
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "?????????????????????? ????????????, ????????, ?????????????????? ???????????????? ????????????",
            of(
                SETTINGS_PARAM_LAYOUT, layout,
                COLOR_SCHEME, theme,
                SETTINGS_OPEN_MSG_LIST, settingOpenMsg
            )
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
