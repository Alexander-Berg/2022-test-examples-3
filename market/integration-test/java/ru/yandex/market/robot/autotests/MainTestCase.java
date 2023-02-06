package ru.yandex.market.robot.autotests;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.yandex.autotests.market.common.selenium.grid.SeleniumGridClient;

import java.io.IOException;

public class MainTestCase {
    @Test
    public void test01() throws IOException {
        String testingUrl = System.getProperty("ir.robot.ui.testing.url");
        DesiredCapabilities capabilities = DesiredCapabilities.chrome();
        SeleniumGridClient seleniumGridClient = new SeleniumGridClient();
        RemoteWebDriver webDriver = seleniumGridClient.find(capabilities);
        webDriver.get(testingUrl);
        Assert.assertEquals("Заголовок страницы", "Интерфейс робота Маркета", webDriver.getTitle());
    }
}
