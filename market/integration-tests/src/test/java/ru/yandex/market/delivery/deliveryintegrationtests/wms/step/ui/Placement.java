package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import java.util.List;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement.EmptyIdScanPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement.PickingByUITPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement.PlaceUitsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation.Inbound;

public class Placement {
    private WebDriver driver;
    private static final Logger log = LoggerFactory.getLogger(Inbound.class);
    private MenuPage menuPage;
    private PickingByUITPage pickingByUit;

    public Placement(WebDriver driver) {
        this.driver = driver;
        this.menuPage = new MenuPage(driver);
    }

    @Step("Размещаем НЗН в ячейку {placementCell}")
    public void placeContainer(String containerLabel, String placementCell) {
        menuPage
                .inputPlacementPath()
                .enterContainer(containerLabel)
                .startPlacement()
                .enterCell(placementCell)
                .enterCart(containerLabel)
                .finishPlacement();
    }

    @Step("Размещаем УИТ в ячейку {placementCell}")
    public void placeUIT(String containerLabel, String placementCell, String id) {
        menuPage
                .inputPlacementPath()
                .enterContainer(containerLabel)
                .startPlacement()
                .enterCell(placementCell)
                .choosePlaceById()
                .enterId(id)
                .placeIds()
                .finishPlacement();
    }

    @Step("Набираем УИТы в пустую НЗН и перемещаем в ячейку {movingCell}")
    public void moveUIT(String emptyContainer, String movingCell, List<String> UITs) {
        menuPage.
                inputMovementPath()
                .enterEmptyContainer(emptyContainer);
        for (int i = 0; i < UITs.size(); i++){
            new PickingByUITPage(driver)
                    .enterUITToMove(UITs.get(i));
        }
        new PickingByUITPage(driver)
                .startPlacement()
                .enterCell(movingCell)
                .enterCart(emptyContainer)
                .finishOptimizationPlacement();
    }

    public EmptyIdScanPage goToMovementMenu() {
        return menuPage.inputMovementPath();
    }

    @Step("Перемещаем НЗН в ячейку")
    public void moveContainer(String container, String movingCell) {
        menuPage.
                inputMovementPath()
                .changePickingMode()
                .enterIdToMove(container)
                .startPlacement()
                .enterCell(movingCell)
                .enterCart(container)
                .finishOptimizationPlacement();
    }

    @Step("Набираем УИТы в пустую НЗН и перемещаем в ячейку поуитно")
    public void moveUITByPieces(String emptyContainer, String movingCell, List<String> UITs) {
        menuPage.
                inputMovementPath()
                .enterEmptyContainer(emptyContainer);
        for (int i = 0; i < UITs.size(); i++){
            new PickingByUITPage(driver)
                    .enterUITToMove(UITs.get(i));
        }
        new PickingByUITPage(driver)
                .startPlacement()
                .enterCell(movingCell)
                .choosePlaceByUIT();
        for (int i = 0; i < UITs.size(); i++){
            new PlaceUitsPage(driver)
                    .enterId(UITs.get(i));
        }
        new PlaceUitsPage(driver).placeIds()
                .finishOptimizationPlacement();
    }
}
