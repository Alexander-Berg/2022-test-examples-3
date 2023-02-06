package ru.yandex.market.mbo.mdm.common.masterdata.services.verdict;

import java.util.List;
import java.util.Map;
import java.util.Set;

import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampValidationResult;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierIdGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.VerdictFeature;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierNonCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author dmserebr
 * @date 22/10/2020
 */
public class VerdictProtoConverterTest {
    private BeruId beruId;
    private MdmErrorInfoMerger merger;
    private MdmSupplierRepository mdmSupplierRepository;
    private VerdictProtoConverter verdictProtoConverter;

    @Before
    public void setup() {
        beruId = new BeruIdMock();
        merger = mock(MdmErrorInfoMerger.class);
        mdmSupplierRepository = mock(MdmSupplierRepository.class);
        verdictProtoConverter = new VerdictProtoConverter(beruId, merger,
            new MdmSupplierNonCachingService(mdmSupplierRepository, new StorageKeyValueServiceMock()));
        Answer<Object> returnArgumentWithoutChanges = it -> it.getArguments()[0];
        when(merger.merge(any())).then(returnArgumentWithoutChanges);
    }

    /**
     * Check deprecated methods, not needed anymore
     */
    @Deprecated(forRemoval = true)
    @Test
    public void testConvertOkVerdictDeprecated() {
        var verdict = VerdictProtoConverter.convertVerdict(
            VerdictFeature.CPA, VerdictGeneratorHelper.createOkVerdict(VerdictFeature.CPA), false);
        Assertions.assertThat(verdict)
            .isEqualTo(DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(false)
                    .addApplications(DataCampValidationResult.Feature.CPA))
                .build());
        Assertions.assertThat(verdict.getResults(0).hasIsValid()).isFalse();

