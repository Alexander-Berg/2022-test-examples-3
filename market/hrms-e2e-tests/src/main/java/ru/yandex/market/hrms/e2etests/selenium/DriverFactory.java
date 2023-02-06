package ru.yandex.market.hrms.e2etests.selenium;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.codeborne.selenide.WebDriverRunner;
import io.qameta.allure.Allure;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.hrms.e2etests.tools.Retrier;

import static org.openqa.selenium.remote.CapabilityType.ACCEPT_INSECURE_CERTS;
import static org.openqa.selenium.remote.CapabilityType.ACCEPT_SSL_CERTS;

@Resource.Classpath("webdriver.properties")
public class DriverFactory {
    @Property("webdriver.gridurl")
    private String gridurl;

    @Property("webdriver.usegrid")
    private boolean usegrid;

    @Property("webdriver.gridversion")
    private String version;

    @Property("webdriver.recordvideo")
    private boolean recordvideo;

    @Property("webdriver.videourl")
    private String videoUrl;

    public DriverFactory() {
        PropertyLoader.newInstance().populate(this);
    }

    public WebDriver getDriver() {

        WebDriver driver;
        if (usegrid) {
            DesiredCapabilities capability = new DesiredCapabilities("chrome", version, Platform.ANY);
            capability.setCapability("env", Arrays.asList("LANG=ru_RU.UTF-8", "LANGUAGE=ru:ru", "LC_ALL=ru_RU.UTF-8"));
            capability.setCapability("sessionTimeout", "3m");
            capability.setCapability(ACCEPT_INSECURE_CERTS, true);
            capability.setCapability(ACCEPT_SSL_CERTS, true);
            if (recordvideo) {
                capability.setCapability("enableVideo", true);
            }

            driver = Retrier.retrySeleniumStep(() -> createRemoteDriver(capability));

        } else {
            driver = new ChromeDriver();
        }

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
        driver.manage().deleteAllCookies();
        driver.manage().window().maximize();
        WebDriverRunner.setWebDriver(driver);

        return driver;
    }

    private WebDriver createRemoteDriver(DesiredCapabilities capability) {
        RemoteWebDriver remoteWebDriver;
        try {
            remoteWebDriver = new RemoteWebDriver(new URL(gridurl), capability);
            if (recordvideo) {
                Allure.link("Video", videoUrl + remoteWebDriver.getSessionId());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        return remoteWebDriver;
    }
}
