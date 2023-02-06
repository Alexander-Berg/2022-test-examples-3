package ru.yandex.market.mbo.mdm.common.masterdata.services;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.CommonSskuBuilder;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

@SuppressWarnings("checkstyle:MagicNumber")
public class MasterDataBusinessMergeServiceTest extends MdmBaseDbTestClass {
    private static final Comparator<SskuParamValue> COMPARATOR = (a, b) -> {
        if (a.valueEquals(b)) {
            return 0;
        }
        return -1;
    };
    private static final ShopSkuKey BUSINESS_KEY = new ShopSkuKey(16, "sku");
    private static final ShopSkuKey SUPPLIER_KEY1 = new ShopSkuKey(41, "sku");
    private static final ShopSkuKey SUPPLIER_KEY2 = new ShopSkuKey(42, "sku");
    private static final ShopSkuKey SUPPLIER_KEY3 = new ShopSkuKey(43, "sku");
    private static final ShopSkuKey SUPPLIER_KEY4 = new ShopSkuKey(44, "sku");
    private static final ShopSkuKey ORPHAN_KEY = new ShopSkuKey(34, "sku");

    @Autowired
    private MasterDataBusinessMergeService service;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private MdmParamCache paramCache;
    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;
    @Autowired
    private StorageKeyValueService keyValueService;

    @Before
    public void setup() {
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        MdmSupplier businessMan = new MdmSupplier();
        businessMan.setId(BUSINESS_KEY.getSupplierId());
        businessMan.setType(MdmSupplierType.BUSINESS);
        businessMan.setName("Business Man");

        MdmSupplier supplierMan = new MdmSupplier();
        supplierMan.setId(SUPPLIER_KEY1.getSupplierId());
        supplierMan.setType(MdmSupplierType.THIRD_PARTY);
        supplierMan.setBusinessId(businessMan.getId());
        supplierMan.setName("Supplier Man");

        MdmSupplier lonelyOne = new MdmSupplier();
        lonelyOne.setId(ORPHAN_KEY.getSupplierId());
        lonelyOne.setType(MdmSupplierType.THIRD_PARTY);
        lonelyOne.setName("Lonely Man");

        mdmSupplierRepository.insertBatch(businessMan, supplierMan, lonelyOne);
        mdmSupplierCachingService.refresh();
        keyValueService.invalidateCache();
    }

    @Test
    public void whenOnlyBusinessAndNoSuppliersShouldReturnSameBusiness() {
        ServiceSsku businessSsku = new CommonSskuBuilder(paramCache, BUSINESS_KEY)
            .withShelfLife(10, TimeInUnits.TimeUnit.DAY, "хранить в защищённом от света месте")
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Китай")
            .with(KnownMdmParams.VETIS_GUID, "guid1", "guid2")
            .with(KnownMdmParams.GTIN, "12345678", "09683920")
            .buildSupplierOnly();

        CommonSsku merged = service.merge(businessSsku, List.of(), businessSsku.getKey());
        assertSskuEquals(merged, businessSsku);
    }

    @Test
    public void whenOnlyOrphanShouldReturnSameOrphan() {
        ServiceSsku orphanSsku = new CommonSskuBuilder(paramCache, ORPHAN_KEY)
            .withShelfLife(10, TimeInUnits.TimeUnit.DAY, "хранить в защищённом от света месте")
            .withVat(VatRate.VAT_0)
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Китай")
            .with(KnownMdmParams.VETIS_GUID, "guid1", "guid2")
            .with(KnownMdmParams.GTIN, "12345678", "09683920")
            .with(KnownMdmParams.WEIGHT_GROSS, 20L)
            .with(KnownMdmParams.WEIGHT_NET, 16L)
            .with(KnownMdmParams.WEIGHT_TARE, 4L)
            .with(KnownMdmParams.WIDTH, 11L)
            .with(KnownMdmParams.LENGTH, 22L)
            .with(KnownMdmParams.HEIGHT, 33L)
            .buildSupplierOnly();

        CommonSsku merged = service.merge(null, List.of(orphanSsku), orphanSsku.getKey());
        assertSskuEquals(merged, orphanSsku);
    }

