package ru.yandex.market.partner.content.common.db.jooq.enums;

import org.junit.Test;
import ru.yandex.market.ir.http.PartnerContentUi;

public class FileProcessStateTest {

    @Test
    public void valuesShouldMatchProtoEnum() {
        ProtoEnumAssert.assertEnumsHaveSameValues(
            FileProcessState.values(),
            PartnerContentUi.FileProcess.Status.values());
    }
}
