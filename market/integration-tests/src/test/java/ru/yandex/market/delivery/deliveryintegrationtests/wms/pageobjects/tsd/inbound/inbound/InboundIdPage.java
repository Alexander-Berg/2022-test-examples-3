package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class InboundIdPage extends AbstractTsdPage {

    @FindBy(id="doc_0")
    private HtmlElement inboundIdField;

    public InboundIdPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим номер ПУО-приемки")
    public void enterInboundId (String inboundId) {
        inboundIdField.sendKeys(inboundId);
        inboundIdField.sendKeys(Keys.ENTER);
    }
}
