package ru.yandex.market.clab.common.service.barcode;

import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.clab.db.jooq.generated.tables.records.SsBarcodeRecord;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 * @since 19.11.2018
 */
public class SsBarcodeRepositoryImplTestPgaas extends BasePgaasIntegrationTest {

    @Autowired
    private SsBarcodeRepository ssBarcodeRepository;

    @Autowired
    private DSLContext dsl;

    @Before
    public void setUp() {
        List<SsBarcodeRecord> newBarcodes = Arrays.asList(
            new SsBarcodeRecord("barcode11", 1L),
            new SsBarcodeRecord("barcode12", 1L),
            new SsBarcodeRecord("barcode13", 1L),
            new SsBarcodeRecord("barcode21", 2L),
            new SsBarcodeRecord("barcode22", 2L),
            new SsBarcodeRecord("barcode22", 4L)
        );

        dsl.batchInsert(newBarcodes).execute();
    }

    @Test
    public void testGetGoodId() {
        List<Long> goodIds = ssBarcodeRepository.getGoodIds("barcode12");
        assertThat(goodIds).containsExactly(1L);
    }

    @Test
    public void testGetMissingGoodId() {
        List<Long> goodId = ssBarcodeRepository.getGoodIds("barcode123");
        assertThat(goodId).isEmpty();
    }

    @Test
    public void testGetMultipleGoodId() {
        List<Long> goodIds = ssBarcodeRepository.getGoodIds("barcode22");
        assertThat(goodIds).containsExactlyInAnyOrder(2L, 4L);
    }

    @Test
    public void testGetBarcodes() {
        List<GoodBarcodes> ssBarcodes = ssBarcodeRepository.getBarcodes(Arrays.asList(1L, 2L, 3L));
        GoodBarcodes good1Barcodes = new GoodBarcodes(1L, "barcode11", "barcode12", "barcode13");
        GoodBarcodes good2Barcodes = new GoodBarcodes(2L, "barcode21", "barcode22");
        GoodBarcodes good3Barcodes = new GoodBarcodes(3L);
        assertThat(ssBarcodes).containsExactlyInAnyOrder(good1Barcodes, good2Barcodes, good3Barcodes);
    }
}
