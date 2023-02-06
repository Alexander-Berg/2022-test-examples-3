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
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ENABLED_ADV;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_DONE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_PAGE_AFTER_SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Тесты на рекламу для WS")
@Features(FeaturesConst.ADVERTISEMENT)
@Tag(FeaturesConst.ADVERTISEMENT)
@RunWith(DataProviderRunner.class)
public class AdWSTest extends BaseTest {

    private static final String CREDS_PAID = "AdWSTestPaid";
    private static final String CREDS_FREE = "AdWSTestFree";

    private AccLockRule lock = AccLockRule.use().annotation();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth);

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем рекламу и переход на done",
            of(
                SHOW_ADVERTISEMENT, ENABLED_ADV,
                SETTINGS_PARAM_PAGE_AFTER_SENT, SETTINGS_PARAM_DONE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Не показываем рекламу платным пользователям WS")
    @TestCaseId("2361")
    @UseCreds(CREDS_PAID)
    public void shouldNotSeeAdForPaidWS() {
        user.advertisementSteps().shouldNotSeeAllAd();
        user.composeSteps().goToDone(DEV_NULL_EMAIL);
        user.defaultSteps().shouldNotSee(user.pages().MessagePage().directDone())
            .opensFragment(QuickFragments.SETTINGS_OTHER)
            .shouldNotSee(onOtherSettings().blockSetupOther().topPanel().showAdvertisement());
    }

    @Test
    @Title("Показываем рекламу бесплатным пользователям WS")
    @TestCaseId("2362")
    @UseCreds(CREDS_FREE)
    public void shouldSeeAdForFreeWS() {
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().list().get(0))
            .shouldSee(onHomePage().directAd());
        user.advertisementSteps().shouldSeeAllAd();
        user.composeSteps().goToDone(DEV_NULL_EMAIL);
        user.defaultSteps().shouldSee(user.pages().MessagePage().directDone());
    }
}
