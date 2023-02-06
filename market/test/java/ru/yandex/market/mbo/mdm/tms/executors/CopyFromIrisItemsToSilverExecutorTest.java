package ru.yandex.market.mbo.mdm.tms.executors;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepositoryParamValueImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.repository.Mappers;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class CopyFromIrisItemsToSilverExecutorTest extends MdmBaseDbTestClass {
    private static final int BUSINESS = 10;
    private static final int SERVICE = 1;
    private static final int BERU_ID = 123; // business disabled
    private static final String SHOP_SKU_1 = "Coolcat";
    private static final String SHOP_SKU_2 = "Sirius";
    private static final ShopSkuKey BUSINESS_KEY_1 = new ShopSkuKey(BUSINESS, SHOP_SKU_1);
    private static final ShopSkuKey SERVICE_KEY_1 = new ShopSkuKey(SERVICE, SHOP_SKU_1);
    private static final ShopSkuKey BUSINESS_KEY_2 = new ShopSkuKey(BUSINESS, SHOP_SKU_2);
    private static final ShopSkuKey SERVICE_KEY_2 = new ShopSkuKey(SERVICE, SHOP_SKU_2);

    @Autowired
    private FromIrisItemRepository fromIrisItemRepository;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private ServiceSskuConverter serviceSskuConverter;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private MdmParamCache mdmParamCache;

    private SilverSskuRepository silverSskuRepository;
    private CopyFromIrisItemsToSilverExecutor copyFromIrisItemsToSilverExecutor;

    @Before
    public void setUp() throws Exception {
        silverSskuRepository = new SilverSskuRepositoryParamValueImpl(
            jdbcTemplate,
            transactionTemplate,
            mdmSskuGroupManager,
            Mappers.SSKU_SILVER_PARAM_VALUE_MAPPER_WITH_UPDATED_TS_CONTROL
        );

        copyFromIrisItemsToSilverExecutor = new CopyFromIrisItemsToSilverExecutor(
            fromIrisItemRepository,
            mdmSskuGroupManager,
            storageKeyValueService,
            silverSskuRepository,
            serviceSskuConverter,
            transactionTemplate
        );
        loadSuppliers();
        storageKeyValueService.invalidateCache();
    }

    @Test
    public void whenHaveFromIrisItemByStrangeSourceNotSaveToSilver() {
        // given
        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(MasterDataSourceType.MSKU_SOURCE_PREFIX) // msku inherit
            .setType(MdmIrisPayload.MasterDataSource.MDM)
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(100d, 101d, 102d, 103d, 104d, 105d, 100L, 100);
        MdmIrisPayload.ReferenceInformation info = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(SERVICE_KEY_1.getSupplierId())
                .setShopSku(SERVICE_KEY_1.getShopSku()))
            .addInformation(info)
            .build();
        FromIrisItemWrapper fromIrisItem = new FromIrisItemWrapper(item);
        fromIrisItemRepository.insertOrUpdate(fromIrisItem);

        // when
        copyFromIrisItemsToSilverExecutor.execute();

        // then
        Assertions.assertThat(silverSskuRepository.findAll()).isEmpty();
    }

    @Test
    public void whenHaveOtherParamsCopyOnlyVgh() {
        // given
        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId("100")
            .setType(MdmIrisPayload.MasterDataSource.WAREHOUSE)
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(100d, 101d, 102d, 103d, 104d, 105d, 100L, 100);
        MdmIrisPayload.ReferenceInformation info = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(SERVICE_KEY_1.getSupplierId())
                .setShopSku(SERVICE_KEY_1.getShopSku()))
            .addInformation(info)
            .build();
        FromIrisItemWrapper fromIrisItem = new FromIrisItemWrapper(item);
        fromIrisItemRepository.insertOrUpdate(fromIrisItem);

        // when
        copyFromIrisItemsToSilverExecutor.execute();

        // then
        SilverCommonSsku savedSilver = silverSskuRepository.findSsku(
            new SilverSskuKey(BUSINESS_KEY_1, MasterDataSource.fromIrisProto(source)))
            .orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(6);
        Assertions.assertThat(savedSilver.getBaseValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS);
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("100"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("101"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("102"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("103"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_NET))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("104"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_TARE))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("105"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SURPLUS))
            .isEmpty();
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.DQ_SCORE))
            .isEmpty();
    }

    @Test
    public void whenEoxDataAlreadyExistNotOverwriteIt() {
        // given
        SilverSskuKey silverBusinessKey = new SilverSskuKey(
            BUSINESS_KEY_1,
            new MasterDataSource(MasterDataSourceType.SUPPLIER, BUSINESS_KEY_1.getSupplierId() + "")
        );
        SilverCommonSsku silverFromEox = new SilverCommonSsku(silverBusinessKey)
            .addBaseValues(List.of(
                silverNumericValue(
                    silverBusinessKey,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP,
                    KnownMdmParams.LENGTH,
                    "200"
                ),
                silverNumericValue(
                    silverBusinessKey,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP,
                    KnownMdmParams.WIDTH,
                    "201"
                ),
                silverNumericValue(
                    silverBusinessKey,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP,
                    KnownMdmParams.HEIGHT,
                    "202"
                ),
                silverNumericValue(
                    silverBusinessKey,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP,
                    KnownMdmParams.WEIGHT_GROSS,
                    "203"
                ),
                silverNumericValue(
                    silverBusinessKey,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP,
                    KnownMdmParams.WEIGHT_NET,
                    "204"
                ),
                silverNumericValue(
                    silverBusinessKey,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP,
                    KnownMdmParams.WEIGHT_TARE,
                    "205"
                )
            ));
        silverSskuRepository.insertOrUpdateSsku(silverFromEox);

        MdmIrisPayload.Associate source = silverBusinessKey.getMasterDataSource().toIrisProto();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(100d, 101d, 102d, 103d, 104d, 105d, 100L, 100);
        MdmIrisPayload.ReferenceInformation info = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(SERVICE_KEY_1.getSupplierId())
                .setShopSku(SERVICE_KEY_1.getShopSku()))
            .addInformation(info)
            .build();
        FromIrisItemWrapper fromIrisItem = new FromIrisItemWrapper(item);
        fromIrisItemRepository.insertOrUpdate(fromIrisItem);

        // when
        copyFromIrisItemsToSilverExecutor.execute();

        // then
        SilverCommonSsku savedSilver = silverSskuRepository.findSsku(silverBusinessKey).orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(6);
        Assertions.assertThat(savedSilver.getBaseValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.DATACAMP);
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("200"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("201"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("202"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("203"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_NET))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("204"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_TARE))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("205"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SURPLUS))
            .isEmpty();
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.DQ_SCORE))
            .isEmpty();
    }

    private void loadSuppliers() {
        MdmSupplier service = new MdmSupplier()
            .setId(SERVICE)
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessEnabled(true)
            .setBusinessId(BUSINESS);
        MdmSupplier business = new MdmSupplier()
            .setId(BUSINESS)
            .setType(MdmSupplierType.BUSINESS)
            .setBusinessEnabled(true);
        MdmSupplier beru = new MdmSupplier()
            .setId(BERU_ID)
            .setType(MdmSupplierType.FIRST_PARTY)
            .setBusinessEnabled(false);
        mdmSupplierRepository.insertOrUpdateAll(List.of(service, business, beru));
    }

    @Test
    public void whenUpdateVghParamsNotChangeValuesTs() {
        // given
        SilverSskuKey silverBusinessKey = new SilverSskuKey(
            BUSINESS_KEY_1,
            new MasterDataSource(MasterDataSourceType.SUPPLIER, BUSINESS_KEY_1.getSupplierId() + "")
        );

        SskuSilverParamValue oldGuaranteePeriod = (SskuSilverParamValue) silverNumericValue(
            silverBusinessKey,
            SskuSilverParamValue.SskuSilverTransportType.DATACAMP,
            KnownMdmParams.GUARANTEE_PERIOD,
            "200"
        ).setUpdatedTs(Instant.parse("2007-12-03T10:15:30.00Z"));
        SilverCommonSsku otherSilver = new SilverCommonSsku(silverBusinessKey).addBaseValue(oldGuaranteePeriod);
        silverSskuRepository.insertOrUpdateSsku(otherSilver);

        MdmIrisPayload.Associate source = silverBusinessKey.getMasterDataSource().toIrisProto();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(100d, 101d, 102d, 103d, 104d, 105d, 100L, 100);
        MdmIrisPayload.ReferenceInformation info = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(SERVICE_KEY_1.getSupplierId())
                .setShopSku(SERVICE_KEY_1.getShopSku()))
            .addInformation(info)
            .build();
        FromIrisItemWrapper fromIrisItem = new FromIrisItemWrapper(item);
        fromIrisItemRepository.insertOrUpdate(fromIrisItem);

        // when
        copyFromIrisItemsToSilverExecutor.execute();

        //then
        SilverCommonSsku savedSilver = silverSskuRepository.findSsku(silverBusinessKey).orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(7);
        Instant fromIrisTs = fromIrisItemRepository.findById(fromIrisItem.getSourceItemKey()).getReceivedTs();
        Assertions.assertThat(savedSilver.getBaseValues())
            .filteredOn(pv -> pv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS)
            .map(MdmParamValue::getUpdatedTs)
            .containsOnly(fromIrisTs);
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.GUARANTEE_PERIOD))
            .contains(oldGuaranteePeriod);
    }

    @Test
    public void testOneRunLimitAndOffsetBackup() {
        // given
        SilverSskuKey silverBusinessKey1 = new SilverSskuKey(
            BUSINESS_KEY_1,
            new MasterDataSource(MasterDataSourceType.SUPPLIER, BUSINESS_KEY_1.getSupplierId() + "")
        );
        SilverSskuKey silverBusinessKey2 = new SilverSskuKey(
            BUSINESS_KEY_2,
            new MasterDataSource(MasterDataSourceType.SUPPLIER, BUSINESS_KEY_2.getSupplierId() + "")
        );

        MdmIrisPayload.Associate source1 = silverBusinessKey1.getMasterDataSource().toIrisProto();
        MdmIrisPayload.Associate source2 = silverBusinessKey2.getMasterDataSource().toIrisProto();

        MdmIrisPayload.ShippingUnit.Builder shippingUnit1 =
            ItemWrapperTestUtil.generateShippingUnit(100d, 101d, 102d, 103d, null, null, 100L);
        MdmIrisPayload.ShippingUnit.Builder shippingUnit2 =
            ItemWrapperTestUtil.generateShippingUnit(200d, 201d, 202d, 203d, null, null, 200L);

        MdmIrisPayload.ReferenceInformation info1 = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source1)
            .setItemShippingUnit(shippingUnit1)
            .build();
        MdmIrisPayload.ReferenceInformation info2 = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source2)
            .setItemShippingUnit(shippingUnit2)
            .build();

        MdmIrisPayload.Item item1 = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(SERVICE_KEY_1.getSupplierId())
                .setShopSku(SERVICE_KEY_1.getShopSku()))
            .addInformation(info1)
            .build();
        MdmIrisPayload.Item item2 = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(SERVICE_KEY_2.getSupplierId())
                .setShopSku(SERVICE_KEY_2.getShopSku()))
            .addInformation(info2)
            .build();

        FromIrisItemWrapper fromIrisItem1 = new FromIrisItemWrapper(item1);
        FromIrisItemWrapper fromIrisItem2 = new FromIrisItemWrapper(item2);

        fromIrisItemRepository.insertOrUpdateAll(List.of(fromIrisItem1, fromIrisItem2));

        storageKeyValueService.putValue(MdmProperties.COPY_FROM_IRIS_ITEMS_TO_SILVER_ONE_RUN_LIMIT, 1L);
        storageKeyValueService.putValue(MdmProperties.COPY_FROM_IRIS_ITEMS_TO_SILVER_BATCH_SIZE, 1);

        // when 1
        copyFromIrisItemsToSilverExecutor.execute();

        // then 1
        Assertions.assertThat(silverSskuRepository.findSsku(silverBusinessKey1)).isPresent();
        Assertions.assertThat(silverSskuRepository.findSsku(silverBusinessKey2)).isEmpty();
        Assertions.assertThat(offset()).isEqualTo(fromIrisItem1.getSourceItemKey().toSilverSskuKey());

        // when 2
        copyFromIrisItemsToSilverExecutor.execute();

        // then 2
        Assertions.assertThat(silverSskuRepository.findSsku(silverBusinessKey1)).isPresent();
        Assertions.assertThat(silverSskuRepository.findSsku(silverBusinessKey2)).isPresent();
        Assertions.assertThat(offset()).isEqualTo(fromIrisItem2.getSourceItemKey().toSilverSskuKey());
    }

    private SskuSilverParamValue silverNumericValue(SilverSskuKey key,
                                                    SskuSilverParamValue.SskuSilverTransportType transport,
                                                    long paramId,
                                                    String value) {
        return (SskuSilverParamValue) new SskuSilverParamValue()
            .setSskuSilverTransport(transport)
            .setShopSkuKey(key.getShopSkuKey())
            .setMasterDataSource(key.getMasterDataSource())
            .setMdmParamId(paramId)
            .setXslName(mdmParamCache.get(paramId).getXslName())
            .setNumeric(new BigDecimal(value));
    }

    private SilverSskuKey offset() {
        return storageKeyValueService
            .getValue(MdmProperties.COPY_FROM_IRIS_ITEMS_TO_SILVER_OFFSET, SilverSskuKey.class);
    }
}
