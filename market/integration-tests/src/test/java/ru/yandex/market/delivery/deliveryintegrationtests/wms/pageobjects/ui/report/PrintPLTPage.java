package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report;

import static com.codeborne.selenide.Selectors.byXpath;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class PrintPLTPage extends AbstractPage {

    @FindBy(xpath = "//span[@data-e2e='notification__title']")
    private SelenideElement notificationTitleSend;

    @FindBy(xpath = "//div[@data-e2e='printer']//input")
    private SelenideElement printerField;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement printButton;

    @FindBy(xpath = "//div[@data-e2e='parcelOrUit']//input")
    private SelenideElement packageField;

    public PrintPLTPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    protected String getUrl() {
        return "printStickerPage/parcelLabel";
    }

    public void printPackage(String packageNumber, String printer) {
        final By selectPrinter = byXpath(String.format("//button[@data-e2e='printer-selectAll_%s']", printer));
        printerField.click();
        $(selectPrinter).click();
        packageField.sendKeys(packageNumber);
        printButton.click();
    }

    public String getNotificationTitleText() {
        return notificationTitleSend.getText();
    }


}
