package ru.yandex.market.crm.triggers.services.bpm.messages.les;

import java.time.LocalDate;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.crm.core.services.phone.PhoneService;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.lom.DeliveryDateUpdated;
import ru.yandex.market.logistics.les.lom.DeliveryInterval;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.SMS_PHONE_OR_PERSONAL_PHONE_ID;

public class DeliveryDateUpdatedEventConverterTest {

    private static final Uid PERSONAL_PHONE_ID_UID = Uid.asPersonalPhoneId("personalPhoneId");
    private static final LocalDate DATE = LocalDate.of(2021, 1, 1);

    private final PhoneService phoneService = Mockito.mock(PhoneService.class);
    private final DeliveryDateUpdatedEventConverter converter = new DeliveryDateUpdatedEventConverter(phoneService);

    @Before
    public void setUp() {
        when(phoneService.getPhoneUid(nullable(String.class), anyString())).thenReturn(PERSONAL_PHONE_ID_UID);
    }

    @Test
    public void deliveryDateUpdatedEvent() {
        Event event = new Event(
                "lom",
                "1",
                1L,
                "DELIVERY_DATE_UPDATED",
                new DeliveryDateUpdated(
                        123L,
                        "234-LO-345",
                        "+79999999999",
                        new DeliveryInterval(DATE, DATE, null, null, null),
                        null,
                        null
                ),
                null
        );
        UidBpmMessage message = converter.convert(event);

        assertThat(message.getType(), equalTo("LOM_DELIVERY_DATE_UPDATED"));
        assertThat(message.getCorrelationVariables(), equalTo(Map.of("orderId", 123L)));

        assertThat(message.getVariables().get("barcode"), equalTo("234-LO-345"));
        assertThat(message.getVariables().get("recipientPhone"), equalTo("+79999999999"));
        assertThat(message.getVariables().get(SMS_PHONE_OR_PERSONAL_PHONE_ID), equalTo(PERSONAL_PHONE_ID_UID));
        assertThat(message.getVariables().get("deliveryDateMin"), equalTo(DATE));
        assertThat(message.getVariables().get("deliveryDateMax"), equalTo(DATE));
        assertThat(message.getVariables().get("reason"), equalTo(""));
    }

    @Test
    public void deliveryDateUpdatedEventWithReason() {
        Event event = new Event(
                "lom",
                "1",
                1L,
                "DELIVERY_DATE_UPDATED",
                new DeliveryDateUpdated(
                        123L,
                        "234-LO-345",
                        "+79999999999",
                        new DeliveryInterval(DATE, DATE, null, null, null),
                        null,
                        "DELIVERY_DATE_UPDATED_BY_DELIVERY"
                ),
                null
        );
        UidBpmMessage message = converter.convert(event);

        assertThat(message.getType(), equalTo("LOM_DELIVERY_DATE_UPDATED"));
        assertThat(message.getCorrelationVariables(), equalTo(
                ImmutableMap.<String, Object>builder()
                        .put("orderId", 123L)
                        .build()
        ));

        assertThat(message.getVariables().get("barcode"), equalTo("234-LO-345"));
        assertThat(message.getVariables().get("recipientPhone"), equalTo("+79999999999"));
        assertThat(message.getVariables().get(SMS_PHONE_OR_PERSONAL_PHONE_ID), equalTo(PERSONAL_PHONE_ID_UID));
        assertThat(message.getVariables().get("deliveryDateMin"), equalTo(DATE));
        assertThat(message.getVariables().get("deliveryDateMax"), equalTo(DATE));
        assertThat(message.getVariables().get("reason"), equalTo("DELIVERY_DATE_UPDATED_BY_DELIVERY"));
    }
}
