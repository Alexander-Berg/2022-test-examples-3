package ru.yandex.market.mboc.common.masterdata;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.pgaudit.PgAuditRepository;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.services.SskuToRefreshProcessingServiceBaseTest;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class SskuToRefreshProcessingServiceIrrelevantUpdatesTest extends SskuToRefreshProcessingServiceBaseTest {
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private MasterDataRepository masterDataRepository;

    private PgAuditRepository pgAuditRepository;

    @Before
    public void setUp() throws Exception {
        pgAuditRepository = new PgAuditRepository("mdm_audit", jdbcTemplate);
    }

    @Test
    public void noPgUpdatesIfSilverNotChanged() {
        // given
        // supplier
        int businessId = 13;
        int serviceId = 12;
        MdmSupplier business = new MdmSupplier()
            .setId(businessId)
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier service = new MdmSupplier()
            .setId(serviceId)
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(businessId)
            .setBusinessEnabled(true);
        mdmSupplierRepository.insertOrUpdateAll(List.of(business, service));

        // ssku
        String shopSku = "U238";
        ShopSkuKey businessKey = new ShopSkuKey(businessId, shopSku);
        ShopSkuKey serviceKey = new ShopSkuKey(serviceId, shopSku);
        sskuExistenceRepository.markExistence(serviceKey, true);

        // mapping
        long mskuId = 2222L;
        int categoryId = 111;
        mappingsCacheRepository.insertBatch(
            new MappingCacheDao().setCategoryId(categoryId)
                .setShopSkuKey(serviceKey)
                .setMskuId(mskuId),
            new MappingCacheDao().setCategoryId(categoryId)
                .setShopSkuKey(businessKey)
                .setMskuId(mskuId)
        );

        // silver
        String silverSourceId = "vasya";
        MasterDataSourceType silverSourceType = MasterDataSourceType.SUPPLIER;
        Instant silverTs = Instant.now();
        SskuSilverParamValue silverLength = (SskuSilverParamValue) new SskuSilverParamValue()
            .setShopSkuKey(businessKey)
            .setMasterDataSourceId(silverSourceId)
            .setMasterDataSourceType(silverSourceType)
            .setSourceUpdatedTs(silverTs)
            .setMdmParamId(KnownMdmParams.LENGTH)
            .setXslName(mdmParamCache.get(KnownMdmParams.LENGTH).getXslName())
            .setNumeric(new BigDecimal(10));
        SskuSilverParamValue silverWidth = (SskuSilverParamValue) new SskuSilverParamValue()
            .setShopSkuKey(businessKey)
            .setMasterDataSourceId(silverSourceId)
            .setMasterDataSourceType(silverSourceType)
            .setSourceUpdatedTs(silverTs)
            .setMdmParamId(KnownMdmParams.WIDTH)
            .setXslName(mdmParamCache.get(KnownMdmParams.WIDTH).getXslName())
            .setNumeric(new BigDecimal(10));
        SskuSilverParamValue silverHeight = (SskuSilverParamValue) new SskuSilverParamValue()
            .setShopSkuKey(businessKey)
            .setMasterDataSourceId(silverSourceId)
            .setMasterDataSourceType(silverSourceType)
            .setSourceUpdatedTs(silverTs)
            .setMdmParamId(KnownMdmParams.HEIGHT)
            .setXslName(mdmParamCache.get(KnownMdmParams.HEIGHT).getXslName())
            .setNumeric(new BigDecimal(10));
        SskuSilverParamValue silverWeightGross = (SskuSilverParamValue) new SskuSilverParamValue()
            .setShopSkuKey(businessKey)
            .setMasterDataSourceId(silverSourceId)
            .setMasterDataSourceType(silverSourceType)
            .setSourceUpdatedTs(silverTs)
            .setMdmParamId(KnownMdmParams.WEIGHT_GROSS)
            .setXslName(mdmParamCache.get(KnownMdmParams.WEIGHT_GROSS).getXslName())
            .setNumeric(new BigDecimal(1));
        SskuSilverParamValue silverShelfLifeValue = (SskuSilverParamValue) new SskuSilverParamValue()
            .setShopSkuKey(businessKey)
            .setMasterDataSourceId(silverSourceId)
            .setMasterDataSourceType(silverSourceType)
            .setSourceUpdatedTs(silverTs)
            .setMdmParamId(KnownMdmParams.SHELF_LIFE)
            .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE).getXslName())
            .setNumeric(new BigDecimal(1));
        SskuSilverParamValue silverShelfLifeUnit = (SskuSilverParamValue) new SskuSilverParamValue()
            .setShopSkuKey(businessKey)
            .setMasterDataSourceId(silverSourceId)
            .setMasterDataSourceType(silverSourceType)
            .setSourceUpdatedTs(silverTs)
            .setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
            .setXslName(mdmParamCache.get(KnownMdmParams.SHELF_LIFE_UNIT).getXslName())
            .setOption(new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR)));
        SilverCommonSsku silverSsku = new SilverCommonSsku(silverLength.getSilverSskuKey())
            .addBaseValue(silverLength)
            .addBaseValue(silverWidth)
            .addBaseValue(silverHeight)
            .addBaseValue(silverWeightGross)
            .addBaseValue(silverShelfLifeValue)
            .addBaseValue(silverShelfLifeUnit);
        silverSskuRepository.insertOrUpdateSsku(silverSsku);

        // На всякий случай перед началом почистим аудит
        pgAuditRepository.clearAll();

        // Сделаем 5 пересчетов без изменения серебра
        for (int i = 0; i < 5; i++) {
            // when
            sskuToRefreshProcessingService.processShopSkuKeys(List.of(businessKey));

            // then
            // Золотые ВГХ постоянны
            List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findAll();
            Assertions.assertThat(referenceItems).hasSize(1);
            ReferenceItemWrapper referenceItem = referenceItems.iterator().next();
            Assertions.assertThat(referenceItem).isNotNull();
            Assertions.assertThat(referenceItem.getKey()).isEqualTo(serviceKey);
            MdmIrisPayload.ShippingUnit shippingUnit = referenceItem.getCombinedItemShippingUnit();
            Assertions.assertThat(shippingUnit).isNotNull();
            Assertions.assertThat(shippingUnit.getLengthMicrometer().getValue())
                .isEqualTo(silverLength.getNumeric().orElseThrow().movePointRight(4).longValue());
            Assertions.assertThat(shippingUnit.getWidthMicrometer().getValue())
                .isEqualTo(silverWidth.getNumeric().orElseThrow().movePointRight(4).longValue());
            Assertions.assertThat(shippingUnit.getHeightMicrometer().getValue())
                .isEqualTo(silverHeight.getNumeric().orElseThrow().movePointRight(4).longValue());
            Assertions.assertThat(shippingUnit.getWeightGrossMg().getValue())
                .isEqualTo(silverWeightGross.getNumeric().orElseThrow().movePointRight(6).longValue());

            // Сроки годности постоянны
            MasterData masterData = masterDataRepository.findById(serviceKey);
            TimeInUnits goldShelfLife = new TimeInUnits(
                silverShelfLifeValue.getNumeric()
                    .map(BigDecimal::intValueExact)
                    .orElseThrow(),
                silverShelfLifeUnit.getOption()
                    .map(MdmParamOption::getId)
                    .map(KnownMdmParams.TIME_UNITS_OPTIONS::get)
                    .orElseThrow()
            );
            Assertions.assertThat(masterData.getShelfLife()).isEqualTo(goldShelfLife);

            // Запись в reference_item была только в первый раз - новых записей в аудите нет
            Assertions.assertThat(pgAuditRepository.findAll("reference_item")).hasSize(1);

            // Запись в master_data была только в первый раз - новых записей в аудите нет
            Assertions.assertThat(pgAuditRepository.findAll("master_data")).hasSize(1);
        }
    }
}
