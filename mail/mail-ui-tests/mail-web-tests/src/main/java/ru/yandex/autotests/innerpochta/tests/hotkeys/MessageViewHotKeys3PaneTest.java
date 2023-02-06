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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.FORWARD_PREFIX;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на хоткеи при чтении письма для 3х панельного интерфейса")
@Features(FeaturesConst.HOT_KEYS)
@Tag(FeaturesConst.HOT_KEYS)
@Stories(FeaturesConst.GENERAL)
public class MessageViewHotKeys3PaneTest extends BaseTest {

    private static final String RECEIVER_EMAIL = "yandex-team-31613.23887@yandex.ru";

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
    public void logIn() throws InterruptedException, IOException {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем вертикальный 3-пейн",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 3);
        subject = user.apiMessagesSteps().addCcEmails(RECEIVER_EMAIL)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("SHIFT + u - Помечаем непрочитанным в 3pane")
    @TestCaseId("1415")
    public void testMarkAsReadMessageHotKeyWhenReading3Pane() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps().shouldNotSee(onMessageView().messageHead().messageUnread());
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "u");
        user.defaultSteps().opensFragment(QuickFragments.UNREAD);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("SHIFT + i - Помечаем важным в 3pane")
    @TestCaseId("1416")
    public void testMarkImportantMessageHotKeyWhenReading3Pane() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "i");
        user.messagesSteps().shouldSeeThatMessageIsImportant(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "i");
        user.messagesSteps().shouldSeeThatMessageIsNotImportant(subject);
    }

    @Test
    @Title("DELETE - Удаляем в 3pane")
    @TestCaseId("1419")
    public void testDeleteMessageHotKeyWhenReading3Pane() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.hotkeySteps().pressSimpleHotKey(key(Keys.DELETE));
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
    }

    @Test
    @Title("SHIFT + r - Отвечаем на письмо в 3pane")
    @TestCaseId("1418")
    public void testReplyHotKeyWhenReading3Pane() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "r");
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup());
    }

    @Test
    @Title("SHIFT + l - Помечаем кастомной меткой в 3pane")
    @TestCaseId("1420")
    public void testLabelMessageHotKeyWhenReading3Pane() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "l");
        user.defaultSteps().shouldSee(onMessagePage().labelsDropdownMenu().labelImportant());
    }

    @Test
    @Title("SHIFT + m - Перемещаем в 3pane")
    @TestCaseId("1421")
    public void testMoveMessageHotKeyWhenReading3Pane() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "m");
        user.defaultSteps().shouldSee(onMessagePage().moveMessageDropdownMenu().inboxFolder());
    }

    @Test
    @Title("SHIFT + f - Пересылаем в 3pane")
    @TestCaseId("1422")
    public void testForwardMessageHotKeyWhenReading3Pane() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "f");
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup())
            .shouldContainValue(onComposePopup().expandedPopup().sbjInput(), FORWARD_PREFIX);
    }

    @Test
    @Title("SHIFT + click - Выделяем несколько писем")
    @TestCaseId("1423")
    public void testSelectMessagesWithShiftAndMouseClick() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.hotkeySteps().setDestination(onMessagePage().displayedMessages().list().get(3))
            .clicksOnMessageByNumberWhileHolding(key(Keys.SHIFT), 3);
        user.messagesSteps().shouldSeeThatMessagesAreSelected()
            .shouldSeeThatNMessagesAreSelected(4);
    }
}
