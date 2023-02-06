package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;


public class TtsNotifications {

    private static final DatacreatorClient DATACREATOR_CLIENT = new DatacreatorClient();

    @Step("Удаляем сообщения из очереди на отправку")
    public void deleteTtsNotifications(String username) {
        DATACREATOR_CLIENT.deleteTtsNotifications(username);
    }
}
