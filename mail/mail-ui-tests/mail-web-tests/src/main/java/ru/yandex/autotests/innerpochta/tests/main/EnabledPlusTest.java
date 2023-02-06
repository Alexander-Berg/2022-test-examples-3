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
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DISABLE_PROMO;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на Плюс")
@Features({FeaturesConst.MAIN, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.PROMO)
@RunWith(DataProviderRunner.class)
public class EnabledPlusTest extends BaseTest {

    private static final String CREDS = "PlusEnabledAcc";
    private static final String URL_PLUS = "https://plus.yandex.ru/";
    private AccLockRule lock = AccLockRule.use().names(CREDS);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth);

    @DataProvider
    public static Object[][] settings() {
        return new Object[][]{
            {STATUS_ON},
            {EMPTY_STR}
        };
    }

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем показ промо, выключаем компактную шапку",
            of(
                DISABLE_PROMO, FALSE,
                LIZA_MINIFIED_HEADER, EMPTY_STR
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Обводка аватара пользователя в плюсе")
    @TestCaseId("5770")
    @UseDataProvider("settings")
    public void shouldSeePlusOnAvatar(String status) {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем компактную шапку",
            of(LIZA_MINIFIED_HEADER, status)
        );
        user.defaultSteps()
            .shouldSee(onMessagePage().mail360HeaderBlock().plusAvatar())
            .clicksOn(onMessagePage().mail360HeaderBlock().userMenu())
            .shouldSee(onMessagePage().mail360HeaderBlock().userMenuDropdown().userMenuDropdownAvatar());
    }

    @Test
    @Title("Переход на страницу плюса из выпадушки юзера")
    @TestCaseId("5778")
    public void shouldSeePlusPage() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().userMenu())
            .clicksOn(onMessagePage().mail360HeaderBlock().userMenuDropdown().userPlusLink())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(URL_PLUS));
    }
}
