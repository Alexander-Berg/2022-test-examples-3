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
import ru.yandex.autotests.innerpochta.util.KeysOwn;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.SENT_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;


/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на хоткеи для навигации по папкам, письмам и меткам для стандартного интерфейса")
@Features(FeaturesConst.HOT_KEYS)
@Tag(FeaturesConst.HOT_KEYS)
@Stories(FeaturesConst.GENERAL)
public class NavigatingHotKeysTest extends BaseTest {

    private static final String SEARCH_URL_POSTFIX = "/#search?request=subj";
    private static final int MSG_AMOUNT = 3;
    private static final String FLD_NAME = Utils.getRandomName();

    private String msgSubj1;
    private String msgSubj2;

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
        user.apiFoldersSteps().createNewFolder(FLD_NAME);
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomString(), "");
        user.apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, FLD_NAME)
            .sendCoupleMessages(lock.firstAcc(), MSG_AMOUNT);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeMessagesPresent();
        user.leftColumnSteps().expandFoldersIfCan();
    }

    @Test
    @Title("UP/DOWN/RETURN - Навигация по письмам")
    @TestCaseId("1456")
    public void testMessagesNavigationHotKeys() {
        msgSubj1 = user.pages().MessagePage().displayedMessages().list().get(0).subject().getText();
        msgSubj2 = user.pages().MessagePage().displayedMessages().list().get(1).subject().getText();
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_RIGHT))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.messageViewSteps().shouldSeeMessageSubject(msgSubj1);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessagesPresent();
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_RIGHT))
            .pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.messageViewSteps().shouldSeeMessageSubject(msgSubj2);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessagesPresent();
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_UP))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.messageViewSteps().shouldSeeMessageSubject(msgSubj1);
    }

    @Test
    @Title("UP/DOWN - Навигация по поисковой выдаче")
    @TestCaseId("4390")
    public void testSearchResultsNavigationHotKeys() {
        user.defaultSteps().opensDefaultUrlWithPostFix(SEARCH_URL_POSTFIX)
            .shouldSee(onMessagePage().displayedMessages().allResults());
        msgSubj1 = user.pages().MessagePage().displayedMessages().list().get(0).subject().getText();
        msgSubj2 = user.pages().MessagePage().displayedMessages().list().get(1).subject().getText();
        user.hotkeySteps().pressSimpleHotKey(KeysOwn.key(Keys.ARROW_RIGHT))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.messageViewSteps().shouldSeeMessageSubject(msgSubj1);
        webDriverRule.getDriver().navigate().back();
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages().allResults());
        user.hotkeySteps().pressSimpleHotKey(KeysOwn.key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(KeysOwn.key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.messageViewSteps().shouldSeeMessageSubject(msgSubj2);
    }

    @Test
    @Title("LEFT/UP/DOWN/RETURN - Навигация по папкам")
    @TestCaseId("1455")
    public void testFoldersNavigationHotKeys() {
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_LEFT))
            .pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.leftColumnSteps().shouldBeInFolder(FLD_NAME);
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
            FLD_NAME
        );
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.leftColumnSteps().shouldBeInFolder(FLD_NAME);
    }

    @Test
    @Title("LEFT/RIGHT/RETURN - Навигация по колонкам")
    @TestCaseId("1457")
    public void testColumnsNavigationHotKeys() {
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_LEFT))
            .pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.leftColumnSteps().shouldBeInFolder(FLD_NAME);
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_RIGHT))
            .pressSimpleHotKey(key(Keys.RETURN));
        user.defaultSteps().shouldBeOnUrlWith(QuickFragments.MESSAGE);
    }

}
