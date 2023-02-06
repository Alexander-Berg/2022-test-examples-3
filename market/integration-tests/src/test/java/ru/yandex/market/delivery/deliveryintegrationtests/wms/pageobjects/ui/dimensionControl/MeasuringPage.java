package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.dimensionControl;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;

public class MeasuringPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='weight_field']//input")
    private SelenideElement weightInput;

    @FindBy(xpath = "//div[@data-e2e='length_field']//input")
    private SelenideElement lengthInput;

    @FindBy(xpath = "//div[@data-e2e='width_field']//input")
    private SelenideElement widthInput;

    @FindBy(xpath = "//div[@data-e2e='height_field']//input")
    private SelenideElement heightInput;

    @FindBy(xpath = "//button[@data-e2e='save_button']")
    private SelenideElement forwardButton;

    public MeasuringPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим ВГХ для УИТа")
    public UitScanPage enterVGH(String length, String width, String height, String weight) {
        weightInput.setValue(weight);
        lengthInput.setValue(length);
        widthInput.setValue(width);
        heightInput.setValue(height);
        forwardButton.click();
        return new UitScanPage(driver);
    }

}
