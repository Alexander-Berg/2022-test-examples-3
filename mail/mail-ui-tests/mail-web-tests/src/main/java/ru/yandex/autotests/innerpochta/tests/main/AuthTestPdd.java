package ru.yandex.autotests.innerpochta.tests.main;

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
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.util.MailConst.PDD_USER_TAG;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Авторизация")
@Features(FeaturesConst.AUTH)
@Tag(FeaturesConst.AUTH)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AuthTestPdd extends BaseTest {

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().useTusAccount(PDD_USER_TAG);

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @Test
    @Title("Авторизация ПДД пользователем с морды")
    @TestCaseId("1008")
    public void authPDDUserFromMorda() {
        user.defaultSteps().opensMordaUrlWithDomain(YandexDomain.RU.getDomain())
            .clicksOn(onHomePage().logInMordaBtn())
            .switchOnJustOpenedWindow();
        String newUrl = user.defaultSteps().getsCurrentUrl().replace(
            "mail.yandex.ru",
            webDriverRule.getBaseUrl().split("/")[2]
        );
        user.defaultSteps().opensUrl(newUrl);
        user.loginSteps().logInFromPassportFromMorda(lock.firstAcc().getSelfEmail(), lock.firstAcc().getPassword());
    }

}
