package ru.yandex.autotests.innerpochta.tests.homer;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Проверяем параметр origin, с которым приходим в паспорт")
@Features(FeaturesConst.HOMER)
@Stories(FeaturesConst.ORIGIN)
public class HomerOrigin extends BaseTest {

    private static final String ORIGIN_ENTER = "origin=hostroot_homer_auth_ru";
    private static final String ORIGIN_REG = "origin=hostroot_homer_reg_ru";

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @Before
    public void openHomer() {
        user.defaultSteps().opensDefaultUrl();
    }

    @Test
    @Title("Проверяем ориджин Войти в хедере")
    @TestCaseId("173")
    public void shouldSeeLogInOriginHeadBanner() {
        user.defaultSteps()
            .clicksOn(onHomerPage().logInBtnHeadBanner())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(ORIGIN_ENTER));
    }

    @Test
    @Title("Проверяем ориджины Создать аккаунт в хедере")
    @TestCaseId("151")
    public void shouldSeeCreateAccOriginHeader() {
        user.defaultSteps()
            .clicksOn(onHomerPage().createAccountBtnHeadBanner())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(ORIGIN_REG));
    }
}