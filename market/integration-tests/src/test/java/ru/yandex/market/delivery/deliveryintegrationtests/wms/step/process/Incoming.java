package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.process;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotBlank;

import io.qameta.allure.Step;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Box;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving.ContainerInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving.ReceiptInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receivingAdmin.SuppliesListPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui.UISteps;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

public class Incoming {
    private final UISteps uiSteps;

    public Incoming(UISteps uiSteps) {
        this.uiSteps = uiSteps;
    }

    public void acceptItemsAndPlaceThemToPickingCell(Item item, @NotBlank String storageCell) {
        acceptItemsAndPlaceThemToPickingCell(item, storageCell, item.getQuantity());
    }

    @Step("Создаем поставку, принимаем {quantity} товаров и размещаем в ячейку отбора {storageCell}")
    public void acceptItemsAndPlaceThemToPickingCell(Item item, @NotBlank String storageCell, int quantity) {
        String inboundCart = DatacreatorSteps.Label().createContainer(ContainerIdType.L);
        Item inboundItem = item.toBuilder().quantity(quantity).build();

        Inbound inbound = ApiSteps.Inbound().createInbound(inboundItem.getArticle());

        String palletId = placeInboundToPallet(inbound.getFulfillmentId(), quantity);
        receiveInboundItems(inboundItem, inboundCart, palletId);
        closeAndApproveCloseInbound(inbound.getFulfillmentId());
        placeContainerToCell(inboundCart, storageCell);
    }

    public void acceptItemsAndPlaceThemToPickingCell(Item item, @NotBlank String storageCell, @NotBlank String id) {
        acceptItemsAndPlaceThemToPickingCell(item, storageCell, id, item.getQuantity());
    }

    @Step("Создаем поставку, принимаем {quantity} товаров и размещаем в НЗН {id} в ячейку отбора {storageCell}")
    public void acceptItemsAndPlaceThemToPickingCell(Item item, @NotBlank String storageCell, @NotBlank String id, int quantity) {
        Item inboundItem = item.toBuilder().quantity(quantity).build();

        Inbound inbound = ApiSteps.Inbound().createInbound(inboundItem.getArticle());

        String palletId = placeInboundToPallet(inbound.getFulfillmentId(), quantity);
        receiveInboundItems(inboundItem, id, palletId);
        closeAndApproveCloseInbound(inbound.getFulfillmentId());
        placeContainerToCell(id, storageCell);
    }

    public void acceptItemsAndPlaceThemToPalletCell(Item item, @NotBlank String storageCell) {
        acceptItemsAndPlaceThemToPalletCell(item, storageCell, 1);
    }

    @Step("Создаем поставку, принимаем {quantity} товаров и размещаем в ячейку палетного хранения {storageCell}")
    public String acceptItemsAndPlaceThemToPalletCell(Item item, @NotBlank String storageCell, int quantity) {
        String inboundCart = DatacreatorSteps.Label().createContainer(ContainerIdType.PLT);
        Item inboundItem = item.toBuilder().quantity(quantity).build();

        Inbound inbound = ApiSteps.Inbound().createInbound(inboundItem.getArticle());

        String palletId = placeInboundToPallet(inbound.getFulfillmentId(), quantity);
        receiveInboundItems(inboundItem, inboundCart, palletId);
        closeAndApproveCloseInbound(inbound.getFulfillmentId());
        placeContainerToCell(inboundCart, storageCell);
        return inboundCart;
    }

    @Step("Создаем поставку, принимаем нонсортовые товары и размещаем в ячейку отбора")
    public void acceptNonSortItemsAndPlaceThemToPickingCell(Item item, String storageCell) {
        String inboundCart = DatacreatorSteps.Label().createContainer(ContainerIdType.L);
        Inbound inbound = ApiSteps.Inbound().createNonSortInbound(item.getArticle());

        String palletId = placeInboundToPallet(inbound.getFulfillmentId());
        receiveInboundItems(item, inboundCart, palletId);
        closeAndApproveCloseInbound(inbound.getFulfillmentId());
        placeContainerToCell(inboundCart, storageCell);
    }

