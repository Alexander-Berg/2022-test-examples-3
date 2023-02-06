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

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.SENT_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;


/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на хот кеи для навигации по папкам, письмам и меткам 3х панельного интерфейса")
@Features(FeaturesConst.HOT_KEYS)
@Tag(FeaturesConst.HOT_KEYS)
@Stories(FeaturesConst.GENERAL)
public class NavigatingHotKeys3PaneTest extends BaseTest {

    private static final String SEARCH_URL_POSTFIX = "/#search?request=subj";
    private static final String CUSTOM_FLDR = "folder1";
    private static final int MSG_AMOUNT = 2;

    private String msgSubj1;
    private String msgSubj2;
    private String msgSubj3;

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
    public void logIn() {
        msgSubj3 = Utils.getRandomName();
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane-vertical",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        user.apiFoldersSteps().createNewFolder(CUSTOM_FLDR);
        user.apiMessagesSteps().sendMail(lock.firstAcc(), msgSubj3, "");
        user.apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, CUSTOM_FLDR);
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), MSG_AMOUNT);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeMessagesPresent();
        user.leftColumnSteps().expandFoldersIfCan();
    }

    @Test
    @Title("UP/DOWN/RETURN - Навигация в списке писем 3pane")
    @TestCaseId("1452")
    public void testMessagesNavigationHotKeys3Pane() {
        msgSubj1 = user.pages().MessagePage().displayedMessages().list().get(0).subject().getText();
        msgSubj2 = user.pages().MessagePage().displayedMessages().list().get(1).subject().getText();
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_UP))
            .pressSimpleHotKey(key(Keys.ARROW_RIGHT));
        user.messageViewSteps().shouldSeeMessageSubjectInCompactView(msgSubj1);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessagesPresent();
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_DOWN));
        user.messageViewSteps().shouldSeeMessageSubjectInCompactView(msgSubj2);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessagesPresent();
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_UP));
        user.messageViewSteps().shouldSeeMessageSubjectInCompactView(msgSubj1);
    }

    @Test
    @Title("UP/DOWN - Навигация по поисковой выдаче 3pane")
    @TestCaseId("4390")
    public void testSearchResultsNavigationHotKeys3Pane() {
        user.defaultSteps().opensDefaultUrlWithPostFix(SEARCH_URL_POSTFIX)
            .shouldSee(onMessagePage().displayedMessages().allResults());
        msgSubj1 = user.pages().MessagePage().displayedMessages().list().get(0).subject().getText();
        msgSubj2 = user.pages().MessagePage().displayedMessages().list().get(1).subject().getText();
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_RIGHT));
        user.messageViewSteps().shouldSeeMessageSubject(msgSubj1);
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_DOWN));
        user.messageViewSteps().shouldSeeMessageSubject(msgSubj2);
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_UP));
        user.messageViewSteps().shouldSeeMessageSubject(msgSubj1);
    }

    @Test
    @Title("UP/DOWN/RETURN - Навигация в списке папок 3pane")
    @TestCaseId("1451")
    public void testFoldersNavigationHotKeys3Pane() {
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_LEFT))
            .pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.leftColumnSteps().shouldBeInFolder(CUSTOM_FLDR);
        user.messagesSteps().shouldSeeMessagesPresent();
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_UP))
            .pressSimpleHotKey(key(Keys.RETURN))
            .pressSimpleHotKey(key(Keys.SPACE));
        user.defaultSteps().shouldNotSee(onMessagePage().foldersNavigation().customFolders());
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.leftColumnSteps().shouldBeInFolder(SENT_RU);
        user.messagesSteps().shouldSeeMessagesPresent();
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_UP))
            .pressSimpleHotKey(key(Keys.SPACE));
        user.defaultSteps().shouldSeeThatElementTextEquals(
            onMessagePage().foldersNavigation().customFolders().get(2).customFolderName(),
            CUSTOM_FLDR
        );
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.leftColumnSteps().shouldBeInFolder(CUSTOM_FLDR);
    }

    @Test
    @Title("LEFT/DOWN/RETURN - Навигация по колонкам 3pane")
    @TestCaseId("1453")
    public void testColumnsNavigationHotKeys3Pane() {
        user.leftColumnSteps().expandFoldersIfCan();
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_LEFT))
            .pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.leftColumnSteps().shouldBeInFolder(CUSTOM_FLDR);
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_RIGHT))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.defaultSteps().shouldContainText(onMessageView().messageSubject(), msgSubj3);
    }
}
