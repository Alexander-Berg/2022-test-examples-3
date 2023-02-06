package ru.yandex.autotests.innerpochta.tests.main;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.ProxyServerRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetUrlForDomainRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.proxy.LittleHostFilter.hostFilter;
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.proxyServerRule;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_URL_WITHOUT_DOMAIN;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Авторизация")
@Features(FeaturesConst.AUTH)
@Tag(FeaturesConst.AUTH)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AuthTestPddRu extends BaseTest {

    private static final String CREDS_PDD_RU = "AuthorizationTestPddRFAdminkapddrf";

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().annotation();

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @Test
    @Title("Авторизация ПДД пользователем с кириллическим доменом c mail.yandex.ru")
    @TestCaseId("1012")
    @UseCreds(CREDS_PDD_RU)
    public void authCyrilicPDDUserFromHomer() {
        user.defaultSteps().opensDefaultUrl()
            .clicksOn(onHomerPage().logInBtnHeadBanner());
        user.loginSteps().logInFromPassport(lock.firstAcc().getLogin(), lock.firstAcc().getPassword());
    }

}
