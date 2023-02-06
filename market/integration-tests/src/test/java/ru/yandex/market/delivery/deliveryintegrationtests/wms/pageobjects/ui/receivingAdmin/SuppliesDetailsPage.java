package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receivingAdmin;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import static com.codeborne.selenide.Selectors.byXpath;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import ru.yandex.qatools.htmlelements.annotations.Name;
import com.codeborne.selenide.SelenideElement;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

@Slf4j
public class SuppliesDetailsPage extends AbstractPage {
    public SuppliesDetailsPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("suppliesDetailsSummary"));
    }

    @Name("Кнопка открытия меню действий")
    @FindBy(xpath = "//button[@data-e2e='more_actions_button_row_0']")
    private SelenideElement moreActionsButton;

    @Name("Меню дополнительных действий")
    @FindBy(xpath = "//button[@data-e2e='open_asn_receiving_row_0']/..")
    private SelenideElement moreActionsMenu;

    @Name("Переход на вкладку \"Сводка приёмки\"")
    @FindBy(xpath = "//*[@data-e2e='suppliesDetailsSummaryTab']")
    private SelenideElement suppliesDetailsSummaryTab;

    @Name("Переход на вкладку \"Детали поставки\"")
    @FindBy(xpath = "//*[@data-e2e='suppliesDetailsListTab']")
    private SelenideElement suppliesDetailsListTab;

    @Name("Переход на вкладку \"История статусов поставки\"")
    @FindBy(xpath = "//*[@data-e2e='suppliesDetailsStatusHistoryTab']")
    private SelenideElement suppliesDetailsStatusHistoryTab;

    @Name("Переход на вкладку \"Информация о ПУО-приёмке\"")
    @FindBy(xpath = "//*[@data-e2e='suppliesDetailsInformationTab']")
    private SelenideElement suppliesDetailsInformationTab;

    @Name("Переход на вкладку \"Детали потарной приёмки\"")
    @FindBy(xpath = "//*[@data-e2e='palletReceivingDetailsTab']")
    private SelenideElement palletReceivingDetailsTab;

    @Name("Номер паллеты")
    @FindBy(xpath = "//td[@data-e2e='toid_cell_row_0']")
    private SelenideElement palletId;

    @Name("Первый результат поиска")
    @FindBy(xpath = "//tr[@data-e2e='row_row_0']")
    private SelenideElement firstResultRow;

    @Name("Ссывлка на скачивание отчета в PDF")
    @FindBy(xpath = "//a[@data-e2e='download_receiving_report_discrepancies_excel_row_0']")
    private SelenideElement pdfReportDownloadLink;

    @Name("Кнопка \"Закрыть Первичную приемку приёмку\"")
    @FindBy(xpath = "//button[@data-e2e='close_initial_receiving_row_0']")
    private SelenideElement closeInitialReceiptButton;

    @Step("Переходим на вкладку \"Детали потарной приёмки\"")
    public SuppliesDetailsPage switchToPalletReceivingDetailsTab() {
        palletReceivingDetailsTab.click();
        firstResultRow.shouldBe(visible);

        return this;
    }

    @Step("Переходим на вкладку  \"Информация о ПУО-приёмке\"")
    public SuppliesDetailsPage switchToSuppliesDetailsInformationTab() {
        suppliesDetailsInformationTab.click();
        firstResultRow.shouldBe(visible);

        return this;
    }

    @Step("Получаем айди паллеты и закрываем вкладку")
    public String selectPalletAndCloseTab() {
        String pallet = palletId.getText();
        log.info("Pallet {}", pallet);

        driver.close();
        switchToMainWindow();

        return pallet;
    }

    @Step("Открываем отчет о расхождениях первичной приемки")
    public DiscrepanciesReport openInitialReceivingDiscrepanciesReport(String fulfillmentId) {
        log.info("Opening HTML discrepancies report for inbound {}", fulfillmentId);
        final By reportTitle = byXpath(String.format(
                "//span[contains(text(),'АКТ ОБ УСТАНОВЛЕННОМ РАСХОЖДЕНИИ К АКТУ № %s')]",
                fulfillmentId
        ));
        String pdfReportLink = pdfReportDownloadLink.getAttribute("href");
        open(pdfReportLink.replace("PDF", "HTML"));
        $(reportTitle).shouldBe(visible);

        return new DiscrepanciesReport(driver);
    }

    @Step("Открываем меню действий")
    public SuppliesDetailsPage clickMoreActionsButton() {
        moreActionsButton.click();
        moreActionsMenu.shouldBe(visible);
        return this;
    }

    @Step("Нажимаем кнопку \"Закрыть первичную приемку\"")
    public SuppliesDetailsPage clickCloseInitialReceiptButton() {
        closeInitialReceiptButton.click();
        new ModalWindow(driver).clickSubmit();
        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Первичная приёмка завершена"),
                "Не появился диалог с заголовком \"Первичная приёмка завершена\"");
        notificationDialog.waitUntilHidden();
        return this;
    }
}
