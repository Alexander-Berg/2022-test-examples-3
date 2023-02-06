package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class GeneratedPalletLabelReport extends AbstractGeneratedReport {

    @Name("Штрих-код")
    @FindBy(xpath = "//div/img[@class = 'style_18']")
    private HtmlElement barcode;

    @Name("Название службы доставки")
    String deliveryServiceNameXpath = "//div[@class='style_14']";

    @Name("Номер паллеты")
    String palletNumberXpath = "//div[@class='style_15']";

    public GeneratedPalletLabelReport(WebDriver driver) {
        super(driver);
    }

    @Step("Получаем название службы доставки")
    public String getDeliveryServiceName() { return getElementTextInOpenedReport(deliveryServiceNameXpath); }

    @Step("Получаем номер PLT")
    public String getPalletNumber() {
        return getElementTextInOpenedReport(palletNumberXpath);
    }

    @Step("Получаем html-элемент содержащий штрих-код")
    public HtmlElement getBarcode() { return barcode; }

}
