package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android.picking;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android.AbstractAndroidInputPage;

public class AreaInputPage extends AbstractAndroidInputPage {

    @FindBy(xpath = "//android.view.View[@content-desc='wms_button']")
    private SelenideElement backButton;

    @Step("Вводим участок {areaKey}")
    public CartAddingPage inputArea(String areaKey) {
        super.performInput(areaKey);
        return new CartAddingPage();
    }
}
