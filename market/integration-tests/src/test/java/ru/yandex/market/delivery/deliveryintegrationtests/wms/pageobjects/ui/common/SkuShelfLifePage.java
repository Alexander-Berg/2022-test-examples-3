package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.returns.SkuInputPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class SkuShelfLifePage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='text-field_expiration-date']//input")
    private SelenideElement expirationDateInput;

    @FindBy(xpath = "//div[@data-e2e='text-field_creation-date']//input")
    private SelenideElement creationDateInput;

    @FindBy(xpath = "//div[@name='expCheckBox']//input")
    private SelenideElement expCheckBox;

    @FindBy(xpath = "//div[@data-e2e='text-field_to-expire-days']//input")
    private SelenideElement toExpireDaysInput;

    @FindBy(xpath = "//div[@data-e2e='duration-selector']//input")
    private SelenideElement durationSelector;

    public SkuShelfLifePage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("skuShelfLifePage$"));
    }

    @Step("Вводим СГ товара: Дата производства")
    public SkuShelfLifePage enterCreationDate(Item item) {
        creationDateInput.sendKeys(item.getCreationDate());
        creationDateInput.pressEnter();
        return this;
    }

    @Step("Вводим СГ товара: Годен до")
    public SkuInputPage enterExpirationDate(Item item) {
        expirationDateInput.sendKeys(item.getExpDate());
        expirationDateInput.pressEnter();
        return new SkuInputPage(driver, item);
    }
}
