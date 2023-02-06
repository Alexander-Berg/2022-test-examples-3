package ru.yandex.travel.webdriver;

import com.google.inject.Inject;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.yandex.travel.allure.AllureStepLogger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static java.lang.String.format;

public class DefaultWebDriverManager implements WebDriverManager {

    private static final Logger LOG = Logger.getLogger(AllureStepLogger.class.getName());

    private WebDriver driver;

    private final WebDriverConfig config;

    private final DesiredCapabilities capabilities;

    @Inject
    public DefaultWebDriverManager(WebDriverConfig config) {
        this.capabilities = new DesiredCapabilities();
        this.config = config;
    }

    @Override
    @Step("Стартуем браузер")
    public void startDriver() throws Throwable {
        if (config.isLocal()) {
            this.driver = new ChromeDriver();
            return;
        }

        String username = config.getRemoteUsername();
        String password = config.getRemotePassword();

        String host = config.getRemoteHost();
        int port = config.getRemotePort();

        try {
            LOG.info(String.format("Стартуем браузер на %s:%s", host, port));
            URL webDriverUrl = new URL(String.format("http://%s:%s@%s:%s/wd/hub", username, password, host, port));
            this.driver = new RemoteWebDriver(webDriverUrl, DesiredCapabilities.chrome());
            this.driver.manage().window().setSize(new Dimension(1024, 768));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public WebDriver getDriver() {
        return driver;
    }

    @Override
    public void updateCapabilities(Consumer<DesiredCapabilities> consumer) {
        consumer.accept(capabilities);
    }

    @Attachment(value = "{name}", type = "text/html")
    private String logHtml(String name, String html) {
        return html;
    }

    @Attachment(value = "{description}")
    private byte[] saveScreenShot(String description) {
        return (byte[]) getScreenShotFile(OutputType.BYTES);
    }

    private Object getScreenShotFile(OutputType type) {
        return ((TakesScreenshot) getDriver()).getScreenshotAs(type);
    }

    @Override
    @Step("Останавливаем браузер")
    public void stopDriver() {
        if (driver != null) {
            try {
                String url = driver.getCurrentUrl();
                logHtml("Ссылка", format("<a target=\"_blank\" href=\"%s\">%s</a>", url, url));
                logHtml("Активные окна браузера", format("<pre>%s</pre>", driver.getWindowHandles().toString()));
                logHtml("HTML код страницы", driver.getPageSource());
                logHtml("Куки", format("<pre>%s</pre>", driver.manage().getCookies().toString()));
                saveScreenShot("Снимок браузера");
                driver.close();
                driver.quit();
            } catch (Exception e) {
                logHtml(e.getClass().getName(),
                        format("<pre>\nMessage:\n%s\nStackTrace:\n%s\n</pre>",
                                e.getMessage(),
                                ExceptionUtils.getStackTrace(e.fillInStackTrace())
                        )
                );
            }
        }
    }


}
