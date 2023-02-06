package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android;

import com.codeborne.selenide.SelenideElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.support.FindBy;

import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

public abstract class AbstractAndroidInputPage extends AbstractAndroidPage {

    @FindBy(xpath = "//android.view.View[@content-desc='wms_input']/..")
    private SelenideElement input;

    protected void performInput(String value) {
        input.setValue(value);
        ((AndroidDriver<SelenideElement>) getWebDriver()).pressKey(new KeyEvent(AndroidKey.ENTER));
    }
}
