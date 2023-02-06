package ru.yandex.direct.web.entity.excel.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.LongStreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.NumberDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.entity.excel.model.ExcelFileKey;
import ru.yandex.direct.web.entity.excel.model.UploadedImageInfo;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdExportMode;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdExportRequest;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportMode;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.validation.result.PathHelper.pathFromStrings;
import static ru.yandex.direct.web.entity.excel.ExcelTestData.getDefaultInternalAdImportRequest;
import static ru.yandex.direct.web.entity.excel.controller.ExcelController.EXCEL_FILE_PARAM;
import static ru.yandex.direct.web.entity.excel.service.InternalAdValidationService.MAX_IDS_PER_REQUEST;
import static ru.yandex.direct.web.entity.excel.service.validation.ExcelConstraints.XLS_CONTENT_TYPE;
import static ru.yandex.direct.web.entity.excel.service.validation.ExcelDefects.fileContentTypeIsNotSupported;
import static ru.yandex.direct.web.entity.excel.service.validation.ExcelDefects.fileSizeGreaterThanMax;
import static ru.yandex.direct.web.entity.excel.service.validation.InternalAdPathConverters.IMPORT_EXCEL_FILE_CONTENT_TYPE_PATH;
import static ru.yandex.direct.web.entity.excel.service.validation.InternalAdPathConverters.IMPORT_EXCEL_FILE_SIZE_PATH;
import static ru.yandex.direct.web.entity.excel.service.validation.InternalAdPathConverters.toImportExcelFilePath;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class InternalAdValidationServiceTest {

    private static final Long INVALID_ID = -1L;

    @SuppressWarnings("unused")
    private Object[] parametrizedTestData_ForSuccessResult_ValidateExportRequest() {
        return new Object[][]{
                {"request with campaignId", initRequest()
                        .withCampaignIds(Set.of(RandomNumberUtils.nextPositiveLong()))},

                {"request with adGroupId", initRequest()
                        .withAdGroupIds(Set.of(RandomNumberUtils.nextPositiveLong()))},
                {"request with adGroupIds", initRequest()
                        .withAdGroupIds(Set.of(RandomNumberUtils.nextPositiveLong(),
                        RandomNumberUtils.nextPositiveLong()))},

                {"request with adId", initRequest()
                        .withAdIds(Set.of(RandomNumberUtils.nextPositiveLong()))},
                {"request with adIds", initRequest()
                        .withAdIds(Set.of(RandomNumberUtils.nextPositiveLong(),
                        RandomNumberUtils.nextPositiveLong()))},

                {"request with ONLY_AD_GROUPS export mode", initRequest()
                        .withCampaignIds(Set.of(RandomNumberUtils.nextPositiveLong()))
                        .withExportMode(InternalAdExportMode.ONLY_AD_GROUPS)},
                {"request with AD_GROUPS_WITH_ADS export mode", initRequest()
                        .withCampaignIds(Set.of(RandomNumberUtils.nextPositiveLong()))
                        .withExportMode(InternalAdExportMode.AD_GROUPS_WITH_ADS)},
        };
    }

    static InternalAdExportRequest initRequest() {
        return new InternalAdExportRequest()
                .withExportMode(InternalAdExportMode.AD_GROUPS_WITH_ADS);
    }

    @Test
    @Parameters(method = "parametrizedTestData_ForSuccessResult_ValidateExportRequest")
    @TestCaseName("{0}")
    public void checkValidateExportRequest_ExpectSuccessResult(@SuppressWarnings("unused") String testDescription,
                                                               InternalAdExportRequest request) {
        var validationResult = InternalAdValidationService.validateExportRequest(request);

        assertThat(validationResult)
                .is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @SuppressWarnings("unused")
    private Object[] parametrizedTestData_ForResultWithErrors_ValidateExportRequest() {
        Set<Long> setWithNullItem = new HashSet<>();
        setWithNullItem.add(null);

        Set<Long> idsOverLimit = LongStreamEx.range(MAX_IDS_PER_REQUEST + 1)
                .boxed()
                .toSet();
        return new Object[][]{
                {"request without export mode", initRequest()
                        .withCampaignIds(Set.of(RandomNumberUtils.nextPositiveLong()))
                        .withExportMode(null),
                        InternalAdExportRequest.EXPORT_MODE, CommonDefects.notNull()},

                {"request without filter fields", initRequest(),
                        null, CommonDefects.inconsistentState()},
                {"request with several filter fields", initRequest()
                        .withCampaignIds(Set.of(RandomNumberUtils.nextPositiveLong()))
                        .withAdGroupIds(Set.of(RandomNumberUtils.nextPositiveLong())),
                        null, CommonDefects.inconsistentState()},

                {"request with several campaignId", initRequest()
                        .withCampaignIds(Set.of(1L, 2L)),
                        InternalAdExportRequest.CAMPAIGN_IDS, CollectionDefects.collectionSizeInInterval(1, 1)},
                {"request with campaignId=null", initRequest()
                        .withCampaignIds(setWithNullItem),
                        InternalAdExportRequest.CAMPAIGN_IDS, CollectionDefects.notContainNulls()},
                {"request with invalid campaignId", initRequest()
                        .withCampaignIds(Set.of(INVALID_ID)),
                        InternalAdExportRequest.CAMPAIGN_IDS, CommonDefects.validId()},

                {"request with empty adGroupIds", initRequest()
                        .withAdGroupIds(Set.of()),
                        InternalAdExportRequest.AD_GROUP_IDS, CollectionDefects.notEmptyCollection()},
                {"request with adGroupIds count over limit", initRequest()
                        .withAdGroupIds(idsOverLimit),
                        InternalAdExportRequest.AD_GROUP_IDS, CollectionDefects.maxCollectionSize(MAX_IDS_PER_REQUEST)},
                {"request with adGroupId=null", initRequest()
                        .withAdGroupIds(setWithNullItem),
                        InternalAdExportRequest.AD_GROUP_IDS, CollectionDefects.notContainNulls()},
                {"request with invalid adGroupId", initRequest()
                        .withAdGroupIds(Set.of(INVALID_ID)),
                        InternalAdExportRequest.AD_GROUP_IDS, CommonDefects.validId()},

                {"request with adIds and export mode=ONLY_AD_GROUPS", initRequest()
                        .withExportMode(InternalAdExportMode.ONLY_AD_GROUPS)
                        .withAdIds(Set.of(RandomNumberUtils.nextPositiveLong())),
                        InternalAdExportRequest.AD_IDS, CommonDefects.isNull()},
                {"request with empty adIds", initRequest()
                        .withAdIds(Set.of()),
                        InternalAdExportRequest.AD_IDS, CollectionDefects.notEmptyCollection()},
                {"request with adIds count over limit", initRequest()
                        .withAdIds(idsOverLimit),
                        InternalAdExportRequest.AD_IDS, CollectionDefects.maxCollectionSize(MAX_IDS_PER_REQUEST)},
                {"request with adId=null", initRequest()
                        .withAdIds(setWithNullItem),
                        InternalAdExportRequest.AD_IDS, CollectionDefects.notContainNulls()},
                {"request with invalid adIds", initRequest()
                        .withAdIds(Set.of(INVALID_ID)),
                        InternalAdExportRequest.AD_IDS, CommonDefects.validId()},
        };
    }

    @Test
    @Parameters(method = "parametrizedTestData_ForResultWithErrors_ValidateExportRequest")
    @TestCaseName("{0}")
    public void checkValidateExportRequest_ExpectResultWithErrors(@SuppressWarnings("unused") String testDescription,
                                                                  InternalAdExportRequest request,
                                                                  @Nullable String expectedField,
                                                                  Defect<?> expectedDefectType) {
        var validationResult = InternalAdValidationService.validateExportRequest(request);

        Path expectedPath = expectedField == null ? path() : path(field(expectedField));
        assertThat(validationResult)
                .is(matchedBy(hasDefectWithDefinition(
                        validationError(expectedPath, expectedDefectType))));
    }

    // tests for validateImportFile
    @SuppressWarnings("unused")
    private Object[] parametrizedTestData_ForValidateImportFile() {
        Path path = pathFromStrings(EXCEL_FILE_PARAM, IMPORT_EXCEL_FILE_CONTENT_TYPE_PATH);

        return new Object[][]{
                {"valid excel file", getValidExcelFile(), hasNoErrorsAndWarnings()},
                {"invalid excel file", getFileWithContentType("invalidType"),
                        hasDefectWithDefinition(validationError(
                                path(field(toImportExcelFilePath(IMPORT_EXCEL_FILE_CONTENT_TYPE_PATH))),
                                fileContentTypeIsNotSupported()))},
                {"file without contentType", getFileWithContentType(null),
                        hasDefectWithDefinition(validationError(
                                path(field(toImportExcelFilePath(IMPORT_EXCEL_FILE_CONTENT_TYPE_PATH))),
                                CommonDefects.notNull()))},
                {"excel file with file size greater than max", getExcelFileWithSizeGreaterThanMax(),
                        hasDefectWithDefinition(validationError(
                                path(field(toImportExcelFilePath(IMPORT_EXCEL_FILE_SIZE_PATH))),
                                fileSizeGreaterThanMax()))},
        };
    }

    private static MultipartFile getValidExcelFile() {
        MultipartFile mock = mock(MultipartFile.class);
        doReturn(XLS_CONTENT_TYPE)
                .when(mock).getContentType();
        doReturn(1L)
                .when(mock).getSize();

        return mock;
    }

    static MultipartFile getFileWithContentType(@Nullable String contentType) {
        MultipartFile mock = getValidExcelFile();
        doReturn(contentType)
                .when(mock).getContentType();

        return mock;
    }

    static MultipartFile getExcelFileWithSizeGreaterThanMax() {
        MultipartFile mock = getValidExcelFile();
        doReturn(InternalAdValidationService.MAX_FILE_SIZE + 1L)
                .when(mock).getSize();

        return mock;
    }

    @Test
    @Parameters(method = "parametrizedTestData_ForValidateImportFile")
    @TestCaseName("{0}")
    public void checkValidateImportFile_expectSuccessResult(@SuppressWarnings("unused") String testDescription,
                                                            MultipartFile importFile,
                                                            Matcher<ValidationResult<?, Defect<?>>> matcher) {
        var validationResult = InternalAdValidationService.validateImportFile(importFile);

        assertThat(validationResult)
                .is(matchedBy(matcher));
    }

    // tests for validateImportRequest
    @SuppressWarnings("unused")
    private Object[] parametrizedTestData_ForSuccessResult_ValidateImportRequest() {
        return new Object[][]{
                {"request with empty adsImages", initImportRequest()},
                {"request with adsImages", initImportRequest().withAdsImages(List.of(
                        new UploadedImageInfo()
                                .withFileName(RandomStringUtils.randomAlphanumeric(7))
                                .withImageHash(RandomStringUtils.randomAlphanumeric(5))))},

                {"request with onlyValidation = false", initImportRequest().withOnlyValidation(false)},
                {"request with onlyValidation = true", initImportRequest().withOnlyValidation(true)},

                {"request with importMode = ONLY_AD_GROUPS", initImportRequest()
                        .withImportMode(InternalAdImportMode.ONLY_AD_GROUPS)},
                {"request with importMode = ONLY_ADS", initImportRequest()
                        .withImportMode(InternalAdImportMode.ONLY_ADS)},
                {"request with importMode = AD_GROUPS_WITH_ADS", initImportRequest()
                        .withImportMode(InternalAdImportMode.AD_GROUPS_WITH_ADS)},
        };
    }

    static InternalAdImportRequest initImportRequest() {
        return getDefaultInternalAdImportRequest();
    }

    @Test
    @Parameters(method = "parametrizedTestData_ForSuccessResult_ValidateImportRequest")
    @TestCaseName("{0}")
    public void checkValidateImportRequest_ExpectSuccessResult(@SuppressWarnings("unused") String testDescription,
                                                               InternalAdImportRequest request) {
        var validationResult = InternalAdValidationService.validateImportRequest(request);

        assertThat(validationResult)
                .is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @SuppressWarnings("unused")
    private Object[] parametrizedTestData_ForResultWithErrors_ValidateImportRequest() {
        //noinspection ConstantConditions
        return new Object[][]{
                {"request without import mode", initImportRequest().withImportMode(null),
                        CommonDefects.notNull(), pathFromStrings(InternalAdImportRequest.IMPORT_MODE)},
                {"request without onlyValidation", initImportRequest().withOnlyValidation(null),
                        CommonDefects.notNull(), pathFromStrings(InternalAdImportRequest.ONLY_VALIDATION)},

                {"request without excelFileKey", initImportRequest().withExcelFileKey(null),
                        CommonDefects.notNull(), pathFromStrings(InternalAdImportRequest.EXCEL_FILE_KEY)},
                {"request without fileName in excelFileKey", initImportRequest().withExcelFileKey(
                        new ExcelFileKey()
                                .withFileName(null)
                                .withMdsGroupId(RandomStringUtils.randomNumeric(7))),
                        CommonDefects.notNull(),
                        pathFromStrings(InternalAdImportRequest.EXCEL_FILE_KEY, ExcelFileKey.FILE_NAME)},
                {"request without mdsGroup in excelFileKey", initImportRequest().withExcelFileKey(
                        new ExcelFileKey()
                                .withFileName(RandomStringUtils.randomAlphanumeric(12))
                                .withMdsGroupId(null)),
                        CommonDefects.notNull(),
                        pathFromStrings(InternalAdImportRequest.EXCEL_FILE_KEY, ExcelFileKey.MDS_GROUP)},
                {"request with invalid mdsGroup in excelFileKey", initImportRequest().withExcelFileKey(
                        new ExcelFileKey()
                                .withFileName(RandomStringUtils.randomAlphanumeric(12))
                                .withMdsGroupId("123invalidId")),
                        NumberDefects.isWholeNumber(),
                        pathFromStrings(InternalAdImportRequest.EXCEL_FILE_KEY, ExcelFileKey.MDS_GROUP)},

                {"request with adsImages = null", initImportRequest().withAdsImages(null),
                        CommonDefects.notNull(), pathFromStrings(InternalAdImportRequest.ADS_IMAGES)},
                {"request with adsImages = [null]", initImportRequest().withAdsImages(Collections.singletonList(null)),
                        CommonDefects.notNull(), path(field(InternalAdImportRequest.ADS_IMAGES), index(0))},
                {"request without fileName in adsImages", initImportRequest().withAdsImages(List.of(
                        new UploadedImageInfo()
                                .withFileName(null)
                                .withImageHash(RandomStringUtils.randomAlphanumeric(12)))),
                        CommonDefects.notNull(),
                        path(field(InternalAdImportRequest.ADS_IMAGES), index(0), field(UploadedImageInfo.FILE_NAME))},
                {"request without imageHash in adsImages", initImportRequest().withAdsImages(List.of(
                        new UploadedImageInfo()
                                .withImageHash(null)
                                .withFileName(RandomStringUtils.randomAlphanumeric(12)))),
                        CommonDefects.notNull(),
                        path(field(InternalAdImportRequest.ADS_IMAGES), index(0), field(UploadedImageInfo.IMAGE_HASH))},
        };
    }

    @Test
    @Parameters(method = "parametrizedTestData_ForResultWithErrors_ValidateImportRequest")
    @TestCaseName("{0}")
    public void checkValidateImportRequest_ExpectResultWithErrors(@SuppressWarnings("unused") String testDescription,
                                                                  InternalAdImportRequest request,
                                                                  Defect<?> expectedDefectType,
                                                                  Path expectedPath) {
        var validationResult = InternalAdValidationService.validateImportRequest(request);

        assertThat(validationResult)
                .is(matchedBy(hasDefectWithDefinition(validationError(expectedPath, expectedDefectType))));
    }

}
