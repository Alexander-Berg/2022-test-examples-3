package ru.yandex.market.mboc.common.erp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MasterDataLogIdService;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.ErpExportDataProvider;
import ru.yandex.market.mboc.common.erp.model.ErpLogisticsMasterData;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.SupplyEvent;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataDefaultEnhanceService;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.common.utils.RealConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author amaslak
 */
public class ErpLogisticsMasterDataExporterServiceTest extends MdmBaseDbTestClass {

    private static final int SEED = 129847;
    private static final int BUSINESS_ID = BeruIdMock.DEFAULT_PROD_BIZ_ID;
    private static final int TP_BUSINESS_ID = 645756325;
    private static final int TP_SUPPLIER_ID = 555;
    private static final String RS_ID1 = "RS_ID1";
    private static final String RS_ID2 = "RS_ID2";
    private static final String SSKU1 = "111";
    private static final String SSKU2 = "222";

    private static final ShopSkuKey THIRD_PARTY_KEY = key(TP_SUPPLIER_ID, "222");
    private static final ShopSkuKey FIRST_PARTY_KEY1 = key(BeruIdMock.DEFAULT_PROD_FP_ID, RS_ID1 + "." + SSKU1);
    private static final ShopSkuKey FIRST_PARTY_KEY2 = key(BeruIdMock.DEFAULT_PROD_FP_ID, RS_ID2 + "." + SSKU2);

    @Autowired
    private ErpExportDataProvider erpExportDataProvider;

    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;

    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;

    @Autowired
    private MasterDataRepository masterDataRepository;

    @Autowired
    private AlreadySentDataFilterService alreadySentDataFilterService;

    @Autowired
    private MasterDataLogIdService pgLogIdService;

    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;

    private EnhancedRandom enhancedRandom;
    private ErpExportService erpExportService;
    private ErpLogisticsMasterDataExporterDao erpLogisticsMasterDataExporterDao;

    private static ShopSkuKey key(int supplierId, String shopSku) {
        return new ShopSkuKey(supplierId, shopSku);
    }

    @Before
    public void setUp() {
        StorageKeyValueService storageKeyValueService = new StorageKeyValueServiceMock();
        storageKeyValueService.putValue(MdmProperties.LAST_READ_MASTER_DATA_TO_ERP_EXPORT_MODIFIED_TIMESTAMP,
            LocalDateTime.of(2000, 1, 1, 0, 0).toString());
        Random r = new Random(SEED);
        enhancedRandom = TestDataUtils.defaultRandom(r.nextLong());
        BeruIdMock beruIdMock = new BeruIdMock();
        erpLogisticsMasterDataExporterDao = new ErpLogisticsMasterDataExporterDaoMock();
        ErpLogisticsMasterDataExporterService logisticsMasterDataExporter = new ErpLogisticsMasterDataExporterService(
            erpLogisticsMasterDataExporterDao,
            beruIdMock,
            alreadySentDataFilterService
        );
        erpExportService = new ErpExportServiceImpl(
            erpExportDataProvider,
            Mockito.mock(ErpCCCodeMarkupExporterService.class),
            Mockito.mock(ErpShippingUnitExporterService.class),
            logisticsMasterDataExporter,
            beruIdMock,
            mappingsCacheRepository
        );

        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(TP_BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(TP_SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessEnabled(true)
            .setBusinessId(TP_BUSINESS_ID)
        );
        mdmSupplierRepository.insert(new MdmSupplier().setId(100000)
            .setType(MdmSupplierType.REAL_SUPPLIER)
            .setRealSupplierId(RS_ID1));
        mdmSupplierRepository.insert(new MdmSupplier().setId(200000)
            .setType(MdmSupplierType.REAL_SUPPLIER)
            .setRealSupplierId(RS_ID2));
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(BeruIdMock.DEFAULT_PROD_FP_ID)
            .setBusinessId(BUSINESS_ID)
            .setType(MdmSupplierType.FIRST_PARTY));
        mdmSupplierCachingService.refresh();
    }

