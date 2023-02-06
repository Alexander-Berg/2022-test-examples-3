package ru.yandex.autotests.innerpochta.tests.touch.autotests;

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
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.pages.ComposeIframePage.IFRAME_COMPOSE;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_URL_WITHOUT_DOMAIN;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TOUCH_ONBOARDING;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("[Тач] Переход по email и url в описании/месте")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.VIEW_EVENT_POPUP)
@RunWith(DataProviderRunner.class)
public class TouchEmailsAndUrlsInEventTest {

    private static final String EMAIL_IN_DESCRIPTION = "gan4neo@yandex.ru";
    private static final String EMAIL_IN_PLACE = "123qwe@mail.ru";
    private static final String URL_IN_DESCRIPTION_EN = "habr.com";
    private static final String URL_IN_DESCRIPTION_RU = "запутевкой.рф";
    private static final String URL_IN_DESCRIPTION_MIX = "ru.m.wikipedia.org/wiki/Заглавная_страница";
    private static final String URL_IN_DESCRIPTION_VIRUS = "wmconvirus.narod.ru";
    private static final String URL_IN_PLACE = "https://yandex.ru/maps/2/saint-petersburg/?ll=30.305795%2C59." +
        "936439&mode=search&oid=1090697752&ol=biz&sctx=ZAAAAAgBEAAaKAoSCVnaqbncUD5AESdr1EM0%2BE1AEhIJIorJG2AmA" +
        "EAR2UElrmPc4T8iBQABAgMFKAUwADiKvOeZoJuOkaYBQAJIAVXNzMw%2BWABqAnJ1cACdAc3MzD2gAQCoAQA%3D&sll=30.305795%2C" +
        "59.936439&source=wizgeo&sspn=0.034332%2C0.077527&text=%D0%B1%D0%B5%D0%BD%D1%83%D0%B0&utm_medium=maps-des" +
        "ktop&utm_source=serp&z=12";
    private static final String URL_MATCH_RU = "xn--80aeignf2ae1aj.xn--p1ai/";
    private static final String URL_MATCH_MIX = "ru.m.wikipedia.org/wiki/%D0%97%D0%B0%D0%B3%D0%BB%D0%B0%D0%B" +
        "2%D0%BD%D0%B0%D1%8F_%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B8%D1%86%D0%B0" ;
    private static final String URL_MATCH_PLACE = "yandex.ru/maps/2/saint-petersburg/";

    private TouchRulesManager rules = touchRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(clearAcc(() -> steps.user()));

    @DataProvider
    public static Object[][] urls() {
        return new Object[][]{
            {URL_IN_DESCRIPTION_EN, URL_IN_DESCRIPTION_EN},
            {URL_IN_DESCRIPTION_RU, URL_MATCH_RU},
            {URL_IN_DESCRIPTION_MIX, URL_MATCH_MIX},
            {URL_IN_DESCRIPTION_VIRUS, URL_IN_DESCRIPTION_VIRUS},
            {URL_IN_PLACE, URL_MATCH_PLACE}
        };
    }

    @Before
    public void setUp() {
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Редирект в компоуз после тапа по email-у в описании и месте")
    @TestCaseId("1216")
    @DataProvider({EMAIL_IN_DESCRIPTION, EMAIL_IN_PLACE})
    public void shouldRedirectToComposeAfterTapOnEmail(String mailToEmail) {
        Event event = steps.user().settingsCalSteps()
            .formDefaultEvent(steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId())
            .withName(getRandomName())
            .withDescription(EMAIL_IN_DESCRIPTION)
            .withLocation(EMAIL_IN_PLACE);
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .clicksOnLink(mailToEmail)
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(both(containsString(MAIL_URL_WITHOUT_DOMAIN)).and(containsString("compose")));
        steps.user().defaultSteps().shouldSee(steps.user().touchPages().composeIframe().iframe())
            .switchTo(IFRAME_COMPOSE);
        steps.user().defaultSteps()
            .shouldSeeThatElementHasText(steps.pages().touch().composeIframe().yabble(), mailToEmail);
    }

    @Test
    @Title("Переход по URL в описании/месте")
    @TestCaseId("1217")
    @UseDataProvider("urls")
    public void shouldOpenUrlAfterTapOnIt(String urlInDescription, String matchUrl){
        Event event = steps.user().settingsCalSteps()
            .formDefaultEvent(steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId())
            .withName(getRandomName())
            .withDescription(URL_IN_DESCRIPTION_EN + "\n" +
                URL_IN_DESCRIPTION_RU + "\n" +
                URL_IN_DESCRIPTION_MIX + "\n" +
                URL_IN_DESCRIPTION_VIRUS)
            .withLocation(URL_IN_PLACE);
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().events().get(0))
            .clicksOnLink(urlInDescription)
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(matchUrl));
    }
}
