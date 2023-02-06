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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDD_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Тесты на Mute")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.PDD_GENERAL)
public class MuteTest extends BaseTest {

    private static final int THREAD_SIZE = 3;

    private AccLockRule lock = AccLockRule.use().useTusAccount(PDD_USER_TAG);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    private String subj;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        subj = getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subj, "");
        user.apiMessagesSteps().sendThread(lock.firstAcc(), getRandomString(), THREAD_SIZE);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessagesPresent();
    }

    @Test
    @Title("Иконка мьюта есть только в шапке треда")
    @TestCaseId("1957")
    public void muteIconInThreadHeaderOnly() {
        user.defaultSteps().clicksOn(
            onMessagePage().displayedMessages().list().get(0).messageUnread(),
            onMessagePage().displayedMessages().list().get(0).threadMute(),
            onMessagePage().displayedMessages().list().get(0).expandThread()
        );
        onMessagePage().displayedMessages().messagesInThread().waitUntil(not(empty()));
        for (int i = 0; i < THREAD_SIZE; ++i) {
            user.defaultSteps().shouldSee(onMessagePage().displayedMessages().messagesInThread().get(i).messageRead())
                .shouldNotSee(onMessagePage().displayedMessages().messagesInThread().get(i).threadMute());
        }
    }

    @Test
    @Title("Иконка мьюта в шапке треда меняется на прыщ непрочитанности")
    @TestCaseId("1957")
    public void muteIconIsReplacedWithUnreadButton() {
        user.defaultSteps().clicksOn(
            onMessagePage().displayedMessages().list().get(0).messageUnread(),
            onMessagePage().displayedMessages().list().get(0).threadMute(),
            onMessagePage().displayedMessages().list().get(0).expandThread()
        )
            .clicksOn(
                onMessagePage().displayedMessages().messagesInThread().waitUntil(not(empty())).get(0).messageRead()
            )
            .shouldSee(onMessagePage().displayedMessages().list().get(0).messageRead())
            .shouldNotSee(onMessagePage().displayedMessages().list().get(0).threadMute());
    }

    @Test
    @Title("Иконка мьюта в шапке письма не появлвяется")
    @TestCaseId("1957")
    public void shouldNotSeeMuteButtonInMessageHeader() {
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().list().get(0).expandThread())
            .clicksOn(
                onMessagePage().displayedMessages().messagesInThread().waitUntil(not(empty())).get(0).messageUnread()
            )
            .shouldNotSee(onMessagePage().displayedMessages().list().get(0).threadMute())
            .clicksOn(onMessagePage().displayedMessages().messagesInThread().get(0).messageRead())
            .shouldNotSee(onMessagePage().displayedMessages().list().get(0).threadMute());
    }

    @Test
    @Title("Письмо с замьюченной темой должно прочитаться")
    @TestCaseId("1959")
    public void shouldMarkMutedMessageAsRead() {
        user.defaultSteps().clicksOn(
            onMessagePage().displayedMessages().list().get(1).messageUnread(),
            onMessagePage().displayedMessages().list().get(1).threadMute()
        );
        user.apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(subj, lock.firstAcc(), "");
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeThreadCounter(subj, 2);
        assertEquals(
            "Тред не переместился на первое место",
            subj,
            onMessagePage().displayedMessages().list().get(0).subject().getText()
        );
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages().list().get(0).threadMute())
            .clicksOn(onMessagePage().displayedMessages().list().get(0).expandThread())
            .shouldSee(
                onMessagePage().displayedMessages().messagesInThread().waitUntil(not(empty())).get(0).messageRead()
            );
    }
}
