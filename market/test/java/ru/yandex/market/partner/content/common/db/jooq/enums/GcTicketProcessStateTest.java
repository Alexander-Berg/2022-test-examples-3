package ru.yandex.market.partner.content.common.db.jooq.enums;

import org.junit.Test;
import ru.yandex.market.ir.http.PartnerContentUi;

public class GcTicketProcessStateTest {

    @Test
    public void valuesShouldMatchProtoEnum() {
        ProtoEnumAssert.assertEnumsHaveSameValues(
            GcTicketProcessState.values(),
            PartnerContentUi.GcTicketProcess.Status.values());
    }
}
