package init;

import com.codeborne.selenide.Configuration;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class InitDriver {

    public static WebDriver getDriver() {
        WebDriver webDriver;

        final DesiredCapabilities browser = new DesiredCapabilities();

        if (Config.getUseGrid()) {
            try {
                browser.setBrowserName(Config.getGridBrowser());
                browser.setPlatform(Platform.ANY);
                browser.setVersion(Config.getGridBrowserVersion());
                webDriver = new RemoteWebDriver(new URL(Config.getGridURL()), browser);
                ((RemoteWebDriver) webDriver).setFileDetector(new LocalFileDetector());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                throw new Error(e.getMessage());
            }
        } else {
            File fileDriver = new File("resources/yandexdriver");
            if (fileDriver.exists()) {
                System.setProperty("webdriver.chrome.driver", "resources/yandexdriver");
            } else {
                throw new Error("В папке 'resources' нет файла с web драйвером 'yandexdriver'");
            }
            webDriver = new ChromeDriver();
        }
        Configuration.browserSize = "1920x1080";
        return webDriver;
    }
}
