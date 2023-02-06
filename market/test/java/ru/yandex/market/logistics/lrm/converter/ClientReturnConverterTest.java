package ru.yandex.market.logistics.lrm.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryReschedulingReason;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressChangeReason;
import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnStatus;
import ru.yandex.market.logistics.lrm.service.client_return.ClientReturnConverter;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnCancellationMeta.Reason;

@DisplayName("Конвертация клиентских возвратов")
class ClientReturnConverterTest extends LrmTest {

    private final EnumConverter enumConverter = new EnumConverter();
    private final ClientReturnConverter converter = new ClientReturnConverter(enumConverter);

    @DisplayName("ReturnStatus -> ReturnDeliveryStatus")
    @ParameterizedTest
    @EnumSource(
        value = ReturnStatus.class,
        names = {"CREATED", "READY_FOR_UTILIZATION", "UTILIZED"},
        mode = Mode.EXCLUDE
    )
    void returnStatusToReturnDeliveryStatus(ReturnStatus status) {
        softly.assertThat(converter.convertStatus(status)).isNotNull();
    }

    @DisplayName("ReturnStatus -> null ReturnDeliveryStatus")
    @ParameterizedTest
    @EnumSource(
        value = ReturnStatus.class,
        names = {"CREATED", "READY_FOR_UTILIZATION", "UTILIZED"},
        mode = Mode.INCLUDE
    )
    void returnStatusToNull(ReturnStatus status) {
        softly.assertThat(converter.convertStatus(status)).isNull();
    }

    @DisplayName("Reason -> ReturnCancelReason")
    @ParameterizedTest
    @EnumSource(Reason.class)
    void reasonToReturnCancelReason(Reason reason) {
        softly.assertThat(converter.convertCancelReason(reason)).isNotNull();
    }

    @DisplayName("null Reason -> null ReturnCancelReason")
    @Test
    void nullReason() {
        softly.assertThat(converter.convertCancelReason(null)).isNull();
    }

    @DisplayName("TplReturnAtClientAddressChangeReason -> ReturnDeliveryReschedulingReason")
    @ParameterizedTest
    @EnumSource(TplReturnAtClientAddressChangeReason.class)
    void rescheduleReason(TplReturnAtClientAddressChangeReason value) {
        softly.assertThat(enumConverter.convert(value, ReturnDeliveryReschedulingReason.class)).isNotNull();
    }

}
