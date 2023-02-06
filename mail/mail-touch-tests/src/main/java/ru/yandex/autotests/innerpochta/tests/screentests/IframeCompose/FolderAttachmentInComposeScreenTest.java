package ru.yandex.autotests.innerpochta.tests.screentests.IframeCompose;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на аттач-папку с диска в композе")
@Description("У пользователя подготовлена папка на диске")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.ATTACHES)
public class FolderAttachmentInComposeScreenTest {

    private TouchScreenRulesManager rules = touchScreenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(removeAllMessages(() -> stepsProd.user(), DRAFT));

    @Test
    @Title("Добавляем аттач-папку с диска")
    @TestCaseId("1393")
    public void shouldSeeDiskAttachments() {
        Consumer<InitStepsRule> actions = steps -> {
            steps.user().touchSteps().switchToComposeIframe();
            steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().header().clip())
                .clicksOn(steps.pages().touch().composeIframe().attachFilesPopup().fromDisk())
                .turnTrue(steps.pages().touch().composeIframe().diskAttachmentsPage().checkbox().get(0))
                .clicksOn(steps.pages().touch().composeIframe().diskAttachmentsPage().attachBtn())
                .shouldSee(steps.pages().touch().composeIframe().attachments().uploadedAttachment());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }
}
