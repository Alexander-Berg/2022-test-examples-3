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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.FORWARD_PREFIX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_USER_NAME;

/**
 * @author arttimofeev
 */
@Aqua.Test
@Title("Тест на хот кеи при чтении письма для стандартного интерфейса")
@Features(FeaturesConst.HOT_KEYS)
@Tag(FeaturesConst.HOT_KEYS)
@Stories(FeaturesConst.GENERAL)
public class MessageViewHotKeysTest extends BaseTest {

    private static final String RECIEVER_EMAIL = "yandex-team-31613.23887@yandex.ru";

    private Message msg;
    private Message newerMsg;
    private Message olderMsg;
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
    public void logIn() throws InterruptedException, IOException {
        olderMsg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        msg = user.apiMessagesSteps().addCcEmails(RECIEVER_EMAIL)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), Utils.getRandomName(), "");
        newerMsg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
        user.messageViewSteps().shouldSeeMessageSubject(msg.getSubject());
    }

    @Test
    @Title("SHIFT + u - Помечаем прочитанным")
    @TestCaseId("1424")
    public void testMarkAsReadMessageHotKeyWhenReading() {
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "u");
        user.defaultSteps().shouldBeOnUrlWith(QuickFragments.INBOX)
            .opensFragment(QuickFragments.UNREAD);
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("SHIFT + i - Помечаем важным")
    @TestCaseId("1425")
    public void testMarkImportantMessageHotKeyWhenReading() {
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "i");
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeThatMessageIsImportant(msg.getSubject());
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "i");
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeThatMessageIsNotImportant(msg.getSubject());
    }

    @Test
    @Title("SHIFT + s - Помечаем как спам")
    @TestCaseId("1426")
    public void testMarkAsSpamMessageHotKeyWhenReading() {
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "s");
        user.defaultSteps().opensFragment(QuickFragments.SPAM);
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_RIGHT))
            .pressCombinationOfHotKeys(key(Keys.SHIFT), "s");
        user.defaultSteps().clicksIfCanOn(onMessagePage().statusLineBlock().notSpamBtn())
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("DELETE - Удаляем")
    @TestCaseId("1427")
    public void testDeleteMessageHotKeyWhenReading() {
        user.hotkeySteps().pressSimpleHotKey(key(Keys.DELETE));
        user.defaultSteps().opensFragment(TRASH);
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("SHIFT + e - Отвечаем отправителю")
    @TestCaseId("1428")
    public void testReplyHotKeyWhenReading() {
        String selfName = user.apiSettingsSteps().getUserSettings(SETTINGS_USER_NAME);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "e");
        user.defaultSteps().shouldNotSee(onComposePage().composeFieldsBlock().fieldCc());
        user.composeSteps().shouldSeeSendToAreaHas(selfName);
    }

    @Test
    @Title("SHIFT + r - Отвечаем всем.")
    @TestCaseId("4462")
    public void testReplyAllHotKeyWhenReading() {
        String selfName = user.apiSettingsSteps().getUserSettings(SETTINGS_USER_NAME);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "r");
        user.composeSteps().shouldSeeSendToAreaHas(selfName)
            .shouldSeeCCAreaContains(RECIEVER_EMAIL);
    }

    @Test
    @Title("SHIFT + l - Помечаем сообщение кастомной меткой")
    @TestCaseId("1429")
    public void testLabelMessageHotKeyWhenReading() {
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "l");
        user.defaultSteps().shouldSee(user.pages().MessagePage().labelsDropdownMenu().labelImportant());
    }

    @Test
    @Title("SHIFT + m - Перемещаем сообщение в папку")
    @TestCaseId("1430")
    public void testMoveMessageHotKeyWhenReading() {
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "m");
        user.defaultSteps().shouldSee(user.pages().MessagePage().moveMessageDropdownMenu().inboxFolder());
    }

    @Test
    @Title("SHIFT + f - Пересылаем")
    @TestCaseId("1431")
    public void testForwardMessageHotKeyWhenReading() {
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "f");
        user.defaultSteps().shouldContainValue(onComposePopup().expandedPopup().sbjInput(), FORWARD_PREFIX);
    }

    @Test
    @Title("p - Переходим к более новому письму")
    @TestCaseId("3378")
    public void testOpenNewerMessageByPKeyWhenReading() {
        user.hotkeySteps().pressSimpleHotKey("p");
        user.messageViewSteps().shouldSeeMessageSubject(newerMsg.getSubject());
    }

    @Test
    @Title("j - Переходим к более новому письму")
    @TestCaseId("3378")
    public void testOpenNewerMessageByJKeyWhenReading() {
        user.hotkeySteps().pressSimpleHotKey("j");
        user.messageViewSteps().shouldSeeMessageSubject(newerMsg.getSubject());
    }

    @Test
    @Title("n - Переходим к более старому письму")
    @TestCaseId("3378")
    public void testOpenNewerMessageByNKeyWhenReading() {
        user.hotkeySteps().pressSimpleHotKey("n");
        user.messageViewSteps().shouldSeeMessageSubject(olderMsg.getSubject());
    }

    @Test
    @Title("k - Переходим к более старому письму")
    @TestCaseId("3378")
    public void testOpenNewerMessageByKKeyWhenReading() {
        user.hotkeySteps().pressSimpleHotKey("k");
        user.messageViewSteps().shouldSeeMessageSubject(olderMsg.getSubject());
    }
}
