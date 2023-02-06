package ru.yandex.market.abo.core.export.crm;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

/**
 * @author artemmz
 *         created on 17.04.17.
 */
public class CrmShopExportProcessorTest extends EmptyTest {
    @Autowired
    private CrmExportProcessor crmExportProcessor;

    @Test
    @Disabled
    public void runReal() throws Exception {
        crmExportProcessor.export();
    }
}