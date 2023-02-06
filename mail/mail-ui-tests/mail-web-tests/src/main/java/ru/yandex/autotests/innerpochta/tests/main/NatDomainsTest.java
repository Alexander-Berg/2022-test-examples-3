package ru.yandex.autotests.innerpochta.tests.main;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
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
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Тесты на нац домены")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.NAT_DOMAINS)
@RunWith(DataProviderRunner.class)
public class NatDomainsTest extends BaseTest {

    @DataProvider
    public static Object[][] services() {
        return new Object[][]{
            {YandexDomain.KZ, "https://yandex.kz/all"},
            {YandexDomain.BY, "https://yandex.by/all"},
            {YandexDomain.UZ, "https://yandex.uz/all"},
        };
    }

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Test
    @Title("Список сервисов соответствует домену .com")
    @TestCaseId("2669")
    public void shouldSeeComServicesOnly() {
        user.loginSteps().forAcc(lock.firstAcc()).loginsToDomain(YandexDomain.COM);
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .shouldSeeThatElementHasText(onMessagePage().moreItem().get(2), "Календарь")
            .shouldSeeThatElementHasText(onMessagePage().moreItem().get(3), "Заметки")
            .shouldSeeThatElementHasText(onMessagePage().moreItem().get(4), "Контакты")
            .shouldSeeThatElementHasText(onMessagePage().moreItem().get(5), "Мессенджер");
    }

    @Test
    @Title("Страница «Все сервисы» на нацдомене")
    @TestCaseId("5760")
    @UseDataProvider("services")
    public void shouldSeeAllServicesWithDomain(YandexDomain domain, String link) {
        user.loginSteps().forAcc(lock.firstAcc()).loginsToDomain(domain);
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .scrollAndClicksOn(onMessagePage().allServices360Popup().allServices360())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(link));
    }

    @Test
    @Title("Страница «Все сервисы» на нацдомене в компактном меню")
    @TestCaseId("5149")
    @UseDataProvider("services")
    public void shouldSeeAllServicesInCompactMode(YandexDomain domain, String link) {
        user.loginSteps().forAcc(lock.firstAcc()).loginsToDomain(domain);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем компактную шапку",
            of(LIZA_MINIFIED_HEADER, STATUS_ON)
        );
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().moreServices())
            .clicksOn(onMessagePage().servicesPopup().servicesText("Все сервисы"))
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(link));
    }
}
