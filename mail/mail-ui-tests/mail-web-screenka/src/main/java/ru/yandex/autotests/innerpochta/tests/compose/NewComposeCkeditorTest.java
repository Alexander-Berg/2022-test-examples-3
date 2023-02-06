package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
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
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.KeysOwn;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.MORDA_URL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Сkeditor")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class NewComposeCkeditorTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredElements(IGNORED_ELEMENTS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Автоматически обворачиваем в ссылку")
    @TestCaseId("3353")
    public void shouldSeeAddedLink() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .inputsTextInElement(
                    st.pages().mail().composePopup().expandedPopup().bodyInput(),
                    MORDA_URL
                );
            st.user().hotkeySteps().pressHotKeys(
                st.pages().mail().composePopup().expandedPopup().bodyInput(),
                Keys.SPACE
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вставляем картинку из буфера обмена")
    @TestCaseId("4485")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-69602")
    public void shouldPasteImageFromBuffer() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .copyScreenInTransferBuffer();
            st.user().hotkeySteps().pressCombinationOfHotKeys(
                st.pages().mail().composePopup().expandedPopup().bodyInput(),
                KeysOwn.key(Keys.CONTROL),
                "v"
            );
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
