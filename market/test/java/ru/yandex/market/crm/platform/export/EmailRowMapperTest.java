package ru.yandex.market.crm.platform.export;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.CrmInfo;
import ru.yandex.market.crm.platform.commons.SendingType;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Email;
import ru.yandex.market.crm.platform.models.Email.Click;
import ru.yandex.market.crm.platform.models.Email.DeliveryStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author apershukov
 */
class EmailRowMapperTest {

    private final EmailRowMapper mapper = new EmailRowMapper();

    @Test
    void testMapNewEmailFact() {
        var fact = factBuilder()
                .setSendingType(SendingType.PROMO)
                .setCrmInfo(
                        CrmInfo.newBuilder()
                                .setSendingId("test_sending")
                )
                .setDeliveryStatus(DeliveryStatus.UPLOADED)
                .build();

        var row = mapper.apply(fact);
        assertNotNull(row);
        assertEquals("user@yandex.ru", row.getString("email"));
        assertEquals("sending_id#123", row.getString("fact_id"));
        assertEquals(112233, row.getLong("timestamp"));
        assertEquals(1, row.getLong("sending_type"));
        assertEquals("test_sending", row.getString("sending_id"));
        assertFalse(row.getBool("was_delivered"));
        assertEquals(0, row.getLong("opens_count"));
        assertEquals(0, row.getLong("clicks_count"));
    }

    @Test
    void testMapDeliveredEmail() {
        var fact = factBuilder()
                .setDeliveryStatus(DeliveryStatus.DELIVERED)
                .build();

        var row = mapper.apply(fact);
        assertNotNull(row);
        assertTrue(row.getBool("was_delivered"));
    }

    @Test
    void testMapOpenedEmail() {
        var fact = factBuilder()
                .addOpenTime(223344)
                .addOpenTime(556677)
                .build();

        var row = mapper.apply(fact);
        assertNotNull(row);
        assertEquals(2, row.getLong("opens_count"));
    }

    @Test
    void testMapEmailWithMultipleClicks() {
        var fact = factBuilder()
                .addClick(Click.newBuilder().setTimestamp(11223344))
                .addClick(Click.newBuilder().setTimestamp(22334455))
                .addClick(Click.newBuilder().setTimestamp(33445655))
                .build();

        var row = mapper.apply(fact);
        assertNotNull(row);
        assertEquals(3, row.getLong("clicks_count"));
    }

    @Test
    void testSendingIdForTriggerEmail() {
        var crmInfo = CrmInfo.newBuilder()
                .setTriggerId("trigger_key:11")
                .build();

        var fact = factBuilder(SendingType.TRIGGER, crmInfo)
                .build();

        var row = mapper.apply(fact);
        assertNotNull(row);
        assertEquals(SendingType.TRIGGER.getNumber(), row.getLong("sending_type"));
        assertEquals("trigger_key", row.getString("sending_id"));
    }

    @Test
    void testDoNotFillSendingIdForUnknownSendingTypes() {
        var fact = factBuilder(SendingType.OTHER, CrmInfo.newBuilder().build())
                .build();

        var row = mapper.apply(fact);
        assertNotNull(row);
        assertEquals(SendingType.OTHER.getNumber(), row.getLong("sending_type"));
        assertFalse(row.containsKey("sending_id"));
    }

    private static Email.Builder factBuilder(SendingType sendingType, CrmInfo crmInfo) {
        return Email.newBuilder()
                .setUid(Uids.create(UidType.EMAIL, "user@yandex.ru"))
                .setFactId("sending_id#123")
                .setTimestamp(112233)
                .setSendingType(sendingType)
                .setCrmInfo(crmInfo);
    }

    private static Email.Builder factBuilder() {
        return factBuilder(SendingType.PROMO, CrmInfo.newBuilder().build());
    }
}
