package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.YtStorageSupportMode;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.StorageApiSilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MarkupOfferForDeleteQueue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.OfferWithVersion;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.MdmSolomonPushService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType.MEASUREMENT;
import static ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType.SUPPLIER;
import static ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue.SskuSilverTransportType.DATACAMP;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MANUFACTURER_COUNTRY;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MIN_SHIPMENT;

public class MarkupOfferForDeleteQueueExecutorTest extends MdmBaseDbTestClass {
    private static final MasterDataSource SUPPLIER_SOURCE = new MasterDataSource(SUPPLIER, "supplier");
    private static final MasterDataSource MEASUREMENT_SOURCE = new MasterDataSource(MEASUREMENT, "145");
    private static final long LOW_MD_VERSION = 99L;
    private static final long HIGH_MD_VERSION = 100500;

    private static final int BUSINESS_ID_1 = 100;
    private static final int SUPPLIER_1 = 97;
    private static final String SHOP_SKU_1 = "Black";

    @Autowired
    private MarkupOfferForDeleteQueue markupOfferForDeleteQueue;
    @Autowired
    private StorageApiSilverSskuRepository storageApiSilverSskuRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private MdmSupplierRepository supplierRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;

    private MarkupOfferForDeleteQueueExecutor markupOfferForDeleteQueueExecutor;

