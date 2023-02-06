package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator;

import io.qameta.allure.Step;
import org.hamcrest.Matchers;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

public class Label {
    private static final DatacreatorClient dataCreator = new DatacreatorClient();

    //Методы datacreator могут не работать корректно при одновременном обращении из 2 потоков

    @Step("Создаем контейнер {containerIdType} и получаем его айди")
    public synchronized String createContainer(ContainerIdType containerIdType) {

        return dataCreator
                .getContainer(containerIdType.getCode())
                .body(Matchers.matchesPattern(containerIdType.getRegexp()))
                .extract()
                .body()
                .asString();
    }

    @Step("Создаем дроп и получаем его айди")
    public synchronized String createDrop() {
        return dataCreator
                .getDrop()
                .body(Matchers.matchesPattern(ContainerIdType.DROP.getRegexp()))
                .extract()
                .body()
                .asString();
    }

    @Step("Создаем дроп и получаем его айди")
    public synchronized String createDrop(String carrierCode) {
        return dataCreator
                .getDropWithCarrier(carrierCode)
                .body(Matchers.matchesPattern(ContainerIdType.DROP.getRegexp()))
                .extract()
                .body()
                .asString();
    }

    @Step("Создаем посылку и получаем её айди")
    public synchronized ParcelId createParcel() {
        return new ParcelId(dataCreator
                .getParcel()
                .body(Matchers.matchesPattern(ContainerIdType.P.getRegexp()))
                .extract()
                .body()
                .asString());
    }
}
