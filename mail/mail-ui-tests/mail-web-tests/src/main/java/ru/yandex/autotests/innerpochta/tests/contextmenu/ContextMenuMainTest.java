package ru.yandex.autotests.innerpochta.tests.contextmenu;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
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

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.*;

@Aqua.Test
@Title("Контекстное меню - общее")
@Features(FeaturesConst.CONTEXT_MENU)
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class ContextMenuMainTest extends BaseTest {

    private static String subject = getRandomString();

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
    public void login() {
        user.apiMessagesSteps().sendMail(lock.firstAcc().getSelfEmail(), subject, getRandomString());
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[][] layouts() {
        return new Object[][]{
                {LAYOUT_2PANE},
                {LAYOUT_3PANE_VERTICAL},
                {SETTINGS_LAYOUT_3PANE_HORIZONTAL}
        };
    }

    @Test
    @Title("Закрываем контекстное меню горячей клавишей «ESC»")
    @TestCaseId("3153")
    public void shouldCloseContextMenuByEsc() {
        user.defaultSteps().rightClick(onMessagePage().foldersNavigation().inbox());
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ESCAPE));
        user.defaultSteps().shouldNotSee(onMessagePage().allMenuList());
    }

    @Test
    @Title("Закрываем контекстное меню по клику вне")
    @TestCaseId("3154")
    public void shouldCloseContextMenuByOutsideClick() {
        user.defaultSteps().rightClick(onMessagePage().foldersNavigation().inbox())
                .clicksOn(onMessagePage().displayedMessages())
                .shouldNotSee(onMessagePage().allMenuList());
    }

    @Test
    @Title("Открываем контекстное меню в результатах поиска")
    @TestCaseId("3148")
    public void shouldSeeContextMenuInSearchResults() {
        user.defaultSteps().inputsTextInElement(onHomePage().mail360HeaderBlock().searchInput(), subject)
                .clicksOn(onHomePage().mail360HeaderBlock().searchBtn());
        user.messagesSteps().rightClickOnMessageWithSubject(subject);
        user.defaultSteps().shouldSee(onMessagePage().allMenuListInMsgList());
    }

    @Test
    @Title("Кликаем правой кнопкой мыши по письму")
    @TestCaseId("3151")
    @UseDataProvider("layouts")
    public void shouldNotOpenTheMessage(String layout) {
        user.apiSettingsSteps()
                .callWithListAndParams("Включаем " + layout, of(SETTINGS_PARAM_LAYOUT, layout));
        user.defaultSteps().refreshPage();
        user.messagesSteps().rightClickOnMessageWithSubject(subject);
        user.defaultSteps().shouldSee(onMessagePage().allMenuListInMsgList());
    }
}