    @Step("Принимаем товары в корзину")
    public String acceptItemsToCart(Item item) {
        String inboundCart = DatacreatorSteps.Label().createContainer(ContainerIdType.L);
        Inbound inbound = ApiSteps.Inbound().createInbound(item.getArticle());

        String palletId = placeInboundToPallet(inbound.getFulfillmentId());
        receiveInboundItems(item, inboundCart, palletId);
        closeAndApproveCloseInbound(inbound.getFulfillmentId());
        return inboundCart;
    }

    @Step("Размещаем аномалию")
    public void placeAndConsolidateAnomaly(String anomalyCartId,
                                           String containerId,
                                           String anomalyPlacementLoc,
                                           String areaKey) {
        uiSteps.Navigation().selectTasksWithLocation()
                .placeAnomaly(anomalyCartId, anomalyPlacementLoc, areaKey)
                .consolidateAnomaly(anomalyCartId, containerId, areaKey);
    }

    public String placeInboundToPallet(String inboundId) {
        return placeInboundToPallet(inboundId, 1);
    }

    @Step("Принимаем поставку на паллету")
    public String placeInboundToPallet(String inboundId, int amount) {
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().initialReceiveItem(inboundId, amount);
        uiSteps.Login().PerformLogin();
        return uiSteps.Receiving().findPalletOfInbound(inboundId);
    }

    @Step("Первично принимаем поставку на паллету по нормеру поставки в Аксапте")
    public String placeInboundByExternalRequestIdToPallet(String inboundId, String externalRequestId, int amount) {
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().initialReceiveItemByExternalRequestId(externalRequestId, amount);
        uiSteps.Login().PerformLogin();
        return uiSteps.Receiving().findPalletOfInbound(inboundId);
    }

    @Step("Выполняем вторичную приемку")
    public void receiveInboundItems(
            Item inboundItem,
            String inboundCart,
            String pallet
    ) {
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(inboundItem, pallet, inboundCart);
    }

    @Step("Закрываем, затем закрываем с проверкой ПУО-приемку через ui")
    public void closeAndApproveCloseInbound(String fulfillmentId) {
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().findInboundInReceivingAdmin(fulfillmentId);
        SuppliesListPage suppliesListPage = uiSteps.Receiving().closeInboundOnSuppliesListPage();
        uiSteps.Receiving().approveCloseInboundOnSuppliesListPage(suppliesListPage);
    }

    @Step("Ищем поставку в админке, затем закрываем с проверкой ПУО-приемку через ui")
    public void approveCloseInbound(String fulfillmentId) {
        uiSteps.Login().PerformLogin();
        SuppliesListPage suppliesListPage = uiSteps.Receiving().findInboundInReceivingAdmin(fulfillmentId);
        uiSteps.Receiving().approveCloseInboundOnSuppliesListPage(suppliesListPage);
    }

    @Step("Ищем поставку в админке, затем закрываем ПУО. Затем закрываем с проверкой ПУО-приемку. Получаем " +
            "предупреждение")
    public void approveCloseInboundWithWarning(String fulfillmentId) {
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().findInboundInReceivingAdmin(fulfillmentId);
        SuppliesListPage suppliesListPage = uiSteps.Receiving().closeInboundOnSuppliesListPage();
        uiSteps.Receiving().approveCloseInboundWithWarning(suppliesListPage);
    }

    @Step("Ищем поставку в админке, затем закрываем с проверкой ПУО-приемку через ui")
    public void closeInitialInbound(String fulfillmentId) {
        uiSteps.Login().PerformLogin();
        SuppliesListPage suppliesListPage = uiSteps.Receiving().findInboundInReceivingAdmin(fulfillmentId);
        uiSteps.Receiving().closeInitialInboundOnSuppliesListPage(suppliesListPage);
    }

    @Step("Размещаем контейнер в ячейку")
    public void placeContainerToCell(String inboundCart, String storageCell) {
        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeContainer(inboundCart, storageCell);
    }

    public void initiallyReceiveReturnContainersWithoutQualityAttributes(String box) {
        initiallyReceiveReturnContainersWithoutQualityAttributes(Collections.singletonList(box));
    }

