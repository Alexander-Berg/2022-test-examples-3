package ru.yandex.travel.orders.services.avia.aeroflot;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.javamoney.moneta.Money;
import org.junit.Test;

import ru.yandex.avia.booking.enums.PassengerCategory;
import ru.yandex.travel.testing.misc.TestResources;

import static org.assertj.core.api.Assertions.assertThat;

public class AeroflotOrderSyncMessageParserTest {
    private AeroflotMqParser parser = new AeroflotMqParser(AeroflotMqProperties.builder()
            .apiTimeZoneId(ZoneId.of("CST6CDT"))
            .mqTimeZoneId(ZoneId.of("Europe/Moscow"))
            .build());

    @Test
    public void testPaidNotTicketedMessage() {
        String message = TestResources.readResource("aeroflot/mq_sync/aeroflot_mq_not_paid_order.xml");
        AeroflotMqData order = parser.parseMessage(message);

        assertThat(order.getSentDate()).isEqualTo(ZonedDateTime.parse("2019-05-22T17:11:52+03:00"));
        assertThat(order.getBookingDate()).isEqualTo(ZonedDateTime.parse("2019-05-22T09:11:00-05:00"));
        assertThat(order.getSentDateMsc().toLocalDateTime()).isEqualTo(LocalDateTime.parse("2019-05-22T17:11:52"));
        assertThat(order.getBookingDateMsc().toLocalDateTime()).isEqualTo(LocalDateTime.parse("2019-05-22T17:11:00"));
        assertThat(order.getPnr()).isEqualTo("HUNEDQ");
        assertThat(order.getEmail()).isEqualTo("some-13@yandex-team.ru");
        assertThat(order.getPhone()).isEqualTo("+71234567890");
        assertThat(order.getPassengers()).isEqualTo(List.of(
                AeroflotMqData.PassengerRef.builder()
                        .firstName("SERGEY")
                        .lastName("MQSUCCESTWO")
                        .category(PassengerCategory.ADULT)
                        .build()
        ));

        assertThat(order.getTickets()).isNull();
        assertThat(order.getTotalPrice()).isNull();
        assertThat(order.isPaid()).isFalse();

        assertThat(order.getSourceMessage()).isEqualTo(message);
    }

    @Test
    public void testPaidAndTicketed() {
        String message = TestResources.readResource("aeroflot/mq_sync/aeroflot_mq_paid_order.xml");
        AeroflotMqData order = parser.parseMessage(message);

        assertThat(order.getPnr()).isEqualTo("GKZQVV");
        assertThat(order.getPassengers()).isEqualTo(List.of(
                AeroflotMqData.PassengerRef.builder()
                        .firstName("PASS")
                        .lastName("AONE")
                        .category(PassengerCategory.ADULT)
                        .build(),
                AeroflotMqData.PassengerRef.builder()
                        .firstName("SADADASDAD")
                        .lastName("ATWO")
                        .category(PassengerCategory.ADULT)
                        .build(),
                AeroflotMqData.PassengerRef.builder()
                        .firstName("ASAD")
                        .lastName("CHONE")
                        .category(PassengerCategory.CHILD)
                        .build()
        ));

        assertThat(order.getTickets()).isEqualTo(List.of(
                "5552109045173",
                "5552109045174",
                "5552109045175"
        ));
        assertThat(order.getTotalPrice()).isEqualTo(Money.of(9778, "RUB"));
        assertThat(order.isPaid()).isTrue();
    }
}
