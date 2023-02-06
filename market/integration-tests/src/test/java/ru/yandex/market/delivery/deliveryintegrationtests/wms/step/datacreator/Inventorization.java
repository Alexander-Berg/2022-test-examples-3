package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator;

import io.qameta.allure.Step;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;

public class Inventorization {
    private static final DatacreatorClient DATACREATOR_CLIENT = new DatacreatorClient();

    @Step("Создаем или получаем существующее задание на инвентаризацию ячейки {loc}")
    public void createOrGetExistingInventorizationTask(String loc) {
        DATACREATOR_CLIENT.createOrGetExistingInventorization(loc);
    }
}
