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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_OFF;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_TRANSLATE;

/**
 * @author a-zoshchuk
 */

@Aqua.Test
@Title("Яблы получателей")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.YABBLE)
public class RecieverYabbleTest {

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
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем переводчик, включаем открытие письма в списке писем",
            of(
                SETTINGS_PARAM_TRANSLATE, STATUS_OFF,
                SETTINGS_OPEN_MSG_LIST, STATUS_TRUE
            )
        );
        stepsProd.user().apiMessagesSteps().addCcEmails(DEV_NULL_EMAIL)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), Utils.getRandomName(), Utils.getRandomString());
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Открываем выпадушку яббла контакта")
    @TestCaseId("2684")
    public void shouldSeeContactDropdown() {
        Consumer<InitStepsRule> actions = st -> {
            expandReceiversList(st);
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().msgView().messageHead().contactsInCC().get(0))
                .shouldSee(st.pages().mail().msgView().contactBlockPopup().composeLetterBtn());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Переходим в композ из яббла контакта")
    @TestCaseId("2685")
    public void shouldSeeCompose() {
        Consumer<InitStepsRule> actions = st -> {
            expandReceiversList(st);
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().msgView().messageHead().contactsInCC().get(0))
                .clicksOn(st.pages().mail().msgView().contactBlockPopup().composeLetterBtn())
                .shouldSee(st.pages().mail().composePopup().expandedPopup());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Разворачиваем получателей в шапке письма")
    @TestCaseId("1073")
    public void shouldSeeMoreInfoAboutCcBcc() {
        Consumer<InitStepsRule> actions = st -> {
            expandReceiversList(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().messageHead().messageRead());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Открываем письмо и разворачиваем список получателей")
    private void expandReceiversList(InitStepsRule st) {
        st.user().defaultSteps().refreshPage();
        st.user().messagesSteps().clicksOnMessageByNumber(0);
        st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageTextBlock().text())
            .clicksOn(st.pages().mail().msgView().messageHead().showFieldToggler());
    }

}
