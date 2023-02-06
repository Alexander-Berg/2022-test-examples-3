package ru.yandex.market.mbo.mdm.common.masterdata.services;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MercuryHashDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MasterDataLogIdService;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MercuryHashRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.ErpExportDataProvider;
import ru.yandex.market.mboc.common.erp.AlreadySentDataFilterService;
import ru.yandex.market.mboc.common.erp.ErpExportService;
import ru.yandex.market.mboc.common.erp.ErpExportServiceImpl;
import ru.yandex.market.mboc.common.erp.ErpShippingUnitExporterDao;
import ru.yandex.market.mboc.common.erp.ErpShippingUnitExporterService;
import ru.yandex.market.mboc.common.erp.model.ErpExportData;
import ru.yandex.market.mboc.common.erp.model.ErpMercurySskuMasterData;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepositoryImpl;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.RealConverter;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MbocCommon;

import static org.mockito.Mockito.times;

/**
 * @author dmserebr
 * @date 20/11/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ErpShippingUnitExporterServiceTest extends MdmBaseDbTestClass {

    private static final int BUSINESS_ID = BeruIdMock.DEFAULT_PROD_BIZ_ID;
    private static final int SUPPLIER_ID = 10;
    private static final String SHOP_SKU_1 = "ssku1";
    private static final String PROCESSOR_NAME = "erpShippingUnitExporterService";

    private static final ShopSkuKey EXTERNAL_SHOP_SKU_KEY = new ShopSkuKey(
        BeruIdMock.DEFAULT_PROD_FP_ID,
        RealConverter.generateSSKU(String.valueOf(SUPPLIER_ID), SHOP_SKU_1)
    );

    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;

    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;

    @Autowired
    private MasterDataRepositoryImpl masterDataRepository;

    @Autowired
    private AlreadySentDataFilterService alreadySentDataFilterService;

    @Autowired
    private MercuryHashRepository mercuryHashRepository;

    @Autowired
    private ReferenceItemRepository referenceItemRepository;

    @Autowired
    private MasterDataLogIdService pgLogIdService;

    @Autowired
    private ErpExportDataProvider erpExportDataProvider;

    @Autowired
    private BeruId beruId;

    private ErpShippingUnitExporterDao erpShippingUnitExporterDao;
    private ErpShippingUnitExporterService shippingUnitExporterService;
    private ErpExportService commonErpExportService;

    @Before
    public void before() {
        erpShippingUnitExporterDao = Mockito.mock(ErpShippingUnitExporterDao.class);
        Mockito.when(erpShippingUnitExporterDao.insertSskuMasterData(Mockito.anyList())).thenAnswer(invocation -> {
            List<ErpMercurySskuMasterData> data = invocation.getArgument(0);
            return data.size();
        });
        MboMappingsService mboMappingsService = Mockito.mock(MboMappingsService.class);
        Mockito.when(mboMappingsService.searchLiteApprovedMappingsByKeys(Mockito.any())).thenAnswer(invocation -> {
            MboMappings.SearchLiteMappingsByKeysRequest request = invocation.getArgument(0);
            List<MbocCommon.MappingInfoLite> mappingInfos = request.getKeysList().stream()
                .map(key -> MbocCommon.MappingInfoLite.newBuilder()
                    .setSupplierId(key.getSupplierId()).setShopSku(key.getShopSku()).build())
                .collect(Collectors.toList());

            return MboMappings.SearchLiteMappingsResponse.newBuilder()
                .addAllMapping(mappingInfos).build();
        });

        shippingUnitExporterService = new ErpShippingUnitExporterService(
            erpShippingUnitExporterDao,
            beruId,
            alreadySentDataFilterService
        );

        commonErpExportService = new ErpExportServiceImpl(
            erpExportDataProvider,
            null,
            shippingUnitExporterService,
            null,
            beruId,
            null
        );

        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID)
            .setType(MdmSupplierType.REAL_SUPPLIER)
            .setRealSupplierId(String.valueOf(SUPPLIER_ID)));
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(BeruIdMock.DEFAULT_PROD_FP_ID)
            .setBusinessId(BUSINESS_ID)
            .setType(MdmSupplierType.FIRST_PARTY));
        mdmSupplierCachingService.refresh();
    }

    @Test
    public void testNoCandidatesForExport() {
        masterDataRepository.insert(new MasterData().setShopSkuKey(EXTERNAL_SHOP_SKU_KEY));
        pgLogIdService.updateModifiedSequence(1);

        shippingUnitExporterService.exportShippingUnitsForMercury(ErpExportData.EMPTY_DATA);

        captureErpInsertMappingsRequest(0);
    }

    @Test
    public void testSingleExportSuccessful() {
        masterDataRepository.insert(new MasterData()
            .setShopSkuKey(EXTERNAL_SHOP_SKU_KEY)
            .setUseInMercury(true).setVetisGuids(List.of("guid")).setCustomsCommodityCode("111"));
        pgLogIdService.updateModifiedSequence(1);

        referenceItemRepository.insert(ItemWrapperTestUtil.createReferenceNetWeightItem(
            EXTERNAL_SHOP_SKU_KEY.getSupplierId(), EXTERNAL_SHOP_SKU_KEY.getShopSku(), 1000
        ));

        commonErpExportService.exportShippingUnits(Set.of(EXTERNAL_SHOP_SKU_KEY), true);

        var expectedItem = new ErpMercurySskuMasterData();
        expectedItem.setSskuId(EXTERNAL_SHOP_SKU_KEY.getShopSku());
        expectedItem.setWeightNetInGrams(1.0);
        expectedItem.setVetisGuids(List.of("guid"));
        expectedItem.setCustomsCommodityCode("111");

        List<ErpMercurySskuMasterData> actualList = captureErpInsertMappingsRequest(1).get(0);
        Assertions.assertThat(actualList).isEqualTo(List.of(expectedItem));

        List<MercuryHashDao> exported = mercuryHashRepository.findAll();
        Assertions.assertThat(exported)
            .isEqualTo(List.of(new MercuryHashDao(EXTERNAL_SHOP_SKU_KEY.getShopSku(), expectedItem.hashCode(),
                    PROCESSOR_NAME)));
    }

    @Ignore("MBO-23658")
    @Test
    public void testNoExportIfCustomCommodityCodeNotSet() {
        masterDataRepository.insert(new MasterData()
            .setShopSkuKey(EXTERNAL_SHOP_SKU_KEY)
            .setUseInMercury(true).setVetisGuids(List.of("guid")));
        referenceItemRepository.insert(ItemWrapperTestUtil.createReferenceNetWeightItem(
            EXTERNAL_SHOP_SKU_KEY.getSupplierId(), EXTERNAL_SHOP_SKU_KEY.getShopSku(), 1000
        ));

        commonErpExportService.exportShippingUnits(Set.of(EXTERNAL_SHOP_SKU_KEY), true);

        Assertions.assertThat(captureErpInsertMappingsRequest(1).get(0)).isEmpty();
    }

    @Test
    public void testSameItemDoesNotExportedTwice() {
        MasterData masterData = new MasterData()
            .setShopSkuKey(EXTERNAL_SHOP_SKU_KEY)
            .setUseInMercury(true).setVetisGuids(List.of("guid")).setCustomsCommodityCode("111");

        masterDataRepository.insert(masterData);
        pgLogIdService.updateModifiedSequence(1);
        referenceItemRepository.insert(ItemWrapperTestUtil.createReferenceNetWeightItem(
            EXTERNAL_SHOP_SKU_KEY.getSupplierId(), EXTERNAL_SHOP_SKU_KEY.getShopSku(), 1000
        ));

        commonErpExportService.exportShippingUnits(Set.of(EXTERNAL_SHOP_SKU_KEY), true);
        commonErpExportService.exportShippingUnits(Set.of(EXTERNAL_SHOP_SKU_KEY), true);

        List<List<ErpMercurySskuMasterData>> captured = captureErpInsertMappingsRequest(1);
        // after the first time the data is exported, after the second is not exported
        // (because all master data is processed)
        Assertions.assertThat(captured.get(0)).hasSize(1);
    }

    @Test
    public void testSameItemDoesNotExportedTwiceIfCountrySet() {
        MasterData masterData = new MasterData()
            .setShopSkuKey(EXTERNAL_SHOP_SKU_KEY)
            .setUseInMercury(true).setVetisGuids(List.of("guid")).setCustomsCommodityCode("111");

        masterDataRepository.insert(masterData);
        pgLogIdService.updateModifiedSequence(1);
        referenceItemRepository.insert(ItemWrapperTestUtil.createReferenceNetWeightItem(
            EXTERNAL_SHOP_SKU_KEY.getSupplierId(), EXTERNAL_SHOP_SKU_KEY.getShopSku(), 1000
        ));

        commonErpExportService.exportShippingUnits(Set.of(EXTERNAL_SHOP_SKU_KEY), true);

        // set manufacturer country
        masterData.setManufacturerCountries(List.of("China"));
        masterDataRepository.update(masterData);
        pgLogIdService.updateModifiedSequence(1);
        commonErpExportService.exportShippingUnits(Set.of(EXTERNAL_SHOP_SKU_KEY), true);

        List<List<ErpMercurySskuMasterData>> captured = captureErpInsertMappingsRequest(1);
        // after the first time the data is exported, after the second is not exported
        Assertions.assertThat(captured.get(0)).hasSize(1);
    }

    @Test
    public void testSameItemExportedTwiceIfGuidChanged() {
        MasterData masterData = new MasterData()
            .setShopSkuKey(EXTERNAL_SHOP_SKU_KEY)
            .setUseInMercury(true).setVetisGuids(List.of("guid")).setCustomsCommodityCode("111");

        masterDataRepository.insert(masterData);
        pgLogIdService.updateModifiedSequence(1);
        referenceItemRepository.insert(ItemWrapperTestUtil.createReferenceNetWeightItem(
            EXTERNAL_SHOP_SKU_KEY.getSupplierId(), EXTERNAL_SHOP_SKU_KEY.getShopSku(), 1000
        ));

        commonErpExportService.exportShippingUnits(Set.of(EXTERNAL_SHOP_SKU_KEY), true);

        // set guid
        masterData.setVetisGuids(List.of("guid2"));
        masterDataRepository.update(masterData);
        pgLogIdService.updateModifiedSequence(1);
        commonErpExportService.exportShippingUnits(Set.of(EXTERNAL_SHOP_SKU_KEY), true);

        List<List<ErpMercurySskuMasterData>> captured = captureErpInsertMappingsRequest(2);
        // after the first time the data is exported, after the second is not exported
        Assertions.assertThat(captured.get(0)).hasSize(1);

        var expectedItem = new ErpMercurySskuMasterData();
        expectedItem.setSskuId(EXTERNAL_SHOP_SKU_KEY.getShopSku());
        expectedItem.setWeightNetInGrams(1.0);
        expectedItem.setVetisGuids(List.of("guid2"));
        expectedItem.setCustomsCommodityCode("111");

        Assertions.assertThat(captured.get(1)).isEqualTo(List.of(expectedItem));

        Assertions.assertThat(mercuryHashRepository.findAll())
            .isEqualTo(List.of(new MercuryHashDao(EXTERNAL_SHOP_SKU_KEY.getShopSku(), expectedItem.hashCode(),
                    PROCESSOR_NAME)));
    }

    @Test
    public void testSameItemExportedTwiceIfGtinChanged() {
        MasterData masterData = new MasterData()
            .setShopSkuKey(EXTERNAL_SHOP_SKU_KEY)
            .setVetisGuids(List.of("guid"))
            .setUseInMercury(true).setGtins(List.of("666")).setCustomsCommodityCode("111");

        masterDataRepository.insert(masterData);
        pgLogIdService.updateModifiedSequence(1);
        referenceItemRepository.insert(ItemWrapperTestUtil.createReferenceNetWeightItem(
            EXTERNAL_SHOP_SKU_KEY.getSupplierId(), EXTERNAL_SHOP_SKU_KEY.getShopSku(), 1000
        ));

        commonErpExportService.exportShippingUnits(Set.of(EXTERNAL_SHOP_SKU_KEY), true);

        // set gtin
        masterData.setGtins(List.of("666", "777"));
        masterDataRepository.update(masterData);
        pgLogIdService.updateModifiedSequence(1);
        commonErpExportService.exportShippingUnits(Set.of(EXTERNAL_SHOP_SKU_KEY), true);

        List<List<ErpMercurySskuMasterData>> captured = captureErpInsertMappingsRequest(2);
        // after the first time the data is exported, after the second is not exported
        Assertions.assertThat(captured.get(0)).hasSize(1);

        var expectedItem = new ErpMercurySskuMasterData();
        expectedItem.setSskuId(EXTERNAL_SHOP_SKU_KEY.getShopSku());
        expectedItem.setWeightNetInGrams(1.0);
        expectedItem.setGtins(List.of("666", "777"));
        expectedItem.setVetisGuids(List.of("guid"));
        expectedItem.setCustomsCommodityCode("111");

        Assertions.assertThat(captured.get(1)).isEqualTo(List.of(expectedItem));

        Assertions.assertThat(mercuryHashRepository.findAll())
            .isEqualTo(List.of(new MercuryHashDao(EXTERNAL_SHOP_SKU_KEY.getShopSku(), expectedItem.hashCode(),
                    PROCESSOR_NAME)));
    }

    @Test
    public void testSameItemExportedTwiceIfWeightChanged() {
        MasterData masterData = new MasterData()
            .setShopSkuKey(EXTERNAL_SHOP_SKU_KEY)
            .setUseInMercury(true).setVetisGuids(List.of("guid")).setCustomsCommodityCode("111");

        masterDataRepository.insert(masterData);
        pgLogIdService.updateModifiedSequence(1);
        referenceItemRepository.insert(ItemWrapperTestUtil.createReferenceNetWeightItem(
            EXTERNAL_SHOP_SKU_KEY.getSupplierId(), EXTERNAL_SHOP_SKU_KEY.getShopSku(), 1000
        ));

        commonErpExportService.exportShippingUnits(Set.of(EXTERNAL_SHOP_SKU_KEY), true);

        // touch masterdata and set weight in reference item repository
        masterDataRepository.update(masterData);
        pgLogIdService.updateModifiedSequence(1);
        referenceItemRepository.update(ItemWrapperTestUtil.createReferenceNetWeightItem(
            EXTERNAL_SHOP_SKU_KEY.getSupplierId(), EXTERNAL_SHOP_SKU_KEY.getShopSku(), 2500
        ));
        commonErpExportService.exportShippingUnits(Set.of(EXTERNAL_SHOP_SKU_KEY), true);

        List<List<ErpMercurySskuMasterData>> captured = captureErpInsertMappingsRequest(2);
        // after the first time the data is exported, after the second is not exported
        Assertions.assertThat(captured.get(0)).hasSize(1);

        var expectedItem = new ErpMercurySskuMasterData();
        expectedItem.setSskuId(EXTERNAL_SHOP_SKU_KEY.getShopSku());
        expectedItem.setWeightNetInGrams(2.5);
        expectedItem.setVetisGuids(List.of("guid"));
        expectedItem.setCustomsCommodityCode("111");

        Assertions.assertThat(captured.get(1)).isEqualTo(List.of(expectedItem));

        Assertions.assertThat(mercuryHashRepository.findAll())
            .isEqualTo(List.of(new MercuryHashDao(EXTERNAL_SHOP_SKU_KEY.getShopSku(), expectedItem.hashCode(),
                    PROCESSOR_NAME)));
    }

    private List<List<ErpMercurySskuMasterData>> captureErpInsertMappingsRequest(int numberOfTimes) {
        //noinspection unchecked
        ArgumentCaptor<List<ErpMercurySskuMasterData>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(erpShippingUnitExporterDao, times(numberOfTimes))
            .insertSskuMasterData(captor.capture());
        var a = captor.getAllValues();
        return captor.getAllValues();
    }
}
