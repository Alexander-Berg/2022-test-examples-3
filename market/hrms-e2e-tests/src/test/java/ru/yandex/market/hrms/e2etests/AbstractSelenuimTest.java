package ru.yandex.market.hrms.e2etests;


import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.hrms.e2etests.selenium.DriverFactory;
import ru.yandex.market.hrms.e2etests.selenium.SaveBrowserContextExtension;
import ru.yandex.market.hrms.e2etests.selenium.SeleniumUtil;
import ru.yandex.market.hrms.e2etests.steps.HrmsUiSteps;
import ru.yandex.market.hrms.e2etests.steps.PassportUiSteps;

public abstract class AbstractSelenuimTest {

    protected WebDriver driver;
    protected HrmsUiSteps hrmsUi = new HrmsUiSteps();
    private PassportUiSteps passportUi = new PassportUiSteps();

    @RegisterExtension
    public SaveBrowserContextExtension saveBrowserContextExtension = new SaveBrowserContextExtension();

    @BeforeEach
    @Step("Общая для тестов подготовка данных")
    public void initialSetUp() {
        openNewBrowser();
        passportUi.login();
    }

    @AfterEach
    @Step("Общая для тестов очистка данных")
    public void finalTearDown() {
        closeBrowser();
    }

    protected void openNewBrowser() {
        closeBrowser();

        driver = new DriverFactory().getDriver();
        saveBrowserContextExtension.setDriver(driver);
    }

    protected void closeBrowser() {
        SeleniumUtil.closeBrowser(driver);
    }
}
