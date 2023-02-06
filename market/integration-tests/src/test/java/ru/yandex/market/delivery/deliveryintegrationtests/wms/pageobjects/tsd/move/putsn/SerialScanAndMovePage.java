package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.move.putsn;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class SerialScanAndMovePage extends AbstractTsdPage {

    @FindBy(xpath = "//input[@type='text' and @id='SN_0']")
    private HtmlElement snInputField;

    public SerialScanAndMovePage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим серийник перемещаемого товара")
    public void enterSN (String sn) {
        snInputField.sendKeys(sn);
        snInputField.sendKeys(Keys.ENTER);

        if (!warningDialog.IsPresentWithMessage("больше нет товара")) {
            waitSpinnerIfPresent();
        }
    }
}
