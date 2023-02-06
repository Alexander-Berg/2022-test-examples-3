package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.qatools.htmlelements.annotations.Name;

public class GeneratedEffectivenessOfOperationsReport extends AbstractGeneratedReport {

    @Name("Дата/время начала отчета")
    String startDateXpath = "//div[text()='Дата/время начала']/..//following-sibling::td[1]/div";

    @Name("Дата/время окончания отчета")
    String endDateXpath = "//div[text()='Дата/время окончания']/..//following-sibling::td[1]/div";

    @Name("Данные из отчета эффективности")
    String effectivenessReportDataXpath = "//tbody//tr[@class='style_19']/td[2]";

    public GeneratedEffectivenessOfOperationsReport(WebDriver driver) {
        super(driver);
    }

    @Step("Получаем дату/время начала отчета")
    public String getStartDate() {
        return getElementTextInOpenedReport(startDateXpath);
    }

    @Step("Получаем дату/время окончания отчета")
    public String getEndDate() {
        return getElementTextInOpenedReport(endDateXpath);
    }

    @Step("Получаем данные из отчета эффективности операций")
    public String getEffectivenessReportData() {
        return getElementTextInOpenedReport(effectivenessReportDataXpath);
    }

}
