package ru.yandex.market.logistics.lrm.les;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressCancelReason;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressModificationSource;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressReasonType;
import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.converter.EnumConverter;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnReasonType;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnCancellationMeta.Reason;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnCancellationMeta.Source;

class TplLesEnumsTest extends LrmTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @DisplayName("В TplReturnAtClientAddressReasonType из LRM")
    @ParameterizedTest
    @EnumSource(ReturnReasonType.class)
    void toShipmentRecipientType(ReturnReasonType value) {
        softly.assertThatCode(() -> enumConverter.convert(value, TplReturnAtClientAddressReasonType.class))
            .doesNotThrowAnyException();
    }

    @DisplayName("Из TplReturnAtClientAddressCancelReason в LRM")
    @ParameterizedTest
    @EnumSource(TplReturnAtClientAddressCancelReason.class)
    void fromTplReturnAtClientAddressCancelReason(TplReturnAtClientAddressCancelReason value) {
        softly.assertThatCode(() -> enumConverter.convert(value, Reason.class))
            .doesNotThrowAnyException();
    }

    @DisplayName("Из TplReturnAtClientAddressModificationSource в LRM")
    @ParameterizedTest
    @EnumSource(TplReturnAtClientAddressModificationSource.class)
    void fromTplReturnAtClientAddressModificationSource(TplReturnAtClientAddressModificationSource value) {
        softly.assertThatCode(() -> enumConverter.convert(value, Source.class))
            .doesNotThrowAnyException();
    }

}
