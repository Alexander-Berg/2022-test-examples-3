package ru.yandex.autotests.innerpochta.tests.autotests.Advertisement;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TOUCH_PART;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.OLD_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT_TOUCH;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на ограничения показа рекламы в доменах и локалях")
@Features(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AdDomainsPermissionsTest {

    private static final String GENERAL = "general";

    @DataProvider
    public static Object[][] comDomains() {
        return new Object[][]{
            {YandexDomain.COM},
            {YandexDomain.COMTR}
        };
    }

    @DataProvider
    public static Object[][] KUBRDomains() {
        return new Object[][]{
            {YandexDomain.BY},
            {YandexDomain.KZ}
        };
    }

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount(OLD_USER_TAG));
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем показ рекламы",
            of(SHOW_ADVERTISEMENT_TOUCH, TRUE)
        );
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), Utils.getRandomName(), "");
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Настройка рекламы есть только для русской локали")
    @Description("Проверяется на одном языке, потому что в коде проверка, что язык не русский, а какой он всё равно")
    @TestCaseId("584")
    public void shouldSeeAdvSettingOnlyForRu() {
        steps.user().defaultSteps()
            .switchLanguage("en")
            .shouldNotSee(steps.pages().touch().messageList().advertisement())
            .opensDefaultUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(GENERAL))
            .shouldNotSee(steps.pages().touch().settings().advertToggler());
    }

    @Test
    @Title("Не должны видеть рекламу на доменах COM и COM.TR")
    @TestCaseId("584")
    @UseDataProvider("comDomains")
    public void shouldNotSeeAdOnComAndComTr(YandexDomain dmn) {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).loginsToDomain(dmn);
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().messageBlock())
            .shouldNotSee(steps.pages().touch().messageList().advertisement()).opensCurrentUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(GENERAL))
            .shouldSee(steps.pages().touch().settings().closeBtn())
            .shouldNotSee(steps.pages().touch().settings().advertToggler());
    }

    @Test
    @Title("Должны видеть рекламу на доменах КУБР")
    @TestCaseId("584")
    @UseDataProvider("KUBRDomains")
    public void shouldSeeAdOnKUBR(YandexDomain dmn) {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).loginsToDomain(dmn);
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().advertisement())
            .opensCurrentUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(GENERAL))
            .shouldSee(steps.pages().touch().settings().advertToggler());
    }
}