    @Test
    public void checkLatestMergeRule() {
        Instant present = Instant.now();
        Instant past = present.minusSeconds(100);
        Instant future = present.plusSeconds(100);
        ServiceSsku businessSsku = new CommonSskuBuilder(paramCache, BUSINESS_KEY)
            .withShelfLife(16, TimeInUnits.TimeUnit.MONTH, "изменено давно").customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID, "123456780").customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.RSL_ACTIVATION_TS, "01.02.2022 12:34:00").customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.EXPIR_DATE, true).customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.DANGEROUS_GOOD, false).customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.RSL_OUT_DAYS, 40L).customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.WEIGHT_GROSS, 20L).customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.RSL_IN_DAYS, 21L).customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.WEIGHT_NET, 16L).customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.WEIGHT_TARE, 4L).customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.LENGTH, 22L).customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.HEIGHT, 33L).customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.WIDTH, 11L).customized(p -> p.setUpdatedTs(past))

            .withLifeTime(30, TimeInUnits.TimeUnit.DAY, "meow").customized(p -> p.setUpdatedTs(present))
            .with(KnownMdmParams.RSL_IN_PERCENTS, 16L).customized(p -> p.setUpdatedTs(present))
            .with(KnownMdmParams.BOX_COUNT, 2L).customized(p -> p.setUpdatedTs(present))

            .withGuaranteePeriod(21, TimeInUnits.TimeUnit.YEAR, "wow").customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.MANUFACTURER, "ПАО 'ПАО'").customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.USE_IN_MERCURY, true).customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.RSL_OUT_PERCENTS, 5L)
            .buildSupplierOnly();

        ServiceSsku supplierSsku1 = new CommonSskuBuilder(paramCache, SUPPLIER_KEY1)
            .withGuaranteePeriod(31, TimeInUnits.TimeUnit.MONTH, "sfgdf").customized(p -> p.setUpdatedTs(past))
            .withLifeTime(8, TimeInUnits.TimeUnit.HOUR, "terwt").customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.USE_IN_MERCURY, false).customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.MANUFACTURER, "xxx").customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.BOX_COUNT, 1L).customized(p -> p.setUpdatedTs(past))

            .withShelfLife(7, TimeInUnits.TimeUnit.DAY, "53453245").customized(p -> p.setUpdatedTs(present))
            .with(KnownMdmParams.DANGEROUS_GOOD, true).customized(p -> p.setUpdatedTs(present))
            .with(KnownMdmParams.RSL_OUT_PERCENTS, 1L).customized(p -> p.setUpdatedTs(present))
            .with(KnownMdmParams.WEIGHT_GROSS, 60L).customized(p -> p.setUpdatedTs(present))
            .with(KnownMdmParams.RSL_OUT_DAYS, 16L).customized(p -> p.setUpdatedTs(present))
            .with(KnownMdmParams.RSL_IN_DAYS, 96L).customized(p -> p.setUpdatedTs(present))
            .with(KnownMdmParams.LENGTH, 33L).customized(p -> p.setUpdatedTs(present))
            .with(KnownMdmParams.HEIGHT, 44L).customized(p -> p.setUpdatedTs(present))
            .with(KnownMdmParams.WIDTH, 22L).customized(p -> p.setUpdatedTs(present))

            .with(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID, "23454235").customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.RSL_ACTIVATION_TS, "01.03.2022 12:34:00")
            .with(KnownMdmParams.EXPIR_DATE, false).customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.RSL_IN_PERCENTS, 12L)
            .with(KnownMdmParams.WEIGHT_TARE, -11L).customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.WEIGHT_NET, 71L).customized(p -> p.setUpdatedTs(future))
            .buildSupplierOnly();

        ServiceSsku supplierSsku2 = new CommonSskuBuilder(paramCache, SUPPLIER_KEY2)
            .with(KnownMdmParams.RSL_ACTIVATION_TS, "01.04.2022 12:34:00")
            .with(KnownMdmParams.EXPIR_DATE, false).customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.WEIGHT_TARE, 9L).customized(p -> p.setUpdatedTs(past))
            .with(KnownMdmParams.WEIGHT_NET, 8L).customized(p -> p.setUpdatedTs(past))

            .withGuaranteePeriod(22, TimeInUnits.TimeUnit.HOUR, "vvv").customized(p -> p.setUpdatedTs(present))
            .with(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID, "888786").customized(p -> p.setUpdatedTs(present))
            .with(KnownMdmParams.USE_IN_MERCURY, true).customized(p -> p.setUpdatedTs(present))
            .with(KnownMdmParams.RSL_IN_PERCENTS, 55L)
            .with(KnownMdmParams.MANUFACTURER, "zzz").customized(p -> p.setUpdatedTs(present))

            .withShelfLife(17, TimeInUnits.TimeUnit.DAY, "999999").customized(p -> p.setUpdatedTs(future))
            .withLifeTime(4, TimeInUnits.TimeUnit.MONTH, "yyy").customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.RSL_OUT_PERCENTS, 665L).customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.DANGEROUS_GOOD, false).customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.RSL_OUT_DAYS, 123L).customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.RSL_IN_DAYS, 87L).customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.BOX_COUNT, 10L).customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.WEIGHT_GROSS, 94L).customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.LENGTH, 7L).customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.HEIGHT, 6L).customized(p -> p.setUpdatedTs(future))
            .with(KnownMdmParams.WIDTH, 220L).customized(p -> p.setUpdatedTs(future))
            .buildSupplierOnly();

        List<ShopSkuKey> keyVariants = List.of(BUSINESS_KEY, SUPPLIER_KEY1, SUPPLIER_KEY2);
        for (ShopSkuKey supposedKey : keyVariants) {
            ServiceSsku expected = new CommonSskuBuilder(paramCache, supposedKey)
                .withGuaranteePeriod(21, TimeInUnits.TimeUnit.YEAR, "wow")
                .withLifeTime(4, TimeInUnits.TimeUnit.MONTH, "yyy")
                .withShelfLife(17, TimeInUnits.TimeUnit.DAY, "999999")
                .with(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID, "23454235")

                .with(KnownMdmParams.EXPIR_DATE, false)
                .with(KnownMdmParams.MANUFACTURER, "ПАО 'ПАО'")
                .with(KnownMdmParams.DANGEROUS_GOOD, false)
                .with(KnownMdmParams.USE_IN_MERCURY, true)
                .with(KnownMdmParams.BOX_COUNT, 10L)

                .with(KnownMdmParams.RSL_IN_DAYS, 87L)
                .with(KnownMdmParams.RSL_OUT_DAYS, 123L)
                .with(KnownMdmParams.RSL_IN_PERCENTS, 55L)
                .with(KnownMdmParams.RSL_OUT_PERCENTS, 665L)
                .with(KnownMdmParams.RSL_ACTIVATION_TS, "01.04.2022 12:34:00")

                .with(KnownMdmParams.WEIGHT_TARE, -11L)
                .with(KnownMdmParams.WEIGHT_NET, 71L)
                .with(KnownMdmParams.WEIGHT_GROSS, 94L)
                .with(KnownMdmParams.LENGTH, 7L)
                .with(KnownMdmParams.HEIGHT, 6L)
                .with(KnownMdmParams.WIDTH, 220L)
                .buildSupplierOnly();
            CommonSsku merged = service.merge(businessSsku, List.of(supplierSsku1, supplierSsku2), supposedKey);
            assertSskuEquals(merged, expected);
        }
    }

    @Test
    public void checkUniqueUnionMergeRule() {
        ServiceSsku businessSsku = new CommonSskuBuilder(paramCache, BUSINESS_KEY)
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Мордор", "Шир")
            .with(KnownMdmParams.VETIS_GUID, "guid1", "guid2")
            .with(KnownMdmParams.DOCUMENT_REG_NUMBER, "A", "B")
            .buildSupplierOnly();

        ServiceSsku supplierSsku1 = new CommonSskuBuilder(paramCache, SUPPLIER_KEY1)
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Гондор", "Рохан", "Мордор")
            .with(KnownMdmParams.VETIS_GUID, "guid3", "guid2")
            .with(KnownMdmParams.GTIN, "зелёный", "красный")
            .buildSupplierOnly();

        ServiceSsku supplierSsku2 = new CommonSskuBuilder(paramCache, SUPPLIER_KEY2)
            .with(KnownMdmParams.GTIN, "зелёный", "пурпурный", "красный")
            .with(KnownMdmParams.DOCUMENT_REG_NUMBER, "C", "B")
            .buildSupplierOnly();


        List<ShopSkuKey> keyVariants = List.of(BUSINESS_KEY, SUPPLIER_KEY1, SUPPLIER_KEY2);
        for (ShopSkuKey requestedKey : keyVariants) {
            CommonSsku merged = service.merge(businessSsku, List.of(supplierSsku1, supplierSsku2),
                requestedKey);
            ServiceSsku expected = new CommonSskuBuilder(paramCache, requestedKey)
                .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Гондор", "Мордор", "Рохан", "Шир")
                .with(KnownMdmParams.GTIN, "зелёный", "красный", "пурпурный")
                .with(KnownMdmParams.VETIS_GUID, "guid1", "guid2", "guid3")
                .with(KnownMdmParams.DOCUMENT_REG_NUMBER, "A", "B", "C")
                .buildSupplierOnly();
            assertSskuEquals(merged, expected);
        }
    }

    @Test
    public void checkServiceSplitMergeRule() {
        ServiceSsku businessSsku = new CommonSskuBuilder(paramCache, BUSINESS_KEY).buildSupplierOnly();

        ServiceSsku supplierSsku1 = new CommonSskuBuilder(paramCache, SUPPLIER_KEY1)
            .withVat(VatRate.VAT_18)
            .with(KnownMdmParams.MIN_SHIPMENT, 11L)
            .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 22L)
            .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 33L)
            .with(KnownMdmParams.DELIVERY_TIME, 44L)
            .with(KnownMdmParams.SUPPLY_SCHEDULE, new MdmParamOption().setId(2), new MdmParamOption().setId(3))
            .buildSupplierOnly();

        ServiceSsku supplierSsku2 = new CommonSskuBuilder(paramCache, SUPPLIER_KEY2)
            .withVat(VatRate.VAT_20)
            .with(KnownMdmParams.MIN_SHIPMENT, 111L)
            .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 222L)
            .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 333L)
            .with(KnownMdmParams.DELIVERY_TIME, 444L)
            .with(KnownMdmParams.SUPPLY_SCHEDULE, new MdmParamOption().setId(3), new MdmParamOption().setId(4))
            .buildSupplierOnly();

        CommonSsku expected = new CommonSskuBuilder(paramCache, BUSINESS_KEY)
            .startServiceValues(SUPPLIER_KEY1.getSupplierId())
            .withVat(VatRate.VAT_18)
            .with(KnownMdmParams.MIN_SHIPMENT, 11L)
            .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 22L)
            .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 33L)
            .with(KnownMdmParams.DELIVERY_TIME, 44L)
            .with(KnownMdmParams.SUPPLY_SCHEDULE, new MdmParamOption().setId(2), new MdmParamOption().setId(3))
            .startServiceValues(SUPPLIER_KEY2.getSupplierId())
            .withVat(VatRate.VAT_20)
            .with(KnownMdmParams.MIN_SHIPMENT, 111L)
            .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 222L)
            .with(KnownMdmParams.TRANSPORT_UNIT_SIZE, 333L)
            .with(KnownMdmParams.DELIVERY_TIME, 444L)
            .with(KnownMdmParams.SUPPLY_SCHEDULE, new MdmParamOption().setId(3), new MdmParamOption().setId(4))
            .endServiceValues()
            .build();
        CommonSsku merged = service.merge(businessSsku, List.of(supplierSsku1, supplierSsku2), BUSINESS_KEY);
        assertSskuEquals(merged, expected);
    }

    @Test
    public void whenMergeDisabledAndRequestedSupplierSskuShouldReturnSupplierSskuWithoutMerge() {
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, false);
        ServiceSsku businessSsku = new ServiceSsku(BUSINESS_KEY);

        ServiceSsku supplierSsku1 = new CommonSskuBuilder(paramCache, SUPPLIER_KEY1)
            .with(KnownMdmParams.QUANTUM_OF_SUPPLY, 16L)
            .with(KnownMdmParams.BOX_COUNT, 33L)
            .with(KnownMdmParams.HEAVY_GOOD, true)
            .with(KnownMdmParams.MANUFACTURER, "Ливерпульский ливерно-оружейный завод \"Жуки-ударники\"")
            .buildSupplierOnly();

        ServiceSsku supplierSsku2 = new CommonSskuBuilder(paramCache, SUPPLIER_KEY2)
            .with(KnownMdmParams.BOX_COUNT, 20L)
            .with(KnownMdmParams.HEAVY_GOOD, true)
            .with(KnownMdmParams.MANUFACTURER, "ПАО Арабеска")
            .buildSupplierOnly();

        ServiceSsku supplierSsku3 = new CommonSskuBuilder(paramCache, SUPPLIER_KEY3)
            .with(KnownMdmParams.BOX_COUNT, 20L)
            .with(KnownMdmParams.HEAVY_GOOD, false)
            .with(KnownMdmParams.MANUFACTURER, "Ливерпульский ливерно-оружейный завод \"Жуки-ударники\"")
            .buildSupplierOnly();

        ServiceSsku supplierSsku4 = new CommonSskuBuilder(paramCache, SUPPLIER_KEY4)
            .with(KnownMdmParams.BOX_COUNT, 42L)
            .with(KnownMdmParams.HEAVY_GOOD, false)
            .with(KnownMdmParams.MANUFACTURER, "ПАО Арабеска")
            .buildSupplierOnly();

        CommonSsku merged = service.merge(businessSsku, List.of(
            supplierSsku1,
            supplierSsku2,
            supplierSsku3,
            supplierSsku4
        ), supplierSsku2.getKey());
        assertSskuEquals(merged, supplierSsku2);
    }

    private static void assertSskuEquals(CommonSsku actual, ServiceSsku expected) {
        Assertions.assertThat(actual.getKey()).isEqualTo(expected.getKey());
        Assertions.assertThat(actual.getBaseValues())
            .usingElementComparator(COMPARATOR)
            .containsExactlyInAnyOrderElementsOf(expected.getValues());
    }

    private static void assertSskuEquals(CommonSsku actual, CommonSsku expected) {
        Assertions.assertThat(actual.getKey()).isEqualTo(expected.getKey());
        Assertions.assertThat(actual.getBaseValues())
            .usingElementComparator(COMPARATOR)
            .containsExactlyInAnyOrderElementsOf(expected.getBaseValues());
        Assertions.assertThat(actual.getServiceSskus().size()).isEqualTo(expected.getServiceSskus().size());
        actual.getServiceSskus().forEach((serviceId, actualServiceSsku) -> {
            ServiceSsku expectedServiceSsku = expected.getServiceSsku(serviceId).orElseThrow();
            Assertions.assertThat(actualServiceSsku.getValues())
                .usingElementComparator(COMPARATOR)
                .containsExactlyInAnyOrderElementsOf(expectedServiceSsku.getValues());
        });
    }
}
