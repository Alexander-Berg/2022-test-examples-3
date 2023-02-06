package ru.yandex.autotests.innerpochta.tests.hotkeys;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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

import java.util.Arrays;
import java.util.Collection;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;


/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на общие хот кеи для стандартного и 3х панельного интерфейсов")
@RunWith(Parameterized.class)
@Features(FeaturesConst.HOT_KEYS)
@Tag(FeaturesConst.HOT_KEYS)
@Stories(FeaturesConst.GENERAL)
public class CommonHotKeysTest extends BaseTest {

    private static final String REQ_PARAM = "request=";
    private static final String SEARH_REQ1 = "a";
    private static final String SEARH_REQ2 = "qwer";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @Parameterized.Parameter
    public String layoutType;

    @Parameterized.Parameters(name = "layoutID: {0}")
    public static Collection<Object[]> testData() {
        Object[][] data = new Object[][]{
            {LAYOUT_2PANE},
            {SETTINGS_LAYOUT_3PANE_HORIZONTAL}
        };
        return Arrays.asList(data);
    }

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth).around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams("Переключаем интерфейс", of(SETTINGS_PARAM_LAYOUT, layoutType));
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomString(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessagesPresent();
    }

    @Test
    @Title("Вызов помощи по хоткеям")
    @TestCaseId("1404")
    public void testHelpHotKey() {
        user.hotkeySteps().pressSimpleHotKey("?");
        user.messagesSteps().shouldSeeHotKeysInfo();
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ESCAPE))
            .pressCombinationOfHotKeys(key(Keys.SHIFT), "/");
        user.messagesSteps().shouldSeeHotKeysInfo();
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ESCAPE))
            .pressCombinationOfHotKeys(key(Keys.SHIFT), "7");
        user.messagesSteps().shouldSeeHotKeysInfo();
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ESCAPE));
    }

    @Test
    @Title("W, C - Написание письма")
    @TestCaseId("1405")
    public void testComposeHotKeys() {
        user.hotkeySteps().pressSimpleHotKey("w");
        user.composeSteps().shouldSeeSendButton();
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessagesPresent();
        user.hotkeySteps().pressSimpleHotKey("c");
        user.composeSteps().shouldSeeSendButton();
    }

    @Test
    @Title("CONTROL + i - Переход в инбокс")
    @TestCaseId("1406")
    public void testInboxHotKey() {
        user.defaultSteps().opensDefaultUrl();
        user.leftColumnSteps().opensSpamFolder()
            .shouldBeInFolder("Спам");
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.CONTROL), "i");
        user.leftColumnSteps().shouldBeInFolder("Входящие");
    }

    @Test
    @Title("/ - Поиск")
    @TestCaseId("1407")
    public void testSearchHotKey() {
        user.hotkeySteps().pressSimpleHotKey("/");
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), SEARH_REQ1);
        user.hotkeySteps().pressHotKeys(onMessagePage().mail360HeaderBlock().searchInput(), Keys.RETURN);
        user.defaultSteps().shouldBeOnUrl(lock.firstAcc(), QuickFragments.SEARCH, REQ_PARAM + SEARH_REQ1);
        user.hotkeySteps().pressHotKeys(onMessagePage().mail360HeaderBlock().searchInput(), SEARH_REQ2)
            .pressHotKeys(onMessagePage().mail360HeaderBlock().searchInput(), Keys.RETURN);
        user.defaultSteps().shouldBeOnUrl(lock.firstAcc(), QuickFragments.SEARCH, REQ_PARAM + SEARH_REQ1 + SEARH_REQ2);
    }

    @Test
    @Title("p - Напечатать")
    @TestCaseId("1408")
    public void testPrintHotKey() {
        user.defaultSteps().opensDefaultUrl();
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().shouldSee(onMessageView().messageViewer());
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_RIGHT))
            .pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressCombinationOfHotKeys(key(Keys.SHIFT), "p");
        user.defaultSteps().switchOnWindow(1)
            .shouldBeOnUrl(containsString("/print"))
            .switchOnJustOpenedWindow();
    }
}
