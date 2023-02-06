package ru.yandex.market.mboc.common.masterdata.services.united;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.VerdictFeature;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictGeneratorHelper;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class VerdictUnitedProcessingServiceSetupTest extends UnitedProcessingServiceSetupBaseTest {

    private static final ShopSkuKey BUSINESS_KEY = new ShopSkuKey(1000, "sku");
    private static final ShopSkuKey STAGE3_KEY1 = new ShopSkuKey(31, "sku");

    private static final Integer CATEGORY_ID = 123;

    @Test
    public void whenExpirDateExistOnMskuThenCheckIt() {
        // given
        prepareBusinessGroup(BUSINESS_KEY.getSupplierId(), STAGE3_KEY1.getSupplierId());
        long mskuId = 2222L;
        // Правильная поставщиковская ССКУ c плохим shelfLife
        var eoxSsku = SilverCommonSsku.fromCommonSsku(builder(BUSINESS_KEY)
                .with(KnownMdmParams.WIDTH, 14L)
                .with(KnownMdmParams.HEIGHT, 14L)
                .with(KnownMdmParams.LENGTH, 14L)
                .with(KnownMdmParams.WEIGHT_GROSS, 14L)
                .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Китай")
                .with(KnownMdmParams.SHELF_LIFE, 1000000L)
                .with(KnownMdmParams.SHELF_LIFE_UNIT, new MdmParamOption(2))
                .build(),
            new MasterDataSource(MasterDataSourceType.SUPPLIER, "DATACAMP"));
        silverSskuRepository.insertOrUpdateSsku(eoxSsku);
        sskuExistenceRepository.markExistence(List.of(STAGE3_KEY1), true);

        CommonMsku commonMsku = prepareMskuWithShelfLife(mskuId, true);
        mskuRepository.insertOrUpdateMsku(commonMsku);

        mappingsCacheRepository.insert(
            new MappingCacheDao().setCategoryId(CATEGORY_ID)
                .setShopSkuKey(BUSINESS_KEY)
                .setMskuId(mskuId)
        );

        // when
        processShopSkuKeys(List.of(BUSINESS_KEY, STAGE3_KEY1));

        // then
        var goldenVerdict = sskuGoldenVerdictRepository.findById(BUSINESS_KEY);
        Assertions.assertThat(goldenVerdict.isValid()).isFalse();
        Assertions.assertThat(goldenVerdict.getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.SHELF_LIFE))));

        var partnerVerdict = sskuPartnerVerdictRepository.findById(BUSINESS_KEY);
        Assertions.assertThat(partnerVerdict.isValid()).isFalse();
        Assertions.assertThat(partnerVerdict.getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get()
                    .excelValueMustBeInRange(SskuMasterDataFields.SHELF_LIFE, "1000000 лет", "3 дня", "10 лет"))));
    }

    @Test
    public void whenExpirDateTrueOnMskuButNoShelfLifeGenerateVerdict() {
        // given
        prepareBusinessGroup(BUSINESS_KEY.getSupplierId(), STAGE3_KEY1.getSupplierId());
        // just normal ssku with empty services
        long mskuId = 2222L;
        // Правильная поставщиковская ССКУ c плохим shelfLife
        var eoxSsku = SilverCommonSsku.fromCommonSsku(builder(BUSINESS_KEY)
                .with(KnownMdmParams.WIDTH, 14L)
                .with(KnownMdmParams.HEIGHT, 14L)
                .with(KnownMdmParams.LENGTH, 14L)
                .with(KnownMdmParams.WEIGHT_GROSS, 14L)
                .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Китай")
                .build(),
            new MasterDataSource(MasterDataSourceType.SUPPLIER, "DATACAMP"));
        silverSskuRepository.insertOrUpdateSsku(eoxSsku);
        sskuExistenceRepository.markExistence(List.of(STAGE3_KEY1), true);

        CommonMsku commonMsku = prepareMskuWithShelfLife(mskuId, true);
        mskuRepository.insertOrUpdateMsku(commonMsku);

        mappingsCacheRepository.insert(
            new MappingCacheDao().setCategoryId(CATEGORY_ID)
                .setShopSkuKey(BUSINESS_KEY)
                .setMskuId(mskuId)
        );

        // when
        processShopSkuKeys(List.of(BUSINESS_KEY, STAGE3_KEY1));

        // then
        var goldenVerdict = sskuGoldenVerdictRepository.findById(BUSINESS_KEY);
        Assertions.assertThat(goldenVerdict.isValid()).isFalse();
        Assertions.assertThat(goldenVerdict.getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.SHELF_LIFE))));

        var partnerVerdict = sskuPartnerVerdictRepository.findById(BUSINESS_KEY);
        Assertions.assertThat(partnerVerdict.isValid()).isFalse();
        Assertions.assertThat(partnerVerdict.getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.SHELF_LIFE))));

    }

    @Test
    public void whenExpirDateFalseOnMskuButNoShelfLifeNotGenerateVerdict() {
        // given
        prepareBusinessGroup(BUSINESS_KEY.getSupplierId(), STAGE3_KEY1.getSupplierId());
        // just normal ssku with empty services
        long mskuId = 2222L;
        // Правильная поставщиковская ССКУ c плохим shelfLife
        var eoxSsku = SilverCommonSsku.fromCommonSsku(builder(BUSINESS_KEY)
                .with(KnownMdmParams.WIDTH, 14L)
                .with(KnownMdmParams.HEIGHT, 14L)
                .with(KnownMdmParams.LENGTH, 14L)
                .with(KnownMdmParams.WEIGHT_GROSS, 14L)
                .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Россия", "Китай")
                .build(),
            new MasterDataSource(MasterDataSourceType.SUPPLIER, "DATACAMP"));
        silverSskuRepository.insertOrUpdateSsku(eoxSsku);
        sskuExistenceRepository.markExistence(List.of(STAGE3_KEY1), true);

        CommonMsku commonMsku = prepareMskuWithShelfLife(mskuId, false);
        mskuRepository.insertOrUpdateMsku(commonMsku);

        mappingsCacheRepository.insert(
            new MappingCacheDao().setCategoryId(CATEGORY_ID)
                .setShopSkuKey(BUSINESS_KEY)
                .setMskuId(mskuId)
        );

        // when
        processShopSkuKeys(List.of(BUSINESS_KEY, STAGE3_KEY1));

        // then
        var goldenVerdict = sskuGoldenVerdictRepository.findById(BUSINESS_KEY);
        Assertions.assertThat(goldenVerdict.isValid()).isTrue();

        var partnerVerdict = sskuPartnerVerdictRepository.findById(BUSINESS_KEY);
        Assertions.assertThat(partnerVerdict.isValid()).isTrue();
    }

    private CommonMsku prepareMskuWithShelfLife(Long mskuId, boolean isShelfLifeRequired) {
        MskuParamValue shelfLifeRequired = new MskuParamValue();
        shelfLifeRequired
            .setMasterDataSourceType(MasterDataSourceType.SUPPLIER)
            .setSourceUpdatedTs(Instant.now())
            .setMdmParamId(KnownMdmParams.EXPIR_DATE)
            .setBool(isShelfLifeRequired);
        MskuParamValue someParam = new MskuParamValue();
        someParam
            .setMasterDataSourceType(MasterDataSourceType.SUPPLIER)
            .setSourceUpdatedTs(Instant.now())
            .setMdmParamId(KnownMdmParams.WIDTH)
            .setNumeric(BigDecimal.valueOf(50));

        return new CommonMsku(new ModelKey(CATEGORY_ID, mskuId), List.of(shelfLifeRequired, someParam))
            .setMskuId(mskuId);
    }


}
