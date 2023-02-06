package ru.yandex.autotests.innerpochta.tests.settings;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Логин с помощью красивого адреса")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.DOMAIN_SETTINGS)
@RunWith(DataProviderRunner.class)
public class LoginWithDomainTest extends BaseTest {

    private static final String login = "testdomainprod14@test-7.ru";
    private static final String pass = "Y0Usha11N0Tpass";

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Test
    @Title("Логин с помощью красивого адреса")
    @TestCaseId("6036")
    public void shouldLoginWithDomain() {
        user.defaultSteps().opensDefaultUrlWithDomain(YandexDomain.RU.getDomain())
            .clicksOn(onHomerPage().logInBtnHeadBanner());
        user.loginSteps().logInFromPassport(login, pass);
    }
}
