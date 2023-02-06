package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report;

import io.qameta.allure.Attachment;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class LabelsMenuPage extends AbstractPage {

    @FindBy(xpath = "//button[@data-e2e='parcelLabel']")
    private SelenideElement printPLTButton;

    public LabelsMenuPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    protected String getUrl() {
        return "chooseStickerPage";
    }

    @Attachment(value = "Page screenshot", type = "image/png")
    public byte[] saveScreenshot(byte[] screenShot) {
        return screenShot;
    }

    public PrintPLTPage openPrintPLTPage() {
        printPLTButton.click();
        return new PrintPLTPage(driver);
    }

}
