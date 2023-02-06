package ru.yandex.market.abo.cpa.quality.recheck.ticket;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author kukabara
 */
public class RecheckTicketTest {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    public void serializeDeserialize() throws IOException {
        RecheckTicket ticket = new RecheckTicket.Builder()
                .withShopId(1)
                .withType(RecheckTicketType.LITE_CPC)
                .withSynopsis("asbasdf")
                .withSourceId(12L)
                .withCheckMethod(RecheckTicketCheckMethod.BY_SIGHT)
                .withResultComment("asb")
                .withUserComment("asdb")
                .withUserId(1L).build();
        ticket.setId(1234L);
        RecheckTicket deserialized = MAPPER.readValue(MAPPER.writeValueAsString(ticket), RecheckTicket.class);

        assertNotNull(deserialized);
        assertEquals(ticket.getShopId(), deserialized.getShopId());
        assertEquals(ticket.getStatus(), deserialized.getStatus());
        assertEquals(ticket.getId(), deserialized.getId());
        assertEquals(ticket.getType(), deserialized.getType());
    }
}