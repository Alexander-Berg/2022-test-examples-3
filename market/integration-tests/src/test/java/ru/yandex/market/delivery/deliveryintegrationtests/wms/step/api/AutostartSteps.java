package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ApiClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;

@Slf4j
public class AutostartSteps {

    private static final ApiClient apiClient = new ApiClient();

    protected AutostartSteps() {}

    @Step("Создаем автостартовую волну с заказом {order.fulfillmentId}")
    public void startWave(Order order) {
        apiClient.createWave(order);
    }

    @Step("Добавляем сортировочную станцию {sortingStation} в настройки автостарта")
    public void addSortingStation(String sortingStation){
        apiClient.addSortingStationToAutostart(sortingStation);
    }

    @Step("Убираем сортировочную станцию {sortingStation} из настроек автостарта")
    public void removeSortingStation(String sortingStation){
        apiClient.removeSortingStationFromAutostart(sortingStation);
    }

    @Step("Получаем сортировочную станцию по номеру волны")
    public String getSortingStationByWaveId(WaveId waveId){
        return apiClient.getSortingStationByWaveId(waveId).extract().path("content[0].sortationStationKey");
    }

}

