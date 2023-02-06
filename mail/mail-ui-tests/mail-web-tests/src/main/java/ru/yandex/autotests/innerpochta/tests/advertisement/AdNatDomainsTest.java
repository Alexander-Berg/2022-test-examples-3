package ru.yandex.autotests.innerpochta.tests.advertisement;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.BE;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.TR;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.UK;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ENABLED_ADV;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Тесты на рекламу на нац доменах")
@Features(FeaturesConst.ADVERTISEMENT)
@Tag(FeaturesConst.ADVERTISEMENT)
@RunWith(DataProviderRunner.class)
public class AdNatDomainsTest extends BaseTest {

    private static final String DIRECT_LINK_TEXT = "Перейти";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @DataProvider
    public static Object[][] domains() {
        return new Object[][]{
            {YandexDomain.BY},
            {YandexDomain.KZ}
        };
    }

    @DataProvider
    public static Object[][] langs() {
        return new Object[][]{
            {TR, "Gelen Kutusu"},
            {UK, "Вхідні"},
            {BE, "Уваходныя"}
        };
    }

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем рекламу",
            of(
                SHOW_ADVERTISEMENT, ENABLED_ADV
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Не показываем рекламу на домене .com")
    @TestCaseId("2476")
    public void shouldNotSeeAdOnComDomain() {
        user.loginSteps().forAcc(lock.firstAcc()).loginsToDomain(YandexDomain.COM);
        user.advertisementSteps().shouldNotSeeAllAd();
        user.composeSteps().goToDone(lock.firstAcc().getSelfEmail());
        user.defaultSteps().shouldNotSee(user.pages().MessagePage().directDone());
    }

    @Test
    @Title("Показываем рекламу на доменах by, kz")
    @TestCaseId("2088")
    @UseDataProvider("domains")
    public void shouldSeeAdOnDomains(YandexDomain domain) {
        user.loginSteps().forAcc(lock.firstAcc()).loginsToDomain(domain);
        user.advertisementSteps().shouldSeeAllAd();
        user.composeSteps().goToDone(lock.firstAcc().getSelfEmail());
        user.defaultSteps().shouldSee(user.pages().MessagePage().directDone());
    }

    @Test
    @Title("Реклама не переводится при смене локали")
    @TestCaseId("2089")
    @UseDataProvider("langs")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68542")
    public void shouldNotTranslateAd(String language, String inboxName) {
        user.defaultSteps().opensDefaultUrlWithPostFix("/?lang=" + language);
        user.leftColumnSteps().shouldBeInFolder(inboxName);
        user.defaultSteps().shouldSeeThatElementHasText(onMessagePage().directLine(), DIRECT_LINK_TEXT);
    }
}
