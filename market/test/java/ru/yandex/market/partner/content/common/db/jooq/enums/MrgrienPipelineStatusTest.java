package ru.yandex.market.partner.content.common.db.jooq.enums;

import org.junit.Test;
import ru.yandex.market.ir.http.PartnerContentUi;

public class MrgrienPipelineStatusTest {

    @Test
    public void valuesShouldMatchProtoEnum() {
        ProtoEnumAssert.assertEnumsHaveSameValues(
            MrgrienPipelineStatus.values(),
            PartnerContentUi.Pipeline.Status.values());
    }
}
