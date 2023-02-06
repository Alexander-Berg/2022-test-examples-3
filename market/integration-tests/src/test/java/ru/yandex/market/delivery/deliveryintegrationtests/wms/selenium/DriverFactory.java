package ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.codeborne.selenide.WebDriverRunner;
import io.qameta.allure.Allure;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;

import static org.openqa.selenium.remote.CapabilityType.ACCEPT_INSECURE_CERTS;
import static org.openqa.selenium.remote.CapabilityType.ACCEPT_SSL_CERTS;

@Resource.Classpath("wms/webdriver.properties")
public class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);

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

    @Property("webdriver.downloadlocalfolder")
    private String downloadLocalFolder;

    public DriverFactory() {
        PropertyLoader.newInstance().populate(this);
    }

    public WebDriver getDriver() {

        log.info("Opening browser");
        WebDriver driver;
        if (usegrid) {
            LoggingPreferences logPrefs = new LoggingPreferences();
            logPrefs.enable(LogType.PERFORMANCE, Level.ALL);
            DesiredCapabilities capability = new DesiredCapabilities("chrome", version, Platform.ANY);
            capability.setCapability("env", Arrays.asList("LANG=ru_RU.UTF-8", "LANGUAGE=ru:ru", "LC_ALL=ru_RU.UTF-8"));
            capability.setCapability("sessionTimeout", "3m");
            capability.setCapability(ACCEPT_INSECURE_CERTS, true);
            capability.setCapability(ACCEPT_SSL_CERTS, true);
            capability.setCapability("goog:loggingPrefs", logPrefs);
            if (recordvideo) {
                capability.setCapability("enableVideo", true);
            }

            driver = Retrier.retrySeleniumStep(() -> createRemoteDriver(capability));

            RemoteWebDriver rd = ((RemoteWebDriver) driver);
            String caps = Arrays.toString(rd.getCapabilities().asMap().entrySet().toArray());
            Allure.addAttachment("RemoteDriver Info",
                    String.format("Session id: %s \n\nDriver capabilities: %s",
                            rd.getSessionId(), caps)
            );

        } else {
            ChromeOptions options = new ChromeOptions();
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", downloadLocalFolder);
            prefs.put("download.prompt_for_download", false);
            prefs.put("download.directory_upgrade", true);
            prefs.put("safebrowsing.enabled", true);
            options.setExperimentalOption("prefs", prefs);
            driver = new ChromeDriver(options);
        }

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
        driver.manage().deleteAllCookies();
        driver.manage().window().maximize();
        WebDriverRunner.setWebDriver(driver);

        log.info("Got browser: {}", driver);
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