    @Before
    public void setUp() throws Exception {
        storageKeyValueService.putValue(MdmProperties.SILVER_SSKU_YT_STORAGE_MODE, YtStorageSupportMode.ENABLED.name());
        storageKeyValueService.putValue(MdmProperties.SHOULD_DELETE_PROCESSED_IN_MARKUP_OFFER_FOR_DELETE_QUEUE, true);

        markupOfferForDeleteQueueExecutor = new MarkupOfferForDeleteQueueExecutor(
            storageKeyValueService,
            markupOfferForDeleteQueue,
            storageApiSilverSskuRepository,
            Mockito.mock(MdmSolomonPushService.class),
            mdmQueuesManager);

        List<MdmSupplier> suppliers = List.of(
            new MdmSupplier().setId(BUSINESS_ID_1).setType(MdmSupplierType.BUSINESS)
                .setBusinessId(null).setBusinessEnabled(false).setDeleted(false),
            new MdmSupplier().setId(SUPPLIER_1).setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BUSINESS_ID_1).setBusinessEnabled(true)
        );
        supplierRepository.insertOrUpdateAll(suppliers);
        sskuExistenceRepository.markExistence(List.of(new ShopSkuKey(SUPPLIER_1, "Black")), true);
    }

    @Test
    public void testSuccessfulEnqueueingAndProcessing() {
        // given
        SilverCommonSsku silverCommonSsku = buildSilverCommonSsku(BUSINESS_ID_1, SUPPLIER_1, SHOP_SKU_1,
            SUPPLIER_SOURCE, HIGH_MD_VERSION);
        storageApiSilverSskuRepository.insertOrUpdateSskus(List.of(silverCommonSsku));

        var offerWithVersion = new OfferWithVersion(SUPPLIER_1, SHOP_SKU_1, HIGH_MD_VERSION);
        markupOfferForDeleteQueue.enqueueAll(List.of(offerWithVersion));

        // when
        markupOfferForDeleteQueueExecutor.execute();

        // then
        Assertions.assertThat(markupOfferForDeleteQueue.findAll().size()).isZero();

        var businessSkuKey = new ShopSkuKey(BUSINESS_ID_1, SHOP_SKU_1);
        SilverCommonSsku modifiedSilverCommonSsku =
            storageApiSilverSskuRepository.findSskus(List.of(businessSkuKey)).get(businessSkuKey).get(0);

        Optional<SskuSilverParamValue> isRemovedParamValueInBasePart = modifiedSilverCommonSsku.getBaseSsku()
            .getParamValue(KnownMdmParams.IS_REMOVED);
        Assertions.assertThat(isRemovedParamValueInBasePart).isNotPresent();

        // present in service part
        Optional<SskuSilverParamValue> isRemovedParamValueInServicePart =
            modifiedSilverCommonSsku.getServiceSsku(SUPPLIER_1).get().getParamValue(KnownMdmParams.IS_REMOVED);
        Assertions.assertThat(isRemovedParamValueInServicePart).isPresent();
        Assertions.assertThat(isRemovedParamValueInServicePart.get().getBool().get()).isTrue();
    }

    @Test
    public void shouldNotMarkupSilverCommonSskuForDeleteWithMeasurementSource() {
        // given
        SilverCommonSsku silverCommonSsku = buildSilverCommonSsku(BUSINESS_ID_1, SUPPLIER_1, SHOP_SKU_1,
            MEASUREMENT_SOURCE, HIGH_MD_VERSION);
        storageApiSilverSskuRepository.insertOrUpdateSskus(List.of(silverCommonSsku));

        var offerWithVersion = new OfferWithVersion(SUPPLIER_1, SHOP_SKU_1, HIGH_MD_VERSION);
        markupOfferForDeleteQueue.enqueueAll(List.of(offerWithVersion));

        // when
        markupOfferForDeleteQueueExecutor.execute();

        // then
        Assertions.assertThat(markupOfferForDeleteQueue.findAll().size()).isZero();

        var businessSkuKey = new ShopSkuKey(BUSINESS_ID_1, SHOP_SKU_1);
        SilverCommonSsku modifiedSilverCommonSsku =
            storageApiSilverSskuRepository.findSskus(List.of(businessSkuKey)).get(businessSkuKey).get(0);

        // IS_REMOVED not present neither in base nor in service parts
        Optional<SskuSilverParamValue> isRemovedParamValueInBasePart = modifiedSilverCommonSsku.getBaseSsku()
            .getParamValue(KnownMdmParams.IS_REMOVED);
        Assertions.assertThat(isRemovedParamValueInBasePart).isNotPresent();

        Optional<SskuSilverParamValue> isRemovedParamValueInServicePart =
            modifiedSilverCommonSsku.getServiceSsku(SUPPLIER_1).get().getParamValue(KnownMdmParams.IS_REMOVED);
        Assertions.assertThat(isRemovedParamValueInServicePart).isNotPresent();
    }

    @Test
    public void shouldNotMarkupSilverCommonSskuForDeleteWhenOriginalMasterDataVersionHigherThanEnqueuedVersion() {
        // given
        SilverCommonSsku silverCommonSsku = buildSilverCommonSsku(BUSINESS_ID_1, SUPPLIER_1, SHOP_SKU_1,
            SUPPLIER_SOURCE, HIGH_MD_VERSION);
        storageApiSilverSskuRepository.insertOrUpdateSskus(List.of(silverCommonSsku));

        var offerWithVersion = new OfferWithVersion(SUPPLIER_1, SHOP_SKU_1, LOW_MD_VERSION); // низкая версия
        markupOfferForDeleteQueue.enqueueAll(List.of(offerWithVersion));

        // when
        markupOfferForDeleteQueueExecutor.execute();

        // then
        Assertions.assertThat(markupOfferForDeleteQueue.findAll().size()).isZero();

        var businessSkuKey = new ShopSkuKey(BUSINESS_ID_1, SHOP_SKU_1);
        SilverCommonSsku modifiedSilverCommonSsku =
            storageApiSilverSskuRepository.findSskus(List.of(businessSkuKey)).get(businessSkuKey).get(0);

        // IS_REMOVED not present neither in base nor in service parts
        Optional<SskuSilverParamValue> isRemovedParamValueInBasePart = modifiedSilverCommonSsku.getBaseSsku()
            .getParamValue(KnownMdmParams.IS_REMOVED);
        Assertions.assertThat(isRemovedParamValueInBasePart).isNotPresent();

        Optional<SskuSilverParamValue> isRemovedParamValueInServicePart =
            modifiedSilverCommonSsku.getServiceSsku(SUPPLIER_1).get().getParamValue(KnownMdmParams.IS_REMOVED);
        Assertions.assertThat(isRemovedParamValueInServicePart).isNotPresent();
    }

    private SskuSilverParamValue createSskuSilverParamValue(long paramId, String xslName,
                                                            Object value,
                                                            ShopSkuKey shopSkuKey,
                                                            MasterDataSource source,
                                                            long masterDataVersion) {
        return TestMdmParamUtils.createSskuSilverParamValue(paramId, xslName,
            value,
            source.getSourceType(), source.getSourceId(),
            shopSkuKey,
            masterDataVersion,
            DATACAMP);
    }

    private SilverCommonSsku buildSilverCommonSsku(int bizId, int supplierId, String shopSku,
                                                   MasterDataSource masterDataSource,
                                                   long masterDataVersion) {
        // Service
        ShopSkuKey serviceSkuKey = new ShopSkuKey(supplierId, shopSku);
        List<SskuSilverParamValue> silverServiceParamValues = List.of(
            createSskuSilverParamValue(MIN_SHIPMENT, "minShipment", 6, serviceSkuKey, masterDataSource,
                masterDataVersion)
        );

        SilverServiceSsku silverServiceSsku = new SilverServiceSsku();
        silverServiceSsku.setKey(new SilverSskuKey(serviceSkuKey, masterDataSource));
        silverServiceSsku.setParamValues(silverServiceParamValues);
        silverServiceSsku.setMasterDataVersion(masterDataVersion);

        // Business
        ShopSkuKey businessSkuKey = new ShopSkuKey(bizId, shopSku);
        List<SskuSilverParamValue> silverBusinessParamValues = List.of(
            createSskuSilverParamValue(MANUFACTURER_COUNTRY, "manufacturerCountry", "Ру", businessSkuKey,
                masterDataSource, masterDataVersion)
        );

        SilverServiceSsku silverBusinessSsku = new SilverServiceSsku();
        silverBusinessSsku.setKey(new SilverSskuKey(businessSkuKey, masterDataSource));
        silverBusinessSsku.setParamValues(silverBusinessParamValues);
        silverBusinessSsku.setMasterDataVersion(masterDataVersion);

        // Common
        return new SilverCommonSsku(silverBusinessSsku.getKey())
            .setBaseSsku(silverBusinessSsku)
            .putServiceSsku(silverServiceSsku);
    }
}
