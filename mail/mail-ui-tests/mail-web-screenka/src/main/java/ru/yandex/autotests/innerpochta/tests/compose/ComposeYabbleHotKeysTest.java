package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
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
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.tests.compose.ComposeYabbleHotKeysTest.CREDS;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("Ябблы в композе: горячие клавиши")
@Features({FeaturesConst.COMPOSE})
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.YABBLE)
@RunWith(DataProviderRunner.class)
@UseCreds(CREDS)
@Description("В аккаунте заготовлены контакты с различными тестовыми данными. " +
    "Удалять нельзя, они нужны для полоски популярных контактов")
public class ComposeYabbleHotKeysTest {

    public static final String CREDS = "ComposeYabbleActionsTest";

    private AccLockRule lock = AccLockRule.use().annotation();
    private ScreenRulesManager rules = screenRulesManager().withLock(lock);
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredElements(IGNORED_ELEMENTS);

    private static final String CONTACT = "robbiter-5550005552@yandex.ru";
    private static final String CONTACT_NAME = "Тестовый Контакт";
    private static final String NO_NAME_CONTACT = "testbot2@yandex.ru";
    private static final String COMMA_NAME = "test1@test1.test1";
    private static final String SEMICOLON_NAME = "test3@test3.test3";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @DataProvider
    public static Object[][] testData() {
        return new Object[][]{
            {Keys.ARROW_LEFT},
            {Keys.ARROW_UP},
            {Keys.ARROW_DOWN},
            {Keys.ARROW_LEFT, Keys.ARROW_LEFT, Keys.ARROW_LEFT, Keys.ARROW_RIGHT}
        };
    }

    @Test
    @Title("Выделение всех яблов через ctrl+a")
    @TestCaseId("2846")
    public void shouldSelectAllYabbles() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().expandCollapseBtn());
            st.user().composeSteps().inputsAddressInFieldTo(CONTACT)
                .addAnotherRecipient(DEV_NULL_EMAIL)
                .addAnotherRecipient(SEMICOLON_NAME);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput());
            st.user().hotkeySteps().pressHotKeysWithDestination(
                st.pages().mail().composePopup().expandedPopup().popupTo(),
                Keys.chord(Keys.CONTROL, "a")
            );
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выделение яблов с помощью стрелок")
    @TestCaseId("2843")
    @UseDataProvider("testData")
    public void shouldSelectYabbleByArrow(Keys key) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().expandCollapseBtn());
            st.user().composeSteps().inputsAddressInFieldTo(COMMA_NAME)
                .addAnotherRecipient(DEV_NULL_EMAIL)
                .addAnotherRecipient(SEMICOLON_NAME);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput());
            st.user().defaultSteps().shouldSee(st.pages().mail().composePopup().yabbleTo());
            st.user().hotkeySteps().pressHotKeysWithDestination(
                st.pages().mail().composePopup().expandedPopup().popupTo(),
                Keys.chord(key)
            );
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Формирование яблов через запятую после нажатия Enter")
    @TestCaseId("2206")
    public void shouldSeeYabbleAfterEnter() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().inputsAddressInFieldTo(CONTACT)
                .addAnotherRecipient(DEV_NULL_EMAIL);
            st.user().hotkeySteps().pressSimpleHotKey(key(Keys.ENTER));
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Редактирование получателя хоткеями")
    @TestCaseId("2842")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68465")
    public void shouldChangeInYabblesWithHotkeys() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().inputsAddressInFieldTo(NO_NAME_CONTACT)
                .addAnotherRecipient(CONTACT);
            st.user().hotkeySteps().pressSimpleHotKey(key(Keys.ENTER));
            st.user().defaultSteps().clicksOn(st.pages().mail().compose().composeFieldsBlock().yabbleToList().get(0));
            st.user().hotkeySteps()
                .pressHotKeys(st.pages().mail().compose().composeFieldsBlock().fieldTo(), Keys.BACK_SPACE);
            st.user().defaultSteps()
                .shouldSeeElementsCount(st.pages().mail().compose().composeFieldsBlock().yabbleToList(), 1)
                .shouldSeeThatElementHasText(
                    st.pages().mail().compose().composeFieldsBlock().yabbleToList().get(0),
                    CONTACT_NAME
                )
                .clicksOn(
                    st.pages().mail().compose().composeFieldsBlock().yabbleTo(),
                    st.pages().mail().compose().composeYabbleDropdown().editYabble()
                );
            st.user().hotkeySteps()
                .pressHotKeys(st.pages().mail().compose().composeFieldsBlock().fieldTo(), Keys.ARROW_RIGHT)
                .pressHotKeys(st.pages().mail().compose().composeFieldsBlock().fieldTo(), Keys.BACK_SPACE)
                .pressSimpleHotKey(key(Keys.ENTER));
            st.user().defaultSteps().shouldSeeThatElementTextEquals(
                st.pages().mail().compose().composeFieldsBlock().yabbleNameTo(),
                CONTACT_NAME.substring(0, CONTACT_NAME.length() - 1)
            );
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }
}
