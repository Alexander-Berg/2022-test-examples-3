package ru.yandex.autotests.innerpochta.tests.main;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author mabelpines
 */
@Aqua.Test
@Title("Тесты на шапку")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class MailHeaderTest extends BaseTest {

    private static final String DISK_URL = "disk.yandex.ru";
    private static final String DOC_URL = "docs.yandex.ru";
    private static final String LOGO_URL = "https://360.yandex.ru/?from=mail-header-360";
    private static final String FINAL_URL = "https://360.yandex.ru";

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
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Закрываем выпадушку сервисов по клику вне её")
    @TestCaseId("3617")
    public void shouldCloseBurgerMenu() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .shouldSee(onMessagePage().allServices360Popup())
            .offsetClick(onMessagePage().displayedMessages(), 0, 100)
            .shouldNotSee(onMessagePage().allServices360Popup());
    }

    @Test
    @Title("Переходим в Диск из шапки")
    @TestCaseId("996")
    public void shouldOpenServiceDisk() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(1))
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(DISK_URL));
    }

    @Test
    @Title("Переходим в Документы из шапки")
    @TestCaseId("996")
    public void shouldOpenServiceDocuments() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(3))
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(DOC_URL));
    }

    @Test
    @Title("Список сервисов соответствует домену .ru")
    @TestCaseId("2669")
    public void shouldSeeRuServicesOnly() {
        user.defaultSteps()
            .clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .shouldSeeThatElementHasText(onMessagePage().moreItem().get(2), "Календарь")
            .shouldSeeThatElementHasText(onMessagePage().moreItem().get(3), "Подписка")
            .shouldSeeThatElementHasText(onMessagePage().moreItem().get(4), "Заметки")
            .shouldSeeThatElementHasText(onMessagePage().moreItem().get(5), "Контакты")
            .shouldSeeThatElementHasText(onMessagePage().moreItem().get(6), "Мессенджер");
    }

    @Test
    @Title("Проверяем переход на лендинг по клику в логотип почты")
    @TestCaseId("3618")
    public void shouldBeOnLandingPageAfterClickInLogo() {
        String url360 = user.pages().HomePage().mail360HeaderBlock().mailLogo().getAttribute("href");
        assertEquals(
            String.format(
                "Неправильный урл на лого 360, ждали %s, получили %s",
                LOGO_URL,
                url360
            ),
            url360,
            LOGO_URL
        );
        user.defaultSteps().clicksOn(user.pages().HomePage().mail360HeaderBlock().mailLogo())
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(FINAL_URL);
    }

    @Test
    @Title("В выпадушке залогина отображается полный адрес юзера")
    @TestCaseId("3940")
    public void shouldSeeUserMenuName() {
        user.defaultSteps().clicksOn(onHomePage().mail360HeaderBlock().userMenu())
            .shouldSee(onHomePage().userMenuDropdown());
        user.defaultSteps().shouldSeeThatElementHasText(
            onHomePage().userMenuDropdown().currentUserName(),
            lock.firstAcc().getSelfEmail()
        );
    }
}
