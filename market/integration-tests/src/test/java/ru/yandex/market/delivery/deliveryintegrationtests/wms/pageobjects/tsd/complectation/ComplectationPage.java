package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.complectation;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class ComplectationPage extends AbstractTsdPage {
    private String itemSerialFieldXpath = "//input[@id = 'altsku_0']";

    @Name("Ячейка")
    @FindBy(xpath="//input[@id = 'order4_0_VAL']")
    private HtmlElement cellField;

    @Name("Подтверждение ячейки")
    @FindBy(xpath="//input[@id = 'order4_0']")
    private HtmlElement cellConfirmfield;

    @Name("Номер партии")
    @FindBy(xpath="//input[@id = 'lottable10_0']")
    private HtmlElement batchNumberField;

    @Name("Поле ввода УИТ")
    @FindBy(xpath="//input[@id = 'altsku_0']")
    private HtmlElement itemSerialField;

    public ComplectationPage(WebDriver driver) {
        super(driver);
    }

    @Step("Проверяем, нужно ли вводить подтверждение ячейки")
    public boolean needConfirmCell() {
        return cellConfirmfield.getAttribute("value").isEmpty();
    }

    @Step("Проверяем, что мы на странице отбора")
    public boolean isDisplayed() {
        return driver.findElements(By.xpath("//label[@id = 'TPKASS2_hdr_A' and text() = 'Отбор']")).size() != 0;
    }

    @Step("Подтверждаем ячейку")
    public void confirmCell() {
        cellConfirmfield.sendKeys(cellField.getAttribute("value"));
        cellConfirmfield.sendKeys(Keys.ENTER);
    }

    @Step("Получаем номер ячейки отбора")
    public String getCell() {
        return cellField.getAttribute("value");
    }

    @Step("Получаем маску партии")
    public String getBatch() {
        return "%" + batchNumberField.getAttribute("value");
    }

    @Step("Вводим УИТ товара")
    public void inputItemSerial(String serial) {
        itemSerialField.sendKeys(serial);
        itemSerialField.sendKeys(Keys.ENTER);
        waitSpinner();
    }
}
