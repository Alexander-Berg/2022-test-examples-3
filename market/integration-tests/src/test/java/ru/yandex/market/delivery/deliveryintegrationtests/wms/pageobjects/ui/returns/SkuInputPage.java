package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.returns;

import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ItemContext;
import com.codeborne.selenide.SelenideElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class SkuInputPage extends ItemContext {

    @FindBy(xpath = "//span[starts-with(.,'ID Поставщика')]")
    private SelenideElement storerElement;

    @FindBy(xpath = "//span[starts-with(.,'Внутренний номер товара')]")
    private SelenideElement skuElement;

    @FindBy(xpath = "//span[starts-with(.,'Название товара')]")
    private SelenideElement nameElement;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    @FindBy(xpath = "//span[text() = 'Да']")
    private SelenideElement yesButton;

    public SkuInputPage(WebDriver driver, Item item) {
        super(driver, item);
        wait.until(urlMatches("skuInputPage$"));
    }

    @Step("Подтверждаем")
    public void confirm() {
        forward.click();
    }

    @Step("Есть повреждения? Нажимаем: Да")
    public SkuCheckPage confirmDamaged() {
        yesButton.click();
        return new SkuCheckPage(this.driver);
    }

    @Step("Проверяем информацию о товаре")
    public SkuInputPage checkInfo() {
        checkName(item.getName());
        checkStorer(item.getVendorId());
        checkSku();
        return this;
    }

    @Step("Проверяем id Поставщика")
    private void checkStorer(long vendorId) {
        String text = StringUtils.substringAfter(storerElement.getText(), "ID Поставщика:").trim();
        assertTrue(StringUtils.isNotBlank(text), "Storer should not be blank");
        assertTrue(StringUtils.isNumeric(text), "Storer should be numeric");
        assertEquals(vendorId, Long.parseLong(text));
    }

    @Step("Проверяем SKU")
    private void checkSku() {
        String text = StringUtils.substringAfter(skuElement.getText(), "Внутренний номер товара:").trim();
        assertTrue(StringUtils.isNotBlank(text), "SKU should not be blank");
        assertTrue(text.startsWith("ROV"), "SKU should start with ROV");
    }

    @Step("Проверяем название")
    private void checkName(String name) {
        String text = StringUtils.substringAfter(nameElement.getText(), "Название товара:").trim();
        assertTrue(StringUtils.isNotBlank(text), "Name should not be blank");
        assertEquals(name, text);
    }
}
