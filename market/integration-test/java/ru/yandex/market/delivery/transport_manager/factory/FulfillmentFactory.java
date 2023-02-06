package ru.yandex.market.delivery.transport_manager.factory;

import java.util.List;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Register;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Sender;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ShipmentType;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class FulfillmentFactory {

    @Nonnull
    public Register createRegister() {
        return new Register(
            ResourceId.builder()
                .setYandexId("TMR1").build(),
            List.of(
                ResourceId.builder()
                    .setYandexId("1000004")
                    .setPartnerId("D0003406042")
                    .build()
            ),
            new DateTime("2020-09-06T00:00:00+03:00"),
            new Sender(
                ResourceId.builder()
                    .setYandexId("118").build(),
                null,
                "ООО Компания 8",
                "https://delivery.yandex.ru",
                "Компания 8",
                null,
                null,
                null,
                null,
                "98768",
                "1234567898",
                null,
                null,
                "OOO"
            ),
            ResourceId.builder().setYandexId("TMU12").build(),
            ShipmentType.SELF_EXPORT_TO_WAREHOUSE
        );
    }

    @Nonnull
    public Register createRegisterDifferentApiType() {
        return new Register(
                ResourceId.builder()
                        .setYandexId("TMR1").build(),
                List.of(
                        ResourceId.builder()
                                .setYandexId("1000004")
                                .setPartnerId("UN2081902XI")
                                .build()
                ),
                new DateTime("2020-09-06T00:00:00+03:00"),
                new Sender(
                        ResourceId.builder()
                                .setYandexId("119").build(),
                        null,
                        "ООО Компания 9",
                        "https://delivery.yandex.ru",
                        "Компания 9",
                        null,
                        null,
                        null,
                        null,
                        "98769",
                        "1234567899",
                        null,
                        null,
                        "OOO"
                ),
                ResourceId.builder().setYandexId("TMU15").build(),
                ShipmentType.ACCEPTANCE
        );
    }
}
