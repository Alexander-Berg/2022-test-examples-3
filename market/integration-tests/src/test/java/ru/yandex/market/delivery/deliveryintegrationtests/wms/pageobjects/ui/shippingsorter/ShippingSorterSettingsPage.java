package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shippingsorter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class ShippingSorterSettingsPage extends AbstractPage {

    @FindBy(xpath = "(//tr[@data-e2e='tableRow'])[2]/td[4]/button")
    private SelenideElement editButton;

    @FindBy(xpath = "//input[@placeholder='dd.mm.yyyy']")
    private SelenideElement pdoDateInput;

    @FindBy(xpath = "//button/span[contains(text(), 'Сохранить')]/..")
    private SelenideElement saveButton;

    public ShippingSorterSettingsPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    @Step("Обновляем ПДО")
    public void setTomorrowDatePdo() {
        editButton.click();
        pdoDateInput.clear();

        DateTimeFormatter from = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        DateTimeFormatter to = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String dateTime = LocalDate.parse(DateUtil.tomorrowDateTime(), from).format(to);
        pdoDateInput.sendKeys(dateTime);
        saveButton.click();
        wait.until(elementToBeClickable(editButton));
    }

    protected String getUrl() {
        return "sorterExitSettingsPage";
    }
}
