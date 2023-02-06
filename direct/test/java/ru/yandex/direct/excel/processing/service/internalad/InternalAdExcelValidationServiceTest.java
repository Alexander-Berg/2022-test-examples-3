package ru.yandex.direct.excel.processing.service.internalad;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.excel.processing.exception.ExcelValidationException;
import ru.yandex.direct.excel.processing.model.ObjectType;
import ru.yandex.direct.excel.processing.model.internalad.ExcelFetchedData;
import ru.yandex.direct.excel.processing.model.internalad.ExcelSheetFetchedData;
import ru.yandex.direct.excel.processing.model.internalad.InternalAdGroupRepresentation;
import ru.yandex.direct.excel.processing.model.internalad.InternalBannerRepresentation;
import ru.yandex.direct.excel.processing.model.internalad.RetargetingConditionRepresentation;
import ru.yandex.direct.excel.processing.model.validation.AdGroupIdsParams;
import ru.yandex.direct.excel.processing.service.internalad.validation.AdSheetFetchedDataValidator;
import ru.yandex.direct.excel.processing.validation.defects.ExcelDefectIds;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.excel.processing.utils.InternalTestDataKt.createSheetFetchedData;
import static ru.yandex.direct.excel.processing.utils.InternalTestDataKt.getDefaultInternalAdGroupRepresentation;
import static ru.yandex.direct.excel.processing.utils.InternalTestDataKt.getDefaultInternalBannerRepresentation;
import static ru.yandex.direct.excel.processing.validation.defects.Defects.adGroupByIdNotFound;
import static ru.yandex.direct.excel.processing.validation.defects.Defects.adGroupIdRequiredForExistingAd;
import static ru.yandex.direct.excel.processing.validation.defects.Defects.inconsistentColumnTitles;
import static ru.yandex.direct.excel.processing.validation.defects.Defects.newAdGroupByNameNotFound;
import static ru.yandex.direct.excel.processing.validation.defects.Defects.notAllowedAddNewAdGroupWithNewAdForImportMode;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(MockitoJUnitRunner.class)
@ParametersAreNonnullByDefault
public class InternalAdExcelValidationServiceTest {

    private InternalAdGroupRepresentation adGroupRepresentation;
    private ExcelSheetFetchedData<InternalAdGroupRepresentation> adGroupsSheet;
    private InternalBannerRepresentation bannerRepresentation;
    private ExcelSheetFetchedData<InternalBannerRepresentation> adsSheet;

    @Mock
    private CryptaSegmentDictionariesService cryptaSegmentDictionariesService;

    @Before
    public void initTestData() {
        adGroupRepresentation = getDefaultInternalAdGroupRepresentation();
        adGroupsSheet = createSheetFetchedData(List.of(adGroupRepresentation));
        bannerRepresentation = getDefaultInternalBannerRepresentation(adGroupRepresentation.getAdGroup());
        adsSheet = createSheetFetchedData(adGroupsSheet.getSheetDescriptor(), List.of(bannerRepresentation));

        when(cryptaSegmentDictionariesService.getCryptaByGoalId(anyLong()))
                .thenReturn((Goal) new Goal()
                        .withKeyword("123")
                        .withKeywordValue("456"));
    }


