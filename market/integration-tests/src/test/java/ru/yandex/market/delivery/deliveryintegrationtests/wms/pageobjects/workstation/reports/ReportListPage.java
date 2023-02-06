package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class ReportListPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(ReportListPage.class);

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$6m6cos_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле фильтрации по заголовку отчета")
    @FindBy(xpath = "//input[@id = 'Ij117ys']")
    private HtmlElement reportTitleField;

    @Name("Иконка перехода к первому отчету в списке")
    @FindBy(xpath = "//input[@id = '$6m6cos_cell_0_0_Img']")
    private HtmlElement firstReportIcon;

    @Name("Выпадающее меню категорий")
    @FindBy(xpath = "//div[@id = 'multiselectparent_Jhkb9de']/a/div/img")
    private HtmlElement categoryMenu;

    @Name("Иконка поиска")
    @FindBy(xpath = "//tr[@class = 'listfilter']/td/input")
    private HtmlElement searchIcon;

    public ReportListPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим {0} в поле фильтрации по заголовку отчета")
    public void inputReportTitle(String inboundId) {
        reportTitleField.sendKeys(inboundId);
        overlayBusy.waitUntilHidden();
    }

    @Step("Жмем кнопку фильтрации")
    public void filterButtonClick() {
        filterButton.click();
        overlayBusy.waitUntilHidden();
    }

    @Step("Выбираем первый элемент в таблице")
    public void openFirstReport() {
        firstReportIcon.click();
        overlayBusy.waitUntilHidden();
    }

    @Step("Выбираем категорию отчета")
    public void chooseReportCategory(String category) {
        String categoryXpath = String.format("//li[@class = 'epnyMultiSelectOption']/label[contains(., '%s')]", category);
        String dropDownMenuXpath = "//div[@class = 'ms-drop bottom']";

        categoryMenu.click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(dropDownMenuXpath)));
        driver.findElement(By.xpath(categoryXpath)).click();
        searchIcon.click();
        overlayBusy.waitUntilHidden();
    }

    @Step("Открываем отчет по названию")
    public void openReportByName(String reportName) {
        String openButtonXpath = String.format("//tbody/tr/td/span[text()='%s']/..//preceding-sibling::td[1]/input", reportName);
        driver.findElement(By.xpath(openButtonXpath)).click();
        overlayBusy.waitUntilHidden();
    }
}
