package ru.yandex.market.crm.triggers.services.bpm.messages.les;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.crm.core.services.phone.PhoneService;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.lom.OrderArrivedPickupPointEvent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.SMS_PHONE_OR_PERSONAL_PHONE_ID;

public class OrderArrivedPickupPointEventConverterTest {

    private static final Uid PERSONAL_PHONE_ID_UID = Uid.asPersonalPhoneId("personalPhoneId");

    private final PhoneService phoneService = Mockito.mock(PhoneService.class);
    private static final Instant DATE = LocalDate.of(2021, 1, 1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant();

    private final OrderArrivedPickupPointEventConverter converter =
            new OrderArrivedPickupPointEventConverter(phoneService);

    @Before
    public void setUp() {
        when(phoneService.getPhoneUid(nullable(String.class), anyString())).thenReturn(PERSONAL_PHONE_ID_UID);
    }

    @Test
    public void orderArrivedPickupPointEvent() {
        Event event = new Event(
                "lom",
                "1",
                1L,
                "ORDER_ARRIVED_PICKUP_POINT",
                new OrderArrivedPickupPointEvent(
                        123L,
                        "234-LO-345",
                        "+79999999999",
                        "1234",
                        DATE,
                        "Москва"
                ),
                null
        );
        UidBpmMessage message = converter.convert(event);

        assertThat(message.getType(), equalTo("LOM_ORDER_ARRIVED_PICKUP_POINT"));
        assertThat(message.getCorrelationVariables(), equalTo(Map.of("orderId", 123L)));

        assertThat(message.getVariables().get("barcode"), equalTo("234-LO-345"));
        assertThat(message.getVariables().get("recipientPhone"), equalTo("+79999999999"));
        assertThat(message.getVariables().get(SMS_PHONE_OR_PERSONAL_PHONE_ID), equalTo(PERSONAL_PHONE_ID_UID));
        assertThat(message.getVariables().get("verificationCode"), equalTo("1234"));
        assertThat(
                message.getVariables().get("deliveryDate"),
                equalTo(DATE.getEpochSecond())
        );
        assertThat(message.getVariables().get("address"), equalTo("Москва"));
        assertThat(message.getVariables().get("recipientRegionId"), equalTo(0));
    }

    @Test
    public void orderArrivedPickupPointEventWithGeoId() {
        Event event = new Event(
                "lom",
                "1",
                1L,
                "ORDER_ARRIVED_PICKUP_POINT",
                new OrderArrivedPickupPointEvent(
                        123L,
                        "234-LO-345",
                        "+79999999999",
                        "1234",
                        DATE,
                        "Москва",
                        213
                ),
                null
        );
        UidBpmMessage message = converter.convert(event);

        assertThat(message.getType(), equalTo("LOM_ORDER_ARRIVED_PICKUP_POINT"));
        assertThat(message.getCorrelationVariables(), equalTo(
                ImmutableMap.<String, Object>builder()
                        .put("orderId", 123L)
                        .build()
        ));

        assertThat(message.getVariables().get("barcode"), equalTo("234-LO-345"));
        assertThat(message.getVariables().get("recipientPhone"), equalTo("+79999999999"));
        assertThat(message.getVariables().get(SMS_PHONE_OR_PERSONAL_PHONE_ID), equalTo(PERSONAL_PHONE_ID_UID));
        assertThat(message.getVariables().get("verificationCode"), equalTo("1234"));
        assertThat(
                message.getVariables().get("deliveryDate"),
                equalTo(DATE.getEpochSecond())
        );
        assertThat(message.getVariables().get("address"), equalTo("Москва"));
        assertThat(message.getVariables().get("recipientRegionId"), equalTo(213));
    }
}
