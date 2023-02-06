package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.returns;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class BoxInputPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='text-field']//input")
    private SelenideElement input;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    public BoxInputPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("boxInputPage$"));
    }

    @Step("Вводим штрихкод коробки")
    public ItemInputPage enterboxId(String boxId) {
        input.sendKeys(boxId);
        forward.click();
        return new ItemInputPage(driver);
    }

}
