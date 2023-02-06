package ru.yandex.market.partner.content.common.db.jooq.enums;

import org.junit.Test;
import ru.yandex.market.ir.http.PartnerContentUi;

public class GcSkuValidationTypeTest {

    @Test
    public void valuesShouldMatchProtoEnum() {
        ProtoEnumAssert.assertEnumsHaveSameValues(
            GcSkuValidationType.values(),
            PartnerContentUi.GcValidationType.values());
    }
}
