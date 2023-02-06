package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.returns;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

@Slf4j
public class SetupDimensionsPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='text-field_length']//div//input")
    private SelenideElement inputLength;

    @FindBy(xpath = "//div[@data-e2e='text-field_width']//div//input")
    private SelenideElement inputWidth;

    @FindBy(xpath = "//div[@data-e2e='text-field_height']//div//input")
    private SelenideElement inputHeight;

    @FindBy(xpath = "//div[@data-e2e='text-field_weight']//div//input")
    private SelenideElement inputWeight;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    public SetupDimensionsPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("setupDimensions"));
    }

    @Step("Вводим ВГХ для нового товара")
    public void enterVgh() {
        inputLength.sendKeys("1");
        inputWidth.sendKeys("2");
        inputHeight.sendKeys("3");
        inputWeight.sendKeys("4");
        forward.click();
    }
}
