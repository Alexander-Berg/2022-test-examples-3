package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.complectation;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common.AcceptDialog;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class AssignmentPage extends AbstractTsdPage {

    private AcceptDialog acceptDialog = new AcceptDialog(driver);

    @Name("Всего заказов")
    @FindBy(xpath="//input[@rname = 'totalorders']")
    private HtmlElement totalorders;

    @Name("Всего отборов")
    @FindBy(xpath="//input[@rname = 'totalpicks']")
    private HtmlElement totalpicks;

    @Name("К-ВО НЗН")
    @FindBy(xpath="//input[@rname = 'order4']")
    private HtmlElement totalNzn;

    @Name("Общий вес")
    @FindBy(xpath="//input[@rname = 'order6']")
    private HtmlElement totalWeight;

    @Name("Общий объем")
    @FindBy(xpath="//input[@rname = 'order7']")
    private HtmlElement totalVolume;

    @Name("Номер тележки")
    @FindBy(xpath="//input[@rname = 'dropid']")
    private HtmlElement cartInputField;

    public AssignmentPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим Тележку")
    public void enterCart(String cartId) {
        cartInputField.sendKeys(cartId);
        cartInputField.sendKeys(Keys.ENTER);
    }

    @Step("Подтверждаем выбор тележки")
    public void acceptSelectedCart() {
        acceptDialog.accept();
    }

    @Step("Получаем общее число отборов")
    public int getTasksNumber() {
        return Integer.valueOf(totalNzn.getAttribute("value"));
    }
}
