package ru.yandex.market.abo.core.export.promo;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * @author kukabara
 */
public class PromoDynamicExportProcessorTest {

    @Test
    public void testEmpty() throws Exception {
        String actualJson = dump();
        String expectedJson = "{\"PROMOS\" : [], \"PROMO_OFFERS\" : []}";
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    private String dump() throws IOException {
        PromoDynamicExportProcessor dumper = spy(new PromoDynamicExportProcessor());
        doReturn(new PromoDynamicExportModel()).when(dumper).getModelForExport();
        return dumper.getText();
    }
}