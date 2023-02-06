package ru.yandex.autotests.innerpochta.tests.autotests.iframeCompose;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.pages.ComposeIframePage.IFRAME_COMPOSE;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_RED_COLOR;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на попап меток в композе")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.ADDITIONAL)
public class ComposeLabelsTest {

    private static final String SETTING_URL_PART = "settings/folders";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        steps.user().apiLabelsSteps().addNewLabel(Utils.getRandomName(), LABELS_PARAM_GREEN_COLOR);
        steps.user().apiLabelsSteps().addNewLabel(Utils.getRandomName(), LABELS_PARAM_RED_COLOR);
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().touchSteps().openComposeViaUrl();
    }

    @Test
    @Title("Должны закрыть попап меток")
    @TestCaseId("1515")
    public void shouldCloseLabelsPopup() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().composeIframe().header().labels())
            .shouldSee(steps.pages().touch().composeIframe().labelsPopup())
            .clicksOn(steps.pages().touch().composeIframe().labelsPopup().closeBtn())
            .shouldNotSee(steps.pages().touch().composeIframe().labelsPopup());
    }

    @Test
    @Title("Должны перейти к редактированию меток из попапа в композе")
    @TestCaseId("1510")
    public void shouldOpenEditLabelFromCompose() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().composeIframe().header().labels())
            .clicksOn(steps.pages().touch().composeIframe().labelsPopup().editLabels())
            .shouldBeOnUrl(containsString(SETTING_URL_PART));
    }

    @Test
    @Title("Должны вернуться из редактрирования меток к сохранённому черновику")
    @TestCaseId("1516")
    public void shouldBackToDraftFromSettings() {
        steps.user().defaultSteps()
            .inputsTextInElement(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
            .clicksOn(steps.pages().touch().composeIframe().header().labels())
            .clicksOn(steps.pages().touch().composeIframe().labelsPopup().editLabels())
            .shouldBeOnUrl(containsString(SETTING_URL_PART))
            .clicksOn(steps.pages().touch().settings().closeBtn())
            .shouldSee(steps.pages().touch().composeIframe().iframe())
            .switchTo(IFRAME_COMPOSE)
            .shouldContainText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL);
    }

    @Test
    @Title("Должны удалить несколько меток в композе")
    @TestCaseId("1517")
    public void shouldDeleteLabelsInCompose() {
        addLabel();
        addLabel();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().composeIframe().deleteLabel())
            .shouldSee(steps.pages().touch().composeIframe().labels())
            .clicksOn(steps.pages().touch().composeIframe().deleteLabel())
            .shouldNotSee(steps.pages().touch().composeIframe().labels());
    }

    @Test
    @Title("Использованная метка должна исчезнуть из списка меток")
    @TestCaseId("1519")
    public void shouldNotSeeUsedLabelInList() {
        addLabel();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().composeIframe().header().labels())
            .shouldSeeElementsCount(steps.pages().touch().composeIframe().labelsPopup().labels(), 2)
            .clicksOn(steps.pages().touch().composeIframe().labelsPopup().closeBtn())
            .clicksOn(steps.pages().touch().composeIframe().deleteLabel())
            .clicksOn(steps.pages().touch().composeIframe().header().labels())
            .shouldSeeElementsCount(steps.pages().touch().composeIframe().labelsPopup().labels(), 3);
    }

    @Step("Добавить метку")
    private void addLabel() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().header().labels())
            .shouldSee(steps.pages().touch().composeIframe().labelsPopup())
            .clicksOn(steps.pages().touch().composeIframe().labelsPopup().labels().get(0));
    }
}
