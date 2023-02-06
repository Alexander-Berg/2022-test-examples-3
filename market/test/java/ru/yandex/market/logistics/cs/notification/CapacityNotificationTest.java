package ru.yandex.market.logistics.cs.notification;

import java.time.LocalDate;
import java.util.EnumSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.cs.AbstractTest;
import ru.yandex.market.logistics.cs.domain.dto.CapacityWarnDto;
import ru.yandex.market.logistics.cs.domain.dto.PartnerDto;
import ru.yandex.market.logistics.cs.domain.enumeration.CounterOverflowReason;
import ru.yandex.market.logistics.cs.domain.enumeration.UnitType;
import ru.yandex.market.logistics.cs.notifications.counter.AlreadyOverflowedCapacityCounterNotification;
import ru.yandex.market.logistics.cs.notifications.counter.HalfCapacityCounterNotification;
import ru.yandex.market.logistics.cs.notifications.counter.ICapacityCounterNotifiable;
import ru.yandex.market.logistics.cs.notifications.counter.Less20CapacityCounterNotification;
import ru.yandex.market.logistics.cs.notifications.counter.LowCapacityCounterNotification;
import ru.yandex.market.logistics.cs.notifications.counter.OverflowCapacityCounterNotification;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

public class CapacityNotificationTest extends AbstractTest {

    private static final String FOOTER = "\uD83C\uDFAF\uD83D\uDD2B\uD83E\uDD84";
    private static final PartnerDto TEST_DELIVERY_PARTNER = PartnerDto.builder()
        .partnerType(PartnerType.DELIVERY)
        .id(1L)
        .readableName("some name")
        .build();

    private static final PartnerDto TEST_FF_PARTNER = PartnerDto.builder()
        .partnerType(PartnerType.FULFILLMENT)
        .id(2L)
        .readableName("some name 2")
        .build();

    private static final PartnerDto TEST_DROPSHIP_PARTNER = PartnerDto.builder()
        .partnerType(PartnerType.DROPSHIP)
        .id(3L)
        .readableName("some name 3")
        .build();

    @Test
    @DisplayName("Перелив из-за ПДД для дропшипов")
    void overflowNotificationForDropshipDateShift() {
        CapacityWarnDto dto = getDto(TEST_DROPSHIP_PARTNER, null, 101L, CounterOverflowReason.ORDER_ROUTE_RECALCULATED);
        ICapacityCounterNotifiable notification = new OverflowCapacityCounterNotification();

        softly.assertThat(notification.getMessageText(dto))
            .isEqualTo(
                "\u26A0 - Превышение капасити Партнера\n" +
                    getOverflowText(dto) + "\n" +
                    FOOTER
            );
    }

    @Test
    @DisplayName("Для повторного уведомления ПДД дропшипов нет сообщения о повторе")
    void alreadyOverflownForDropshipDateShiftNotification() {
        CapacityWarnDto dto = getDto(TEST_DROPSHIP_PARTNER, null, 101L, CounterOverflowReason.ORDER_ROUTE_RECALCULATED);
        ICapacityCounterNotifiable notification = new AlreadyOverflowedCapacityCounterNotification();

        softly.assertThat(notification.getMessageText(dto))
            .isEqualTo(
                    "\u26A0 - Превышение капасити Партнера\n" +
                    getOverflowText(dto) + "\n" +
                    FOOTER
            );
    }

    @Test
    @DisplayName("Нотификация о повторном переливе")
    void alreadyOverflownNotification() {
        CapacityWarnDto dto = getDto(TEST_DROPSHIP_PARTNER, null, 101L, CounterOverflowReason.NEW_ORDER);
        ICapacityCounterNotifiable notification = new AlreadyOverflowedCapacityCounterNotification();

        softly.assertThat(notification.getMessageText(dto))
            .isEqualTo(
                "\u203C️Повторное уведомление\u203C\n" +
                    "\uD83C\uDD98\uD83C\uDD98\uD83C\uDD98 - Превышение капасити Партнера\n" +
                    getOverflowText(dto) + "\n" +
                    FOOTER
            );
    }

    @Test
    @DisplayName("Нотификация о переливе")
    void overflowNotification() {
        CapacityWarnDto dto = getDto(TEST_DROPSHIP_PARTNER, null, 101L, CounterOverflowReason.NEW_ORDER);
        ICapacityCounterNotifiable notification = new OverflowCapacityCounterNotification();

        softly.assertThat(notification.getMessageText(dto))
            .isEqualTo(
                "\uD83C\uDD98\uD83C\uDD98\uD83C\uDD98 - Превышение капасити Партнера\n" +
                    getOverflowText(dto) + "\n" +
                    FOOTER
            );
    }

    @Test
    @DisplayName("Нотификация о 50% для ФФ")
    void HalfCapacityNotification() {
        CapacityWarnDto dto = getDto(TEST_FF_PARTNER, null, 50L, CounterOverflowReason.NEW_ORDER);
        ICapacityCounterNotifiable notification = new HalfCapacityCounterNotification();
        softly.assertThat(notification.getMessageText(dto))
            .isEqualTo(
                "⏰⏰⏰ - Осталось меньше 50% свободной капасити Склада\n" +
                    getText(dto) + "\n" +
                    FOOTER
            );
    }

