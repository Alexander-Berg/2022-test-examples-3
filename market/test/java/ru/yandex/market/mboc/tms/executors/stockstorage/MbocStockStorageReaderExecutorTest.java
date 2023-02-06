package ru.yandex.market.mboc.tms.executors.stockstorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.stockstorage.MbocStockInfo;
import ru.yandex.market.mboc.common.dict.stockstorage.MbocStockRepository;
import ru.yandex.market.mboc.common.services.stockstorage.YtStockInfo;
import ru.yandex.market.mboc.common.services.stockstorage.YtStockStorageReaderMock;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.RealConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.tms.executors.stockstorage.MbocStockStorageReaderExecutor.convertToMbocStockInfo;

@SuppressWarnings("checkstyle:MagicNumber")
public class MbocStockStorageReaderExecutorTest extends BaseDbTestClass {

    private static final int BERU_ID = 12345;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private MbocStockRepository mbocStockRepository;

    private MbocStockStorageReaderExecutor executor;
    private ru.yandex.market.mboc.common.services.stockstorage.YtStockStorageReaderMock ytReader;

    @Before
    public void setup() {
        ytReader = new YtStockStorageReaderMock();
        executor = new MbocStockStorageReaderExecutor(
            ytReader, mbocStockRepository, supplierRepository, BERU_ID);
    }

    @Test
    public void testFirstPartyOffersExpanded() {
        int notRealSupplierId = OfferTestUtils.TEST_SUPPLIER_ID;
        int supplierId1 = OfferTestUtils.TEST_SUPPLIER_ID + 1;
        int supplierId2 = supplierId1 + 1;
        String realId1 = "0023";
        String realId2 = "0024";
        supplierRepository.insert(OfferTestUtils.simpleSupplier()); // 3P supplier
        supplierRepository.insert(OfferTestUtils.simpleSupplier().setType(MbocSupplierType.REAL_SUPPLIER)
            .setId(supplierId1).setRealSupplierId(realId1));
        supplierRepository.insert(OfferTestUtils.simpleSupplier().setType(MbocSupplierType.REAL_SUPPLIER)
            .setId(supplierId2).setRealSupplierId(realId2));

        YtStockInfo normalInfo = new YtStockInfo().setSupplierId(notRealSupplierId).setShopSku("Гав")
            .setWarehouseId(14).setEnabled(true).setFit(4).setShelfLife(11);
        YtStockInfo realInfo1 = new YtStockInfo().setSupplierId(BERU_ID)
            .setShopSku(RealConverter.generateSSKU(realId1, "Мяу"))
            .setWarehouseId(91).setEnabled(false).setFit(5).setFreezed(2);
        YtStockInfo realInfo2 = new YtStockInfo().setSupplierId(BERU_ID)
            .setShopSku(RealConverter.generateSSKU(realId2, "Муу"))
            .setWarehouseId(2).setEnabled(true).setFit(4);

        ytReader.prepareStockInfo(Arrays.asList(normalInfo, realInfo1, realInfo2));
        executor.execute();

        assertThat(mbocStockRepository.findAll())
            .usingElementComparatorIgnoringFields("fitAppearDate", "updatedTs", "fitDisappearDate")
            .containsExactlyInAnyOrder(
                convertToMbocStockInfo(normalInfo),
                convertToMbocStockInfo(realInfo1.setSupplierId(supplierId1).setShopSku("Мяу")),
                convertToMbocStockInfo(realInfo2.setSupplierId(supplierId2).setShopSku("Муу"))
            );
    }

    @Test
    public void shouldInsertNewStocks() {
        int supplierId = OfferTestUtils.TEST_SUPPLIER_ID;
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        YtStockInfo info1 = new YtStockInfo().setSupplierId(supplierId)
            .setShopSku("Yaaay1")
            .setWarehouseId(10).setEnabled(true).setFit(1).setShelfLife(16);
        YtStockInfo info2 = new YtStockInfo().setSupplierId(supplierId)
            .setShopSku("Yaaay2")
            .setWarehouseId(10).setEnabled(true).setFit(4).setShelfLife(11);
        List<MbocStockInfo> infos = new ArrayList<>(Arrays.asList(convertToMbocStockInfo(info1),
            convertToMbocStockInfo(info2)));

        ytReader.prepareStockInfo(Arrays.asList(info1, info2));
        executor.execute();

        YtStockInfo newInfo1 = new YtStockInfo().setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
            .setShopSku("Yaaay3")
            .setWarehouseId(10).setEnabled(true).setFit(4).setShelfLife(11);
        YtStockInfo newInfo2 = new YtStockInfo().setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
            .setShopSku("Yaaay4")
            .setWarehouseId(10).setEnabled(true).setFit(0).setShelfLife(11);
        infos.addAll(Arrays.asList(convertToMbocStockInfo(newInfo1), convertToMbocStockInfo(newInfo2)));

        ytReader.prepareStockInfo(Arrays.asList(info1, info2, newInfo1, newInfo2));
        executor.execute();

        Assertions.assertThat(mbocStockRepository.findAll())
            .usingElementComparatorIgnoringFields("fitAppearDate", "updatedTs", "fitDisappearDate")
            .containsExactlyInAnyOrderElementsOf(infos);

        MbocStockInfo newInfoById1 = mbocStockRepository.findById(convertToMbocStockInfo(newInfo1).getKey());
        Assertions.assertThat(newInfoById1.getFitAppearDate()).isNotNull();
        Assertions.assertThat(newInfoById1.getFitDisappearDate()).isNull();

        MbocStockInfo newInfoById2 = mbocStockRepository.findById(convertToMbocStockInfo(newInfo2).getKey());
        Assertions.assertThat(newInfoById2.getFitAppearDate()).isNull();
        Assertions.assertThat(newInfoById2.getFitDisappearDate()).isNull();
    }

