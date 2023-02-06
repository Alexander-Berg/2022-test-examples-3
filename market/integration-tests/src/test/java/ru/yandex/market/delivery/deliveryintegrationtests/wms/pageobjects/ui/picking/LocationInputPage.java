package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking;

import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import com.codeborne.selenide.SelenideElement;

public class LocationInputPage extends AbstractInputPage {

    @Name("Ячейка")
    @FindBy(xpath = "//span[@data-e2e='loc-text']")
    private SelenideElement cell;


    public LocationInputPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим ячейку отбора")
    public MoveItemPage enterCellId() {
        String cellId = StringUtils.substringAfter(cell.getText(), "Ячейка:").trim();
        super.performInput(cellId);
        return new MoveItemPage(driver);
    }

    @Override
    protected String getUrl() {
        return "locationInputPage$";
    }
}
