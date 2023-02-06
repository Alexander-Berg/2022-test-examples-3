package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.numberOfWindowsToBe;

public class GeneratedLpnLabelReport extends AbstractGeneratedReport {

    @Name("Штрих-код")
    @FindBy(xpath = "//div/img[@class = 'style_8']")
    private HtmlElement barcode;

    @Name("Номер LPN")
    String lpnNumberXpath = "//div[@class='style_5']";

    public GeneratedLpnLabelReport(WebDriver driver) {
        super(driver);
    }

    @Step("Получаем номер LPN")
    public String getLpnNumber() {
        return getElementTextInOpenedReport(lpnNumberXpath);
    }

    @Step("Получаем html-элемент содержащий штрих-код")
    public HtmlElement getBarcode() { return barcode; }
}
