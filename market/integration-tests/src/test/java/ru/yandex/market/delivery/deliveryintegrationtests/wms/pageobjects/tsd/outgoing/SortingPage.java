package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common.YesNoDialog;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class SortingPage extends AbstractTsdPage {

    @FindBy(xpath = "//input[@id = 'sortloc_0']")
    private HtmlElement idInput;

    public SortingPage(WebDriver driver) {
        super(driver);
    }

    @Step("Введите сорт. станцию")
    public CartPage submitStation(String station) {
        idInput.sendKeys(station);
        idInput.sendKeys(Keys.ENTER);
        return new CartPage(driver);
    }

    public static class CartPage extends AbstractTsdPage {

        @FindBy(xpath = "//input[@id = 'id_0']")
        private HtmlElement idInput;

        public CartPage(WebDriver driver) {
            super(driver);
        }

        public SkuPage submitCart(String cart){
            idInput.sendKeys(cart);
            idInput.sendKeys(Keys.ENTER);
            driver.findElement(By.xpath("//input[@id = 'sortloc_1']")).sendKeys(Keys.ENTER);
            return new SkuPage(driver);
        }
    }

    public static class SkuPage extends AbstractTsdPage {

        @FindBy(xpath = "//input[@id = 'SKU_0']")
        private HtmlElement skuInput;

        public SkuPage(WebDriver driver) {
            super(driver);
        }

        public CellPage submitItems(String item) {
                skuInput.sendKeys(item);
                skuInput.sendKeys(Keys.ENTER);
            return new CellPage(driver);
        }
    }

    public static class CellPage extends AbstractTsdPage {
        YesNoDialog yesNoDialog = new YesNoDialog(driver);

        @FindBy(xpath = "//input[@id = 'Confirm_0']")
        private HtmlElement cellInput;

        public CellPage(WebDriver driver) {
            super(driver);
        }

        @Step("Подт.")
        public void submitCell(String cell) {
            cellInput.sendKeys(cell);
            cellInput.sendKeys(Keys.ENTER);

            if (yesNoDialog.IsPresent()) {
                yesNoDialog.yes();
            }
        }
    }
}
