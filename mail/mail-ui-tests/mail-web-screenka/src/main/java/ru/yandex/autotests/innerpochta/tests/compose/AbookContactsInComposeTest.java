package ru.yandex.autotests.innerpochta.tests.compose;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
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

import java.util.Set;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Попап контактов в композе")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AbookContactsInComposeTest {

    private static final int CONTACTS_COUNT = 4;
    private static final String YABBLE_TO_SUGGEST = "yandex";

    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".b-banner"),
        cssSelector(".mail-Logo-Yandex"),
        cssSelector("[id = 'js-messages-direct']")
    );

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
        stepsProd.user().apiAbookSteps().addCoupleOfContacts(5);
    }

    @Test
    @Title("Открываем попап добавления контактов кликом в «Скрытая копия»")
    @TestCaseId("3088")
    public void shouldSeeAbookPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().expandCollapseBtn())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().popupBcc())
                .clicksOn(st.pages().mail().composePopup().abookBtn())
                .shouldSee(st.pages().mail().composePopup().abookPopup());

        parallelRun.withActions(actions).withAdditionalIgnoredElements(IGNORE_THIS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть zero саджест контактов")
    @TestCaseId("3216")
    public void shouldSeeSuggestContacts() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().home().composeButton())
            .shouldSee(st.pages().mail().composePopup().expandedPopup())
            .clicksOn(st.pages().mail().composePopup().expandedPopup().popupTo())
            .shouldSee(st.pages().mail().composePopup().suggestList().waitUntil(not(empty())).get(0));

        parallelRun.withActions(actions).withAdditionalIgnoredElements(IGNORE_THIS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть контрол «Показать все контакты»")
    @TestCaseId("3015")
    public void shouldSeeShowAllContactsButton() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().home().composeButton())
            .shouldSee(st.pages().mail().composePopup().expandedPopup())
            .clicksOn(st.pages().mail().composePopup().expandedPopup().popupTo())
            .shouldSee(
                st.pages().mail().composePopup().suggestList().waitUntil(not(empty())).get(CONTACTS_COUNT),
                st.pages().mail().composePopup().abookBtn()
            );

        parallelRun.withActions(actions).withAdditionalIgnoredElements(IGNORE_THIS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем абук из саджеста при наборе контакта")
    @TestCaseId("3015")
    public void shouldOpenAbookFromYabbleSuggest() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().home().composeButton())
            .shouldSee(st.pages().mail().composePopup().expandedPopup())
            .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().popupTo(), YABBLE_TO_SUGGEST)
            .shouldSee(
                st.pages().mail().composePopup().suggestList().waitUntil(not(empty())).get(CONTACTS_COUNT),
                st.pages().mail().composePopup().abookBtn()
            )
            .clicksOn(st.pages().mail().composePopup().abookBtn())
            .shouldSee(st.pages().mail().abook().abookPopup());

        parallelRun.withActions(actions).withAdditionalIgnoredElements(IGNORE_THIS).withAcc(lock.firstAcc()).run();
    }
}