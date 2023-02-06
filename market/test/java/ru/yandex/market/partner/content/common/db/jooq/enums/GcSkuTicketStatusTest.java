package ru.yandex.market.partner.content.common.db.jooq.enums;

import org.junit.Test;
import ru.yandex.market.ir.http.PartnerContentUi;

public class GcSkuTicketStatusTest {

    @Test
    public void valuesShouldMatchProtoEnum() {
        ProtoEnumAssert.assertEnumsHaveSameValues(
            GcSkuTicketStatus.values(),
            PartnerContentUi.ListGcSkuTicketResponse.Status.values());
    }
}
