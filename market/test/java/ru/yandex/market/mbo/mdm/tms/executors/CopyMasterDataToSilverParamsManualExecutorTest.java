package ru.yandex.market.mbo.mdm.tms.executors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MasterDataToSilverParamValuesQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.service.SskuValidationService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * Reimplemented in https://st.yandex-team.ru/MARKETMDM-91
 * Removed for cleanup code after stopping direct save masterData via endpoints.
 */
@Ignore("Will be changed https://st.yandex-team.ru/MARKETMDM-91")
public class CopyMasterDataToSilverParamsManualExecutorTest extends MdmBaseDbTestClass {

    private static final int BUSINESS = 1;
    private static final int SERVICE3STAGE = 777;
    private static final int SERVICE2STAGE = 666;
    private static final int REAL_SUPPLIER = 999;
    private static final int WHITE_SUPPLIER = 555;
    private static final String SHOP_SKU =  "sku";
    private static final String FIRST_PARTY_SHOP_SKU = "001.1P_ssku";
    private static final String WHITE_SHOP_SKU = "white";
    private static final ShopSkuKey BUSINESS_KEY = new ShopSkuKey(BUSINESS, SHOP_SKU);
    private static final ShopSkuKey BUSINESS_KEY1 = new ShopSkuKey(BUSINESS, "sku777");
    private static final ShopSkuKey SERVICE3STAGE_KEY = new ShopSkuKey(SERVICE3STAGE, SHOP_SKU);
    private static final ShopSkuKey REAL_SUPPLIER_KEY = new ShopSkuKey(REAL_SUPPLIER, SHOP_SKU);

    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private MdmSskuGroupManager groupManager;
    @Autowired
    private MasterDataToSilverParamValuesQRepository queue;
    @Autowired
    private MdmSupplierRepository supplierRepository;
    @Autowired
    private BeruId beruId;
    @Autowired
    private SskuValidationService sskuValidationService;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private ServiceSskuConverter serviceSskuConverter;
    @Autowired
    private StorageKeyValueService skv;
    @Autowired
    private MasterDataBusinessMergeService mergeService;
    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;

    private EnhancedRandom random;
    private CopyMasterDataToSilverParamsManualExecutor executor;
    private MasterData businessMasterData;
    private MasterData service3stMasterData;
    private MasterData service1pMasterData;
    private MasterData realSupplierMasterData;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(28L);
        supplierRepository.insertOrUpdateAll(List.of(
            new MdmSupplier()
                .setType(MdmSupplierType.BUSINESS)
                .setId(BUSINESS)
                .setDeleted(false),
            new MdmSupplier()
                .setType(MdmSupplierType.THIRD_PARTY)
                .setId(SERVICE3STAGE)
                .setBusinessEnabled(true)
                .setBusinessId(BUSINESS)
                .setDeleted(false),
            new MdmSupplier()
                .setType(MdmSupplierType.THIRD_PARTY)
                .setId(SERVICE2STAGE)
                .setBusinessEnabled(false)
                .setBusinessId(BUSINESS)
                .setDeleted(false),
            new MdmSupplier()
                .setType(MdmSupplierType.FIRST_PARTY)
                .setId(beruId.getId())
                .setBusinessEnabled(false)
                .setDeleted(false),
            new MdmSupplier()
                .setType(MdmSupplierType.REAL_SUPPLIER)
                .setId(REAL_SUPPLIER)
                .setBusinessEnabled(false)
                .setDeleted(false)
                .setRealSupplierId("001"),
            new MdmSupplier()
                .setType(MdmSupplierType.MARKET_SHOP)
                .setId(WHITE_SUPPLIER)
                .setBusinessEnabled(true)
                .setDeleted(false)
                .setBusinessId(BUSINESS)
        ));

        masterDataRepository.deleteAll();
        businessMasterData = TestDataUtils.generateMasterData(BUSINESS_KEY, random);
        service3stMasterData = TestDataUtils.generateMasterData(SERVICE3STAGE_KEY, random);
        service1pMasterData = TestDataUtils.generateMasterData(
            new ShopSkuKey(beruId.getId(), FIRST_PARTY_SHOP_SKU), random);
        realSupplierMasterData = TestDataUtils.generateMasterData(REAL_SUPPLIER_KEY, random);
        masterDataRepository.insertOrUpdateAll(List.of(businessMasterData, service3stMasterData, service1pMasterData,
            realSupplierMasterData));

