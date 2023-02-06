package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests;


import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.InboundTable;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.ExceptionHandlingExtension;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.AndroidDriverFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.DriverFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.SeleniumUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.android.AndroidSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.process.ProcessSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.tsd.TsdSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui.UISteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation.WSSteps;

import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

@Epic("Selenium Tests")
@Slf4j
public abstract class AbstractUiTest {
    protected static final String RECEIVING_AND_SHIPPING_DOCK = "DOCK";
    protected WebDriver driver;
    protected WSSteps wsSteps;
    protected TsdSteps tsdSteps;
    protected UISteps uiSteps;
    protected User user;
    protected ProcessSteps processSteps;
    protected String areaKey;
    protected String putawayZone;
    protected InboundTable inboundTable;
    protected AndroidSteps androidSteps;

    @RegisterExtension
    public ExceptionHandlingExtension exceptionHandlingExtension = new ExceptionHandlingExtension();

    private int getUserLockDuration(TestInfo testInfo) {
        RetryableTest annotationParams = testInfo.getTestMethod()
                .flatMap(testMethods -> findAnnotation(testMethods, RetryableTest.class))
                .orElse(null);

        return annotationParams != null ? annotationParams.duration() :
                DatacreatorSteps.Users().getDefaultUserLockDuration();
    }

    @BeforeEach
    @Step("Общая для selenium тестов подготовка данных")
    public void initialSetUp(TestInfo testInfo) {
        areaKey = DatacreatorSteps.Location().createArea();
        putawayZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        inboundTable = DatacreatorSteps
                .Location()
                .createInboundTable(putawayZone);

        user = DatacreatorSteps
                .Users()
                .lockUser(getUserLockDuration(testInfo));

        openNewBrowser();
    }

    @AfterEach
    @Step("Общая для тестов очистка данных")
    public void finalTearDown() {
        closeApp();
        closeBrowser();

        DatacreatorSteps
                .Users()
                .unlockUser(user);

        DatacreatorSteps
                .Location()
                .deleteInboundTable(inboundTable);

        DatacreatorSteps.Location().deletePutawayZone(putawayZone);
        DatacreatorSteps.Location().deleteArea(areaKey);
    }

    protected void openNewBrowser() {
        closeBrowser();

        driver = new DriverFactory().getDriver();
        wsSteps = new WSSteps(driver, user);
        tsdSteps = new TsdSteps(driver, user, inboundTable);
        uiSteps = new UISteps(driver, user, inboundTable);
        processSteps = new ProcessSteps(uiSteps);

        exceptionHandlingExtension.setDriver(driver);
    }

    protected void closeBrowser() {
        SeleniumUtil.closeBrowser(driver);
    }

    protected void openApp() {
        AppiumDriver<SelenideElement> appiumDriver = new AndroidDriverFactory().getDriver();
        WebDriverRunner.setWebDriver(appiumDriver);
        exceptionHandlingExtension.setDriver(appiumDriver);
        androidSteps = new AndroidSteps(user);
    }

    protected void closeApp() {
        if (getWebDriver() instanceof AndroidDriver<?>) {
            getWebDriver().quit();
        }
    }

    protected void switchFromSeleniumToAppium() {
        closeBrowser();
        openApp();
    }

    protected void switchFromAppiumToSelenium() {
        closeApp();
        openNewBrowser();
    }
}
