package ru.yandex.autotests.innerpochta.tests.main;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
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

/**
 * @author eremin-ns
 */
@Aqua.Test
@Title("Почта 360 - Шапка, управление организацией")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.HEAD)
public class Mail360HeaderAdminTest extends BaseTest {

    private static final String YANDEX_ADMIN_PAGE = "https://admin.yandex.ru";
    private static final String YANDEX_SEND_PAGE = "https://send.yandex.ru";

    public AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Before
    public void setUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Переходим в управление организацией в выпадушке «Еще»")
    @TestCaseId("5969")
    public void shouldMoveToAdmin() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .clicksOn(onMessagePage().allServices360Popup().serviceIcons().get(8))
            .shouldContainTextInUrl(YANDEX_ADMIN_PAGE);
    }

    @Test
    @Title("Переходим в Рассылки в выпадушке «Еще»")
    @TestCaseId("6189")
    public void shouldMoveToSend() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .clicksOn(onMessagePage().allServices360Popup().serviceIcons().get(7))
            .shouldContainTextInUrl(YANDEX_SEND_PAGE);
    }
}