    @Step("Первично принимаем возвратные коробки без атрибутов качества")
    public void initiallyReceiveReturnContainersWithoutQualityAttributes(List<String> boxes) {
        uiSteps.Login().PerformLogin();
        ReceiptInputPage receiptInputPage = uiSteps.Receiving().enterPrinterAndGateInInitialReceiving();
        if (!boxes.isEmpty()) {
            for (String box : boxes) {
                receiptInputPage
                        .enterContainerId(box)
                        .clickSaveButtonForInitialPalletFlow();
            }
        }
    }

    @Step("Пытаемся первично допринять палету")
    public void tryToInitiallyReceivePallet(String fulfillmentId, String containerId) {
        uiSteps.Login().PerformLogin();
        ReceiptInputPage receiptInputPage = uiSteps.Receiving().enterPrinterAndGateInInitialReceiving();

        receiptInputPage
                .enterContainerIdWithInitiallyClosedReceipt(fulfillmentId, containerId);

    }

    @Step("Не удалось определить поставку по номеру грузоместа")
    public void initiallyReceiveReturnDuplicateContainersWithoutQualityAttributes(String fulfillmentId,
                                                                                  List<String> containersIds) {
        uiSteps.Login().PerformLogin();
        ReceiptInputPage receiptInputPage = uiSteps.Receiving().enterPrinterAndGateInInitialReceiving();
        if (!containersIds.isEmpty()) {
            for (String box : containersIds) {
                receiptInputPage
                        .enterDuplicateContainerId(box)
                        .enterReturnInboundId(fulfillmentId)
                        .enterContainerId(box)
                        .clickSaveButtonForInitialPalletFlow();
            }
        }
    }

    @Step("Первично принимаем одну дропшип коробку невыкупов без атрибутов качества")
    public void initiallyReceiveUnredeemedNotAllowedBoxesWithoutQualityAttributes(String fulfillmentId,
                                                                                  List<Box> boxes) {
        uiSteps.Login().PerformLogin();
        final ReceiptInputPage receiptInputPage = uiSteps.Receiving().enterPrinterAndGateInInitialReceiving();

        ContainerInputPage containerInputPage = receiptInputPage
                .enterReturnInboundId(fulfillmentId);

        containerInputPage
                .enterContainerId(boxes.get(0).getBoxId())
                .clickSaveButtonForInitialPalletFlow();

        containerInputPage
                .enterNotAllowedContainerId(boxes.get(1).getBoxId());
    }

    @Step("Первично принимаем возвратные коробки без атрибутов качества и одну неизвестную")
    public void initiallyReceiveReturnBoxesAndUndefinedBoxWithoutQualityAttributes(String fulfillmentId,
                                                                                   List<String> boxes) {
        uiSteps.Login().PerformLogin();
        final ReceiptInputPage receiptInputPage = uiSteps.Receiving().enterPrinterAndGateInInitialReceiving();

        ContainerInputPage containerInputPage = receiptInputPage
                .enterReturnInboundId(fulfillmentId);
        for (int i = 0; i < boxes.size() - 1; i++) {
            containerInputPage
                    .enterContainerId(boxes.get(i))
                    .clickSaveButtonForInitialPalletFlow();
        }
        receiptInputPage
                .enterReturnInboundId(fulfillmentId)
                .enterUndefinedContainerId(boxes.get(boxes.size() - 1))
                .clickUndefinedBoxButton();
    }

    @Step("Создаем поставку товара: {sku} и принимаем её на паллету")
    public String createInboundAndPlaceToPallet(String requestFilePath, String sku, Long vendorId) {
        Inbound inbound = ApiSteps.Inbound().putInbound(InboundType.DEFAULT);
        ApiSteps.Inbound().putInboundRegistry(
                requestFilePath,
                inbound,
                sku,
                vendorId,
                false
        );

        return placeInboundToPallet(inbound.getFulfillmentId());
    }


    @Step("Связываем коробку с номером заказа")
    public void linkBoxWithOrderId(String boxId, String orderId) {
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().linkBoxWithOrderId(boxId, orderId);
    }

}