        skv.putValue(MdmProperties.COPY_MD_TO_PV_ENABLED, true);
        skv.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        skv.invalidateCache();
        executor = new CopyMasterDataToSilverParamsManualExecutor(queue, groupManager, skv, masterDataRepository,
            silverSskuRepository, sskuValidationService, serviceSskuConverter, mergeService);
        mdmSupplierCachingService.refresh();
    }

    @Test
    public void whenEnqueued3pShouldWriteByBusinessAndService() {
        queue.enqueue(BUSINESS_KEY);
        executor.execute();

        businessMasterData = masterDataRepository.findById(BUSINESS_KEY);
        service3stMasterData = masterDataRepository.findById(SERVICE3STAGE_KEY);
        Map<Integer, List<SskuSilverParamValue>> silver = silverSskuRepository.findAll().stream()
            .collect(Collectors.groupingBy(spv -> Integer.valueOf(spv.getSilverKey().getSourceId())));
        assertThatSskuEqualToMd(businessMasterData, List.of(service3stMasterData), silver);
    }

    @Test
    public void whenEnqueued1pShouldWriteAllByService() {
        queue.enqueue(new ShopSkuKey(beruId.getId(), FIRST_PARTY_SHOP_SKU));
        executor.execute();

        Map<Integer, List<SskuSilverParamValue>> silver = silverSskuRepository.findAll().stream()
            .collect(Collectors.groupingBy(spv -> Integer.valueOf(spv.getSilverKey().getSourceId())));
        assertThatSskuEqualToMd(service1pMasterData, List.of(), silver);
    }

    @Test
    public void whenEnqueuedRealSupplierShouldWriteAllByService() {
        queue.enqueue(REAL_SUPPLIER_KEY);
        executor.execute();

        Map<Integer, List<SskuSilverParamValue>> silver = silverSskuRepository.findAll().stream()
            .collect(Collectors.groupingBy(spv -> Integer.valueOf(spv.getSilverKey().getSourceId())));
        assertThatSskuEqualToMd(realSupplierMasterData, List.of(), silver);
    }

    @Test
    public void shouldNotWriteWhiteData() {
        var whiteBusinessMasterData =
            TestDataUtils.generateMasterData(new ShopSkuKey(BUSINESS, WHITE_SHOP_SKU), random);
        var whiteMasterData = TestDataUtils.generateMasterData(new ShopSkuKey(WHITE_SUPPLIER, WHITE_SHOP_SKU), random);
        masterDataRepository.insertOrUpdateAll(List.of(whiteBusinessMasterData, whiteMasterData));

        queue.enqueue(new ShopSkuKey(BUSINESS, WHITE_SHOP_SKU));
        executor.execute();

        Map<Integer, List<SskuSilverParamValue>> silver = silverSskuRepository.findAll().stream()
            .collect(Collectors.groupingBy(spv -> Integer.valueOf(spv.getSilverKey().getSourceId())));
        assertThatSskuEqualToMd(whiteBusinessMasterData, List.of(), silver);
    }

    @Test
    public void whenThereIsMasterDataAndSilverDataRewriteOnlyMasterDataParams() {
        // generate silver item and add dimension params
        var business1MD = TestDataUtils.generateMasterData(BUSINESS_KEY1, random);
        masterDataRepository.insertOrUpdate(business1MD);
        queue.enqueue(BUSINESS_KEY1);
        executor.execute();

        // update master_data
        var updatedBusinessMd = new MasterData().setShopSkuKey(BUSINESS_KEY1).setDatacampMasterDataVersion(100500L);
        masterDataRepository.insertOrUpdate(updatedBusinessMd);

        // Делаем новую master data существенно новой
        jdbcTemplate.update(
            "update mdm.master_data set modified_timestamp = :modified_ts"
                + " where supplier_id = :supplier_id and shop_sku = :shop_sku",
            Map.of(
                "modified_ts", LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.MICROS),
                "supplier_id", BUSINESS,
                "shop_sku", BUSINESS_KEY1.getShopSku()
            )
        );

        // check
        queue.enqueue(BUSINESS_KEY1);
        executor.execute();
        var silverParamValues = silverSskuRepository.findAll();
        var silverCommonSsku = new SilverCommonSsku(silverParamValues.get(0).getSilverSskuKey());
        silverCommonSsku.setBaseValues(silverParamValues);
        var silverMap = Map.of(
            BUSINESS_KEY1.getSupplierId(),
            silverParamValues.stream()
                .filter(sskuSilverParamValue ->
                    sskuSilverParamValue.getMdmParamId() != KnownMdmParams.DATACAMP_MASTER_DATA_VERSION)
                .collect(Collectors.toList())
        );
        assertThatSskuEqualToMd(business1MD.setDatacampMasterDataVersion(null), List.of(), silverMap);
        Assertions.assertThat(silverCommonSsku.getBaseValue(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION))
            .flatMap(MdmParamValue::getNumeric)
            .map(BigDecimal::longValue)
            .contains(100500L);
    }

    @Test
    public void whenMasterDataIsOlderThanSilverShouldNotCopy() {
        queue.enqueue(REAL_SUPPLIER_KEY);
        executor.execute();

        SilverCommonSsku silver = silverSskuRepository.findSsku(REAL_SUPPLIER_KEY).get(0);
        SskuSilverParamValue paramValue = updateParamValue(silver.getBaseValues().get(0));
        silver.addBaseValue(paramValue);
        Awaitility.await().atLeast(100, TimeUnit.MILLISECONDS).atMost(100, TimeUnit.MILLISECONDS);
        silverSskuRepository.insertOrUpdateSsku(silver);

        SilverCommonSsku updatedSilver = silverSskuRepository.findSsku(REAL_SUPPLIER_KEY).get(0);

        queue.enqueue(REAL_SUPPLIER_KEY);
        executor.execute();
        silver = silverSskuRepository.findSsku(REAL_SUPPLIER_KEY).get(0);

        Assertions.assertThat(silver.getBaseSsku().getUpdatedTs())
            .isEqualTo(updatedSilver.getBaseSsku().getUpdatedTs());
        Assertions.assertThat(silver).isEqualTo(updatedSilver);
    }

    private void assertThatSskuEqualToMd(MasterData businsesMd, List<MasterData> serviceMd,
                                         Map<Integer, List<SskuSilverParamValue>> silver) {
        CommonSsku ssku = mergeMdToCommonSsku(deleteCargoTypeValues(businsesMd), serviceMd.stream()
            .map(this::deleteCargoTypeValues).collect(Collectors.toList()));
        ServiceSsku silverBaseSSku = toServiceSsku(silver.get(businsesMd.getSupplierId()));
        assertThatSilverSskusAreEqual(silverBaseSSku, ssku.getBaseValues());
        serviceMd.stream()
            .map(md -> toServiceSsku(silver.get(md.getSupplierId())))
            .forEach(silverServiceSsku ->
                assertThatSilverSskusAreEqual(silverServiceSsku,
                    ssku.getServiceValues(silverServiceSsku.getKey().getSupplierId())));
    }

    private MasterData deleteCargoTypeValues(MasterData md) {
        return md.setDangerousGood(null).setHeavyGood20(null).setHeavyGood(null).setPreciousGood(null);
    }

    private void assertThatSilverSskusAreEqual(ServiceSsku silverServiceSsku, List<SskuParamValue> sskuPv) {
        Assertions.assertThat(silverServiceSsku.getValues())
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsExactlyInAnyOrderElementsOf(sskuPv);
    }

    private CommonSsku mergeMdToCommonSsku(MasterData businsesMd, List<MasterData> serviceMd) {
        ServiceSsku baseSsku = serviceSskuConverter.toServiceSsku(businsesMd, (FromIrisItemWrapper) null);
        List<ServiceSsku> serviceSskus = serviceMd.stream()
            .map(service -> serviceSskuConverter.toServiceSsku(service, (FromIrisItemWrapper) null))
            .collect(Collectors.toList());
        serviceSskus.add(baseSsku);
        return mergeService.merge(serviceSskus, baseSsku.getKey());
    }

    private static ServiceSsku toServiceSsku(List<SskuSilverParamValue> paramValues) {
        var result = new ServiceSsku();
        result.setKey(paramValues.get(0).getShopSkuKey());
        result.setParamValues(paramValues.stream()
            .map(SskuSilverParamValue::toSskuParamValue)
            .collect(Collectors.toList()));
        return result;
    }

    private SskuSilverParamValue updateParamValue(SskuSilverParamValue original) {
        if (!CollectionUtils.isEmpty(original.getStrings())) {
            var newValue = new ArrayList<>(original.getStrings());
            newValue.add("A");
            original.setStrings(newValue);
        } else if (!CollectionUtils.isEmpty(original.getNumerics())) {
            var newValue = new ArrayList<>(original.getNumerics());
            newValue.add(BigDecimal.valueOf(1));
            original.setNumerics(newValue);
        } else if (!CollectionUtils.isEmpty(original.getBools())) {
            var newValue = new ArrayList<>(original.getBools());
            newValue.add(false);
            original.setBools(newValue);
        } else if (!CollectionUtils.isEmpty(original.getOptions())) {
            original.setOptions(List.of());
        }
        return original;
    }
}
