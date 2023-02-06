package ru.yandex.autotests.innerpochta.tests.hostroot;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

/**
 * author vasily-k
 */

@Aqua.Test
@Title("Переключение пользователя при мультиавторизации")
@Features(FeaturesConst.HOSTROOT)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class SwitchUserTest extends BaseTest {

    private static final String CREDS_PDD = "AuthorizationTestPdd";
    private static final String CREDS_RU = "AuthorizationTestRu";
    private static final String PDD_DOMAIN_URL = "https://mail.yandex.ru/?pdd_domain=kida-lo-vo.name";
    private static final String FOR_PDD_DOMAIN_URL = "https://mail.yandex.ru/for/kida-lo-vo.name";

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().names(CREDS_PDD, CREDS_RU);

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @Test
    @Title("При переходе на специальный урл дефолтным становится пдд пользователь")
    @TestCaseId("207")
    @DataProvider({PDD_DOMAIN_URL, FOR_PDD_DOMAIN_URL})
    public void shouldSwitchToPddUser(String url) {
        user.loginSteps().forAcc(lock.acc(CREDS_PDD)).logins()
            .multiLoginWith(lock.acc(CREDS_RU));
        user.defaultSteps()
            .shouldHasText(onMessagePage().userName(),lock.acc(CREDS_RU).getLogin())
            .opensUrl(url)
            .shouldHasText(onMessagePage().userName(),lock.acc(CREDS_PDD).getLogin());
    }

    @Test
    @Title("Переключаем на обычного пользователя вручную")
    @TestCaseId("207")
    public void shouldSwitchToSimpleUser() {
        user.loginSteps().forAcc(lock.acc(CREDS_RU)).logins()
            .multiLoginWith(lock.acc(CREDS_PDD));
        user.defaultSteps()
            .shouldHasText(onMessagePage().userName(), lock.acc(CREDS_PDD).getLogin())
            .clicksOn(onMessagePage().userName())
            .shouldSee(onMessagePage().userMenuDropdown())
            .clicksOn(onMessagePage().userMenuDropdown().userList().get(0))
            .shouldHasText(onMessagePage().userName(), lock.acc(CREDS_RU).getLogin());
    }
}
