package ru.yandex.market.delivery.transport_manager.admin.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.config.properties.LmsExtraProperties;
import ru.yandex.market.delivery.transport_manager.config.startrek.StartrekProperties;
import ru.yandex.market.delivery.transport_manager.config.tpl.TplProperties;
import ru.yandex.market.delivery.transport_manager.config.tsum.TsumProperties;
import ru.yandex.market.delivery.transport_manager.config.tsup.TsupProperties;
import ru.yandex.market.delivery.transport_manager.config.yard.YardProperties;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.logistics.front.library.dto.ExternalReferenceObject;

class AdminExternalLinkConverterTest {

    private static final String YOUR_BOOK_TEMPLATE = "Здесь могла быть Ваша бронь";

    AdminExternalLinkConverter adminExternalLinkConverter = new AdminExternalLinkConverter(
        new LmsExtraProperties().setAdminUrl("stub"),
        new TplProperties().setVirtualLinehaul(1L),
        new StartrekProperties().setWebUrl("stub"),
        new YardProperties().setFrontUrl("stub"),
        new TsupProperties().setHost("stub"),
        new TsumProperties().setHost("stub")
    );

    @Test
    void getCsTimeSlotIfBookingSlotIsNull() {
        ExternalReferenceObject expected =
            new ExternalReferenceObject(
                YOUR_BOOK_TEMPLATE,
                "stub/delivery-calendar/inbound?date=null&warehouseId=1&selectId=null",
                true
            );

        var actual = adminExternalLinkConverter
            .getCsTimeSlot(1L, null, null, TransportationUnitType.INBOUND, YOUR_BOOK_TEMPLATE);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getCsTimeSlotIfBookingSlotIsNullAndPartnerIdIsNull() {
        ExternalReferenceObject expected =
            new ExternalReferenceObject(
                YOUR_BOOK_TEMPLATE,
                "stub/delivery-calendar/inbound?date=null&warehouseId=null&selectId=null",
                true
            );

        var actual = adminExternalLinkConverter
            .getCsTimeSlot(null, null, null, TransportationUnitType.INBOUND, YOUR_BOOK_TEMPLATE);
        Assertions.assertEquals(expected, actual);
    }
}
