package ru.yandex.autotests.innerpochta.tests.compose;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author crafty
 */

@Aqua.Test
@Title("Новый композ - Удаление черновика из композа")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
public class ComposeDeleteDraftTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE);
    }

    @Test
    @Title("Удаление пустого черновика из композа")
    @TestCaseId("4679")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68483")
    public void shouldDeleteEmptyDraft() {
        user.defaultSteps()
            .clicksOn(onComposePopup().expandedPopup().composeDeleteBtn())
            .shouldBeOnUrlWith(QuickFragments.INBOX);
        user.defaultSteps().clicksOn(user.pages().MessagePage().foldersNavigation().draftFolder());
        user.messagesSteps().shouldSeeThatFolderIsEmpty();
    }

    @Test
    @Title("Удаление сохранённого черновика из композа")
    @TestCaseId("4680")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68483")
    public void shouldDeleteSavedDraft() {
        user.composeSteps()
            .inputsSendText(getRandomString())
            .shouldSeeThatMessageIsSavedToDraft();
        user.defaultSteps().clicksOn(user.pages().MessagePage().foldersNavigation().draftFolder())
            .refreshPage();
        user.leftColumnSteps().shouldSeeDraftCounter(1);
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps()
            .clicksOn(onComposePopup().expandedPopup().composeDeleteBtn())
            .shouldBeOnUrlWith(QuickFragments.DRAFT);
        user.messagesSteps().shouldSeeThatFolderIsEmpty();
    }
}
