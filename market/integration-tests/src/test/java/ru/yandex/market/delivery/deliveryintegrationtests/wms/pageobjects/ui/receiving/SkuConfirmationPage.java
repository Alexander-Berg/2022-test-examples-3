package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import com.codeborne.selenide.ex.ElementNotFound;
import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.RegistryItemWrapper;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class SkuConfirmationPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='text-field']//input")
    private SelenideElement input;

    @FindBy(xpath = "//input[@name='numOfBoxes']")
    private SelenideElement numOfBoxesInput;

    @FindBy(xpath = "//input[@name='quantityPerBox']")
    private SelenideElement quantityPerBoxInput;

    @FindBy(xpath = "//span[starts-with(.,'Владелец')]")
    private SelenideElement storerElement;

    @FindBy(xpath = "//span[starts-with(.,'SKU')]")
    private SelenideElement skuElement;

    @FindBy(xpath = "//span[starts-with(.,'Контроль СГ')]")
    private SelenideElement shelfLifeElement;

    @FindBy(xpath = "//span[starts-with(.,'Описание на Маркете')]")
    private SelenideElement nameElement;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    @FindBy(xpath = "//button[@data-e2e='anomaly']")
    private SelenideElement anomalyButton;

    @FindBy(xpath = "//button[@data-e2e='show_information_button']")
    private SelenideElement showDataButton;

    public SkuConfirmationPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("skuConfirmation$"));
    }

    @Step("Подтверждаем")
    public void confirm(Item item) {
        if (item.getCheckImei() > 0 || item.getCheckSn() > 0 || item.getCheckCis() > 0) {
            forward.click();
            new IdentityInputPage(driver).enterIdentities(item);
        } else {
            enterQuantity(item.getQuantity());
        }
    }

    @Step("Подтверждаем")
    public CartInputPage confirm(RegistryItemWrapper item) {
        if (item.isCheckImei() || item.isCheckSn() || item.isCheckCis()) {
            forward.click();
            new IdentityInputPage(driver).enterIdentities(item);
        } else {
            enterQuantity(item.getQuantity());
        }
        return new CartInputPage(driver);
    }

    @Step("Подтверждаем")
    public void confirmNoQty(Item item) {
        if (item.getCheckImei() > 0 || item.getCheckSn() > 0 || item.getCheckCis() > 0) {
            forward.click();
            new IdentityInputPage(driver).enterIdentities(item);
        } else {
            forward.click();
        }
    }

    @Step("Подтверждаем")
    public void confirmNoQty(RegistryItemWrapper item) {
        if (item.isCheckImei() || item.isCheckSn() || item.isCheckCis()) {
            forward.click();
            new IdentityInputPage(driver).enterIdentities(item);
        } else {
            forward.click();
        }
    }

    @Step("Подтверждаем товар без доп проверок")
    public void confirmWithoutChecks() {
            forward.click();
    }

    @Step("Подтверждаем что товар отличается от заявленного")
    public MismatchItemPage clickAnomalyButton() {
        anomalyButton.click();
        return new MismatchItemPage(driver);
    }

    @Step("Проверяем информацию о товаре")
    public SkuConfirmationPage checkInfo(Item item) {
        try{
            checkStorer(item.getVendorId());
            checkSku();
            checkShelfLife(item.isShelfLife());
            checkName(item.getName());
        }
        catch (ElementNotFound ex){
            showDataButton.click();
            checkStorer(item.getVendorId());
            checkSku();
            checkShelfLife(item.isShelfLife());
            checkName(item.getName());
        }
        return this;
    }

    @Step("Проверяем информацию о товаре")
    public SkuConfirmationPage checkInfo(RegistryItemWrapper item) {
        try{
            checkStorer(item.getVendorId());
            checkSku();
            checkShelfLife(item.isShelfLife());
            checkName(item.getName());
        }
        catch (ElementNotFound ex){
            showDataButton.click();
            checkStorer(item.getVendorId());
            checkSku();
            checkShelfLife(item.isShelfLife());
            checkName(item.getName());
        }
        return this;
    }

    @Step("Проверяем Storer")
    private void checkStorer(long vendorId) {
        String text = StringUtils.substringAfter(storerElement.getText(), "Владелец:").trim();
        assertTrue(StringUtils.isNotBlank(text), "Storer should not be blank");
        assertTrue(StringUtils.isNumeric(text), "Storer should be numeric");
        assertEquals(vendorId, Long.parseLong(text));
    }

    @Step("Проверяем SKU")
    private void checkSku() {
        String text = StringUtils.substringAfter(skuElement.getText(), "SKU:").trim();
        assertTrue(StringUtils.isNotBlank(text), "SKU should not be blank");
        assertTrue(text.startsWith("ROV"), "SKU should start with ROV");
    }

    @Step("Проверяем контроль срока годности")
    private void checkShelfLife(boolean isShelfLife) {
        String text = StringUtils.substringAfter(shelfLifeElement.getText(), "Контроль СГ:").trim();
        assertTrue(StringUtils.isNotBlank(text), "ShelfLife should not be blank");
        if (isShelfLife) {
            assertEquals("Да", text);
        } else {
            assertEquals("Нет", text);
        }
    }

    @Step("Проверяем название")
    private SkuConfirmationPage checkName(String name) {
        String text = StringUtils.substringAfter(nameElement.getText(), "Описание на Маркете:").trim();
        assertTrue(StringUtils.isNotBlank(text), "Name should not be blank");
        if (name != null) {
            assertEquals(name, text);
        }
        return this;
    }

    @Step("Вводим количество принимаемого товара: {quantity}")
    public SkuConfirmationPage enterQuantity(Integer quantity) {
        input.sendKeys(String.valueOf(quantity));
        input.pressEnter();
        return this;
    }

    @Step("Вводим количество коробов: {quantity}")
    public SkuConfirmationPage enterNumOfBoxes(Integer quantity) {
        numOfBoxesInput.sendKeys(String.valueOf(quantity));
        numOfBoxesInput.pressEnter();
        return this;
    }

    @Step("Вводим количество штук в коробке: {quantity}")
    public SkuConfirmationPage enterQuantityPerBox(Integer quantity) {
        quantityPerBoxInput.sendKeys(String.valueOf(quantity));
        quantityPerBoxInput.pressEnter();
        return this;
    }

    @Step("Нажимаем кнопку Аномалия")
    public AnomalyPage clickAnomaly() {
        anomalyButton.click();
        return new AnomalyPage(driver);
    }
}
