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
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * Created by cosmopanda on 13.04.2016.
 */
@Aqua.Test
@Title("Проверяем кнопку «Развернуть n непрочитанных» в треде")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.GENERAL)
public class ShowUnreadMsgInThreadTest extends BaseTest {

    private static final Integer THREAD_SIZE = 4;
    private static final String BTN_TEXT = "Развернуть непрочитанные";
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private static String SUBJECT = Utils.getRandomName();
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Просмотр писем в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
        user.apiMessagesSteps().sendThread(lock.firstAcc(), SUBJECT, THREAD_SIZE);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Проверка кнопки «развернуть n непрочитанных»")
    @TestCaseId("2141")
    public void shouldSeeUnreadMessages() {
        user.messagesSteps().clicksOnMessageWithSubject(SUBJECT);
        user.defaultSteps().shouldSee(onMessageView().expandUnreadBtn())
            .clicksOn(onMessageView().expandUnreadBtn())
            .shouldNotSee(
                onMessagePage().foldersNavigation().inboxUnreadCounter(),
                onMessageView().expandUnreadBtn()
            );
        user.messageViewSteps().shouldSeeExpandedMsgInThread(THREAD_SIZE);
    }

    @Test
    @Title("Проверка инкремента кнопки «развернуть n непрочитанных»")
    @TestCaseId("2134")
    public void shouldBeIncUnreadMessages() {
        user.messagesSteps().clicksOnMessageWithSubject(SUBJECT);
        user.defaultSteps().shouldSee(onMessageView().expandUnreadBtn())
            .shouldSeeThatElementTextEquals(
                onMessageView().expandUnreadBtn(),
                BTN_TEXT
            );
        user.apiMessagesSteps()
            .sendMessageToThreadWithSubjectWithNoSave(SUBJECT, lock.firstAcc(), Utils.getRandomString());
        user.defaultSteps().shouldSeeThatElementTextEquals(
            onMessageView().expandUnreadBtn(),
            BTN_TEXT
        );
    }
}
