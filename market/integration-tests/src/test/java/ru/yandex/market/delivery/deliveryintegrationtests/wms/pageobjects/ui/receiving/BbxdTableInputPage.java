package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.PrinterInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.dropping.DropMoveInputPage;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

public class BbxdTableInputPage extends AbstractInputPage {

    @FindBy(xpath = "//div[@data-e2e='additionalMenu']")
    private SelenideElement additionalMenuButton;

    @FindBy(xpath = "//button[@data-e2e='Context_bbxd_move_drop']")
    private SelenideElement moveDropButton;

    public BbxdTableInputPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "tableInput";
    }

    @Step("Вводим стол")
    public PrinterInputPage enterTable(String table) {
        performInput(table);
        return new PrinterInputPage(driver);
    }

    @Step("Открываем через три точки - Переместить дропку")
    public DropMoveInputPage assignDrop() {
        additionalMenuButton.click();
        moveDropButton.click();
        return new DropMoveInputPage(driver);
    }
}
