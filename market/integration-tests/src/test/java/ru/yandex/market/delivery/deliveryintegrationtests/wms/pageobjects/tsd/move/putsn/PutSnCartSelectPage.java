package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.move.putsn;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common.AcceptDialog;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class PutSnCartSelectPage extends AbstractTsdPage {

    private AcceptDialog acceptDialog = new AcceptDialog(driver);

    @FindBy(xpath = "//input[@type='text' and @id='fromid_0']")
    private HtmlElement cartInputField;

    public PutSnCartSelectPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим номер тележки")
    public void enterCartId (String cartId) {
        cartInputField.sendKeys(cartId);
        cartInputField.sendKeys(Keys.ENTER);
    }

    @Step("Подтверждаем выбор тележки")
    public void acceptSelectedCart() {
        acceptDialog.accept();
    }

    @Step("Проверяем, что отображается поле ввода тележки")
    public void assertCartInputFieldDisplayed() {
        Assertions.assertTrue(cartInputField.isDisplayed());
    }
}