    private List<MasterData> generateTestData(int n) {
        List<MasterData> testDataList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ShopSkuKey key = key(BeruIdMock.DEFAULT_PROD_FP_ID,
                RS_ID1 + "." + enhancedRandom.nextObject(String.class));
            MasterData masterData = TestDataUtils.generateMasterData(key, enhancedRandom);
            masterDataRepository.insert(masterData);
            pgLogIdService.updateModifiedSequence(1);
            testDataList.add(masterData);
        }
        return testDataList;
    }

    @Test
    public void whenExportLogisticsMasterDataToErpShouldMatchFieldsCorrectly() {
        List<MasterData> expected = generateTestData(10);

        erpExportService.exportLogisticsMasterData(
            expected.stream().map(MasterData::getShopSkuKey).collect(Collectors.toList()), true
        );

        List<ErpLogisticsMasterData> erpRows = erpLogisticsMasterDataExporterDao.findAll();

        assertThat(erpRows).hasSize(expected.size());

        for (MasterData masterData : expected) {
            ShopSkuKey shopSkuKey = masterData.getShopSkuKey();
            String expectedSsku = RealConverter.generateSSKU(RS_ID1,
                RealConverter.getRealShopSku(shopSkuKey.getShopSku()));

            Optional<ErpLogisticsMasterData> first = erpRows.stream()
                .filter(row -> Objects.equals(row.getSskuId(), expectedSsku))
                .findFirst();

            assertThat(first)
                .matches(Optional::isPresent, "Supplier not found in pbd for test data " + masterData);

            assertSoftly(s -> {
                //noinspection OptionalGetWithoutIsPresent
                List<ErpLogisticsMasterData> actualRow = Collections.singletonList(first.get());
                var assertThat = s.assertThat(actualRow);

                assertThat.extracting(ErpLogisticsMasterData::getRealSupplierId).containsOnly(RS_ID1);
                assertThat.extracting(ErpLogisticsMasterData::getSskuId).containsOnly(expectedSsku);
                assertThat.extracting(ErpLogisticsMasterData::getShipmentQuantum)
                    .containsOnly(masterData.getQuantumOfSupply());
                assertThat.extracting(ErpLogisticsMasterData::getMinShipment).containsOnly(masterData.getMinShipment());
                assertThat.extracting(ErpLogisticsMasterData::getDeliveryTime)
                    .containsOnly(masterData.getDeliveryTime());
                assertThat.extracting(ErpLogisticsMasterData::getWarehouseId)
                    .containsOnly(ErpLogisticsMasterData.DEFAULT_WAREHOUSE_ID);
            });
        }
    }

    @Test
    public void whenExportLogisticsMasterDataToErpShouldUseDefaultsInCaseOfMissingData() {
        MasterData expected = generateTestData(1).get(0);
        expected.setSupplySchedule(List.of());
        expected.setQuantityInPack(0);
        expected.setQuantumOfSupply(0);
        expected.setMinShipment(0);
        expected.setDeliveryTime(0);
        masterDataRepository.update(expected);
        pgLogIdService.updateModifiedSequence(1);

        erpExportService.exportLogisticsMasterData(List.of(expected.getShopSkuKey()), true);

        List<ErpLogisticsMasterData> exportedData = erpLogisticsMasterDataExporterDao.findAll();

        assertThat(exportedData).hasSize(1);

        // расставим ожидаемые дефолты
        expected.setSupplySchedule(MasterDataDefaultEnhanceService.DEFAULT_SUPPLY_SCHEDULE_DAYS
            .stream()
            .map(SupplyEvent::new)
            .collect(Collectors.toList()));
        expected.setQuantityInPack(MasterDataDefaultEnhanceService.DEFAULT_QTY_IN_PACK);
        expected.setQuantumOfSupply(MasterDataDefaultEnhanceService.DEFAULT_QUANTUM_OF_SUPPLY);
        expected.setMinShipment(MasterDataDefaultEnhanceService.DEFAULT_MIN_SHIPMENT);
        expected.setDeliveryTime(MasterDataDefaultEnhanceService.DEFAULT_DELIVERY_TIME);

        ShopSkuKey shopSkuKey = expected.getShopSkuKey();
        String expectedSsku = RealConverter.generateSSKU(RS_ID1, RealConverter.getRealShopSku(shopSkuKey.getShopSku()));

        Optional<ErpLogisticsMasterData> first = exportedData.stream()
            .filter(row -> Objects.equals(row.getSskuId(), expectedSsku))
            .findFirst();

        assertThat(first)
            .matches(Optional::isPresent, "Supplier not found in pbd for test data " + expected);

        assertSoftly(s -> {
            //noinspection OptionalGetWithoutIsPresent
            List<ErpLogisticsMasterData> actualRow = Collections.singletonList(first.get());
            var assertThat = s.assertThat(actualRow);

            assertThat.extracting(ErpLogisticsMasterData::getRealSupplierId).containsOnly(RS_ID1);
            assertThat.extracting(ErpLogisticsMasterData::getSskuId).containsOnly(expectedSsku);
            assertThat.extracting(ErpLogisticsMasterData::getShipmentQuantum)
                .containsOnly(expected.getQuantumOfSupply());
            assertThat.extracting(ErpLogisticsMasterData::getMinShipment).containsOnly(expected.getMinShipment());
            assertThat.extracting(ErpLogisticsMasterData::getDeliveryTime).containsOnly(expected.getDeliveryTime());
            assertThat.extracting(ErpLogisticsMasterData::getQuantityInPack).containsOnly(expected.getQuantityInPack());
            assertThat.extracting(ErpLogisticsMasterData::getWarehouseId)
                .containsOnly(ErpLogisticsMasterData.DEFAULT_WAREHOUSE_ID);
        });
    }

    @Test
    public void whenExportLogisticsMasterDataShouldExportOnlyRealSupplierOffers() {
        MasterData masterData1 = TestDataUtils.generateMasterData(THIRD_PARTY_KEY, enhancedRandom);
        MasterData masterData2 = TestDataUtils.generateMasterData(FIRST_PARTY_KEY1, enhancedRandom);
        MasterData masterData3 = TestDataUtils.generateMasterData(FIRST_PARTY_KEY2, enhancedRandom);
        masterDataRepository.insertBatch(Arrays.asList(masterData1, masterData2, masterData3));
        pgLogIdService.updateModifiedSequence(3);

        erpExportService.exportLogisticsMasterData(List.of(FIRST_PARTY_KEY1, FIRST_PARTY_KEY2), true);

        List<ErpLogisticsMasterData> exportedData = erpLogisticsMasterDataExporterDao.findAll();
        List<String> exportedSuppliers = exportedData.stream()
            .map(ErpLogisticsMasterData::getRealSupplierId)
            .collect(Collectors.toList());

        assertThat(exportedSuppliers).containsExactlyInAnyOrder(RS_ID1, RS_ID2);
    }
}
