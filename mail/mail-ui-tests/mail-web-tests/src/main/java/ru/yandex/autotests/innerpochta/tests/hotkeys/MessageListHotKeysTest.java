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

@Aqua.Test
@Title("Тест на хот кеи для работы со списком писем для стандартного интерфейса")
@Features(FeaturesConst.HOT_KEYS)
@Tag(FeaturesConst.HOT_KEYS)
@Stories(FeaturesConst.GENERAL)
public class MessageListHotKeysTest extends BaseTest {

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
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("u - Помечаем прочитанным")
    @TestCaseId("1440")
    public void testMarkAsReadHotKey() {
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
        user.hotkeySteps().pressSimpleHotKey("q");
        user.leftColumnSteps().clicksOnUnreadMessages();
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "u");
        user.leftColumnSteps().clicksOnUnreadMessages();
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Выделяем + i - Помечаем важным")
    @TestCaseId("1441")
    public void testMarkImportantHotKey() {
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
        user.hotkeySteps().pressSimpleHotKey("i");
        user.messagesSteps().shouldSeeThatMessageIsImportant(subject);
    }

    @Test
    @Title("l - Помечаем кастомной меткой")
    @TestCaseId("1442")
    public void testLabelMessageHotKey() {
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "l");
        user.defaultSteps().shouldSee(user.pages().MessagePage().labelsDropdownMenu().labelImportant());
    }

    @Test
    @Title("m - Перемещаем в кастомную папку")
    @TestCaseId("1443")
    public void testMoveMessageHotKey() {
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "m");
        user.defaultSteps().shouldSee(user.pages().MessagePage().moveMessageDropdownMenu().inboxFolder());
    }

    @Test
    @Title("DELETE - Удаляем")
    @TestCaseId("1444")
    public void testDeleteMessageHotKey() {
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
        user.hotkeySteps().pressSimpleHotKey(key(Keys.DELETE));
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
    }

    @Test
    @Title("u - Переходим в непрочитанные")
    @TestCaseId("1445")
    public void testCheckUnreadMessagesHotKey() {
        user.messagesSteps().shouldSeeMessagesPresent();
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "u");
        user.defaultSteps().shouldBeOnUrl(lock.firstAcc(), QuickFragments.UNREAD);
    }

    @Test
    @Title("i - Переходим в важные")
    @TestCaseId("1446")
    public void testCheckImportantMessagesHotKey() {
        user.messagesSteps().shouldSeeMessagesPresent();
        user.hotkeySteps().pressSimpleHotKey("i");
        user.defaultSteps().shouldBeOnUrlWith(QuickFragments.LABEL);
    }

    @Test
    @Title("f - Пересылаем")
    @TestCaseId("1447")
    public void testForwardMessageHotKey() {
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "f");
        user.defaultSteps().shouldContainValue(onComposePopup().expandedPopup().sbjInput(), FORWARD_PREFIX);
    }
}
