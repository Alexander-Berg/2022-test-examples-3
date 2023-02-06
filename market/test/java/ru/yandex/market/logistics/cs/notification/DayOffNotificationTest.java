package ru.yandex.market.logistics.cs.notification;

import java.time.LocalDate;
import java.util.EnumSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.cs.AbstractTest;
import ru.yandex.market.logistics.cs.domain.dto.DayOffDto;
import ru.yandex.market.logistics.cs.domain.dto.PartnerDto;
import ru.yandex.market.logistics.cs.domain.enumeration.DayOffReadableType;
import ru.yandex.market.logistics.cs.domain.enumeration.DayOffType;
import ru.yandex.market.logistics.cs.domain.enumeration.UnitType;
import ru.yandex.market.logistics.cs.notifications.dayoff.DayOffSetNotification;
import ru.yandex.market.logistics.cs.notifications.dayoff.DayOffUnsetNotification;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

@DisplayName("Текст сообщения нотификации")
public class DayOffNotificationTest extends AbstractTest {

    private static final PartnerDto TEST_DELIVERY_PARTNER = PartnerDto.builder()
        .partnerType(PartnerType.DELIVERY)
        .id(1L)
        .readableName("some name")
        .build();

    private static final PartnerDto TEST_DROPSHIP_PARTNER = PartnerDto.builder()
        .partnerType(PartnerType.DROPSHIP)
        .id(2L)
        .readableName("some name 2")
        .build();

    @Test
    @DisplayName("Нотификация о дейоффе СД")
    void dayOffSetNotification() {
        DayOffDto dto = getDto(TEST_DELIVERY_PARTNER, DeliveryType.COURIER, DayOffType.TECHNICAL);
        DayOffSetNotification notification = new DayOffSetNotification();
        softly.assertThat(notification.getMessageText(dto))
            .isEqualTo(
                "\u2705\u2705\u2705 - Сработал DayOff\n" +
                    getText(dto) + "\n" +
                    "\uD83C\uDFAF\uD83D\uDD2B\uD83E\uDD84"
            );
    }

    @Test
    @DisplayName("Нотификация о снятии дейоффа с СД")
    void unsetDayOffNotification() {
        DayOffDto dto = getDto(TEST_DELIVERY_PARTNER, DeliveryType.COURIER, DayOffType.UNSET);
        DayOffUnsetNotification notification = new DayOffUnsetNotification();
        softly.assertThat(notification.getMessageText(dto))
            .isEqualTo(
                "\uD83D\uDEAB\uD83D\uDEAB\uD83D\uDEAB - Откатили DayOff\n" +
                    getText(dto) + "\n" +
                    "\uD83C\uDFAF\uD83D\uDD2B\uD83E\uDD84"
            );
    }

    @Test
    @DisplayName("Нотификация без сервиса и региона")
    void withoutService() {
        DayOffDto dto = getDto(TEST_DROPSHIP_PARTNER, null, DayOffType.MANUAL);
        DayOffSetNotification notification = new DayOffSetNotification();
        softly.assertThat(notification.getMessageText(dto))
            .isEqualTo(
                "\u2705\u2705\u2705 - Сработал DayOff\n" +
                    getText(dto) + "\n" +
                    "\uD83C\uDFAF\uD83D\uDD2B\uD83E\uDD84"
            );
    }

    private DayOffDto getDto(PartnerDto partner, DeliveryType deliveryType, DayOffType dayOffType) {
        return DayOffDto.builder()
            .partner(partner)
            .date(LocalDate.now())
            .unitType(UnitType.ORDER)
            .regionToId(321)
            .regionToName("Регион 2")
            .regionFromId(456)
            .regionFromName("Регион 1")
            .threshold(100L)
            .partnerCapacityId(123L)
            .serviceCapacityType(CapacityService.SHIPMENT)
            .deliveryType(deliveryType)
            .count(44L)
            .dayOffType(dayOffType)
            .build();
    }

    private String getText(DayOffDto dto) {
        PartnerDto partner = dto.getPartner();
        PartnerType type = partner.getPartnerType();
        return "\n*" + partner.getPartnerType().name() + "*: [" + partner.getReadableName() +
            " (" + partner.getId() +
            ")](https://lms.market.yandex-team.ru/lms/partner/" + partner.getId() + ")\n" +
            (PartnerType.DELIVERY == type ? getRegion(dto) : "") +
            "*Date*: " + dto.getDate() + "\n" +
            (PartnerType.DELIVERY == type ? "*Delivery type*: " + dto.getDeliveryType().getName() + "\n" : "") +
            getCapacityType(dto) +
            "*Capacity*: [" + dto.getThreshold() + " " + dto.getUnitType().name().toLowerCase() + "s" +
            "](https://lms.market.yandex-team.ru/lms/partner-capacity/" + dto.getPartnerCapacityId() + ")\n" +
            (
                dto.getDayOffType() != DayOffType.UNSET ?
                    (
                        "*Current*: " + dto.getCount() + " " + dto.getUnitType().name().toLowerCase() + "s\n" +
                        "*DayOff Type*: " +
                        DayOffReadableType.getReadableTypeFromDayOffType(dto.getDayOffType()).getReadableName() +
                        "\n"
                    ) : ""
            );
    }

    private String getCapacityType(DayOffDto dto) {
        return EnumSet.of(PartnerType.FULFILLMENT, PartnerType.DELIVERY).contains(dto.getPartner().getPartnerType())
            ? "*Capacity type*: " + dto.getServiceCapacityType().getTitle() + "\n"
            : "";
    }

    private String getRegion(DayOffDto dto) {
        return "*RegionFrom*: " + dto.getRegionFromName() + " (" + dto.getRegionFromId() + ")\n" +
            "*RegionTo*: " + dto.getRegionToName() + " (" + dto.getRegionToId() + ")\n";
    }
}
