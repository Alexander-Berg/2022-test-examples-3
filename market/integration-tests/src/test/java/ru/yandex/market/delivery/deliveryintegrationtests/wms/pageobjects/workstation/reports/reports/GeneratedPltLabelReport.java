package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class GeneratedPltLabelReport extends AbstractGeneratedReport {

    @Name("Штрих-код")
    @FindBy(xpath = "//div/img[@class = 'style_8']")
    private HtmlElement barcode;

    @Name("Номер PLT")
    String pltNumberXpath = "//div[@class='style_5']";

    public GeneratedPltLabelReport(WebDriver driver) {
        super(driver);
    }

    @Step("Получаем номер PLT")
    public String getPltNumber() {
        return getElementTextInOpenedReport(pltNumberXpath);
    }

    @Step("Получаем html-элемент содержащий штрих-код")
    public HtmlElement getBarcode() { return barcode; }
}
