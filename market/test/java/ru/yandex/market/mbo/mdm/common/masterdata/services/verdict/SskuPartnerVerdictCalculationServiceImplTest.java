package ru.yandex.market.mbo.mdm.common.masterdata.services.verdict;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.VghValidationRequirements;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuPartnerVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.VerdictFeature;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.CommonSskuBuilder;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class SskuPartnerVerdictCalculationServiceImplTest extends MdmBaseDbTestClass {
    private static final ShopSkuKey SHOP_SKU_KEY = new ShopSkuKey(14, "12312");

    @Autowired
    private MdmParamCache mdmParamCache;

    @Autowired
    private VerdictCalculationByCsHelper verdictCalculationByCsHelper;

    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Before
    public void setUp() {
        storageKeyValueService.putValue(MdmProperties.CALCULATE_MD_PARTNER_VERDICTS_ENABLED_KEY, true);
        storageKeyValueService.invalidateCache();
    }

    @Test
    public void calculateVerdictAllOk() {
        var ssku = builder()
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "China")
            .build();


        List<SskuPartnerVerdictResult> result = verdictCalculationByCsHelper
            .calculateAndWritePartnerVerdictsForServiceSskus(
                emptyPartnerVerdictCalculationData(List.of(toSilver(ssku))),
                Map.of()
            );

        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isTrue();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED));
    }

    @Test
    public void calculateVerdictNoShippingUnit() {
        var ssku = builder()
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "China")
            .build();

        List<SskuPartnerVerdictResult> result = verdictCalculationByCsHelper
            .calculateAndWritePartnerVerdictsForServiceSskus(
                emptyPartnerVerdictCalculationData(List.of(toSilver(ssku))),
                Map.of()
            );

        // at the moment, we do not require weight-dimensions from supplier
        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isTrue();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED));
    }

    @Test
    public void calculateVerdictWeightTooLarge() {
        var ssku = builder()
            .withVghAfterInheritance(10, 10, 10, 999)
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "China")
            .build();

        List<SskuPartnerVerdictResult> result = verdictCalculationByCsHelper
            .calculateAndWritePartnerVerdictsForServiceSskus(
                emptyPartnerVerdictCalculationData(List.of(toSilver(ssku))),
                Map.of()
            );

        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isFalse();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED).getErrorInfos())
            .containsExactlyInAnyOrder(
                MbocErrors.get().excelValueMustBeInRange(
                    SskuMasterDataFields.WEIGHT_GROSS, "999",
                    WeightDimensionsValidator.WEIGHT_MIN.toString(),
                    WeightDimensionsValidator.WEIGHT_MAX.toString()),
                MbocErrors.get().excelWeightDimensionsInconsistent(
                    SskuMasterDataFields.WEIGHT_GROSS,
                    new BigDecimal(999), new BigDecimal(10), new BigDecimal(10), new BigDecimal(10)));
    }

    @Test
    public void testRecomputeSskuPartnerVerdictIfNotExist() {
        // when
        var ssku = builder()
            .build();

        List<SskuPartnerVerdictResult> results = verdictCalculationByCsHelper
            .calculateAndWritePartnerVerdictsForServiceSskus(
                emptyPartnerVerdictCalculationData(List.of(toSilver(ssku))),
                Map.of()
            );

        // then
        Assertions.assertThat(results.get(0).isValid()).isFalse();
        Assertions.assertThat(results.get(0).getKey()).isEqualTo(SHOP_SKU_KEY);
        Assertions.assertThat(results.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(results.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.MANUFACTURER_COUNTRY))));
    }

    @Test
    public void testMasterDataVersionNotSavedAfterVerdictCalculationForServiceKeyFromNonBusinessGroup() {
        // give
        var ssku = builder()
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "China")
            .build();

        // when

        List<SskuPartnerVerdictResult> results = verdictCalculationByCsHelper
            .calculateAndWritePartnerVerdictsForServiceSskus(
                emptyPartnerVerdictCalculationData(List.of(toSilver(ssku))),
                Map.of()
            );

        Assertions.assertThat(results).hasSize(1);

        Assertions.assertThat(results.get(0).isValid()).isTrue();
        Assertions.assertThat(results.get(0).getKey()).isEqualTo(SHOP_SKU_KEY);
        Assertions.assertThat(results.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(results.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED));

        Assertions.assertThat(results.get(0).getContentVersionId()).isNotNull();
    }

    private static PartnerVerdictCalculationData emptyPartnerVerdictCalculationData(List<SilverCommonSsku> sskus) {
        return new PartnerVerdictCalculationData(
            Map.of(SHOP_SKU_KEY, List.of()),
            sskus,
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            List.of(),
            Map.of(),
            Map.of(),
            VghValidationRequirements.NO_REQUIREMENTS);
    }

    private CommonSskuBuilder builder() {
        return new CommonSskuBuilder(mdmParamCache, SHOP_SKU_KEY);
    }

    private SilverCommonSsku toSilver(CommonSsku commonSsku) {
        return SilverCommonSsku.fromCommonSsku(commonSsku, new MasterDataSource(MasterDataSourceType.SUPPLIER, "123"));
    }

}
