package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.ReceivingStation;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.ZoneConfig;

public class ReceivingConveyor {

    private static final DatacreatorClient dataCreator = new DatacreatorClient();

    public void createZoneConfig(ZoneConfig zoneConfig) {
        dataCreator.createZoneConfig(zoneConfig);
    }

    public void deleteZoneConfig(String name) {
        dataCreator.deleteZoneConfig(name);
    }

    public void createReceivingStation(ReceivingStation receivingStation) {
        dataCreator.createReceivingStation(receivingStation);
    }

    public void deleteReceivingStation(String name) {
        dataCreator.deleteReceivingStation(name);
    }
}
