package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.numberOfWindowsToBe;

public class GeneratedParcelLabelReport extends AbstractGeneratedReport {

    @Name("Штрих-код")
    @FindBy(xpath = "//div/img[@class = 'style_9']")
    private HtmlElement barcode;

    @Name("Номер посылки")
    protected String parcelNumberXpath = "//div[@class='style_6']";

    public GeneratedParcelLabelReport(WebDriver driver) {
        super(driver);
    }

    @Step("Получаем номер посылки")
    public String getParcelNumber() {
        return getElementTextInOpenedReport(parcelNumberXpath);
    }

    @Step("Получаем html-элемент содержащий штрих-код")
    public HtmlElement getBarcode() { return barcode; }
}
