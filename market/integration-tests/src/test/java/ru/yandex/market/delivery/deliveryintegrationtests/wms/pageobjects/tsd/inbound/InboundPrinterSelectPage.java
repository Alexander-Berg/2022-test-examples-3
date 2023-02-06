package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class InboundPrinterSelectPage extends AbstractTsdPage {

    @Name("Айди принтера")
    @FindBy(xpath="//input[@id='printerid_0']")
    private HtmlElement inputField;

    @Name("Яч.Приемки")
    @FindBy(xpath="//input[@id='loc_0']")
    private HtmlElement inboundCell;

    public InboundPrinterSelectPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим айди принтера")
    public void selectPrinter(String printerName) {
        inputField.sendKeys(printerName);
        inputField.sendKeys(Keys.ENTER);
    }

    @Step("Вводим ячейку приемки")
    public void selectInboundCell(String cell) {
        inboundCell.sendKeys(cell);
        inboundCell.sendKeys(Keys.ENTER);
    }
}