    @Test
    public void shouldUpdateOnlyChangedStocks() {
        int supplierId = OfferTestUtils.TEST_SUPPLIER_ID;
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        YtStockInfo info1 = new YtStockInfo().setSupplierId(supplierId)
            .setShopSku("Yaaay1")
            .setWarehouseId(10)
            .setFit(0);
        YtStockInfo info2 = new YtStockInfo().setSupplierId(supplierId)
            .setShopSku("Yaaay2")
            .setWarehouseId(10)
            .setFit(4);
        mbocStockRepository.insertBatch(Arrays.asList(convertToMbocStockInfo(info1), convertToMbocStockInfo(info2)));

        YtStockInfo newInfo1 = info1.copy()
            .setFit(20);
        YtStockInfo newInfo2 = info2.copy()
            // updating technical fields should not lead to changes
            .setFfUpdatedTs(DateTimeUtils.dateTimeNow().plusMinutes(30));
        ytReader.prepareStockInfo(Arrays.asList(newInfo1, newInfo2));

        executor.execute();

        Assertions.assertThat(mbocStockRepository.findAll())
            .usingElementComparatorIgnoringFields("fitAppearDate", "updatedTs", "fitDisappearDate")
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(
                convertToMbocStockInfo(newInfo1), convertToMbocStockInfo(info2)));
    }

    @Test
    public void shouldChangeEmptyFitTsIfFitChanged() {
        int supplierId = OfferTestUtils.TEST_SUPPLIER_ID;
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        YtStockInfo info1 = new YtStockInfo().setSupplierId(supplierId)
            .setShopSku("Yaaay1")
            .setWarehouseId(10)
            .setFit(0);
        YtStockInfo info2 = new YtStockInfo().setSupplierId(supplierId)
            .setShopSku("Yaaay2")
            .setWarehouseId(10)
            .setFit(4);
        YtStockInfo info22 = new YtStockInfo().setSupplierId(supplierId)
            .setShopSku("Yaaay22")
            .setWarehouseId(10)
            .setFit(4);
        YtStockInfo info3 = new YtStockInfo().setSupplierId(supplierId)
            .setShopSku("Yaaay3")
            .setWarehouseId(10)
            .setFit(5);
        YtStockInfo info4 = new YtStockInfo().setSupplierId(supplierId)
            .setShopSku("Yaaay4")
            .setWarehouseId(10)
            .setFit(0);
        ytReader.prepareStockInfo(Arrays.asList(info1, info2, info22, info3, info4));
        executor.execute();

        YtStockInfo newInfo1 = info1.copy()
            .setFit(1);
        YtStockInfo newInfo2 = info2.copy()
            .setFit(0);
        YtStockInfo newInfo22 = info22.copy()
            .setFit(0);
        YtStockInfo newInfo3 = info3.copy()
            // still positive, will not affect empty fit ts
            .setFit(2);
        YtStockInfo newInfo4 = info4.copy()
            // fit is still 0
            .setFreezed(4);
        ytReader.prepareStockInfo(Arrays.asList(newInfo1, newInfo2, newInfo22, newInfo3, newInfo4));

        executor.execute();

        Assertions.assertThat(mbocStockRepository.findById(convertToMbocStockInfo(newInfo1).getKey())
            .getFitDisappearDate()).isNull();
        Assertions.assertThat(mbocStockRepository.findById(convertToMbocStockInfo(newInfo1).getKey())
            .getFitAppearDate()).isNotNull();
        Assertions.assertThat(mbocStockRepository.findById(convertToMbocStockInfo(newInfo2).getKey())
            .getFitDisappearDate()).isNotNull();
        Assertions.assertThat(mbocStockRepository.findById(convertToMbocStockInfo(newInfo22).getKey())
            .getFitDisappearDate()).isNotNull();
        Assertions.assertThat(mbocStockRepository.findById(convertToMbocStockInfo(newInfo2).getKey())
            .getFitAppearDate()).isNotNull();
        Assertions.assertThat(mbocStockRepository.findById(convertToMbocStockInfo(newInfo3).getKey())
            .getFitDisappearDate()).isNull();
        Assertions.assertThat(mbocStockRepository.findById(convertToMbocStockInfo(newInfo3).getKey())
            .getFitAppearDate()).isNotNull();
        Assertions.assertThat(mbocStockRepository.findById(convertToMbocStockInfo(newInfo4).getKey())
            .getFitDisappearDate()).isNull();
        Assertions.assertThat(mbocStockRepository.findById(convertToMbocStockInfo(newInfo4).getKey())
            .getFitAppearDate()).isNull();
    }
}
