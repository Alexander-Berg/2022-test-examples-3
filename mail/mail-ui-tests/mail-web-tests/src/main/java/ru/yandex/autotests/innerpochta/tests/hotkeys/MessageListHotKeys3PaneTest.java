package ru.yandex.autotests.innerpochta.tests.hotkeys;


import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
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

import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.FORWARD_PREFIX;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;

@Aqua.Test
@Title("Тест на хот кеи для работы со списком писем для 3х панельного интерфейса")
@Features(FeaturesConst.HOT_KEYS)
@Tag(FeaturesConst.HOT_KEYS)
@Stories(FeaturesConst.GENERAL)
public class MessageListHotKeys3PaneTest extends BaseTest {

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
        user.apiSettingsSteps().callWithListAndParams(
                "Включаем 3-PANE",
                of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Выделяем + q - Помечаем прочитанным в 3pane")
    @TestCaseId("1432")
    public void testMarkAsReadHotKey3Pane() {
        user.messagesSteps().selectMessageWithSubject(subject);
        user.hotkeySteps().pressSimpleHotKey("q");
        user.defaultSteps().opensFragment(QuickFragments.UNREAD);
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().selectMessageWithSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "u");
        user.defaultSteps().opensFragment(QuickFragments.UNREAD);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Выделяем + i - Помечаем важным в 3pane")
    @TestCaseId("1433")
    public void testMarkImportantHotKey3Pane() {
        user.messagesSteps().selectMessageWithSubject(subject);
        user.hotkeySteps().pressSimpleHotKey("i");
        user.messagesSteps().shouldSeeThatMessageIsImportant(subject);
    }

    @Test
    @Title("Выделяем + l - Помечаем кастомной меткой в 3pane")
    @TestCaseId("1434")
    public void testLabelMessageHotKey3Pane() {
        user.messagesSteps().selectMessageWithSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "l");
        user.defaultSteps().shouldSee(user.pages().MessagePage().labelsDropdownMenu().labelImportant());
    }

    @Test
    @Title("Выделяем + m - Перемещаем в кастомную папку в 3pane")
    @TestCaseId("1435")
    public void testMoveMessageHotKey3Pane() {
        user.messagesSteps().selectMessageWithSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "m");
        user.defaultSteps().shouldSee(onMessagePage().moveMessageDropdownMenu().inboxFolder());
    }

    @Test
    @Title("Выделяем + DELETE - Удаляем в 3pane")
    @TestCaseId("1436")
    public void testDeleteMessageHotKey3Pane() {
        user.messagesSteps().selectMessageWithSubject(subject);
        user.hotkeySteps().pressSimpleHotKey(key(Keys.DELETE));
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
    }

    @Test
    @Title("u - Помечаем непрочитанным в 3pane")
    @TestCaseId("1437")
    public void testCheckUnreadMessagesHotKey3Pane() {
        user.messagesSteps().shouldSeeMessagesPresent();
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "u");
        user.defaultSteps().shouldBeOnUrl(lock.firstAcc(), QuickFragments.UNREAD);
    }

    @Test
    @Title("i - Переходим в  «Важные» в 3pane")
    @TestCaseId("1438")
    public void testCheckImportantMessagesHotKey3Pane() {
        user.messagesSteps().shouldSeeMessagesPresent();
        user.hotkeySteps().pressSimpleHotKey("i");
        user.defaultSteps().shouldBeOnUrlWith(QuickFragments.LABEL);
    }

    @Test
    @Title("Выделяем + f - Пересылаем в 3pane")
    @TestCaseId("1439")
    public void testForwardMessageHotKey3Pane() {
        user.messagesSteps().selectMessageWithSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "f");
        user.defaultSteps().shouldContainValue(onComposePopup().expandedPopup().sbjInput(), FORWARD_PREFIX);
    }
}
