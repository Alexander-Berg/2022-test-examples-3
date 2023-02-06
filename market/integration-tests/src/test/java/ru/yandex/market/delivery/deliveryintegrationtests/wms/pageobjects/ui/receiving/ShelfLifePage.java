package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ItemContext;
import com.codeborne.selenide.SelenideElement;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;

import static java.lang.Integer.parseInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class ShelfLifePage extends ItemContext {

    @FindBy(xpath = "//div[@data-e2e='text-field_expiration-date']//input")
    private SelenideElement expirationDateInput;

    @FindBy(xpath = "//div[@data-e2e='text-field_creation-date']//input")
    private SelenideElement creationDateInput;

    @FindBy(xpath = "//label[@class='_1xu5iLKLegzyojZt']")
    private SelenideElement expCheckBox;

    @FindBy(xpath = "//div[@data-e2e='text-field_to-expire-days']//input")
    private SelenideElement toExpireDaysInput;

    @FindBy(xpath = "//select[@data-e2e='duration-selector']")
    private SelenideElement durationSelector;

    @FindBy(xpath = "//option[@value='month']")
    private SelenideElement durationMonths;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    public ShelfLifePage(WebDriver driver, Item item) {
        super(driver, item);
        wait.until(urlMatches("shelfLife$"));
    }

    @Step("Вводим: Годен до")
    public ShelfLifePage enterExpirationDate(Item item) {
        expirationDateInput.sendKeys(item.getExpDate());
        return new ShelfLifePage(driver, item);
    }

    @Step("Вводим Дату производства товара")
    public ShelfLifePage enterCreationDate(Item item) {
        creationDateInput.sendKeys(item.getCreationDate());
        return new ShelfLifePage(driver, item);
    }

    @Step("Нажимаем чекбокс: Есть только дата окончания срока годности")
    public ShelfLifePage clickExpCheckBox() {
        expCheckBox.click();
        return new ShelfLifePage(driver, item);
    }

    @Step("Указываем продолжительность срока годности в месяцах")
    public ShelfLifePage enterDurationInMonths(String quantity) {
        toExpireDaysInput.sendKeys(quantity);
        durationSelector.click();
        durationMonths.click();
        return new ShelfLifePage(driver, item);
    }

    @Step("Проверяем срок годности товара в днях, ожидаемый результат: {expectedDaysQuantity}")
    public ShelfLifePage checkToExpireDaysQuantity(String expectedDaysQuantity) {
        String actualDaysQuantity = toExpireDaysInput.getAttribute("value");
        assertTrue(StringUtils.isNotBlank(actualDaysQuantity), "Days quantity should not be blank");
        //проверяем, что разница между ожидаемым и фактическим количеством не больше 1 дня
        assertTrue(Math.abs(parseInt(expectedDaysQuantity) - parseInt(actualDaysQuantity)) <= 1);
        return new ShelfLifePage(driver, item);
    }

    @Step("Нажимаем кнопку: Далее")
    public BarcodeInputPage confirm() {
        forward.click();
        return new BarcodeInputPage(driver);
    }

    @Step("Нажимаем кнопку: Далее")
    public void confirmAnomaly() {
        forward.click();
        new ModalWindow(driver).clickForward();
    }
}
