package ru.yandex.market.crm.triggers.services.bpm.messages.les;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.crm.core.services.phone.PhoneService;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.lom.OrderTransportationRecipientEvent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.SMS_PHONE_OR_PERSONAL_PHONE_ID;

public class OrderTransportationRecipientEventConverterTest {

    private static final Uid PERSONAL_PHONE_ID_UID = Uid.asPersonalPhoneId("personalPhoneId");

    private final PhoneService phoneService = Mockito.mock(PhoneService.class);
    private final OrderTransportationRecipientEventConverter converter =
            new OrderTransportationRecipientEventConverter(phoneService);

    @Before
    public void setUp() {
        when(phoneService.getPhoneUid(nullable(String.class), anyString())).thenReturn(PERSONAL_PHONE_ID_UID);
    }

    @Test
    public void orderTransportationEvent() {
        Event event = new Event(
                "lom",
                "1",
                1L,
                "ORDER_TRANSPORTATION_RECIPIENT",
                new OrderTransportationRecipientEvent(
                        123L,
                        "234-LO-345",
                        "+79999999999"
                ),
                null
        );
        UidBpmMessage message = converter.convert(event);

        assertThat(message.getType(), equalTo("LOM_ORDER_TRANSPORTATION_RECIPIENT"));
        assertThat(message.getCorrelationVariables(), equalTo(Map.of("orderId", 123L)));

        assertThat(message.getVariables().get("barcode"), equalTo("234-LO-345"));
        assertThat(message.getVariables().get("recipientPhone"), equalTo("+79999999999"));
        assertThat(message.getVariables().get(SMS_PHONE_OR_PERSONAL_PHONE_ID), equalTo(PERSONAL_PHONE_ID_UID));
    }
}
