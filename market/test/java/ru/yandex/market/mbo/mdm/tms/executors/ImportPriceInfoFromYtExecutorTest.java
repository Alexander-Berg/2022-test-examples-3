package ru.yandex.market.mbo.mdm.tms.executors;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.priceinfo.PriceInfo;
import ru.yandex.market.mbo.mdm.common.priceinfo.PriceInfoRepository;
import ru.yandex.market.mbo.mdm.common.priceinfo.YtPriceInfoReaderMock;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class ImportPriceInfoFromYtExecutorTest extends MdmBaseDbTestClass {
    private static final int BLUE_SUPPLIER_ID = 1;
    private static final int WHITE_SUPPLIER_ID = 2;
    private static final int UNKNOWN_SUPPLIER_ID = 3;

    @Autowired
    private PriceInfoRepository priceInfoRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;

    private ImportPriceInfoFromYtExecutor executor;
    private final YtPriceInfoReaderMock ytReader = new YtPriceInfoReaderMock();

    @Before
    public void setup() {
        executor = new ImportPriceInfoFromYtExecutor(priceInfoRepository, mdmSupplierRepository, ytReader);
        prepareSuppliers();
    }

    @Test
    public void testBlueSupplierPriceInfoStored() {
        int supplierId = BLUE_SUPPLIER_ID;

        PriceInfo yaaayInfo1 = new PriceInfo("Yaaay.", supplierId, 1, LocalDate.now());
        PriceInfo coolerInfo = new PriceInfo("Cooler", supplierId, 1, LocalDate.now());
        PriceInfo cupcakeInfo = new PriceInfo("Cupcake in the eye", supplierId, 1, LocalDate.now());

        ytReader.preparePriceInfo(Arrays.asList(yaaayInfo1, coolerInfo, cupcakeInfo));
        executor.execute();

        assertThat(priceInfoRepository.findAll()).containsExactlyInAnyOrder(yaaayInfo1, coolerInfo, cupcakeInfo);
    }

    @Test
    public void testFilterWhiteSupplierData() {
        int supplierId = WHITE_SUPPLIER_ID;

        PriceInfo yaaayInfo1 = new PriceInfo("Yaaay.", supplierId, 1, LocalDate.now());
        PriceInfo coolerInfo = new PriceInfo("Cooler", supplierId, 1, LocalDate.now());
        PriceInfo cupcakeInfo = new PriceInfo("Cupcake in the eye", supplierId, 1, LocalDate.now());

        ytReader.preparePriceInfo(Arrays.asList(yaaayInfo1, coolerInfo, cupcakeInfo));
        executor.execute();

        assertThat(priceInfoRepository.findAll()).isEmpty();
    }

    @Test
    public void testRetainUnknownSupplierStockInfoStored() {
        int supplierId = UNKNOWN_SUPPLIER_ID;

        PriceInfo yaaayInfo1 = new PriceInfo("Yaaay.", supplierId, 1, LocalDate.now());
        PriceInfo coolerInfo = new PriceInfo("Cooler", supplierId, 1, LocalDate.now());
        PriceInfo cupcakeInfo = new PriceInfo("Cupcake in the eye", supplierId, 1, LocalDate.now());

        ytReader.preparePriceInfo(Arrays.asList(yaaayInfo1, coolerInfo, cupcakeInfo));
        executor.execute();

        assertThat(priceInfoRepository.findAll()).containsExactlyInAnyOrder(yaaayInfo1, coolerInfo, cupcakeInfo);
    }

    @Test
    public void testMixedSupplierStockInfo() {
        PriceInfo yaaayInfo1 = new PriceInfo("Yaaay.", BLUE_SUPPLIER_ID, 1, LocalDate.now());
        PriceInfo coolerInfo = new PriceInfo("Cooler", WHITE_SUPPLIER_ID, 1, LocalDate.now());
        PriceInfo cupcakeInfo =
            new PriceInfo("Cupcake in the eye", UNKNOWN_SUPPLIER_ID, 1, LocalDate.now());

        ytReader.preparePriceInfo(Arrays.asList(yaaayInfo1, coolerInfo, cupcakeInfo));
        executor.execute();

        assertThat(priceInfoRepository.findAll()).containsExactlyInAnyOrder(yaaayInfo1, cupcakeInfo);
    }

    @Test
    public void testDuplicateShopSkuKeysNotSaved() {
        PriceInfo yaaayInfo1 = new PriceInfo("Yaaay.", BLUE_SUPPLIER_ID, 1, LocalDate.now());
        PriceInfo yaaayInfo2 = new PriceInfo("Yaaay.", BLUE_SUPPLIER_ID, 100500, LocalDate.now());
        PriceInfo cupcakeInfo =
            new PriceInfo("Cupcake in the eye", UNKNOWN_SUPPLIER_ID, 1, LocalDate.now());

        ytReader.preparePriceInfo(Arrays.asList(yaaayInfo1, yaaayInfo2, cupcakeInfo));
        executor.execute();

        assertThat(priceInfoRepository.findAll()).containsExactlyInAnyOrder(yaaayInfo2, cupcakeInfo);
    }

    private void prepareSuppliers() {
        MdmSupplier blueSupplier = new MdmSupplier()
            .setId(BLUE_SUPPLIER_ID)
            .setType(MdmSupplierType.THIRD_PARTY);
        MdmSupplier whiteSupplier = new MdmSupplier()
            .setId(WHITE_SUPPLIER_ID)
            .setType(MdmSupplierType.MARKET_SHOP);
        mdmSupplierRepository.insertBatch(blueSupplier, whiteSupplier);
    }
}
