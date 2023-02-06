package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.dimensionControl;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class UitScanPage extends AbstractInputPage {

    @FindBy(xpath = "//div[@data-e2e='additionalMenu']")
    private SelenideElement additionalMenuButton;

    @FindBy(xpath = "//button[@data-e2e='Context_exit_from_measurement']")
    private SelenideElement exitFromMeasurementButton;

    @FindBy(xpath = "//button[@data-e2e='button_forward']//span[text()='Да']")
    private SelenideElement acceptExitButton;

    public UitScanPage(WebDriver driver) {
        super(driver);
    }

    @Step("Сканируем УИТ для обмера - {uit}")
    public MeasuringPage enterUit(String uit) {
        super.performInput(uit);
        return new MeasuringPage(driver);
    }

    @Step("Завершаем обмер на мобильной станции")
    public void finishMobileMeasure() {
        additionalMenuButton.click();
        exitFromMeasurementButton.click();
        acceptExitButton.click();
    }

    @Override
    protected String getUrl() {
        return "uitScanPage$";
    }
}
