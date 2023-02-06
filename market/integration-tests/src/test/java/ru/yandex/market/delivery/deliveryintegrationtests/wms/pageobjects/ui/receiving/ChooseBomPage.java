package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ItemContext;
import com.codeborne.selenide.SelenideElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class ChooseBomPage extends ItemContext {

    @FindBy(xpath = "//span[starts-with(.,'Владелец')]")
    private SelenideElement storerElement;

    @FindBy(xpath = "//span[starts-with(.,'SKU')]")
    private SelenideElement skuElement;

    @FindBy(xpath = "//span[starts-with(.,'Контроль СГ')]")
    private SelenideElement shelfLifeElement;

    @FindBy(xpath = "//span[starts-with(.,'Описание')]")
    private SelenideElement nameElement;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement createNewBom;

    public ChooseBomPage(WebDriver driver, Item item) {
        super(driver, item);
        wait.until(urlMatches("chooseBomPage$"));
    }

    @Step("Нажимаем кнопку Создать новую часть товара")
    public void createNewBom() {
        createNewBom.click();
    }

    @Step("Проверяем информацию о товаре")
    public ChooseBomPage checkInfo() {
        checkStorer(item.getVendorId());
        checkSku();
        checkShelfLife(item.isShelfLife());
        checkName(item.getName());
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
    private void checkName(String name) {
        String text = StringUtils.substringAfter(nameElement.getText(), "Описание на Маркете:").trim();
        assertTrue(StringUtils.isNotBlank(text), "Name should not be blank");
        if (name != null) {
            assertEquals(name, text);
        }
    }

}
