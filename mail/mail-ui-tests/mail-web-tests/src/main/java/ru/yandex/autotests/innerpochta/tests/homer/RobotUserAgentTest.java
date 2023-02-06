package ru.yandex.autotests.innerpochta.tests.homer;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.webcommon.util.prop.WebDriverProperties;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.util.DomainConsts.AZ;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.BE;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.BY;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.COIL;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.COM;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.COMAM;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.COMGE;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.COMTR;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.EE;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.EN;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.FR;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.KG;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.KK;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.KZ;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.LT;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.LV;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.MD;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.RU;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.TJ;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.TM;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.TR;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.UK;
import static ru.yandex.autotests.innerpochta.util.DomainConsts.UZ;

/**
 * @author vasily-k
 */

@Aqua.Test
@Title("Роботные пользователи в Гомере")
@Features(FeaturesConst.HOMER)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class RobotUserAgentTest extends BaseTest {

    private static final String BE_TEXT = "Увайсці";
    private static final String KK_TEXT = "Кіру";
    private static final String UK_TEXT = "Увійти";
    private static final String EN_TEXT = "Log in";
    private static final String TR_TEXT = "Giriş yap";
    private static final String RU_TEXT = "Войти";

    private static final String USER_AGENT_OPTION = "--user-agent=baiduspider";

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @Override
    public DesiredCapabilities setCapabilities() {
        DesiredCapabilities capabilities = new DesiredCapabilities(
            WebDriverProperties.props().driverType(),
            WebDriverProperties.props().version(),
            WebDriverProperties.props().platform()
        );
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments(USER_AGENT_OPTION);
        capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
        return capabilities;
    }

    @Test
    @Title("Отдаем роботному пользователю язык, соответствующий домену")
    @TestCaseId("210")
    @DataProvider({COMAM, AZ, EE, COMGE, KG, LT, LV, MD, RU, TJ, TM, UZ, COMTR, BY, COM, FR, COIL, KZ})
    public void shouldSeeLanguageSuitDomain(String domain) {
        user.defaultSteps().opensDefaultUrlWithDomain(domain)
            .shouldHasText(onHomerPage().logInBtnHeadBanner(), getLoginButtonTextForLanguage(domain));
    }

    @Test
    @Title("Отдаем роботному пользователю язык из lang=")
    @TestCaseId("211")
    @DataProvider({BE, KK, UK, EN, TR})
    public void shouldSeeLanguageSuitLangParameter(String lang) {
        user.defaultSteps().opensDefaultUrlWithPostFix("/?lang=" + lang)
            .shouldHasText(onHomerPage().logInBtnHeadBanner(), getLoginButtonTextForLanguage(lang));
    }

    private String getLoginButtonTextForLanguage(String lang) {
        switch (lang) {
            case COM:
            case FR:
            case COIL:
            case EN:
                return EN_TEXT;
            case COMTR:
            case TR:
                return TR_TEXT;
            case BY:
            case BE:
                return BE_TEXT;
            case KZ:
            case KK:
                return KK_TEXT;
            case UK:
                return UK_TEXT;
            default:
                return RU_TEXT;
        }
    }
}
