package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Delayer;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import java.util.concurrent.TimeUnit;

public class MeasuringPage extends AbstractTsdPage {

    @FindBy(id="length_0")
    private HtmlElement lengthInput;
    @FindBy(id="width_0")
    private HtmlElement widthInput;
    @FindBy(id="height_0")
    private HtmlElement heightInput;
    @FindBy(id="weight_0")
    private HtmlElement weightInput;

    public MeasuringPage(WebDriver driver) {
        super(driver);
    }

    @Step("Заполняем ВГХ товара")
    public void enterKorobyts (String length, String width, String height, String weight) {
        lengthInput.sendKeys(length);
        widthInput.sendKeys(width);
        heightInput.sendKeys(height);
        weightInput.sendKeys(weight);
        weightInput.sendKeys(Keys.ENTER);
        Delayer.delay(5, TimeUnit.SECONDS); // без этого ожидания ВГХ не успевают сохраниться

    }
}