        verdict = VerdictProtoConverter.convertVerdict(
            VerdictFeature.FULFILLMENT, VerdictGeneratorHelper.createOkVerdict(VerdictFeature.FULFILLMENT), false);
        Assertions.assertThat(verdict)
            .isEqualTo(DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(false)
                    .addApplications(DataCampValidationResult.Feature.FULFILLMENT))
                .build());
        Assertions.assertThat(verdict.getResults(0).hasIsValid()).isFalse();
    }

    /**
     * Check deprecated methods, not needed anymore
     */
    @Deprecated(forRemoval = true)
    @Test
    public void testConvertForbiddingVerdictDeprecated() {
        List<ErrorInfo> errors = List.of(
            MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.MANUFACTURER_COUNTRY),
            MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.MANUFACTURER));
        var verdict = VerdictProtoConverter.convertVerdict(
            VerdictFeature.CPA, VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.CPA, errors), false);
        Assertions.assertThat(verdict)
            .isEqualTo(DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(true)
                    .addApplications(DataCampValidationResult.Feature.CPA)
                    .addMessages(DataCampExplanation.Explanation.newBuilder()
                        .setNamespace("mboc.ci.error")
                        .setCode("mboc.error.excel-value-is-required")
                        .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                            .setName("header")
                            .setValue("Страна производства"))
                        .setText("Отсутствует значение для колонки 'Страна производства'")
                        .setLevel(DataCampExplanation.Explanation.Level.ERROR)
                        .setDetails("{\"header\":\"Страна производства\"}"))
                    .addMessages(DataCampExplanation.Explanation.newBuilder()
                        .setNamespace("mboc.ci.error")
                        .setCode("mboc.error.excel-value-is-required")
                        .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                            .setName("header")
                            .setValue("Изготовитель"))
                        .setText("Отсутствует значение для колонки 'Изготовитель'")
                        .setLevel(DataCampExplanation.Explanation.Level.ERROR)
                        .setDetails("{\"header\":\"Изготовитель\"}")))
                .build());
        Assertions.assertThat(verdict.getResults(0).hasIsValid()).isFalse();
        verdict = VerdictProtoConverter.convertVerdict(
            VerdictFeature.CPA,
            VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.CPA, errors).setBanned(true), false);
        Assertions.assertThat(verdict)
            .isEqualTo(DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(true)
                    .addApplications(DataCampValidationResult.Feature.CPA)
                    .addMessages(DataCampExplanation.Explanation.newBuilder()
                        .setNamespace("mboc.ci.error")
                        .setCode("mboc.error.excel-value-is-required")
                        .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                            .setName("header")
                            .setValue("Страна производства"))
                        .setText("Отсутствует значение для колонки 'Страна производства'")
                        .setLevel(DataCampExplanation.Explanation.Level.ERROR)
                        .setDetails("{\"header\":\"Страна производства\"}"))
                    .addMessages(DataCampExplanation.Explanation.newBuilder()
                        .setNamespace("mboc.ci.error")
                        .setCode("mboc.error.excel-value-is-required")
                        .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                            .setName("header")
                            .setValue("Изготовитель"))
                        .setText("Отсутствует значение для колонки 'Изготовитель'")
                        .setLevel(DataCampExplanation.Explanation.Level.ERROR)
                        .setDetails("{\"header\":\"Изготовитель\"}")))
                .build());
        Assertions.assertThat(verdict.getResults(0).hasIsValid()).isFalse();
        verdict = VerdictProtoConverter.convertVerdict(
            VerdictFeature.CPA,
            VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.CPA, errors).setBanned(false), false);
        Assertions.assertThat(verdict)
            .isEqualTo(DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(false)
                    .addApplications(DataCampValidationResult.Feature.CPA)
                    .addMessages(DataCampExplanation.Explanation.newBuilder()
                        .setNamespace("mboc.ci.error")
                        .setCode("mboc.error.excel-value-is-required")
                        .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                            .setName("header")
                            .setValue("Страна производства"))
                        .setText("Отсутствует значение для колонки 'Страна производства'")
                        .setLevel(DataCampExplanation.Explanation.Level.ERROR)
                        .setDetails("{\"header\":\"Страна производства\"}"))
                    .addMessages(DataCampExplanation.Explanation.newBuilder()
                        .setNamespace("mboc.ci.error")
                        .setCode("mboc.error.excel-value-is-required")
                        .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                            .setName("header")
                            .setValue("Изготовитель"))
                        .setText("Отсутствует значение для колонки 'Изготовитель'")
                        .setLevel(DataCampExplanation.Explanation.Level.ERROR)
                        .setDetails("{\"header\":\"Изготовитель\"}")))
                .build());
        Assertions.assertThat(verdict.getResults(0).hasIsValid()).isFalse();
    }

    /**
     * Check deprecated methods, not needed anymore
     */
    @Deprecated(forRemoval = true)
    @Test
    public void testConvertOkPartnerVerdict() {
        var verdict = VerdictProtoConverter.convertVerdict(
            VerdictFeature.CPA, VerdictGeneratorHelper.createOkVerdict(VerdictFeature.CPA), true);
        Assertions.assertThat(verdict)
            .isEqualTo(DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(false)
                    .addApplications(DataCampValidationResult.Feature.CPA))
                .build());
        Assertions.assertThat(verdict.getResults(0).hasIsValid()).isFalse();

        verdict = VerdictProtoConverter.convertVerdict(
            VerdictFeature.FULFILLMENT, VerdictGeneratorHelper.createOkVerdict(VerdictFeature.FULFILLMENT), true);
        Assertions.assertThat(verdict)
            .isEqualTo(DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(false)
                    .addApplications(DataCampValidationResult.Feature.FULFILLMENT))
                .build());
        Assertions.assertThat(verdict.getResults(0).hasIsValid()).isFalse();
    }

    @Test
    public void testConvertForbiddenPartnerVerdictIsNotBanned() {
        List<ErrorInfo> errors = List.of(
            MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_DIMENSIONS));
        var verdict = VerdictProtoConverter.convertVerdict(
            VerdictFeature.CPA, VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.CPA, errors), true);
        Assertions.assertThat(VerdictProtoConverter.convertVerdict(
            VerdictFeature.CPA, VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.CPA, errors), true))
            .isEqualTo(DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(false)
                    .addApplications(DataCampValidationResult.Feature.CPA)
                    .addMessages(DataCampExplanation.Explanation.newBuilder()
                        .setNamespace("mboc.ci.error")
                        .setCode("mboc.error.excel-value-is-required")
                        .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                            .setName("header")
                            .setValue("Габариты в сантиметрах с учетом упаковки"))
                        .setText("Отсутствует значение для колонки 'Габариты в сантиметрах с учетом упаковки'")
                        .setLevel(DataCampExplanation.Explanation.Level.ERROR)
                        .setDetails("{\"header\":\"Габариты в сантиметрах с учетом упаковки\"}")))
                .build());
        Assertions.assertThat(verdict.getResults(0).hasIsValid()).isFalse();
    }

    /**
     * Check deprecated methods, not needed anymore
     */
    @Deprecated(forRemoval = true)
    @Test
    public void shouldConvertPartnerVerdictsWithGoldFiltering() {
        Set<ErrorInfo> goldenErrors = Set.of(
            MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.LIFE_TIME));
        List<ErrorInfo> errors = List.of(
            MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.LIFE_TIME),
            MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_DIMENSIONS)
        );
        var verdict = VerdictProtoConverter.convertPartnerVerdict(
            VerdictFeature.UNSPECIFIED,
            VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED, errors), goldenErrors);

        Assertions.assertThat(verdict)
            .isEqualTo(DataCampResolution.Verdict.newBuilder().addResults(
                    DataCampValidationResult.ValidationResult.newBuilder()
                        .setIsBanned(false)
                        .addApplications(DataCampValidationResult.Feature.UNKNOWN_FEATURE)
                        .addMessages(DataCampExplanation.Explanation.newBuilder()
                            .setNamespace("mboc.ci.error")
                            .setCode("mboc.error.excel-value-is-required")
                            .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                                .setName("header")
                                .setValue("Габариты в сантиметрах с учетом упаковки"))
                            .setText("Отсутствует значение для колонки 'Габариты в сантиметрах с учетом упаковки'")
                            .setLevel(DataCampExplanation.Explanation.Level.ERROR)
                            .setDetails("{\"header\":\"Габариты в сантиметрах с учетом упаковки\"}")))
                .build());
        Assertions.assertThat(verdict.getResults(0).hasIsValid()).isFalse();
    }

    @Test
    public void testConvertOkVerdictForNoErrors() {
        // given
        var noErrors = new CommonSskuErrorInfos(List.of(), List.of());

        // when
        var verdict = verdictProtoConverter.convertVerdictWithPersonalization(noErrors, 1,
            MdmSupplierIdGroup.createNoBusinessGroup(1));

        // then
        Assertions.assertThat(verdict).containsExactlyInAnyOrder(
            DataCampResolution.Verdict.newBuilder().addResults(
                    DataCampValidationResult.ValidationResult.newBuilder()
                        .setIsBanned(false)
                        .addApplications(DataCampValidationResult.Feature.CPA))
                .build(),
            DataCampResolution.Verdict.newBuilder().addResults(
                    DataCampValidationResult.ValidationResult.newBuilder()
                        .setIsBanned(false)
                        .addApplications(DataCampValidationResult.Feature.FULFILLMENT))
                .build()
        );
    }

    @Test
    public void testConvertOkVerdictForNoErrorsWithDbs() {
        // given
        int businessId = 1;
        int blueServiceId = 11;
        int dbsServiceId = 12;
        MdmSupplierIdGroup group = MdmSupplierIdGroup.createBusinessGroup(businessId,
            List.of(blueServiceId, dbsServiceId));
        MdmSupplierGroup fakeGroup = MdmSupplierGroup.createBusinessGroup(
            new MdmSupplier().setId(businessId),
            List.of(
                new MdmSupplier().setId(blueServiceId),
                new MdmSupplier().setId(dbsServiceId)
            ));
        var noErrors = new CommonSskuErrorInfos(List.of(), List.of());
        when(mdmSupplierRepository.findAllDbsServices()).thenReturn(Set.of(dbsServiceId));
        when(mdmSupplierRepository.getAllFlatBusinessRelations()).thenReturn(Map.of(businessId, fakeGroup));

        // when
        var verdictByDbs = verdictProtoConverter.convertVerdictWithPersonalization(noErrors, dbsServiceId, group);
        var verdictByBlue = verdictProtoConverter.convertVerdictWithPersonalization(noErrors, blueServiceId, group);
        var verdictByBiz = verdictProtoConverter.convertVerdictWithPersonalization(noErrors, businessId, group);

        // then
        Assertions.assertThat(verdictByDbs).containsExactlyInAnyOrder(
            DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(false)
                    .addApplications(DataCampValidationResult.Feature.DBS))
                .build()
        );

        Assertions.assertThat(verdictByBlue).containsExactlyInAnyOrder(
            DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(false)
                    .addApplications(DataCampValidationResult.Feature.CPA))
                .build(),
            DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(false)
                    .addApplications(DataCampValidationResult.Feature.FULFILLMENT))
                .build()
        );

        Assertions.assertThat(verdictByBiz).containsExactlyInAnyOrder(
            DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(false)
                    .addApplications(DataCampValidationResult.Feature.CPA))
                .build(),
            DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(false)
                    .addApplications(DataCampValidationResult.Feature.FULFILLMENT))
                .build(),
            DataCampResolution.Verdict.newBuilder().addResults(
                DataCampValidationResult.ValidationResult.newBuilder()
                    .setIsBanned(false)
                    .addApplications(DataCampValidationResult.Feature.DBS))
                .build()
        );
    }

    @Test
    public void testConvertBannedVerdict() {
        // given
        List<ErrorInfo> goldenErrors = List.of(
            MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.MANUFACTURER_COUNTRY),
            MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.MANUFACTURER));

        var errors = new CommonSskuErrorInfos(goldenErrors, List.of());

        // when
        var verdicts = verdictProtoConverter.convertVerdictWithPersonalization(errors, 1,
            MdmSupplierIdGroup.createNoBusinessGroup(1));

        // then
        Assertions.assertThat(verdicts).containsExactlyInAnyOrder(
            DataCampResolution.Verdict.newBuilder().addResults(
                    DataCampValidationResult.ValidationResult.newBuilder()
                        .setIsBanned(true)
                        .addApplications(DataCampValidationResult.Feature.CPA)
                        .addMessages(DataCampExplanation.Explanation.newBuilder()
                            .setNamespace("mboc.ci.error")
                            .setCode("mboc.error.excel-value-is-required")
                            .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                                .setName("header")
                                .setValue("Изготовитель"))
                            .setText("Отсутствует значение для колонки 'Изготовитель'")
                            .setLevel(DataCampExplanation.Explanation.Level.WARNING)
                            .setDetails("{\"header\":\"Изготовитель\"}"))
                        .addMessages(DataCampExplanation.Explanation.newBuilder()
                            .setNamespace("mboc.ci.error")
                            .setCode("mboc.error.excel-value-is-required")
                            .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                                .setName("header")
                                .setValue("Страна производства"))
                            .setText("Отсутствует значение для колонки 'Страна производства'")
                            .setLevel(DataCampExplanation.Explanation.Level.ERROR)
                            .setDetails("{\"header\":\"Страна производства\"}")))

                .build(),
            DataCampResolution.Verdict.newBuilder().addResults(
                    DataCampValidationResult.ValidationResult.newBuilder()
                        .setIsBanned(false)
                        .addApplications(DataCampValidationResult.Feature.FULFILLMENT)

                        .addMessages(DataCampExplanation.Explanation.newBuilder()
                            .setNamespace("mboc.ci.error")
                            .setCode("mboc.error.excel-value-is-required")
                            .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                                .setName("header")
                                .setValue("Изготовитель"))
                            .setText("Отсутствует значение для колонки 'Изготовитель'")
                            .setLevel(DataCampExplanation.Explanation.Level.WARNING)
                            .setDetails("{\"header\":\"Изготовитель\"}"))
                        .addMessages(DataCampExplanation.Explanation.newBuilder()
                            .setNamespace("mboc.ci.error")
                            .setCode("mboc.error.excel-value-is-required")
                            .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                                .setName("header")
                                .setValue("Страна производства"))
                            .setText("Отсутствует значение для колонки 'Страна производства'")
                            .setLevel(DataCampExplanation.Explanation.Level.WARNING)
                            .setDetails("{\"header\":\"Страна производства\"}")))
                .build());
    }
}
