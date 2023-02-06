package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.sorting;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common.AcceptDialog;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class CartSelectPage extends AbstractTsdPage {

    private static final String SCREEN_ID = "BAP01LT";

    private AcceptDialog acceptDialog = new AcceptDialog(driver);

    @Name("Номер тележки")
    @FindBy(id="ID_0")
    private HtmlElement cartInputField;

    public CartSelectPage(WebDriver driver) {
        super(driver, SCREEN_ID);
    }

    @Step("Вводим номер тележки")
    public SortingInfoPage enterCartId (String cartId) {
        assertScreenIsOpen();
        cartInputField.sendKeys(cartId);
        cartInputField.sendKeys(Keys.ENTER);

        return new SortingInfoPage(driver);
    }
}
