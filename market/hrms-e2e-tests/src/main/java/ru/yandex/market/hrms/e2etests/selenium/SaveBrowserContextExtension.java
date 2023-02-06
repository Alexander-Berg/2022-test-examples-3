package ru.yandex.market.hrms.e2etests.selenium;

import io.qameta.allure.Attachment;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveBrowserContextExtension implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {
    private final Logger log = LoggerFactory.getLogger(SaveBrowserContextExtension.class);

    private WebDriver driver;

    public SaveBrowserContextExtension(WebDriver driver){
        super();
        this.driver = driver;
    }

    public SaveBrowserContextExtension(){
        super();
    }

    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveData();
        throw throwable;
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveData();
        throw throwable;
    }

    @Attachment(value = "Page screenshot", type = "image/png")
    public byte[] saveScreenshot() {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    @Attachment(value = "Page source")
    public String savePageSource() {
        return driver.getPageSource();
    }

    public void saveData() {
        if (driver == null) {
            log.info("No browser is open");
            return;
        }

        log.info("Saving browser data");

        try {
            saveScreenshot();
            savePageSource();
        } catch (Throwable error) {
            log.info("Failed to save browser data: {}", error.getMessage());
        }
    }
}
