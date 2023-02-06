package ru.yandex.autotests.innerpochta.tests.messagefullview;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
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

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Проверяем количество писем в треде после удаления")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.GENERAL)
public class DeleteMessageTest extends BaseTest {

    private static final int THREAD_SIZE = 3;
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));
    private Message firstThreadMsg;

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем тредный режим",
            of(SETTINGS_FOLDER_THREAD_VIEW, TRUE)
        );
        firstThreadMsg = user.apiMessagesSteps()
            .sendThread(lock.firstAcc(), Utils.getRandomName(), THREAD_SIZE);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Удаляем письмо из треда")
    @TestCaseId("4357")
    public void shouldDeleteMessageFromThread() {
        user.messagesSteps().shouldSeeThreadCounter(firstThreadMsg.getSubject(), THREAD_SIZE);
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(firstThreadMsg.getMid()));
        user.defaultSteps().clicksOn(onMessageView().toolbar().deleteButton());
        user.messagesSteps().shouldSeeThreadCounter(firstThreadMsg.getSubject(), THREAD_SIZE - 1);
    }
}
