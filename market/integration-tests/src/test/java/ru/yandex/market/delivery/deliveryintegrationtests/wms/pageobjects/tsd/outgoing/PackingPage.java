package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class PackingPage extends AbstractTsdPage {

    @Name("Сканируйте Посылку")
    @FindBy(xpath = "//input[@id = 'fromid_0']")
    private HtmlElement parcelInput;

    @Name("Сканируйте Паллет (DRP)")
    @FindBy(xpath = "//input[@id = 'toid_0']")
    private HtmlElement dropIdInput;

    public PackingPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим номерпосылки")
    public PackingPage inputParcel(String parcelId) {
        parcelInput.sendKeys(parcelId);

        return this;
    }

    @Step("Вводим номер дропа")
    public PackingCellPage inputDropId(String dropId) {
        dropIdInput.sendKeys(dropId);
        dropIdInput.sendKeys(Keys.ENTER);

        return new PackingCellPage(driver);
    }
}
