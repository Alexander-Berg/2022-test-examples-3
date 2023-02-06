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
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_SCROLL_3PANE_MESSAGELIST_DOWN;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Длинный список писем 3пейн")
@Features(FeaturesConst.THREE_PANE)
@Tag(FeaturesConst.THREE_PANE)
@Stories(FeaturesConst.GENERAL)
public class LargeMessage3PaneListTests extends BaseTest {

    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth);

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane-vertical",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        make40MessagesInInbox();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Подгрузка писем при скролле списка писем")
    @TestCaseId("5252")
    public void shouldLoadMoreMessagesAfterScroll() {
        user.defaultSteps().shouldSeeElementsCount(onMessagePage().displayedMessages().list(), 30)
            .executesJavaScript(SCRIPT_SCROLL_3PANE_MESSAGELIST_DOWN)
            .shouldSeeElementsCount(onMessagePage().displayedMessages().list(), 40);
    }

    @Step("Проверяем количество писем в ящике и если надо, дополняем до 40")
    private void make40MessagesInInbox() {
        int numOfMessagesInInbox = user.apiMessagesSteps().getAllMessagesInFolder(INBOX).size();
        if (numOfMessagesInInbox < 40) {
            for (int i = numOfMessagesInInbox; i < 40; i++) {
                user.apiMessagesSteps().sendMailWithNoSave(
                    lock.firstAcc(),
                    Utils.getRandomString(),
                    Utils.getRandomString()
                );
            }
        }
    }
}
