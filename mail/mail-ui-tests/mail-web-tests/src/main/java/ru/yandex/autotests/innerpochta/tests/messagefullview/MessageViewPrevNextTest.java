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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тест блок пред/след")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.NEXT_PREV)
@Stories(FeaturesConst.NEXT_PREV)
public class MessageViewPrevNextTest extends BaseTest {
    private static final int MSG_COUNT = 3;

    private Message msg1;
    private Message msg2;
    private Message msg3;

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
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, EMPTY_STR)
        );
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), MSG_COUNT);
        List<Message> messages = user.apiMessagesSteps().getAllMessages();
        msg1 = messages.get(0);
        msg2 = messages.get(1);
        msg3 = messages.get(2);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Проверяем наличие блока пред/след")
    @TestCaseId("2165")
    public void shouldSeeNextMsgBlock() {
        user.messagesSteps().clicksOnMessageWithSubject(msg1.getSubject());
        user.defaultSteps().shouldSee(
            onMessageView().messageViewSideBar(),
            onMessageView().messageViewSideBar().nextBtn()
        )
            .shouldNotSee(onMessageView().messageViewSideBar().prevBtn());
    }

    @Test
    @Title("Проверяем открытие следующего сообщения")
    @TestCaseId("2175")
    public void shouldOpenNextMsg() {
        user.messagesSteps().clicksOnMessageWithSubject(msg1.getSubject());
        user.defaultSteps().clicksOn(onMessageView().messageViewSideBar().nextBtn());
        user.messageViewSteps().shouldSeeMessageSubject(msg2.getSubject());
        user.defaultSteps().shouldSee(
            onMessageView().messageViewSideBar().nextBtn(),
            onMessageView().messageViewSideBar().prevBtn()
        );
    }

    @Test
    @Title("Проверяем открытие предыдущего сообщения")
    @TestCaseId("2176")
    public void shouldOpenPrevMsg() {
        user.messagesSteps().clicksOnMessageWithSubject(msg2.getSubject());
        user.defaultSteps().clicksOn(onMessageView().messageViewSideBar().prevBtn());
        user.messageViewSteps().shouldSeeMessageSubject(msg1.getSubject());
    }
}
