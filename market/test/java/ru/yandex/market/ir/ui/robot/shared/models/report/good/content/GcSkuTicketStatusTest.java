package ru.yandex.market.ir.ui.robot.shared.models.report.good.content;

import org.junit.Test;
import ru.yandex.market.ir.http.PartnerContentUi;
import ru.yandex.market.ir.ui.robot.shared.models.report.EnumAssert;
import ru.yandex.market.robot.shared.models.report.good.content.GcSkuTicketStatus;

public class GcSkuTicketStatusTest {

    @Test
    public void valuesShouldMatchProtoEnum() {
        EnumAssert.assertEnumsHaveSameValues(
            PartnerContentUi.ListGcSkuTicketResponse.Status.values(),
            GcSkuTicketStatus.values());
    }
}
