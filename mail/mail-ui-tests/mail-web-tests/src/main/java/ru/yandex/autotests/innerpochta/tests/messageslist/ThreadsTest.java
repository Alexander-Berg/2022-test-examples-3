package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
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
 * @author mariya-murm
 */
@Aqua.Test
@Title("Действия с письмами в тредах")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.THREAD)
public class ThreadsTest extends BaseTest {

    private String firstlineTwo;
    private String firstlineOne;
    private String subject;
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
    public void logIn() {
        subject = getRandomString();
        firstlineOne = getRandomString();
        firstlineTwo = getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, firstlineOne);
        user.apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(subject, lock.firstAcc(), firstlineTwo);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Firstline треда изменяется при удалении письма из треда")
    @TestCaseId("4755")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-64025")
    public void shouldSeeFirstlineChangesAfterDeletingMsgInThread() {
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages())
            .shouldHasText(onMessagePage().displayedMessages().list().get(0).firstLine(), firstlineTwo);
        user.messagesSteps().expandsMessagesThread(subject)
            .selectMessagesInThreadCheckBoxWithNumber(0);
        user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().deleteButton())
            .shouldSee(onMessagePage().displayedMessages())
            .shouldHasText(onMessagePage().displayedMessages().list().get(0).firstLine(), firstlineOne);
    }
}
