package ru.yandex.autotests.innerpochta.tests.screentests.MessageView;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule.addLabelIfNeed;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты просмотр письма")
@Description("У пользователя приготовлены письма: с аттачем в 300Мб, с инлайн аттачем, html-письмо, длинное письмо")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class SpecificMessagesScreenTest {

    private static final String SUBJ_HEAVY_ATTACH = "Heavy attachment";

    private TouchScreenRulesManager rules = touchScreenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private AddLabelIfNeedRule addLabel = addLabelIfNeed(() -> stepsTest.user());
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(addLabel);

    @Test
    @Title("Открытие письма с тяжёлым аттачем")
    @TestCaseId("593")
    public void shouldSeeHeavyAttachment() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSee(st.pages().touch().messageList().messageBlock())
            .clicksOnElementWithText(st.pages().touch().messageList().subjectList(), SUBJ_HEAVY_ATTACH)
            .shouldNotSee(st.pages().touch().messageView().msgLoaderInView())
            .shouldSee(st.pages().touch().messageView().attachments().get(0));

        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(FOLDER_ID.makeTouchUrlPart("8")).run();
    }

    @Test
    @Title("Просмотр html-письма")
    @TestCaseId("93")
    @DataProvider({"0", "1", "2", "3"})
    public void shouldSeeHtmlLetter(int num) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().touch().messageList().messages().waitUntil(not(empty())).get(num))
            .shouldSee(st.pages().touch().messageView().msgBody());

        parallelRun.withAcc(acc.firstAcc()).withActions(actions)
            .withUrlPath(FOLDER_ID.makeTouchUrlPart("10")).run();
    }
}
