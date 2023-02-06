package ru.yandex.autotests.innerpochta.tests.homer;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.Keys.ENTER;
import static org.openqa.selenium.Keys.TAB;

/**
 * author vasily-k
 */

@Aqua.Test
@Title("Переключение табом на кнопки в Гомере и нажатие по Enter")
@Features(FeaturesConst.HOMER)
@Stories(FeaturesConst.HOT_KEYS)
public class HomerHotkeysTest extends BaseTest {

    private static final int CREATE_ACCOUNT_BUTTON_NUMBER = 4;
    private static final int LOGIN_BUTTON_NUMBER = 3;

    private static final String PASSPORT_AUTH_URL = "https://passport.yandex.ru/auth";
    private static final String PASSPORT_REGISTER_URL = "https://passport.yandex.ru/registration";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @Before
    public void init() {
        user.defaultSteps().opensDefaultUrl();
    }

    @Test
    @Title("Нажимаем на «Войти» по Enter")
    @TestCaseId("153")
    public void shouldPushLoginButton() {
        switchToButtonWithNumber(LOGIN_BUTTON_NUMBER);
        shouldBeActive(onHomerPage().logInBtnHeadBanner());
        user.hotkeySteps().pressHotKeys(onHomerPage().logInBtnHeadBanner(), ENTER);
        user.defaultSteps().shouldBeOnUrl(startsWith(PASSPORT_AUTH_URL));
    }

    @Test
    @Title("Нажимаем на «Начать пользоваться» по Enter")
    @TestCaseId("153")
    public void shouldPushCreateAccountButton() {
        switchToButtonWithNumber(CREATE_ACCOUNT_BUTTON_NUMBER);
        shouldBeActive(onHomerPage().createAccountBtnHeadBanner());
        user.hotkeySteps().pressHotKeys(onHomerPage().createAccountBtnHeadBanner(), ENTER);
        user.defaultSteps().shouldBeOnUrl(startsWith(PASSPORT_REGISTER_URL));
    }

    @Step("Переключаемся на кнопку нажатием на TAB {0} раз")
    private void switchToButtonWithNumber(int buttonNumber) {
        for (int i = 0; i < buttonNumber; ++i) {
            user.hotkeySteps().pressHotKeys(onHomerPage().pageContent(), TAB);
        }
    }

    @Step("Элемент {0} должен быть в фокусе")
    private void shouldBeActive(MailElement element) {
        assertEquals(element, webDriverRule.getDriver().switchTo().activeElement());
    }
}
