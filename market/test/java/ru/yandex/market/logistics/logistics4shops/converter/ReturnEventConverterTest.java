package ru.yandex.market.logistics.logistics4shops.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import ru.yandex.market.logistics.logistics4shops.AbstractTest;
import ru.yandex.market.logistics.logistics4shops.event.model.FulfilmentBoxItemsReceived.BoxItem;
import ru.yandex.market.logistics.logistics4shops.event.model.RegularReturnStatusData.Box.RecipientType;
import ru.yandex.market.logistics.logistics4shops.event.model.ReturnStatusChangedPayload;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnSource;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatus;
import ru.yandex.market.lrm.client.model.ShipmentRecipientType;
import ru.yandex.market.lrm.client.model.UnitCountType;

@DisplayName("Тесты конвертации событий LRM в B2B")
class ReturnEventConverterTest extends AbstractTest {

    private final EnumConverter enumConverter = new EnumConverter();
    private final ReturnEventConverter converter = new ReturnEventConverter(
        new OrderIdConverter(),
        enumConverter,
        new ProtoConverter()
    );

    @ParameterizedTest
    @EnumSource(ReturnStatus.class)
    @DisplayName("Конвертация ReturnStatus из LRM в B2B")
    void returnStatus(ReturnStatus status) {
        softly.assertThat(converter.convertReturnStatus(status)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ReturnSource.class)
    @DisplayName("Конвертация ReturnSource из LRM в B2B")
    void returnSource(ReturnSource value) {
        softly.assertThat(enumConverter.convert(value, ReturnStatusChangedPayload.ReturnSource.class)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(
        value = ShipmentRecipientType.class,
        names = {"DELIVERY_SERVICE_WITH_COURIER", "YA_GO_SHOP"},
        mode = Mode.EXCLUDE
    )
    @DisplayName("Конвертация ShipmentRecipientType из LRM в B2B")
    void recipientType(ShipmentRecipientType value) {
        softly.assertThat(enumConverter.convert(value, RecipientType.class)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(UnitCountType.class)
    @DisplayName("Конвертация UnitCountType из LRM в B2B")
    void unitCountType(UnitCountType value) {
        softly.assertThat(enumConverter.convert(value, BoxItem.UnitCountType.class)).isNotNull();
    }

}
