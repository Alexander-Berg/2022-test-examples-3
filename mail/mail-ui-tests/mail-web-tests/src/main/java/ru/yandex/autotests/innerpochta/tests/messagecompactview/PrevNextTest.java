package ru.yandex.autotests.innerpochta.tests.messagecompactview;

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
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Блок пред/след при просмотре письма в списке писем")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.NEXT_PREV)
public class PrevNextTest extends BaseTest {

    private static final int MSG_COUNT = 3;
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));
    private List<Message> messages;

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем и 2Pane",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), MSG_COUNT);
        messages = user.apiMessagesSteps().getAllMessages();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Проверяем наличие блока пред/след")
    @TestCaseId("1613")
    public void shouldSeePrevNextButtons() {
        user.messagesSteps().clicksOnMessageByNumber(1);
        user.defaultSteps().shouldSee(
            onMessageView().prevMessageCompact(),
            onMessageView().nextMessageCompact()
        );
    }

    @Test
    @Title("Проверяем открытие следующего сообщения")
    @TestCaseId("1613")
    public void shouldOpenNextMsg() {
        user.messagesSteps().clicksOnMessageByNumber(1);
        user.defaultSteps().clicksOn(onMessageView().nextMessageCompact());
        user.messageViewSteps().shouldSeeMessageSubjectInCompactView(messages.get(2).getSubject());
    }

    @Test
    @Title("Проверяем открытие предыдущего сообщения")
    @TestCaseId("1613")
    public void shouldOpenPrevMsg() {
        user.messagesSteps().clicksOnMessageByNumber(1);
        user.defaultSteps().clicksOn(onMessageView().prevMessageCompact());
        user.messageViewSteps().shouldSeeMessageSubjectInCompactView(messages.get(0).getSubject());
    }
}
