package ru.yandex.autotests.innerpochta.tests.main;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
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

import static org.hamcrest.Matchers.containsString;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Авторизация")
@Features(FeaturesConst.AUTH)
@Tag(FeaturesConst.AUTH)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AuthTest extends BaseTest {

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().useTusAccount();

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @Test
    @Title("Авторизоваться на домене com, com.tr")
    @TestCaseId("1007")
    @DataProvider({"com", "com.tr"})
    public void authOnDomainsComComTr(String domain) {
        user.defaultSteps().opensDefaultUrlWithDomain(domain)
            .clicksOn(onHomerPage().logInBtnHeadBanner());
        user.loginSteps().logInFromPassport(lock.firstAcc().getLogin(), lock.firstAcc().getPassword());
        user.defaultSteps().shouldBeOnUrl(containsString(domain));

    }

}
