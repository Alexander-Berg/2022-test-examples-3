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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableBiMap.of;
import static ru.yandex.autotests.innerpochta.touch.pages.UnsubscribeIframe.IFRAME_SUBS_LIZA;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.PROMO_UNSUBSCRIBE_POPUP;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Промо попапа управления рассылками")
@Features({FeaturesConst.SETTINGS, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.PROMO)
@RunWith(DataProviderRunner.class)
public class SubscriptionsPromoTest extends BaseTest {

    private static final String CREDS_LESS_THEN_10_ML = "SubscriptionsLessThan10MLTest";

    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AccLockRule lockLessThen10ML = AccLockRule.use().names(CREDS_LESS_THEN_10_ML);
    private RestAssuredAuthRule authLessThen10ML = RestAssuredAuthRule.auth(lockLessThen10ML);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(lockLessThen10ML)
        .around(authLessThen10ML);

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Сбрасываем настройку показа промо отписок",
            of(PROMO_UNSUBSCRIBE_POPUP, EMPTY_STR)
        )
            .callWithListAndParams("Отключаем компактную шапку", of(LIZA_MINIFIED_HEADER, EMPTY_STR));
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Закрываем промо рассылок по крестику")
    @TestCaseId("5034")
    public void shouldCloseUnsubscribePromo() {
        user.defaultSteps()
            .shouldSee(onUnsubscribePopupPage().unsubscribePromo())
            .clicksOn(onUnsubscribePopupPage().closeUnsubscribePromo())
            .shouldNotSee(onUnsubscribePopupPage().unsubscribePromo())
            .refreshPage()
            .shouldNotSee(onUnsubscribePopupPage().unsubscribePromo());
    }

    @Test
    @Title("Открываем попап отписок из промо отписок")
    @TestCaseId("5035")
    public void shouldOpenUnsubscribePopupFromPopup() {
        user.defaultSteps()
            .shouldSee(onUnsubscribePopupPage().unsubscribePromo())
            .clicksOn(onUnsubscribePopupPage().openPopupFromUnsubscribePromo())
            .shouldNotSee(onUnsubscribePopupPage().unsubscribePromo())
            .switchTo(IFRAME_SUBS_LIZA)
            .shouldSee(onUnsubscribePopupPage().closeSubs());
    }

    @Test
    @Title("Не показываем промо рассылок тем, у кого их меньше десяти")
    @TestCaseId("5036")
    public void shouldNotSeeUnsubscribePromoIfLessThan10ML() {
        user.loginSteps().forAcc(lockLessThen10ML.firstAcc()).logins();
        user.apiSettingsSteps().withAuth(authLessThen10ML).callWithListAndParams(
            "Сбрасываем настройку показа промо отписок",
            of(PROMO_UNSUBSCRIBE_POPUP, EMPTY_STR)
        );
        user.defaultSteps().refreshPage()
            .shouldNotSee(onUnsubscribePopupPage().unsubscribePromo());
    }

    @Test
    @Title("Промо не закрывается после рефреша")
    @TestCaseId("5043")
    public void shouldSeeUnsubscribePromoAfterRefresh() {
        user.defaultSteps()
            .shouldSee(onUnsubscribePopupPage().unsubscribePromo())
            .refreshPage()
            .shouldSee(onUnsubscribePopupPage().unsubscribePromo());
    }

    @Test
    @Title("Промо не показывается в компактной шапке")
    @TestCaseId("5044")
    public void shouldNotSeePromoInCompactHeader() {
        user.apiSettingsSteps().callWithListAndParams("Включаем компактную шапку", of(LIZA_MINIFIED_HEADER, STATUS_ON));
        user.defaultSteps().refreshPage()
            .shouldNotSee(onUnsubscribePopupPage().unsubscribePromo());
    }

}
