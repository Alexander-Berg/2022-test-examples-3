package ru.yandex.market.delivery.transport_manager.factory;

import java.util.List;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.delivery.Register;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Sender;
import ru.yandex.market.logistic.gateway.common.model.delivery.ShipmentType;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class DeliveryFactory {
    @Nonnull
    public Register createRegister(String yandexId, String barcode) {
        return new Register(
            ResourceId.builder().setYandexId(yandexId).build(),
            List.of(
                ResourceId.builder()
                    .setYandexId(barcode)
                    .setPartnerId("UN2081902XI")
                    .build()
            ),
            new DateTime("2020-09-06T00:00:00+03:00"),
            new Sender(
                ResourceId.builder().setYandexId("119").build(),
                "ООО Компания 9",
                "https://delivery.yandex.ru",
                LegalForm.OOO,
                "98769",
                "1234567899",
                null,
                null,
                null,
                "Компания 9",
                null,
                null,
                null
            ),
            ResourceId.builder().setYandexId("TMU15").build(),
            ShipmentType.ACCEPTANCE
        );
    }
}
