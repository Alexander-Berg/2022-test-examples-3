package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.move.putsn;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common.AcceptDialog;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class PutSnCellSelectPage extends AbstractTsdPage {

    private AcceptDialog acceptDialog = new AcceptDialog(driver);

    @FindBy(xpath = "//input[@type='text' and @id='TOLOC_0']")
    private HtmlElement cartInputField;

    public PutSnCellSelectPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим номер ячейки")
    public void enterCellId (String cellId) {
        cartInputField.sendKeys(cellId);
        cartInputField.sendKeys(Keys.ENTER);
    }

    @Step("Подтверждаем выбор ячейки")
    public void acceptSelectedCell() {
        acceptDialog.accept();
    }
}
