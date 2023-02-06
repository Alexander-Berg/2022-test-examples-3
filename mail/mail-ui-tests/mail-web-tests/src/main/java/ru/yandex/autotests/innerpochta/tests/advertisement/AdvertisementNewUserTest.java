package ru.yandex.autotests.innerpochta.tests.advertisement;


import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.passport.api.core.objects.UserWithProps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Тесты на рекламу")
@Features(FeaturesConst.ADVERTISEMENT)
@Tag(FeaturesConst.ADVERTISEMENT)
@RunWith(DataProviderRunner.class)
public class AdvertisementNewUserTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().createAndUseTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Не показываем рекламу для нового пользователя")
    @TestCaseId("2092")
    public void shouldNotSeeAdForNewUser() {
        user.defaultSteps().opensDefaultUrl();
        user.advertisementSteps().shouldNotSeeAllAd();
        user.composeSteps().goToDone(lock.firstAcc().getSelfEmail());
        user.defaultSteps().shouldNotSee(user.pages().MessagePage().directDone());
    }
}
