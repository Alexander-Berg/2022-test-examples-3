package ru.yandex.autotests.innerpochta.tests.autotests.Search;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_TOUCH;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Просмотр писем в поиске")
@Features(FeaturesConst.SEARCH)
@Stories({FeaturesConst.FULL_VIEW})
public class ViewInSearchTest {

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    private String subj;

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        subj = Utils.getRandomName();
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SEARCH_TOUCH.makeTouchUrlPart());
    }

    @Test
    @Title("Из просмотра письма возвращаемся в поисковую выдачу")
    @TestCaseId("306")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldBackFromMsgViewInSearch() {
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), subj, "");
        findAndOpenMsg();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().header().backToListBtn())
            .shouldSee(steps.pages().touch().search().messageBlock());
    }

    @Test
    @Title("Просмотр черновиков из поиска")
    @TestCaseId("1083")
    public void shouldOpenDraftInSearch() {
        steps.user().apiMessagesSteps().createDraftWithSubject(subj);
        findAndOpenMsg();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().toolbar().openDraft());
    }

    @Test
    @Title("Просмотр шаблонов из поиска")
    @TestCaseId("1083")
    public void shouldOpenTemplateInSearch() {
        subj = steps.user().apiMessagesSteps().createTemplateMessage(accLock.firstAcc());
        steps.user().defaultSteps().refreshPage();
        findAndOpenMsg();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().toolbar().openDraft());
    }

    @Test
    @Title("Просмотр письма из поиска")
    @TestCaseId("1114")
    public void shouldOpenMsgInSearch() {
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "");
        findAndOpenMsg();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().toolbar().delete());
    }

    @Test
    @Title("Просмотр треда из поиска")
    @TestCaseId("1114")
    public void shouldOpenThreadInSearch() {
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), subj, "");
        findAndOpenMsg();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().threadHeader());
    }

    @Test
    @Title("Должны удалить письмо из просмотра письма в поиске")
    @TestCaseId("1088")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldDeleteMsgFromViewInSearch() {
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "");
        findAndOpenMsg();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().toolbar().delete())
            .shouldSee(steps.pages().touch().search().messageBlock().avatarDeleteMsg());
    }

    @Test
    @Title("Должны удалить письмо из просмотра письма в поиске")
    @TestCaseId("1088")
    @Description("В случае фикса совместить с тестом для телефонов")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("QUINN-7023")
    public void shouldDeleteMsgFromViewInSearchTablet() {
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "");
        findAndOpenMsg();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().toolbar().delete())
            .shouldSee(steps.pages().touch().search().messageBlock().avatarDeleteMsg());
    }

    @Step("Открываем письмо, раскрываем детали письма")
    private void findAndOpenMsg() {
        steps.user().defaultSteps().inputsTextInElement(steps.pages().touch().search().header().input(), subj)
            .clicksOn(steps.pages().touch().search().header().find())
            .clicksOn(steps.pages().touch().search().messageBlock());
    }
}