    @Test
    @DisplayName("Нотификация о 10% для СД")
    void LowCapacityNotification() {
        CapacityWarnDto dto = getDto(TEST_DELIVERY_PARTNER, DeliveryType.COURIER, 90L, CounterOverflowReason.NEW_ORDER);
        ICapacityCounterNotifiable notification = new LowCapacityCounterNotification();
        softly.assertThat(notification.getMessageText(dto))
            .isEqualTo(
                "⚠️⚠️⚠️ - Осталось меньше 10% свободной капасити Службы доставки\n" +
                    getText(dto) + "\n" +
                    FOOTER
            );
    }

    @Test
    @DisplayName("Нотификация для ФФ")
    void ffNotification() {
        CapacityWarnDto dto = getDto(TEST_FF_PARTNER, null, 50L, CounterOverflowReason.NEW_ORDER);
        ICapacityCounterNotifiable notification = new HalfCapacityCounterNotification();
        softly.assertThat(notification.getMessageText(dto))
            .isEqualTo(
                "⏰⏰⏰ - Осталось меньше 50% свободной капасити Склада\n" +
                    getText(dto) + "\n" +
                    FOOTER
            );
    }

    @Test
    @DisplayName("Нотификация о 80% для СД")
    void Less20PercentNotification() {
        CapacityWarnDto dto = getDto(TEST_DELIVERY_PARTNER, DeliveryType.PICKUP, 80L, CounterOverflowReason.NEW_ORDER);
        ICapacityCounterNotifiable notification = new Less20CapacityCounterNotification();
        softly.assertThat(notification.getMessageText(dto))
            .isEqualTo(
                "⏰⏰⏰ - Осталось меньше 20% свободного капасити Службы доставки\n" +
                    getText(dto) + "\n" +
                    FOOTER
            );
    }

    private CapacityWarnDto getDto(PartnerDto partnerDto, DeliveryType type, Long count, CounterOverflowReason reason) {
        return CapacityWarnDto.builder()
            .partner(partnerDto)
            .date(LocalDate.now())
            .regionToId(321)
            .regionToName("Регион 2")
            .regionFromId(456)
            .regionFromName("Регион 1")
            .threshold(100L)
            .capacityId(123L)
            .deliveryType(type)
            .count(count)
            .partnerCapacityId(123L)
            .unitType(UnitType.ORDER)
            .serviceCapacityType(CapacityService.INBOUND)
            .eventReason(reason)
            .build();

    }

    private String getOverflowText(CapacityWarnDto dto) {
        PartnerDto partner = dto.getPartner();
        PartnerType type = partner.getPartnerType();

        return "\n*" + type.name() +
            "*: [" + partner.getReadableName() +
            " (" + partner.getId() +
            ")](https://lms.market.yandex-team.ru/lms/partner/" + partner.getId() + ")\n" +
            "*CapacityId*: " + dto.getCapacityId() + "\n" +
            (type == PartnerType.DELIVERY ? getRegion(dto) : "") +
            "*Date*: " + dto.getDate() + "\n" +
            "*Capacity Total*: [" + dto.getThreshold() + " " + dto.getUnitType().name().toLowerCase() + "s" +
            "](https://lms.market.yandex-team.ru/lms/partner-capacity/" + dto.getPartnerCapacityId() + ")\n" +
            "*Current*: " + dto.getCount() + " " + dto.getUnitType().name().toLowerCase() + "s\n" +
            "*Overflowing by*: 1%\n*Reason*: " + dto.getEventReason().getName() + "\n";
    }

    private String getText(CapacityWarnDto dto) {
        PartnerDto partner = dto.getPartner();
        PartnerType type = partner.getPartnerType();
        String capacityType = getCapacityType(dto);
        return "\n*" + type.name() +
            "*: [" + partner.getReadableName() +
            " (" + partner.getId() +
            ")](https://lms.market.yandex-team.ru/lms/partner/" + partner.getId() + ")\n" +
            "*CapacityId*: " + dto.getCapacityId() + "\n" +
            (type == PartnerType.DELIVERY ? getRegion(dto) : "") +
            "*Date*: " + dto.getDate() + "\n" +
            (type == PartnerType.DELIVERY ? "*Delivery type*: " + dto.getDeliveryType().getName() + "\n" : "") +
            capacityType +
            "*Capacity Total*: [" + dto.getThreshold() + " " + dto.getUnitType().name().toLowerCase() + "s" +
            "](https://lms.market.yandex-team.ru/lms/partner-capacity/" + dto.getPartnerCapacityId() + ")\n" +
            "*Current*: " + dto.getCount() + " " + dto.getUnitType().name().toLowerCase() + "s\n";
    }

    private String getCapacityType(CapacityWarnDto dto) {
        return EnumSet.of(PartnerType.FULFILLMENT, PartnerType.DELIVERY).contains(dto.getPartner().getPartnerType())
            ? "*Capacity type*: " + dto.getServiceCapacityType().getTitle() + "\n"
            : "";
    }

    private String getRegion(CapacityWarnDto dto) {
        return "*RegionFrom*: " + dto.getRegionFromName() + " (" + dto.getRegionFromId() + ")\n" +
            "*RegionTo*: " + dto.getRegionToName() + " (" + dto.getRegionToId() + ")\n";
    }
}
