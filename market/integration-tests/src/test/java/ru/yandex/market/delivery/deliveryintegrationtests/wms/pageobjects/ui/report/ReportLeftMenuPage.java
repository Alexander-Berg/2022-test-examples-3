package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report;

import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import java.io.File;
import java.io.IOException;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class ReportLeftMenuPage extends AbstractPage {

    @FindBy(xpath = "//*[@data-e2e='Домашняя страница']")
    private SelenideElement homePageLink;

    @FindBy(xpath = "//*[@data-e2e='outboundsact']")
    private SelenideElement acceptanceTransferWithdrawalActLink;

    @FindBy(xpath = "//*[@data-e2e='receiptoutboundsact']")
    private SelenideElement receiptOutboundActLink;

    @FindBy(xpath = "//*[@data-e2e='waste_notice']")
    private SelenideElement receiptWasteNoticeActLink;

    @FindBy(xpath = "//*[@data-e2e='opsexeceffectiveness']")
    private SelenideElement openOpsExecEffectivenessLink;

    @FindBy(xpath = "//*[@data-e2e='opsexeceffectiveness_grouped']")
    private SelenideElement openOpsExecEffectivenessOptimizedLink;

    @FindBy(xpath = "//*[@data-e2e='transfertoutilizeract']")
    private SelenideElement opentransferUtilizedLink;

    @FindBy(xpath = "//*[@data-e2e='waste']")
    private SelenideElement wasteActLink;

    public ReportLeftMenuPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    protected String getUrl() {
        return "reportsPage";
    }

    @Attachment(value = "Page screenshot", type = "image/png")
    public byte[] saveScreenshot(byte[] screenShot) {
        return screenShot;
    }

    public OutboundsActPage openAcceptanceTransferWithdrawalAct() {
        homePageLink.click();
        acceptanceTransferWithdrawalActLink.click();
        return new OutboundsActPage(driver);
    }

    public ReceiptOutboundsActPage openReceiptOutboundAct() {
        homePageLink.click();
        receiptOutboundActLink.click();
        return new ReceiptOutboundsActPage(driver);
    }

    public WasteActPage openWasteAct() {
        homePageLink.click();
        wasteActLink.click();
        return new WasteActPage(driver);
    }

    @Step("Открываем Уведомление о готовности имущества к отгрузке")
    public WasteNoticeActPage openWasteNoticeAct() {
        homePageLink.click();
        receiptWasteNoticeActLink.click();
        return new WasteNoticeActPage(driver);
    }

    public OpsexecEffectivenessActPage openOpsExecEffectiveness() {
        homePageLink.click();
        openOpsExecEffectivenessLink.click();
        return new OpsexecEffectivenessActPage(driver);
    }

    public OpsexecEffectivenessOptimizedActPage opsexecEffectivenessOptimizedActPage() {
        homePageLink.click();
        openOpsExecEffectivenessOptimizedLink.click();
        return new OpsexecEffectivenessOptimizedActPage(driver);
    }

    public TransferUtilActPage openTransferUtilActPage() {
        homePageLink.click();
        opentransferUtilizedLink.click();
        return new TransferUtilActPage(driver);
    }

}
