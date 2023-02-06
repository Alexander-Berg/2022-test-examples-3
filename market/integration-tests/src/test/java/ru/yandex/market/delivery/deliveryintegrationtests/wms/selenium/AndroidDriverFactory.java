package ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.codeborne.selenide.SelenideElement;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;

@Slf4j
@Resource.Classpath({"wms/appium.properties", "wms/infor.properties"})
public class AndroidDriverFactory {

    @Property("appium.url")
    private  String appiumUrl;
    @Property("appium.apk.path")
    private String apkPath;
    @Property("appium.config.path")
    private String configPath;
    @Property("infor.host")
    private String webAppHost;
    @Property("infor.ui")
    private String uiPath;
    @Property("appium.video.url")
    private String videoUrl;

    private AndroidDriver<SelenideElement> driver;

    private static final String WEBVIEW = "WEBVIEW_ru.yandex.market.wms";

    public AndroidDriverFactory() {
        PropertyLoader.newInstance().populate(this);
    }

    public AppiumDriver<SelenideElement> getDriver() {
        try {
            driver = new AndroidDriver<>(new URL(appiumUrl), getAndroidDesiredCapabilities());
            Allure.link("AppVideo", videoUrl + driver.getSessionId());
            driver.pushFile(configPath, createConfigFile());
            log.info("Got emulator " + driver.getSessionId());
            driver.resetApp();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Retrier.retry(this::switchToWebview, Retrier.RETRIES_BIG, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);

        return driver;
    }

    private DesiredCapabilities getAndroidDesiredCapabilities() {
        DesiredCapabilities capabilities = new DesiredCapabilities();

        capabilities.setCapability("platformName", "Android");
        capabilities.setCapability("deviceName", "android-phone");
        capabilities.setCapability("platformVersion", "8.1");
        capabilities.setCapability("appPackage", "ru.yandex.market.wms");
        capabilities.setCapability("appActivity", ".ui.main.MainActivity");
        capabilities.setCapability("app", apkPath);
        capabilities.setCapability("orientation", "PORTRAIT");
        capabilities.setCapability("autoGrantPermissions", "true");
        capabilities.setCapability("browser", "android-phone");
        capabilities.setCapability("version", "8.1");
        capabilities.setCapability("platform", "Android");
        capabilities.setCapability("newCommandTimeout", "180");
        capabilities.setCapability("automationName", "UiAutomator2");
        capabilities.setCapability("enableVideo", true);

        return capabilities;
    }

    protected void switchToWebview() {
        driver.context("NATIVE_APP");
        Set<String> contexts = driver.getContextHandles();
        if (contexts.contains(WEBVIEW)) {
            driver.context(WEBVIEW);
            log.info("Switched to webview " + WEBVIEW);
        } else {
            throw new AssertionError("Webview NOT available");
        }
    }

    private File createConfigFile() {
        File config = new File("wms_url.cfg");
        byte[] url = (webAppHost + uiPath).getBytes();

        try {
            FileOutputStream outputStream = new FileOutputStream(config);
            outputStream.write(url);
            outputStream.close();
        } catch (IOException e) {
            log.info("Caught exception while creating config file: " + e.getMessage());
            e.printStackTrace();
        }

        return config;
    }
}
