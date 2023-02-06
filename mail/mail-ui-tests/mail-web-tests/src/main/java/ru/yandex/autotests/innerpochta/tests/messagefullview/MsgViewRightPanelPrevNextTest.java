package ru.yandex.autotests.innerpochta.tests.messagefullview;

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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Тест на «пред» и «след» в правой колонке")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.RIGHT_PANEL)
public class MsgViewRightPanelPrevNextTest extends BaseTest {

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private Message firstSingleMessageInBox;
    private Message secondSingleMessageInBox;
    private Message lastSingleMessageInBox;
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        lastSingleMessageInBox = user.apiMessagesSteps().sendMailWithNoSave(
            lock.firstAcc(),
            Utils.getRandomName(),
            Utils.getRandomString()
        );
        secondSingleMessageInBox = user.apiMessagesSteps().sendMailWithNoSave(
            lock.firstAcc(),
            Utils.getRandomName(),
            Utils.getRandomString()
        );
        firstSingleMessageInBox = user.apiMessagesSteps().sendMailWithNoSave(
            lock.firstAcc(),
            Utils.getRandomName(),
            Utils.getRandomString()
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Проверяем правильность переходов и наличия «пред.» и «след.» в одиночных письмах")
    @TestCaseId("3759")
    public void shouldInteractWithPrevNextButtons() {
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().shouldBeOnUrl(containsString(firstSingleMessageInBox.getMid()))
            .shouldSee(onMessageView().messageViewSideBar().nextBtn())
            .shouldNotSee(onMessageView().messageViewSideBar().prevBtn())
            .clicksOn(onMessageView().messageViewSideBar().nextBtn())
            .shouldBeOnUrl(containsString(secondSingleMessageInBox.getMid()))
            .shouldSee(
                onMessageView().messageViewSideBar().prevBtn(),
                onMessageView().messageViewSideBar().nextBtn()
            )
            .clicksOn(onMessageView().messageViewSideBar().nextBtn())
            .shouldBeOnUrl(containsString(lastSingleMessageInBox.getMid()))
            .shouldSee(onMessageView().messageViewSideBar().prevBtn())
            .shouldNotSee(onMessageView().messageViewSideBar().nextBtn())
            .clicksOn(onMessageView().messageViewSideBar().prevBtn())
            .shouldBeOnUrl(containsString(secondSingleMessageInBox.getMid()))
            .shouldSee(
                onMessageView().messageViewSideBar().prevBtn(),
                onMessageView().messageViewSideBar().nextBtn()
            );
    }

    @Test
    @Title("Проверяем правильность переходов и наличия «пред.» и «след.» в треде")
    @TestCaseId("3759")
    public void shouldInteractWithPrevNextButtonsInThread() {
        String threadSubj = Utils.getRandomName();
        Message firstThreadMsg = user.apiMessagesSteps().sendThread(
            lock.firstAcc(),
            threadSubj,
            2
        );
        Message lastThreadMsg = user.apiMessagesSteps().getAllMessagesWithSubject(threadSubj).get(1);
        user.defaultSteps().refreshPage();
        user.messagesSteps().expandsMessagesThread(threadSubj);
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().messagesInThread().get(1));
        user.defaultSteps().shouldBeOnUrl(containsString(lastThreadMsg.getMid()))
            .shouldSee(
                onMessageView().messageViewSideBar().prevBtn(),
                onMessageView().messageViewSideBar().nextBtn()
            )
            .clicksOn(onMessageView().messageViewSideBar().prevBtn())
            .shouldBeOnUrl(containsString(firstThreadMsg.getMid()))
            .shouldSee(onMessageView().messageViewSideBar().nextBtn())
            .shouldNotSee(onMessageView().messageViewSideBar().prevBtn())
            .clicksOn(onMessageView().messageViewSideBar().nextBtn())
            .shouldBeOnUrl(containsString(lastThreadMsg.getMid()))
            .clicksOn(onMessageView().messageViewSideBar().nextBtn())
            .shouldBeOnUrl(containsString(firstSingleMessageInBox.getMid()))
            .shouldSee(
                onMessageView().messageViewSideBar().prevBtn(),
                onMessageView().messageViewSideBar().nextBtn()
            );
    }
}
