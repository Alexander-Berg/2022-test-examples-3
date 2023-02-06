package ru.yandex.autotests.innerpochta.tests.settings;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.ImmutableBiMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.touch.pages.UnsubscribeIframe.IFRAME_SUBS_LIZA;
import static ru.yandex.autotests.innerpochta.util.experiments.ExperimentsConstants.OPT_IN;
import static ru.yandex.autotests.innerpochta.util.experiments.ExperimentsConstants.OPT_IN_PROMO;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DISABLE_PROMO;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.PROMO_MANAGER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.PROMO_OPT_IN_MODAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.PROMO_OPT_IN_SUBS;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на промо опт-ина")
@Features({FeaturesConst.SETTINGS})
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.PROMO)
@RunWith(DataProviderRunner.class)
public class OptInPromoTest extends BaseTest {

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
    public void prepare() {
        Map<String, java.io.Serializable> settings = new HashMap<>();
        settings.put(PROMO_OPT_IN_SUBS, EMPTY_STR);
        settings.put(PROMO_OPT_IN_MODAL, EMPTY_STR);
        settings.put(PROMO_MANAGER, EMPTY_STR);
        settings.put(DISABLE_PROMO, FALSE);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().addExperimentsWithYexp(OPT_IN_PROMO);
        user.apiSettingsSteps().callWithListAndParams(settings);
        user.defaultSteps().refreshPage();
    }

    @Test
    @Title("Закрываем промо опт-ина по крестику")
    @TestCaseId("6221")
    public void shouldCloseOptInPromo() {
        user.defaultSteps()
            .shouldSee(user.pages().MessagePage().optInPromo())
            .clicksOn(user.pages().MessagePage().optInPromo().closeBtn())
            .shouldNotSee(user.pages().MessagePage().optInPromo())
            .refreshPage()
            .shouldNotSee(user.pages().MessagePage().optInPromo());
    }

    @Test
    @Title("Закрываем промо опт-ина кликом в кнопку «Позже»")
    @TestCaseId("6222")
    public void shouldClickLaterOnOptInPromo() {
        user.defaultSteps()
            .shouldSee(user.pages().MessagePage().optInPromo())
            .clicksOn(user.pages().MessagePage().optInPromo().laterBtn())
            .shouldNotSee(user.pages().MessagePage().optInPromo())
            .refreshPage()
            .shouldNotSee(user.pages().MessagePage().optInPromo());
    }

    @Test
    @Title("Должны нажать кнопку «Включить» в промо опт-ина")
    @TestCaseId("6223")
    public void shouldClickEnableOnOptInPromo() {
        user.defaultSteps()
            .shouldSee(user.pages().MessagePage().optInPromo())
            .clicksOn(user.pages().MessagePage().optInPromo().enableBtn())
            .switchTo(IFRAME_SUBS_LIZA)
            .shouldSee(user.pages().SubscriptionsSettingsPage().subscribeAndEnableBtn())
            .shouldNotSee(user.pages().MessagePage().optInPromo());
    }

    @Test
    @Title("Должны увидеть промо-тултип через неделю после основного")
    @TestCaseId("6224")
    public void shouldSeePromoTooltipAfterModalPromo() {
        changePromoOptInSetting(601200000);
        user.defaultSteps().refreshPage()
            .shouldNotSee(user.pages().MessagePage().promoTooltip());
        changePromoOptInSetting(604800000);
        user.defaultSteps().refreshPage()
            .shouldSee(user.pages().MessagePage().promoTooltip());
    }

    @Test
    @Title("Должны нажать кнопку «Включить» в промо-тултипе опт-ина")
    @TestCaseId("6225")
    public void shouldClickEnableOnOptInPromoTooltip() {
        changePromoOptInSetting(604800000);
        user.defaultSteps().refreshPage()
            .shouldSee(user.pages().MessagePage().promoTooltip())
            .clicksOn(user.pages().MessagePage().promoTooltip().enableBtn())
            .switchTo(IFRAME_SUBS_LIZA)
            .shouldSee(user.pages().SubscriptionsSettingsPage().subscribeAndEnableBtn())
            .shouldNotSee(user.pages().MessagePage().promoTooltip());
    }

    @Test
    @Title("Закрываем промо-тултип опт-ина кликом в кнопку «Позже»")
    @TestCaseId("6226")
    public void shouldClickLaterOnOptInPromoTooltip() {
        changePromoOptInSetting(604800000);
        user.defaultSteps().refreshPage()
            .shouldSee(user.pages().MessagePage().promoTooltip())
            .clicksOn(user.pages().MessagePage().promoTooltip().laterBtn())
            .refreshPage()
            .shouldNotSee(user.pages().MessagePage().promoTooltip());
    }

    @Step("Меняем время показа промо")
    private void changePromoOptInSetting(int millisec) {
        user.apiSettingsSteps().callWithListAndParams(
            "Меняем время показа промо",
            of(
                PROMO_OPT_IN_MODAL, String.valueOf(System.currentTimeMillis() - millisec),
                PROMO_MANAGER, "%7B%22last-time-show-promo%22%3A" +
                    (System.currentTimeMillis() - millisec) +
                    "%2C%22last-promo-was-name%22%3A%22promo-opt-in-modal%22%7D"
            )
        );
    }
}
