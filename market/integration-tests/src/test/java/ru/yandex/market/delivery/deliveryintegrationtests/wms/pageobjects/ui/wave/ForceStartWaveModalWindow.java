package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.wave;

import com.codeborne.selenide.SelenideElement;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;

public class ForceStartWaveModalWindow extends ModalWindow {

    private static final String MENU_ITEM_XPATH_TEMPLATE =
            "//div[@role='dialog']//button[@data-e2e='dynamic-select-selectAll_%s']";

    @FindBy(xpath = "//div[@role='dialog']//div[@data-e2e='dynamic-select']//input")
    private SelenideElement input;

    public ForceStartWaveModalWindow(WebDriver driver) {
        super(driver);
    }

    public void inputSortingStation(String sortingStation) {
        input.click();
        SelenideElement sortingStationMenuItem = findMenuItem(sortingStation);
        sortingStationMenuItem.click();

        Assertions.assertEquals(
                sortingStation,
                input.getAttribute("value"),
                "Сортировочная станция в диалоге принудительного запуска отличается от ожидаемой"
        );
    }

    public void inputConsolidationLine(String consolidationLine) {
        input.click();
        SelenideElement consolidationLineMenuItem = findMenuItem(consolidationLine);
        consolidationLineMenuItem.click();

        Assertions.assertEquals(
                consolidationLine,
                input.getAttribute("value"),
                "Линия консолидации в диалоге принудительного запуска отличается от ожидаемой"
        );
    }

    private SelenideElement findMenuItem(String menuItemText) {
        return $(byXpath(String.format(MENU_ITEM_XPATH_TEMPLATE, menuItemText)));
    }
}
