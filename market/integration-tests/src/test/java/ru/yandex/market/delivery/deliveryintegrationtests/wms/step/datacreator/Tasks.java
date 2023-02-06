package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator;

import io.qameta.allure.Step;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;

public class Tasks {

    private static final DatacreatorClient dataCreator = new DatacreatorClient();

    @Step("Удаляем задания BBXD пользователя")
    public void deleteBbxdTasks(String username) {
        dataCreator.deleteBbxdTask(username);
    }
}
