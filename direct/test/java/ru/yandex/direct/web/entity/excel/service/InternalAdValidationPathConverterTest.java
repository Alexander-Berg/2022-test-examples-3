package ru.yandex.direct.web.entity.excel.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.excel.model.ExcelFileKey;
import ru.yandex.direct.web.entity.excel.model.UploadedImageInfo;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdExportMode;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdExportRequest;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportRequest;
import ru.yandex.direct.web.validation.model.WebDefect;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.validation.result.PathHelper.pathFromStrings;
import static ru.yandex.direct.web.entity.excel.controller.ExcelController.EXCEL_FILE_PARAM;
import static ru.yandex.direct.web.entity.excel.service.InternalAdValidationServiceTest.getExcelFileWithSizeGreaterThanMax;
import static ru.yandex.direct.web.entity.excel.service.InternalAdValidationServiceTest.getFileWithContentType;
import static ru.yandex.direct.web.entity.excel.service.InternalAdValidationServiceTest.initImportRequest;
import static ru.yandex.direct.web.entity.excel.service.InternalAdValidationServiceTest.initRequest;
import static ru.yandex.direct.web.entity.excel.service.validation.InternalAdPathConverters.IMPORT_EXCEL_FILE_CONTENT_TYPE_PATH;
import static ru.yandex.direct.web.entity.excel.service.validation.InternalAdPathConverters.IMPORT_EXCEL_FILE_SIZE_PATH;
import static ru.yandex.direct.web.entity.excel.service.validation.InternalAdPathConverters.toImportExcelFilePath;

/**
 * Тесты для проверки конвертации путей ошибок валидации
 */
@DirectWebTest
@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class InternalAdValidationPathConverterTest {

    @Autowired
    private InternalAdValidationService internalAdValidationService;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();


    @SuppressWarnings("unused")
    private Object[] parametrizedTestData_ForValidateExportRequestPathConverter() {
        return new Object[][]{
                {"path is empty when get request validation", initRequest(), ""},

                {"exportMode path", initRequest()
                        .withCampaignIds(Set.of(RandomNumberUtils.nextPositiveLong()))
                        .withExportMode(null),
                        InternalAdExportRequest.EXPORT_MODE},

                {"campaignIds path", initRequest()
                        .withCampaignIds(Set.of(1L, 2L)),
                        InternalAdExportRequest.CAMPAIGN_IDS},

                {"adGroupIds path", initRequest()
                        .withAdGroupIds(Set.of()),
                        InternalAdExportRequest.AD_GROUP_IDS},

                {"adIds path", initRequest()
                        .withExportMode(InternalAdExportMode.ONLY_AD_GROUPS)
                        .withAdIds(Set.of(RandomNumberUtils.nextPositiveLong())),
                        InternalAdExportRequest.AD_IDS},
        };
    }

    @Test
    @Parameters(method = "parametrizedTestData_ForValidateExportRequestPathConverter")
    @TestCaseName("check {0}")
    public void checkValidateExportRequestPathConverter(@SuppressWarnings("unused") String testDescription,
                                                        InternalAdExportRequest request,
                                                        String expectedPath) {
        var validationResult = InternalAdValidationService.validateExportRequest(request);

        buildWebValidationResult_AndCheckPath(validationResult, expectedPath);
    }


    @SuppressWarnings("unused")
    private Object[] parametrizedTestData_ForValidateImportFilePathConverter() {
        Path path = pathFromStrings(EXCEL_FILE_PARAM, IMPORT_EXCEL_FILE_CONTENT_TYPE_PATH);

        return new Object[][]{
                {"contentType path", getFileWithContentType(null),
                        toImportExcelFilePath(IMPORT_EXCEL_FILE_CONTENT_TYPE_PATH)},
                {"size path", getExcelFileWithSizeGreaterThanMax(),
                        toImportExcelFilePath(IMPORT_EXCEL_FILE_SIZE_PATH)},
        };
    }

    @Test
    @Parameters(method = "parametrizedTestData_ForValidateImportFilePathConverter")
    @TestCaseName("check excel file {0}")
    public void checkValidateImportFilePathConverter(@SuppressWarnings("unused") String testDescription,
                                                     MultipartFile importFile,
                                                     String expectedPath) {
        var validationResult = InternalAdValidationService.validateImportFile(importFile);

        buildWebValidationResult_AndCheckPath(validationResult, expectedPath);
    }


    @SuppressWarnings("unused")
    private Object[] parametrizedTestData_ForValidateImportRequestPathConverter() {
        //noinspection ConstantConditions
        return new Object[][]{
                {"importMode path", initImportRequest().withImportMode(null),
                        pathFromStrings(InternalAdImportRequest.IMPORT_MODE)},

                {"onlyValidation path", initImportRequest().withOnlyValidation(null),
                        pathFromStrings(InternalAdImportRequest.ONLY_VALIDATION)},

                {"excelFileKey path", initImportRequest().withExcelFileKey(null),
                        pathFromStrings(InternalAdImportRequest.EXCEL_FILE_KEY)},
                {"excelFileKey fileName path", initImportRequest().withExcelFileKey(
                        new ExcelFileKey()
                                .withFileName(null)
                                .withMdsGroupId(RandomStringUtils.randomNumeric(7))),
                        pathFromStrings(InternalAdImportRequest.EXCEL_FILE_KEY, ExcelFileKey.FILE_NAME)},
                {"excelFileKey mdsGroup path", initImportRequest().withExcelFileKey(
                        new ExcelFileKey()
                                .withFileName(RandomStringUtils.randomAlphanumeric(12))
                                .withMdsGroupId(null)),
                        pathFromStrings(InternalAdImportRequest.EXCEL_FILE_KEY, ExcelFileKey.MDS_GROUP)},

                {"adsImages path", initImportRequest().withAdsImages(null),
                        pathFromStrings(InternalAdImportRequest.ADS_IMAGES)},
                {"adsImages item path", initImportRequest().withAdsImages(Collections.singletonList(null)),
                        path(field(InternalAdImportRequest.ADS_IMAGES), index(0))},
                {"adsImages fileName path", initImportRequest().withAdsImages(List.of(
                        new UploadedImageInfo()
                                .withFileName(null)
                                .withImageHash(RandomStringUtils.randomAlphanumeric(12)))),
                        path(field(InternalAdImportRequest.ADS_IMAGES), index(0), field(UploadedImageInfo.FILE_NAME))},
                {"adsImages imageHash path", initImportRequest().withAdsImages(List.of(
                        new UploadedImageInfo()
                                .withImageHash(null)
                                .withFileName(RandomStringUtils.randomAlphanumeric(12)))),
                        path(field(InternalAdImportRequest.ADS_IMAGES), index(0), field(UploadedImageInfo.IMAGE_HASH))},
        };
    }

    @Test
    @Parameters(method = "parametrizedTestData_ForValidateImportRequestPathConverter")
    @TestCaseName("check {0}")
    public void checkValidateImportRequestPathConverter(@SuppressWarnings("unused") String testDescription,
                                                        InternalAdImportRequest request,
                                                        Path expectedPath) {
        var validationResult = InternalAdValidationService.validateImportRequest(request);

        buildWebValidationResult_AndCheckPath(validationResult, expectedPath.toString());
    }


    private void buildWebValidationResult_AndCheckPath(ValidationResult<?, Defect> validationResult,
                                                       String expectedPath) {
        var webValidationResult = internalAdValidationService.buildWebValidationResult(validationResult);

        assertThat(webValidationResult.getErrors())
                .extracting(WebDefect::getPath)
                .containsExactly(expectedPath);
    }

}
