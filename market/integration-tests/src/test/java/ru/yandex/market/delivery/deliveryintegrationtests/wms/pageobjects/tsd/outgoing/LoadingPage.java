package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common.AcceptDialog;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class LoadingPage extends AbstractTsdPage {

    private AcceptDialog acceptDialog = new AcceptDialog(driver);

    @Name("Сканируйте Паллет (DRP)")
    @FindBy(xpath = "//input[@id = 'fromid_0']")
    private HtmlElement dropIdInput;

    @Name("Введите номер Отгрузки)")
    @FindBy(xpath = "//input[@id = 'toid_0']")
    private HtmlElement outId;

    @Name("Введите номер Ячейки)")
    @FindBy(xpath = "//input[@id = 'toloc_0']")
    private HtmlElement locId;

    public LoadingPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим номер дропа")
    public LoadingPage inputDropId(String dropId) {
        dropIdInput.sendKeys(dropId);

        return this;
    }

    @Step("Вводим номер отгрузки (машины)")
    public LoadingPage inputCarId(String carId) {

        outId.sendKeys(carId);
        outId.sendKeys(Keys.ENTER);

        return this;
    }

    @Step("Вводим номер ячейки")
    public AcceptDialog inputCellId(String cellId) {
        locId.sendKeys(cellId);
        locId.sendKeys(Keys.ENTER);

        return acceptDialog;
    }
}
