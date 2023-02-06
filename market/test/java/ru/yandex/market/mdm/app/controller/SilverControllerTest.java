package ru.yandex.market.mdm.app.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.mbo.mdm.common.masterdata.model.ImpersonalSourceId;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.YtStorageSupportMode;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SilverControllerTest extends MdmBaseDbTestClass {
    private static final int BUSINESS_ID = 123;
    private static final int SERVICE_ID = 456;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        storageKeyValueService.putValue(MdmProperties.SILVER_SSKU_YT_STORAGE_MODE, YtStorageSupportMode.ENABLED.name());
        storageKeyValueService.invalidateCache();
        SilverController silverController = new SilverController(silverSskuRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(silverController).build();

        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(BUSINESS_ID)
            .setType(MdmSupplierType.BUSINESS)
        );
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(SERVICE_ID)
            .setBusinessEnabled(true)
            .setBusinessId(BUSINESS_ID)
            .setType(MdmSupplierType.THIRD_PARTY)
        );
    }

    @Test
    public void testFindSilverBySsku() throws Exception {
        Random random = new Random("Happy birthday, cloudcat!".hashCode());

        ShopSkuKey key = new ShopSkuKey(BUSINESS_ID, "shop_sku111");
        MasterDataSource supplier =
            new MasterDataSource(MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name());
        MasterDataSource measurement = new MasterDataSource(MasterDataSourceType.MEASUREMENT, "172");
        SilverSskuKey supplierSilverKey = new SilverSskuKey(key, supplier);
        SilverSskuKey measurementSilverKey = new SilverSskuKey(key, measurement);

        Instant now = Instant.now();

        SilverCommonSsku supplierSilver = new SilverCommonSsku(supplierSilverKey);
        KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(supplierSilver::addBaseValue);
        supplierSilver.addBaseValue(
            new MdmParamValue().setMdmParamId(KnownMdmParams.SHELF_LIFE)
                .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE).getXslName())
                .setNumeric(BigDecimal.ONE)
        );
        supplierSilver.addBaseValue(
            new MdmParamValue().setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
                .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE_UNIT).getXslName())
                .setOption(
                    new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.HOUR)))
        );
        supplierSilver.setUpdatedTs(now.plusSeconds(1500));

        SilverCommonSsku measurementSilver = new SilverCommonSsku(measurementSilverKey);
        KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(measurementSilver::addBaseValue);
        measurementSilver.addBaseValue(
            new MdmParamValue().setMdmParamId(KnownMdmParams.SHELF_LIFE)
                .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE).getXslName())
                .setNumeric(BigDecimal.TEN)
        );
        measurementSilver.addBaseValue(
            new MdmParamValue().setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
                .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE_UNIT).getXslName())
                .setOption(
                    new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR)))
        );
        measurementSilver.setUpdatedTs(now.plusSeconds(2500));

        silverSskuRepository.insertOrUpdateSskus(List.of(supplierSilver, measurementSilver));

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(
            "/mdm-api/silver/find_by_ssku/{ssku}/{supplierId}",
            key.getShopSku(), key.getSupplierId()
        );
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("$[0].supplier_id").value(key.getSupplierId()))
            .andExpect(jsonPath("$[0].shop_sku").value(key.getShopSku()))
            .andExpect(jsonPath("$[0].source_type").value(supplier.getSourceType().name()))
            .andExpect(jsonPath("$[0].source_id").value(supplier.getSourceId()))
            .andExpect(jsonPath("$[0].fields.length_cm.value")
                .value(supplierSilver.getBaseValue(KnownMdmParams.LENGTH)
                    .flatMap(MdmParamValue::getNumeric)
                    .orElseThrow()))
            .andExpect(jsonPath("$[0].fields.width_cm.value")
                .value(supplierSilver.getBaseValue(KnownMdmParams.WIDTH)
                    .flatMap(MdmParamValue::getNumeric)
                    .orElseThrow()))
            .andExpect(jsonPath("$[0].fields.height_cm.value")
                .value(supplierSilver.getBaseValue(KnownMdmParams.HEIGHT)
                    .flatMap(MdmParamValue::getNumeric)
                    .orElseThrow()))
            .andExpect(jsonPath("$[0].fields.weight_gross_kg.value")
                .value(supplierSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS)
                    .flatMap(MdmParamValue::getNumeric)
                    .orElseThrow()))
            .andExpect(jsonPath("$[0].fields.weight_net_kg.value")
                .value(supplierSilver.getBaseValue(KnownMdmParams.WEIGHT_NET)
                    .flatMap(MdmParamValue::getNumeric)
                    .orElseThrow()))
            .andExpect(jsonPath("$[0].fields.weight_tare_kg.value")
                .value(supplierSilver.getBaseValue(KnownMdmParams.WEIGHT_TARE)
                    .flatMap(MdmParamValue::getNumeric)
                    .orElseThrow()))
            .andExpect(jsonPath("$[0].fields.shelf_life.value").value("1 час"))
            .andExpect(jsonPath("$[1].supplier_id").value(key.getSupplierId()))
            .andExpect(jsonPath("$[1].shop_sku").value(key.getShopSku()))
            .andExpect(jsonPath("$[1].source_type").value(measurement.getSourceType().name()))
            .andExpect(jsonPath("$[1].source_id").value(measurement.getSourceId()))
            .andExpect(jsonPath("$[1].fields.length_cm.value")
                .value(measurementSilver.getBaseValue(KnownMdmParams.LENGTH)
                    .flatMap(MdmParamValue::getNumeric)
                    .orElseThrow()))
            .andExpect(jsonPath("$[1].fields.width_cm.value")
                .value(measurementSilver.getBaseValue(KnownMdmParams.WIDTH)
                    .flatMap(MdmParamValue::getNumeric)
                    .orElseThrow()))
            .andExpect(jsonPath("$[1].fields.height_cm.value")
                .value(measurementSilver.getBaseValue(KnownMdmParams.HEIGHT)
                    .flatMap(MdmParamValue::getNumeric)
                    .orElseThrow()))
            .andExpect(jsonPath("$[1].fields.weight_gross_kg.value")
                .value(measurementSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS)
                    .flatMap(MdmParamValue::getNumeric)
                    .orElseThrow()))
            .andExpect(jsonPath("$[1].fields.weight_net_kg.value")
                .value(measurementSilver.getBaseValue(KnownMdmParams.WEIGHT_NET)
                    .flatMap(MdmParamValue::getNumeric)
                    .orElseThrow()))
            .andExpect(jsonPath("$[1].fields.weight_tare_kg.value")
                .value(measurementSilver.getBaseValue(KnownMdmParams.WEIGHT_TARE)
                    .flatMap(MdmParamValue::getNumeric)
                    .orElseThrow()))
            .andExpect(jsonPath("$[1].fields.shelf_life.value").value("10 лет"));
    }

    @Test
    public void whenNoSignificantFieldNotReturnItem() throws Exception {
        Random random = new Random("Happy birthday, cloudcat!".hashCode());

        ShopSkuKey key = new ShopSkuKey(BUSINESS_ID, "shop_sku111");
        MasterDataSource supplier =
            new MasterDataSource(MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name());
        SilverSskuKey supplierSilverKey = new SilverSskuKey(key, supplier);

        Instant now = Instant.now();

        SilverCommonSsku supplierSilver = new SilverCommonSsku(supplierSilverKey);
        supplierSilver.addBaseValue(
            TestMdmParamUtils.createRandomMdmParamValue(random, mdmParamCache.get(KnownMdmParams.IS_TRACEABLE))
        );
        supplierSilver.setUpdatedTs(now.plusSeconds(1500));

        silverSskuRepository.insertOrUpdateSskus(List.of(supplierSilver));

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(
            "/mdm-api/silver/find_by_ssku/{ssku}/{supplierId}",
            key.getShopSku(), key.getSupplierId()
        );
        mockMvc.perform(request).andExpect(status().isNotFound());
    }
}
