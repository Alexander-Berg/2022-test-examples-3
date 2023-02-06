package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class PackingCellPage extends AbstractTsdPage {

    @Name("Сканируйте Посылку")
    @FindBy(xpath = "//input[@id = 'fromid_0']")
    private HtmlElement parcelInput;

    @Name("Сканируйте Паллет (DRP)")
    @FindBy(xpath = "//input[@id = 'toid_0']")
    private HtmlElement dropIdInput;

    @Name("Ячейка")
    @FindBy(xpath = "//input[@id = 'toloc_0']")
    private HtmlElement cellInput;

    public PackingCellPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим Ячейку упаковки")
    public void inputPackingCell(String packingCell) {
        cellInput.sendKeys(packingCell);
        cellInput.sendKeys(Keys.ENTER);
        waitSpinner();
    }

}
