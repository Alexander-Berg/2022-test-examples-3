package ru.yandex.autotests.innerpochta.tests.homer;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_BASE_URL;
import static ru.yandex.autotests.passport.api.core.cookie.YandexCookies.L;

/**
 * @author vasily-k
 */

@Aqua.Test
@Title("Общие тесты на Гомера")
@Features(FeaturesConst.HOMER)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class HomerGeneralTest extends BaseTest {

    private static final String NORETPATH = "/?noretpath=1";
    private static final String CORP_AUTH_URL = "https://passport.yandex-team.ru/auth";
    private static final String RETPATH = "retpath=https%3A%2F%2Fmail.yandex-team.ru";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().className();

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @Before
    public void setUp() {
        webDriverRule.getDriver().manage().deleteAllCookies();
    }

    //TODO: оторвать добавление параметра и старую кнопку возвращения в паспорте по завершению их эксперимента
    @Test
    @Title("Ссылка «Вернуться на сервис» ведет на Гомер, пользователь без L-куки")
    @TestCaseId("136")
    public void shouldScrollToMobilePromoNoLCookie() {
        user.defaultSteps().opensDefaultUrl()
            .clicksOn(onHomerPage().logInBtnHeadBanner());
        String newUri = fromUri(webDriverRule.getDriver().getCurrentUrl()).queryParam("new", 1).build().toString();
        user.defaultSteps().opensUrl(newUri)
            .clicksOn(onPassportPage().backToPrevStep())
            .shouldBeOnUrl(MAIL_BASE_URL + NORETPATH);
    }

    //TODO: оторвать добавление параметра и старую кнопку возвращения в паспорте по завершению их эксперимента
    @Test
    @Title("Ссылка «Вернуться на сервис» ведет на Гомер, пользователь c L-кукой")
    @TestCaseId("136")
    public void shouldScrollToMobilePromoLCookie() {
        user.loginSteps().forAcc(lock.firstAcc()).getsCookie(L, YandexDomain.RU);
        String newUri = fromUri(webDriverRule.getDriver().getCurrentUrl()).queryParam("new", 1).build().toString();
        user.defaultSteps().opensUrl(newUri)
            .clicksOn(onPassportPage().backToPrevStep())
            .shouldBeOnUrl(MAIL_BASE_URL + NORETPATH);
    }

    @Test
    @Title("Редиректим корп на паспортный домик")
    @TestCaseId("155")
    public void shouldRedirectToPassportFromCorp() {
        user.defaultSteps().opensUrl(UrlProps.urlProps().getCorpUri().toString())
            .shouldBeOnUrl(startsWith(CORP_AUTH_URL))
            .shouldBeOnUrl(containsString(RETPATH));
    }
}
