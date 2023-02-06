package ru.yandex.market.ir.ui.robot.shared.models.report.partner.content;

import org.junit.Test;
import ru.yandex.market.ir.http.PartnerContentUi;
import ru.yandex.market.ir.ui.robot.shared.models.report.EnumAssert;
import ru.yandex.market.robot.shared.models.report.partner.content.PipelineStatus;

public class PipelineStatusTest {

    @Test
    public void valuesShouldMatchProtoEnum() {
        EnumAssert.assertEnumsHaveSameValues(
            PartnerContentUi.Pipeline.Status.values(),
            PipelineStatus.values());
    }
}