    @Test
    public void checkValidateExcelFetchedData() {
        var excelFetchedData = ExcelFetchedData.create(adGroupsSheet, List.of(adsSheet));
        ValidationResult<ExcelFetchedData, Defect> vr = validateExcelFetchedData(excelFetchedData);

        assertThat(vr)
                .is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void checkValidateExcelFetchedData_WhenGotInconsistentColumnTitles_ForAdGroups() {
        adGroupsSheet = ExcelSheetFetchedData.create(RandomStringUtils.randomAlphabetic(7),
                adGroupsSheet.getSheetDescriptor(), List.of(adGroupRepresentation),
                Collections.emptyList(), List.of(RandomStringUtils.randomAlphanumeric(3)));

        var excelFetchedData = ExcelFetchedData.create(adGroupsSheet, List.of(adsSheet));
        ValidationResult<ExcelFetchedData, Defect> vr = validateExcelFetchedData(excelFetchedData);

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(field(ExcelFetchedData.AD_GROUPS_PATH)),
                        inconsistentColumnTitles(excelFetchedData.getAdGroupsSheet())
                ))));
    }

    @Test
    public void checkValidateExcelFetchedData_WhenGotInconsistentColumnTitles_ForAds() {
        adsSheet = ExcelSheetFetchedData.create(RandomStringUtils.randomAlphabetic(7),
                adGroupsSheet.getSheetDescriptor(), List.of(bannerRepresentation),
                List.of(RandomStringUtils.randomAlphanumeric(5)), Collections.emptyList());

        var excelFetchedData = ExcelFetchedData.create(adGroupsSheet, List.of(adsSheet));
        ValidationResult<ExcelFetchedData, Defect> vr = validateExcelFetchedData(excelFetchedData);

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(field(ExcelFetchedData.ADS_PATH), index(0)),
                        inconsistentColumnTitles(excelFetchedData.getAdsSheets().get(0))
                ))));
    }

    @Test
    public void checkValidateExcelFetchedData_WhenAdGroupIdRequiredForExistingAd() {
        bannerRepresentation.setAdGroupId(null);
        bannerRepresentation.getBanner().setId(RandomNumberUtils.nextPositiveLong());

        var excelFetchedData = ExcelFetchedData.create(adGroupsSheet, List.of(adsSheet));
        ValidationResult<ExcelFetchedData, Defect> vr = validateExcelFetchedData(excelFetchedData);

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(field(ExcelFetchedData.ADS_PATH), index(0),
                                field(AdSheetFetchedDataValidator.AD_PATH), index(0)),
                        adGroupIdRequiredForExistingAd(adsSheet.getSheetName(),
                                bannerRepresentation.getBanner().getId())
                ))));
    }

    @Test
    public void checkValidateExcelFetchedData_WhenNotAllowedAddNewAdGroupWithNewAdForImportMode() {
        bannerRepresentation.setAdGroupId(null);
        bannerRepresentation.getBanner().setId(null);

        var excelFetchedData = ExcelFetchedData.create(adGroupsSheet, List.of(adsSheet));
        ValidationResult<ExcelFetchedData, Defect> vr =
                validateExcelFetchedData(excelFetchedData, Set.of(ObjectType.AD));

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(field(ExcelFetchedData.ADS_PATH), index(0),
                                field(AdSheetFetchedDataValidator.AD_PATH), index(0)),
                        notAllowedAddNewAdGroupWithNewAdForImportMode(adsSheet.getSheetName(),
                                bannerRepresentation.getAdGroupName())
                ))));
    }

    @Test
    public void checkValidateExcelFetchedData_WhenNewAdGroupByNameNotFound() {
        bannerRepresentation.setAdGroupId(null)
                .setAdGroupName("invalidName");
        bannerRepresentation.getBanner().setId(null);

        var excelFetchedData = ExcelFetchedData.create(adGroupsSheet, List.of(adsSheet));
        ValidationResult<ExcelFetchedData, Defect> vr = validateExcelFetchedData(excelFetchedData);

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(field(ExcelFetchedData.ADS_PATH), index(0),
                                field(AdSheetFetchedDataValidator.AD_PATH), index(0)),
                        newAdGroupByNameNotFound(adsSheet.getSheetName(), bannerRepresentation.getAdGroupName())
                ))));
    }

    @Test
    public void checkValidateExcelFetchedData_WhenAdGroupByIdNotFound() {
        bannerRepresentation.setAdGroupId(RandomNumberUtils.nextPositiveLong());
        bannerRepresentation.getBanner().setId(null);

        var excelFetchedData = ExcelFetchedData.create(adGroupsSheet, List.of(adsSheet));
        ValidationResult<ExcelFetchedData, Defect> vr = validateExcelFetchedData(excelFetchedData);

        //noinspection ConstantConditions
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(field(ExcelFetchedData.ADS_PATH), index(0),
                                field(AdSheetFetchedDataValidator.AD_PATH), index(0)),
                        adGroupByIdNotFound(adsSheet.getSheetName(), bannerRepresentation.getAdGroupId())
                ))));
    }

    @Test
    public void checkValidateAdGroupRepresentations() {
        adGroupRepresentation.getAdGroup().setId(RandomNumberUtils.nextPositiveLong());
        adGroupRepresentation.setRetargetingConditionRepresentation(new RetargetingConditionRepresentation(
                List.of(new Rule().withType(RuleType.NOT).withGoals(
                        List.of((Goal) new Goal().withType(GoalType.GOAL),
                                (Goal) new Goal().withType(GoalType.AUDIENCE)))),
                cryptaSegmentDictionariesService
        ));

        ExcelValidationException exception = assertThrows(ExcelValidationException.class, () ->
                InternalAdExcelValidationService.validateAdGroupRepresentations(List.of(adGroupRepresentation)));
        assertThat(exception.getDefectId()).isEqualTo(ExcelDefectIds.MORE_THAN_ONE_GOAL_TYPE_IN_ONE_RULE);
        assertThat(exception.getParams()).is(matchedBy(beanDiffer(
                new AdGroupIdsParams(List.of(adGroupRepresentation.getAdGroup().getId())))
                .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void checkValidateAdGroupRepresentations_AllowMoreThanOneGoalType_IfAllHasCryptaType() {
        adGroupRepresentation.getAdGroup().setId(RandomNumberUtils.nextPositiveLong());
        adGroupRepresentation.setRetargetingConditionRepresentation(new RetargetingConditionRepresentation(
                List.of(new Rule().withType(RuleType.NOT).withGoals(
                        List.of(defaultGoalByType(GoalType.INTERNAL), defaultGoalByType(GoalType.BEHAVIORS)))),
                cryptaSegmentDictionariesService
        ));

        InternalAdExcelValidationService.validateAdGroupRepresentations(List.of(adGroupRepresentation));
    }

    private ValidationResult<ExcelFetchedData, Defect> validateExcelFetchedData(ExcelFetchedData excelFetchedData) {
        return validateExcelFetchedData(excelFetchedData, Set.of(ObjectType.AD_GROUP, ObjectType.AD));
    }

    private ValidationResult<ExcelFetchedData, Defect> validateExcelFetchedData(ExcelFetchedData excelFetchedData,
                                                                                Set<ObjectType> objectTypesForImport) {
        return InternalAdExcelValidationService.validateExcelFetchedData(excelFetchedData, objectTypesForImport);
    }

}
