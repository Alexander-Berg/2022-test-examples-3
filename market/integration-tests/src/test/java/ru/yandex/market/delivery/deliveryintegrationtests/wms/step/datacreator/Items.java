package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.request.DatacreatorGetItemRequest;
import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;

import java.util.List;

@Resource.Classpath("wms/test.properties")
public class Items {
    private static final DatacreatorClient dataCreator = new DatacreatorClient();

    @Step("Получаем серийник по партии и номеру ячейки")
    public String getItemSerialByLocLot(String cellId, String batchNumber) {
        return dataCreator.getItem(DatacreatorGetItemRequest
                .builder()
                .loc(cellId)
                .lot(batchNumber)
                .build()
        ).extract()
                .jsonPath()
                .getString("serialNumber");
    }

    @Step("Получаем номер партии по артикулу поставщика и номеру ячейки")
    public String getItemLotByArticleLoc(String article, String cellId) {
        return dataCreator.getItem(DatacreatorGetItemRequest
                .builder()
                .loc(cellId)
                .manufacturerSku(article)
                .build()
        ).extract()
                .jsonPath()
                .getString("lot");
    }

    @Step("Получаем номер партии для УИТа {serialNumber}")
    public String getItemLotBySerialNumber(String serialNumber) {
        return dataCreator.getItem(DatacreatorGetItemRequest
                        .builder()
                        .serialNumber(serialNumber)
                        .build()
                ).extract()
                .jsonPath()
                .getString("lot");
    }

    //TODO переписать на получение через ui, когда там поддержат блокировки, чтобы проверять текст в интерфейсе
    @Step("Получаем список блокировок по номеру партии - {lot}")
    public List<String> getStatusesByLot(String lot) {
        return dataCreator.getStatusesByLot(lot);
    }

    @Step("Проставляем партии {0} блокировку {1}")
    public void placeHoldOnLot(String lot, InventoryHoldStatus holdStatus) {
        dataCreator.placeHoldOnLot(lot, holdStatus);
    }

    @Step("Проверяем общее число товаров в заказе - {orderKey}")
    public void checkTotalOpenQty(String orderKey, Integer expectedQty) {
        String count = dataCreator.checkTotalOpenQty(orderKey);
        Assertions.assertEquals(expectedQty, Integer.parseInt(count), "Total open qty is wrong:");
    }

}
