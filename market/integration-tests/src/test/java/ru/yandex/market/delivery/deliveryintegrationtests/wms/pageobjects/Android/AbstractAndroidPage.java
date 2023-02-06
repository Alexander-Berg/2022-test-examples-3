package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android;

import java.util.concurrent.TimeUnit;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import io.appium.java_client.android.AndroidDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;

import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

public abstract class AbstractAndroidPage {
    private AndroidDriver<SelenideElement> androidDriver;

    protected AbstractAndroidPage() {
        this.androidDriver = (AndroidDriver<SelenideElement>) getWebDriver();
        androidDriver.context("NATIVE_APP");
        androidDriver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
        WebDriverRunner.setWebDriver(androidDriver);
        page(this);
    }

    protected void hideKeyboard() {
        if (androidDriver.isKeyboardShown()) {
            androidDriver.hideKeyboard();
        }
    }
}
