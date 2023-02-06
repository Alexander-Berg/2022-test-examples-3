package ru.yandex.market.partner.content.common.utils;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.ir.http.PartnerContentApi;
import ru.yandex.market.partner.content.common.db.jooq.enums.TicketStatus;

public class TicketStatusUtilsTest {

    @Test
    public void convertApiStatusToTicketStatus() {
        for (PartnerContentApi.TicketStatus value : PartnerContentApi.TicketStatus.values()) {
            TicketStatus[] ticketStatuses = TicketStatusUtils.convertToTicketStatus(value);

            Assert.assertNotNull(ticketStatuses);
            Assert.assertTrue(ticketStatuses.length > 0);
        }
    }
}