package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Тест на включение/отключение тредов")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class GroupBySubjectTest extends BaseTest {

    private static final int THREAD_MESSAGES = 4;

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
    public void logIn() throws IOException {
        subject = Utils.getRandomName();
        user.apiMessagesSteps().sendThread(lock.firstAcc(), subject, THREAD_MESSAGES);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Включаем галочку «группировать письма»")
    @TestCaseId("1513")
    public void testEnableGroupBySubject() {
        user.messagesSteps().enablesGroupBySubject()
            .shouldSeeThreadCounter(subject, THREAD_MESSAGES);
    }

    @Test
    @Title("Выключаем галочку «группировать письма»")
    @TestCaseId("1514")
    public void testDisableGroupBySubject() {
        user.messagesSteps().disablesGroupBySubject();
        user.defaultSteps().shouldNotSee(onMessagePage().displayedMessages().list().get(0).threadCounter());
    }
}
