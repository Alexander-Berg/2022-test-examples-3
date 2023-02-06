package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.tests.compose.ComposeYabbleFormationTest.CREDS;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("Формирование и отображение ябблов в композе")
@Features({FeaturesConst.COMPOSE, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.YABBLE)
@UseCreds(CREDS)
@Description("В аккаунте заготовлены контакты с различными тестовыми данными. " +
    "Удалять нельзя, они нужны для полоски популярных контактов")
public class ComposeYabbleFormationTest {

    public static final String CREDS = "ComposeYabbleActionsTest";

    private AccLockRule lock = AccLockRule.use().annotation();
    private ScreenRulesManager rules = screenRulesManager().withLock(lock);
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();

    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredElements(IGNORED_ELEMENTS);

    private static final String NO_NAME_CONTACT = "testbot2@yandex.ru";
    private static final String GROUP_TITLE = "yabblegroup";
    private static final String COMMA_NAME = "test1@test1.test1";
    private static final String DOT_NAME = "test2@test2.test2";
    private static final String SEMICOLON_NAME = "test3@test3.test3";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(removeAllMessages(() -> stepsProd.user(), INBOX));

    @Test
    @Title("Формирования ябла контакта, у которого в имени есть разделители")
    @TestCaseId("2914")
    public void shouldSeeYabbleWithSymbols() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().inputsAddressInFieldTo(COMMA_NAME)
                .addAnotherRecipient(DOT_NAME)
                .addAnotherRecipient(SEMICOLON_NAME);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput());
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Формирование ябла для контакта без имени")
    @TestCaseId("2847")
    public void shouldSeeYabbleForNoNameContact() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().inputsAddressInFieldTo(NO_NAME_CONTACT);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput());
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем формирование группового ябла")
    @TestCaseId("2861")
    public void shouldSeeGroupYabble() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().inputsAddressInFieldTo(GROUP_TITLE);
            st.user().defaultSteps().waitInSeconds(1)
                .shouldSee(st.pages().mail().composePopup().suggestList().get(0));
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput());
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Добавление яббла из меню контакта в шапке письма")
    @TestCaseId("2867")
    public void shouldAddYabbleFromContactMenu() {
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(DEV_NULL_EMAIL, getRandomString(), getRandomString());
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().msgView().messageHead().fromName())
                .shouldSee(st.pages().mail().msgView().mailCard())
                .clicksOn(st.pages().mail().msgView().mailCard().composeBtn())
                .shouldSee(st.pages().mail().composePopup().expandedPopup());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
