package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class ShipingPage extends AbstractTsdPage {

    @Name("НЗН")
    @FindBy(xpath = "//input[@id = 'fromid_0']")
    private HtmlElement nznInput;

    public ShipingPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим номерпосылки")
    public void shipByNzn(String nzn) {
        nznInput.sendKeys(nzn);
        nznInput.sendKeys(Keys.ENTER);
        waitSpinner();
    }
}
