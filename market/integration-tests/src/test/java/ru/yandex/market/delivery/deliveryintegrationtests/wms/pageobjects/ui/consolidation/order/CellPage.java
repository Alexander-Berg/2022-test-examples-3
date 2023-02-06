package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.consolidation.order;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import com.codeborne.selenide.SelenideElement;

public class CellPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement isLastItemInCartYesBtn;

    public CellPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "cellPutwall";
    }

    @Step("Сканируем ячейку путвола")
    public AbstractInputPage enterCell(String cell, boolean isLast) {
        super.performInput(cell);
        if (isLast) {
            isLastItemInCartYesBtn.click();
            return new SortStationPage(driver);
        } else {
            return new UitPage(driver);
        }
    }
}
