package ru.yandex.travel.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.function.Consumer;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public interface WebDriverManager {

    void startDriver() throws Throwable;

    void stopDriver();

    WebDriver getDriver();

    void updateCapabilities(Consumer<DesiredCapabilities> updated);
}
