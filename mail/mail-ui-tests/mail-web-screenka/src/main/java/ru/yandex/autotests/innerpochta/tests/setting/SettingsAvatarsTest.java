package ru.yandex.autotests.innerpochta.tests.setting;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
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
import ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule.addMessageIfNeed;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGE_AVATARS;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Проверяем включение и выключение аватарок в списке писем")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.AVATARS)
@RunWith(DataProviderRunner.class)
public class SettingsAvatarsTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);
    private AddMessageIfNeedRule addMsg = addMessageIfNeed(() -> stepsProd.user(), () -> lock.firstAcc());

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(addMsg);

    @Test
    @Title("Настройка показа портретов отправителей в 2pane")
    @TestCaseId("4433")
    @DataProvider({STATUS_ON, EMPTY_STR})
    public void shouldSetAvatarsSettingsTwoPane(String status) {
        setLayout(LAYOUT_2PANE);
        setAvatarsSetting(status);
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().opensDefaultUrl()
            .refreshPage()
            .shouldSee(st.pages().mail().home().displayedMessages().list().get(0).checkBox());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Настройка показа портретов отправителей в 3pane")
    @TestCaseId("4175")
    @DataProvider({STATUS_ON, EMPTY_STR})
    public void shouldSetAvatarsSettingsThreePane(String status) {
        setLayout(LAYOUT_3PANE_VERTICAL);
        setAvatarsSetting(status);
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().opensDefaultUrl()
            .refreshPage()
            .shouldSee(st.pages().mail().home().displayedMessages().list().get(0).avatarImg());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("По клику на аватарку - чекбокс выделяется письмо")
    @TestCaseId("4165")
    public void shouldSetCheckboxInMsg() {
        setLayout(LAYOUT_2PANE);
        setAvatarsSetting(STATUS_ON);
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().displayedMessages().list().get(0).checkBox());
            st.user().messagesSteps().shouldSeeThatMessagesAreSelected();
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    private void setLayout(String layout) {
        stepsProd.user().apiSettingsSteps().callWith(of(SETTINGS_PARAM_LAYOUT, layout));
    }

    private void setAvatarsSetting(String status) {
        stepsProd.user().apiSettingsSteps().callWith(of(SETTINGS_PARAM_MESSAGE_AVATARS, status));
    }
}
