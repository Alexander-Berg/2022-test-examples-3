package ru.yandex.autotests.innerpochta.tests.messagecompactview;


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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Открыть письмо в новой вкладке из компактного просмотра")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.GENERAL)
public class OpenMessageInNewTabTest extends BaseTest {

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));
    private Message msg;

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем открывание письма в списке писем, 2Pane и треды",
            of(SETTINGS_OPEN_MSG_LIST, TRUE)
        );
        msg = user.apiMessagesSteps().sendMailWithNoSave(
            lock.firstAcc(), getRandomName(), getRandomString()
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessagesPresent();
    }

    @Test
    @Title("Открыть письмо в новой вкладке нажатием на дату при просмотре в списке писем")
    @TestCaseId("937")
    public void shouldOpenMessageInNewTabByClickingOnDate() {
        user.messagesSteps().clicksOnMessageByNumber(0)
            .opensMsgFullView();
        user.defaultSteps()
            .shouldBeOnUrl(lock.firstAcc(), QuickFragments.MSG_FRAGMENT.makeUrlPart(msg.getMid()));
    }
}
