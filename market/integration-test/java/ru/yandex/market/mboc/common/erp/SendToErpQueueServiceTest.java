package ru.yandex.market.mboc.common.erp;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToErpQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.service.queue.SendToErpQueueService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.erp.model.ErpCCCodeMarkupChange;
import ru.yandex.market.mboc.common.erp.model.ErpLogisticsMasterData;
import ru.yandex.market.mboc.common.erp.model.ErpMercurySskuMasterData;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.SupplyEvent;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.GeobaseCountry;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.GeobaseCountryUtil;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SendToErpQueueServiceTest extends MdmBaseIntegrationTestClass {
    @Autowired
    private ErpCCCodeMarkupExporterRepository erpCCCodeMarkupExporterRepository;
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private BeruId beruId;
    @Autowired
    private MskuRepository mdmMskuRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private SendToErpQueueRepository sendToErpQueueRepository;
    @Autowired
    private ErpExportService erpExportService;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private ErpShippingUnitExporterDao erpShippingUnitExporterDao;
    @Autowired
    private ErpLogisticsMasterDataExporterDao erpLogisticsMasterDataExporterDao;

    private SendToErpQueueService sendToErpQueueService;

    @Before
    public void setUp() throws Exception {
        sendToErpQueueService = new SendToErpQueueService(
            sendToErpQueueRepository,
            erpExportService,
            storageKeyValueService
        );

        MdmSupplier beruSupplier = new MdmSupplier()
            .setId(beruId.getId())
            .setType(MdmSupplierType.FIRST_PARTY)
            .setBusinessEnabled(true)
            .setBusinessId(beruId.getBusinessId());
        MdmSupplier beruBusiness = new MdmSupplier()
            .setId(beruId.getBusinessId())
            .setType(MdmSupplierType.BUSINESS);
        mdmSupplierRepository.insertBatch(beruSupplier, beruBusiness);

        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        storageKeyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        storageKeyValueService.invalidateCache();
    }

    @Test
    public void testQueueProcessing() {
        // given
        String shopSku = "9876.5678";
        long mskuId = 819;
        ShopSkuKey shopSkuKey = new ShopSkuKey(beruId.getId(), shopSku);

        MappingCacheDao mapping = new MappingCacheDao()
            .setMskuId(mskuId)
            .setShopSkuKey(shopSkuKey);
        mappingsCacheRepository.insert(mapping);

        String customCommCodePrefix = "pax.romana";
        MskuParamValue mskuParamValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(mskuId)
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString(customCommCodePrefix);
        var msku = new CommonMsku(
            mskuId,
            List.of(mskuParamValue)
        );
        mdmMskuRepository.insertOrUpdateMsku(msku);

        MasterData masterData = new MasterData()
            .setShopSkuKey(shopSkuKey)
            .setManufacturerCountries(List.of("Гондурас"));
        masterDataRepository.insertOrUpdate(masterData);
        ReferenceItemWrapper referenceItem = ItemWrapperTestUtil.createSurplusCisReferenceItem(
            shopSkuKey,
            MdmIrisPayload.SurplusHandleMode.ACCEPT,
            MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED
        );
        referenceItemRepository.insertOrUpdate(referenceItem);

        // when
        sendToErpQueueRepository.enqueue(shopSkuKey, MdmEnqueueReason.DEVELOPER_TOOL);
        sendToErpQueueService.processQueueItems();

        //then
        Assertions.assertThat(sendToErpQueueRepository.getUnprocessedItemsCount()).isZero();

        List<ErpCCCodeMarkupChange> erpRows = erpCCCodeMarkupExporterRepository.findAll();
        Assertions.assertThat(erpRows).hasSize(1);
        ErpCCCodeMarkupChange change = erpRows.iterator().next();

        Assertions.assertThat(change.getShopSku()).isEqualTo(shopSku);
        Assertions.assertThat(change.getPrefixHSCode()).isEqualTo(customCommCodePrefix);
        List<String> expectedCountries = masterData.getManufacturerCountries().stream()
            .map(GeobaseCountryUtil::countryByName)
            .flatMap(Optional::stream)
            .map(GeobaseCountry::getIsoName)
            .collect(Collectors.toList());
        Assertions.assertThat(change.getManufacturerCountries()).containsExactlyInAnyOrderElementsOf(expectedCountries);
        Assertions.assertThat(change.getCisHandleMode()).isEqualTo(MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED);
    }

    @Test
    public void whenNoSignificantChangeNotSendToErp() {
        // given
        String shopSku = "9876.5678";
        long mskuId = 819;
        ShopSkuKey shopSkuKey = new ShopSkuKey(beruId.getId(), shopSku);

        MappingCacheDao mapping = new MappingCacheDao()
            .setMskuId(mskuId)
            .setShopSkuKey(shopSkuKey);
        mappingsCacheRepository.insert(mapping);

        String customCommCodePrefix = "pax.romana";
        MskuParamValue mskuParamValue = (MskuParamValue) new MskuParamValue()
            .setMskuId(mskuId)
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString(customCommCodePrefix);
        var msku = new CommonMsku(
            mskuId,
            List.of(mskuParamValue)
        );
        mdmMskuRepository.insertOrUpdateMsku(msku);

        MasterData masterData = new MasterData()
            .setShopSkuKey(shopSkuKey)
            .setManufacturerCountries(List.of("Гондурас"));
        masterDataRepository.insertOrUpdate(masterData);

        // when
        sendToErpQueueRepository.enqueue(shopSkuKey, MdmEnqueueReason.DEVELOPER_TOOL);
        sendToErpQueueService.processQueueItems();
        List<ErpCCCodeMarkupChange> firstIterationErpRows = erpCCCodeMarkupExporterRepository.findAll();

        sendToErpQueueRepository.enqueue(shopSkuKey, MdmEnqueueReason.DEVELOPER_TOOL);
        sendToErpQueueService.processQueueItems();
        List<ErpCCCodeMarkupChange> secondIterationErpRows = erpCCCodeMarkupExporterRepository.findAll();

        //then
        Assertions.assertThat(secondIterationErpRows).hasSize(1);
        Assertions.assertThat(secondIterationErpRows).containsExactlyInAnyOrderElementsOf(firstIterationErpRows);
    }

    @Test
    public void testSendMercuryShippingUnits() {
        // given
        String shopSku1 = "9876.5678";
        String shopSku2 = "9876.1234";
        ShopSkuKey shopSkuKey1 = new ShopSkuKey(beruId.getId(), shopSku1);
        ShopSkuKey shopSkuKey2 = new ShopSkuKey(beruId.getBusinessId(), shopSku2);

        masterDataRepository.insertBatch(
            new MasterData()
                .setShopSkuKey(shopSkuKey1)
                .setUseInMercury(true).setVetisGuids(List.of("guid1")).setCustomsCommodityCode("111"),
            new MasterData()
                .setShopSkuKey(shopSkuKey2)
                .setUseInMercury(true).setVetisGuids(List.of("guid2")).setCustomsCommodityCode("222")
        );

        referenceItemRepository.insertBatch(
            ItemWrapperTestUtil.createReferenceNetWeightItem(
                shopSkuKey1.getSupplierId(), shopSku1, 1000
            ),
            ItemWrapperTestUtil.createReferenceNetWeightItem(
                shopSkuKey2.getSupplierId(), shopSku2, 2000
            )
        );

        // when
        sendToErpQueueRepository.enqueue(shopSkuKey1, MdmEnqueueReason.DEVELOPER_TOOL);
        sendToErpQueueRepository.enqueue(shopSkuKey2, MdmEnqueueReason.DEVELOPER_TOOL);
        sendToErpQueueService.processQueueItems();

        // then
        var expectedItem1 = new ErpMercurySskuMasterData();
        expectedItem1.setSskuId(shopSku1);
        expectedItem1.setWeightNetInGrams(1.0);
        expectedItem1.setVetisGuids(List.of("guid1"));
        expectedItem1.setCustomsCommodityCode("111");

        var expectedItem2 = new ErpMercurySskuMasterData();
        expectedItem2.setSskuId(shopSku2);
        expectedItem2.setWeightNetInGrams(2.0);
        expectedItem2.setVetisGuids(List.of("guid2"));
        expectedItem2.setCustomsCommodityCode("222");

        List<ErpMercurySskuMasterData> actualList = erpShippingUnitExporterDao.findAll();
        Assertions.assertThat(actualList).containsExactlyInAnyOrder(expectedItem1, expectedItem2);
    }

    @Test
    public void testSendingLogisticMasterData() {
        // given
        String realSupplierId = "9876";
        String shopSku = realSupplierId + ".5678";
        ShopSkuKey shopSkuKey = new ShopSkuKey(beruId.getId(), shopSku);
        MasterData masterData =  new MasterData()
            .setShopSkuKey(shopSkuKey)
            .setMinShipment(10)
            .setQuantumOfSupply(11)
            .setDeliveryTime(12)
            .setQuantityInPack(13)
            .setSupplySchedule(List.of(new SupplyEvent(DayOfWeek.FRIDAY), new SupplyEvent(DayOfWeek.SATURDAY)));
        masterDataRepository.insert(masterData);

        //when
        sendToErpQueueRepository.enqueue(shopSkuKey, MdmEnqueueReason.DEVELOPER_TOOL);
        sendToErpQueueService.processQueueItems();

        //then
        ErpLogisticsMasterData expected = new ErpLogisticsMasterData()
            .setRealSupplierId(realSupplierId)
            .setSskuId(shopSku)
            .setMinShipment(masterData.getMinShipment())
            .setShipmentQuantum(masterData.getQuantumOfSupply())
            .setDeliveryTime(masterData.getDeliveryTime())
            .setWarehouseId(ErpLogisticsMasterData.DEFAULT_WAREHOUSE_ID)
            .setQuantityInPack(masterData.getQuantityInPack())
            .setCalendarId("пт,сб");
        Assertions.assertThat(erpLogisticsMasterDataExporterDao.findAll()).containsExactly(expected);
    }
}
