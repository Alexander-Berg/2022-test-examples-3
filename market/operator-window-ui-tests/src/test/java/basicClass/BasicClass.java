package ui_tests.src.test.java.basicClass;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import pages.Pages;
import tools.Tools;
import unit.Config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class BasicClass {

    /**
     * залогиниться в паспорте
     *
     * @param webDriver
     * @param login     логин
     * @param pass      пароль
     */
    public static void logInToSystem(WebDriver webDriver, String login, String pass) {
        do {
            try {
                webDriver.get("https://passport.yandex-team.ru/auth/?retpath=" + Config.getProjectURL());
                Pages.loginPage(webDriver)
                        .setLogin(login)
                        .setPass(pass)
                        .loginButtonClick();
            } catch (Throwable t) {
                throw new Error("Не удалось авторизоваться в системе \n" + t);
            }
            Tools.waitElement(webDriver).waitTime(5000);
        } while (Tools.findElement(webDriver).findElements(By.xpath("//*[contains(text(),'Внутренняя ошибка'])")).size() != 0);
        //Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//*[text()='Загрузка...']"));
        Tools.waitElement(webDriver).waitInvisibilityElementTheTime(
                By.xpath("//*[text()='Загрузка...']"),
                Config.DEF_TIME_WAIT_LOAD_PAGE);
        // Оставил метод специально, сейчас из-за того что браузеры в гриде не обновили, не работает перевод
        try {
            Tools.waitElement(webDriver).waitInvisibleLoadingElement();
            Tools.waitElement(webDriver).waitElementToAppearInDOMTheTime(By.xpath("//*[@class='_p4p5hTc lb_uDcSY']"), Config.DEF_TIME_WAIT_LOAD_PAGE);
        }catch (Error e){
            String randomName = Tools.other().getRandomText();
            Tools.other().captureScreenshot(webDriver, "tests.logInToSystem", randomName);
            throw new Error(e+" скриншот - logInToSystem/"+randomName);
        }


    }

    /**
     * инициализация нового WebDriver
     */
    public static WebDriver newWebDriver() {
        WebDriver webDriver;

        final DesiredCapabilities browser = new DesiredCapabilities();
        if (Config.isRecordVideo()) {
            browser.setCapability("enableVideo", true);
        }

        if (Config.getUseSeleniumGrid()) {
            try {
                browser.setBrowserName(Config.getSeleniumGridBrowserName());
                browser.setPlatform(Platform.ANY);
                browser.setCapability("browserTimeout", "12000");
                browser.setCapability("timeout", "120000");
                browser.setVersion(Config.getSeleniumGridBrowserVersion());
                webDriver = new RemoteWebDriver(new URL(Config.getSeleniumGridUrl()), browser);
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
            webDriver = new ChromeDriver(browser);
        }

        webDriver.manage().window().setSize(new Dimension(1366, 768));
        return webDriver;
    }
}
