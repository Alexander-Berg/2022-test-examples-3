package ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions;

import java.lang.reflect.Method;
import java.util.Optional;

import com.codeborne.selenide.WebDriverRunner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.qameta.allure.Attachment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.gson.JsonParser.parseString;

public class ExceptionHandlingExtension implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(ExceptionHandlingExtension.class);

    private WebDriver driver;

    public ExceptionHandlingExtension(WebDriver driver){
        super();
        this.driver = driver;
    }

    public ExceptionHandlingExtension(){
        super();
    }

    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * При падении теста вне шагов @BeforeEach и @AfterEach выполняется:
     *  - сохранение скриншота
     *  - сохранение исходного кода страницы
     *  - лога запросов браузера с типом XHR, за исключением запросов в метрику
     *  - прикрепление сохраненных данных к allure-отчету
     *  - выброс исключения, которое привело к падению теста
     * @param context
     * @param throwable
     * @throws Throwable
     */
    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveData();
        throw throwable;
    }

    /**
     * При падении теста на шагах внутри @BeforeEach выполняется:
     *  - сохранение скриншота
     *  - сохранение исходного кода страницы
     *  - лога запросов браузера с типом XHR, за исключением запросов в метрику
     *  - прикрепление сохраненных данных к allure-отчету
     *  - выброс исключения, которое привело к падению теста
     * @param context
     * @param throwable
     * @throws Throwable
     */
    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        saveData();
        throw throwable;
    }

    /**
     * При выбросе исключения на шагах внутри @AfterEach выполняется:
     *  - сохранение скриншота
     *  - сохранение исходного кода страницы
     *  - лога запросов браузера с типом XHR, за исключением запросов в метрику
     *  - прикрепление сохраненных данных к allure-отчету
     *  - запись в лог факта выброса исключения
     *  - тест считается успешно выполненным
     * @param context
     * @param throwable
     */
    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) {
        saveData();
        Optional<Method> testMethod = context.getTestMethod();
        String testName = testMethod.isPresent() ? testMethod.get().getAnnotation(DisplayName.class).value()
                : "Unknown Test";
        log.info(String.format(
                "Caught Exception during tearDown execution after test \"%s\": %s", testName, throwable.getMessage())
        );
        throwable.printStackTrace();
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
            AttachBrowserRequestsLog();
        } catch (Throwable error) {
            log.info("Failed to save browser data: {}", error.getMessage());
        }
    }

    /**
     * Прикрепление к allure-отчету лога запросов браузера с типом XHR, за исключением запросов в метрику
     */
    @Attachment(value = "Browser requests log", type = "text/plain")
    private String AttachBrowserRequestsLog() {
        LogEntries logs;
        try {
            logs = WebDriverRunner.getWebDriver().manage().logs().get("performance");
        } catch (NoSuchSessionException e) {
            log.info("Caught exception: " + e.getMessage());
            e.printStackTrace();
            return "Session was not found, no logs to attach";
        }
        StringBuilder logsBrowser = new StringBuilder();

        for (LogEntry le : logs) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String log = gson.toJson(parseString(le.getMessage()));
            boolean isRequest =log.contains("Network.requestWillBeSent");
            boolean isXhr = log.contains("\"type\": \"XHR\"");
            boolean isYaMetrics = log.contains("mc.yandex.ru") || log.contains("/clck/click");

            if (isRequest && isXhr && !isYaMetrics)  {
                logsBrowser.append(log);
            }
        }

        return logsBrowser.toString();
    }
}
