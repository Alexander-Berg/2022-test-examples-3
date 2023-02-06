package ru.yandex.autotests.innerpochta.tests.main;

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
import org.openqa.selenium.WebElement;
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

import static org.openqa.selenium.Keys.ENTER;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тест на локали")
@Stories(FeaturesConst.LOCALE)
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@RunWith(DataProviderRunner.class)
public class LocaleTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @DataProvider
    public static Object[][] langs() {
        return new Object[][]{
            {"English", "Inbox"},
            {"Türkçe", "Gelen Kutusu"},
            {"Українська", "Вхідні"},
            {"Беларуская", "Уваходныя"},
            {"Қазақ", "Кіріс"}
        };
    }

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Смена локализации")
    @TestCaseId("1541")
    @UseDataProvider("langs")
    public void testLocales(String language, String inboxName) {
        user.defaultSteps().clicksOn(onMessagePage().footerLineBlock().languageSwitch())
            .clicksOnElementWithText(onSettingsPage().languageSelect().languagesList(), language);
        user.leftColumnSteps().shouldBeInFolder(inboxName);
    }

    @Test
    @Title("Открываем новый композ в каждой локали")
    @TestCaseId("5736")
    @UseDataProvider("langs")
    public void shouldOpenNewCompose(String language, String inboxName) {
        user.defaultSteps().clicksOn(onMessagePage().footerLineBlock().languageSwitch())
            .clicksOnElementWithText(onSettingsPage().languageSelect().languagesList(), language);
        user.leftColumnSteps().shouldBeInFolder(inboxName);
        user.defaultSteps().clicksOn(onMessagePage().composeButton())
            .shouldSee(onComposePopup().expandedPopup().toolbarBlock());
        inputStyledText(
            onComposePopup().expandedPopup().toolbarBlock().bold(),
            onComposePopup().expandedPopup().toolbarBlock().italic(),
            onComposePopup().expandedPopup().toolbarBlock().underline(),
            onComposePopup().expandedPopup().toolbarBlock().strike()
        );
        user.defaultSteps().inputsTextInElement(
            user.pages().ComposePopup().expandedPopup().popupTo(),
            lock.firstAcc().getSelfEmail()
        )
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), Utils.getRandomString())
            .clicksOn(onComposePopup().expandedPopup().sendBtn())
            .shouldSee(onComposePopup().doneScreenInboxLink());
    }

    @Step("Применяем стиль и пишем строку в тело письма")
    private void inputStyledText(WebElement... buttons) {
        for (WebElement button : buttons) {
            user.defaultSteps().clicksOn(button)
                .appendTextInElement(onComposePopup().expandedPopup().bodyInput(), getRandomString());
            user.hotkeySteps().pressHotKeys(onComposePopup().expandedPopup().bodyInput(), ENTER);
            user.defaultSteps().clicksOn(button);
        }
    }
}
