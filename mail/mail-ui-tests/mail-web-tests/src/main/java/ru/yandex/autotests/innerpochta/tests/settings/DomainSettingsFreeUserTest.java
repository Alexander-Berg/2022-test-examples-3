package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
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

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Тесты на настройки домена для бесплатного пользователя")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.DOMAIN_SETTINGS)
public class DomainSettingsFreeUserTest extends BaseTest {

    private static final String TUNING_URL = "https://mail360.yandex.ru/";
    private static final String FROM_LABEL =  "from=mail_beautiful_email";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Редирект на тюнинг при клике на кнопку Подключить")
    @TestCaseId("6028")
    public void shouldRedirectToTuning() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_DOMAIN)
            .clicksOn(domainSettingsPage().enableButton())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(TUNING_URL))
            .shouldBeOnUrl(containsString(FROM_LABEL));
    }
}
