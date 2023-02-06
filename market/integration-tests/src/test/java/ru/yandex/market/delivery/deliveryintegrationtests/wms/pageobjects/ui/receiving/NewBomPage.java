package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class NewBomPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='text-field']//input")
    private SelenideElement input;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement saveNewBom;

    public NewBomPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("newBomPage$"));
    }

    @Step("Вводим название части товара {name} и сохраняем")
    public SkuConfirmationPage enterBomName(Item item, String name) {
        input.sendKeys(name);
        saveNewBom.click();
        return new SkuConfirmationPage(driver);
    }

}
