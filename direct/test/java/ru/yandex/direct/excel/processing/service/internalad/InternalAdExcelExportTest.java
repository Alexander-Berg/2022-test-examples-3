package ru.yandex.direct.excel.processing.service.internalad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.DesktopInstalledAppsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.service.AdGroupAdditionalTargetingService;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.DeviceNamesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamiliesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamily;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.OldInternalBanner;
import ru.yandex.direct.core.entity.banner.model.old.TemplateVariable;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.excel.processing.configuration.ExcelProcessingTest;
import ru.yandex.direct.excel.processing.exception.ExcelRuntimeException;
import ru.yandex.direct.excel.processing.exception.ExcelValidationException;
import ru.yandex.direct.excel.processing.model.CampaignInfoWithWorkbook;
import ru.yandex.direct.excel.processing.model.internalad.ExcelFetchedData;
import ru.yandex.direct.excel.processing.model.internalad.InternalAdExportParameters;
import ru.yandex.direct.excel.processing.model.internalad.InternalAdGroupRepresentation;
import ru.yandex.direct.excel.processing.utils.ChangeableObjectIdDiffer;
import ru.yandex.direct.excelmapper.MapperTestUtils;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.UaTraitsConstants.OS_FAMILY_VALID_VALUES;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.CAMPAIGN_NOT_FOUND;
import static ru.yandex.direct.core.entity.internalads.service.validation.defects.InternalAdDefectIds.INVALID_PLACE_ID;
import static ru.yandex.direct.core.entity.internalads.service.validation.defects.InternalAdDefectIds.TEMPLATE_NOT_FOUND;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validInternalNetworkTargeting;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validYandexUidFiltering;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validYandexUidTargeting;
import static ru.yandex.direct.core.testing.data.TestBanners.activeInternalBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeInternalBannerWithManyResources;
import static ru.yandex.direct.core.testing.data.TestGroups.activeInternalAdGroup;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils.TEMPLATE_6_RESOURCE_AGE;
import static ru.yandex.direct.core.validation.ValidationUtils.hasValidationIssues;
import static ru.yandex.direct.excel.processing.service.internalad.InternalAdExcelService.AD_GROUP_SHEET_NAME;
import static ru.yandex.direct.excel.processing.service.internalad.InternalAdExcelService.AD_SHEET_NAME_PREFIX;
import static ru.yandex.direct.excel.processing.validation.defects.ExcelDefectIds.ADS_OR_AD_GROUPS_BELONG_TO_DIFFERENT_CAMPAIGNS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.validation.result.DefectIds.OBJECT_NOT_FOUND;

/**
 * Для того, чтобы обновить эталонный файл нужно раскомментировать строку с вызовом метода writeWorkbookToFile
 * и обновить path на свой в этом методе
 * ВАЖНО: если меняется схема эксель файла, при котором файл выгруженный со старой схемой нельзя будет импортировать,
 * то надо обязательно создать тикет на починку автотестов фронта: https://st.yandex-team.ru/dashboard/27532#178421
 * выбрать `Исправить тест`, пример тикета: https://st.yandex-team.ru/DIRECT-148318
 */
@ExcelProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class InternalAdExcelExportTest {

    private static final int COLUMN_COUNT = 100;
    private static final String RESOURCE_DIRECTORY = "etalonexcel/";
    private static final String ETALON_FILE_NAME = "export-internal-ad.xlsx";
    private static final String ETALON_FILE_NAME_FOR_ONLY_AD_GROUPS = "export-internal-ad_with_only_ad_groups.xlsx";
    private static final String ETALON_FILE_NAME_FOR_AD_WITH_MODERATION_INFO =
            "export-internal-ad-with_moderation_info.xlsx";
    private static final String ETALON_FILE_NAME_FOR_TEMPLATE_EXPORT = "export-internal-ad-template.xlsx";
    private static final String ETALON_FILE_NAME_HIDDEN_COLUMNS = "export-internal-ad-with-hidden-columns.xlsx";

    private static final long NOT_EXISTING_OBJECT_ID = Long.MAX_VALUE;
    private static final long AD_GROUP_LEVEL = 12;
    private static final int AD_GROUP_RF = 13;
    private static final int AD_GROUP_RF_RESET = 14;

    private static final Integer AD_GROUP_MAX_CLICKS_COUNT = 15;
    private static final Integer AD_GROUP_MAX_CLICKS_PERIOD = 300;
    private static final Integer AD_GROUP_MAX_STOPS_COUNT = 16;
    private static final Integer AD_GROUP_MAX_STOPS_PERIOD =
            CampaignConstants.MAX_CLICKS_AND_STOPS_PERIOD_WHOLE_CAMPAIGN_VALUE;

    private static final BeanFieldPath CAMPAIGN_ID_PATH = newPath("0", "1");
    /**
     * параметры объектов начинаются со строки {@link InternalAdExcelService#START_ROW_WITH_OBJECTS_DATA}
     * поэтому пропускаем первые строки где нет объектов
     */
    private static final String ROWS_INTERVAL_WITHOUT_OBJECTS_DATA =
            "0-" + (InternalAdExcelService.START_ROW_WITH_OBJECTS_DATA - 1);
    private static final String ROWS_INTERVAL_WITH_OBJECTS_DATA =
            String.format("^([^%s]|[1-9][0-9]+)", ROWS_INTERVAL_WITHOUT_OBJECTS_DATA);
    private static final BeanFieldPath AD_GROUP_ID_PATH = newPath(ROWS_INTERVAL_WITH_OBJECTS_DATA, "0");
    private static final BeanFieldPath AD_ID_PATH = newPath(ROWS_INTERVAL_WITH_OBJECTS_DATA, "2");
    private static final BeanFieldPath[] FIELD_PATHS_FOR_AD_GROUPS_SHEET =
            new BeanFieldPath[]{CAMPAIGN_ID_PATH, AD_GROUP_ID_PATH};
    private static final BeanFieldPath[] FIELD_PATHS_FOR_ADS_SHEET =
            new BeanFieldPath[]{CAMPAIGN_ID_PATH, AD_GROUP_ID_PATH, AD_ID_PATH};

    @Autowired
    private Steps steps;

    @Autowired
    private InternalAdExcelService internalAdExcelService;

    @Autowired
    private AdGroupAdditionalTargetingService additionalTargetingService;

    @Autowired
    private RetargetingConditionService retargetingConditionService;

    @Autowired
    private CryptaSegmentDictionariesService cryptaSegmentDictionariesService;

    private ClientInfo clientInfo;

    private CampaignInfo campaignInfo;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void initTestData() {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo);
        doReturn(List.of("Мужчины", "Женщины"))
                .when(cryptaSegmentDictionariesService).getGenderValues();
        doReturn(List.of("<18", "18-24", "24+"))
                .when(cryptaSegmentDictionariesService).getAgeValues();
        doReturn(List.of("Низкий", "Средний", "Высокий"))
                .when(cryptaSegmentDictionariesService).getIncomeValues();
    }


    @Test
    public void checkInternalAdExcelExport() {
        InternalAdGroup internalAdGroup1 = activeInternalAdGroup(campaignInfo.getCampaignId(), AD_GROUP_LEVEL)
                .withName("first internalGroup name");
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(internalAdGroup1, campaignInfo);
        steps.bannerSteps().createActiveInternalBanner(adGroupInfo);

        InternalAdGroup internalAdGroup2 =
                activeInternalAdGroup(campaignInfo.getCampaignId(), AD_GROUP_LEVEL,
                        AD_GROUP_RF, AD_GROUP_RF_RESET, null, null,
                        AD_GROUP_MAX_CLICKS_COUNT, AD_GROUP_MAX_CLICKS_PERIOD,
                        AD_GROUP_MAX_STOPS_COUNT, AD_GROUP_MAX_STOPS_PERIOD)
                        .withName("second internalGroup name");
        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createAdGroup(internalAdGroup2, campaignInfo);
        steps.bannerSteps().createActiveInternalBanner(adGroupInfo2);
        OldInternalBanner internalBanner = activeInternalBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withTemplateId(TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_WITH_AGE_VARIABLE)
                .withTemplateVariables(List.of(
                        new TemplateVariable()
                                .withTemplateResourceId(TEMPLATE_6_RESOURCE_AGE)
                                .withInternalValue("+18"))
                )
                .withStatusActive(false)
                .withLanguage(Language.KK);
        steps.bannerSteps().createActiveInternalBanner(adGroupInfo2, internalBanner);

        InternalAdExportParameters exportParameters = createExportParameters(campaignInfo.getCampaignId());

        Workbook workbook = internalAdExcelService.createWorkbook(exportParameters).getWorkbook();
        compareWithExpectedData(workbook, ETALON_FILE_NAME);

        // проверяем тут, чтобы не писать отдельный сложный тест, который будет создавать все тоже самое
        checkInternalAdExcelExport_DataFromExcelFile(workbook);
    }

    public void checkInternalAdExcelExport_DataFromExcelFile(Workbook workbook) {
        ExcelFetchedData dataFromExcelFile = getDataFromExcelFile(workbook);

        createWorkbookAndCompareWithExpectedData(dataFromExcelFile, ETALON_FILE_NAME, false);
    }

    @Test
    public void checkInternalAdExcelExport_OnlyAdGroups() {
        InternalAdGroup internalAdGroup = activeInternalAdGroup(campaignInfo.getCampaignId(), AD_GROUP_LEVEL)
                .withName("another internalGroup name")
                .withGeo(List.of(Region.RUSSIA_REGION_ID, -Region.MOSCOW_REGION_ID));
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(internalAdGroup, campaignInfo);
        addAdGroupAdditionalTargetings(adGroupInfo.getAdGroupId());
        steps.bannerSteps().createActiveInternalBanner(adGroupInfo);
        addRetargetingCondition();
        InternalAdGroup internalAdGroup2 = activeInternalAdGroup(campaignInfo.getCampaignId(), null)
                .withName("internalGroup with Russia and Crimea geo")
                .withGeo(List.of(Region.RUSSIA_REGION_ID, Region.CRIMEA_REGION_ID));
        steps.adGroupSteps().createAdGroup(internalAdGroup2, campaignInfo);

        InternalAdExportParameters exportParameters = createExportParameters(campaignInfo.getCampaignId())
                .withExportAdGroupsWithAds(false);

        Workbook workbook = internalAdExcelService.createWorkbook(exportParameters).getWorkbook();
        compareWithExpectedData(workbook, ETALON_FILE_NAME_FOR_ONLY_AD_GROUPS);

        // проверяем тут, чтобы не писать отдельный сложный тест, который будет создавать все тоже самое
        checkInternalAdExcelExport_DataFromExcelFile_OnlyAdGroups(workbook);
    }

    public void checkInternalAdExcelExport_DataFromExcelFile_OnlyAdGroups(Workbook workbook) {
        ExcelFetchedData dataFromExcelFile = getDataFromExcelFile(workbook);

        List<InternalAdGroupRepresentation> adGroupRepresentations = dataFromExcelFile.getAdGroupsSheet().getObjects();
        assertThat(adGroupRepresentations)
                .hasSize(2);

        // проставляем значения из базы, которые при экспорте должна пройти через clientGeoService.convertForWeb
        // в эксель файле ожидаем получить только RUSSIA_REGION_ID
        adGroupRepresentations.get(1).getAdGroup()
                .withGeo(List.of(Region.RUSSIA_REGION_ID, Region.CRIMEA_REGION_ID))
                // для проверки что заменим пустой level на дефолтное значение
                .withLevel(null);

        createWorkbookAndCompareWithExpectedData(dataFromExcelFile, ETALON_FILE_NAME_FOR_ONLY_AD_GROUPS, false);
    }

    @Test
    public void checkInternalAdExcelExport_HiddenColumns() {
        InternalAdGroup internalAdGroup = activeInternalAdGroup(campaignInfo.getCampaignId(), AD_GROUP_LEVEL)
                .withName("first internalGroup name")
                .withGeo(List.of(Region.RUSSIA_REGION_ID, -Region.MOSCOW_REGION_ID));
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(internalAdGroup, campaignInfo);
        addAdGroupAdditionalTargetings(adGroupInfo.getAdGroupId());
        addRetargetingCondition();

        InternalAdGroup internalAdGroup2 = activeInternalAdGroup(campaignInfo.getCampaignId(), AD_GROUP_LEVEL)
                .withName("second internalGroup name");
        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createAdGroup(internalAdGroup2, campaignInfo);
        addAdGroupAdditionalTargetingsWithDeviceName(adGroupInfo2.getAdGroupId());

        OldInternalBanner internalBanner = activeInternalBannerWithManyResources(
                adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), "abc", null, "def", null);
        steps.bannerSteps().createActiveInternalBanner(adGroupInfo, internalBanner);

        OldInternalBanner internalBanner2 = activeInternalBannerWithManyResources(
                adGroupInfo2.getCampaignId(), adGroupInfo2.getAdGroupId(), "cba", "ghi", null, null);
        steps.bannerSteps().createActiveInternalBanner(adGroupInfo2, internalBanner2);

        InternalAdExportParameters exportParameters = createExportParameters(campaignInfo.getCampaignId())
                .withHideEmptyColumns(true);

        Workbook workbook = internalAdExcelService.createWorkbook(exportParameters).getWorkbook();
        compareWithExpectedData(workbook, ETALON_FILE_NAME_HIDDEN_COLUMNS);

        checkInternalAdExcelExport_DataFromExcelFile_HiddenColumns(workbook);
    }

    public void checkInternalAdExcelExport_DataFromExcelFile_HiddenColumns(Workbook workbook) {
        ExcelFetchedData dataFromExcelFile = getDataFromExcelFile(workbook);

        createWorkbookAndCompareWithExpectedData(dataFromExcelFile, ETALON_FILE_NAME_HIDDEN_COLUMNS, true);
    }

    @Test
    public void checkInternalAdExcelExport_ModerationInfo() {
        campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaignWithModeratedPlace(clientInfo);

        InternalAdGroup internalAdGroup = activeInternalAdGroup(campaignInfo.getCampaignId(), AD_GROUP_LEVEL)
                .withName("internalGroup name")
                .withGeo(List.of(Region.RUSSIA_REGION_ID, -Region.MOSCOW_REGION_ID));
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(internalAdGroup, campaignInfo);

        steps.internalBannerSteps().createModeratedInternalBanner(adGroupInfo, BannerStatusModerate.READY, "111");

        InternalAdExportParameters exportParameters = createExportParameters(campaignInfo.getCampaignId());
        Workbook workbook = internalAdExcelService.createWorkbook(exportParameters).getWorkbook();
        compareWithExpectedData(workbook, ETALON_FILE_NAME_FOR_AD_WITH_MODERATION_INFO);

        ExcelFetchedData dataFromExcelFile = getDataFromExcelFile(workbook);
        createWorkbookAndCompareWithExpectedData(dataFromExcelFile, ETALON_FILE_NAME_FOR_AD_WITH_MODERATION_INFO,
                false);
    }

    @Test
    public void checkInternalAdExcelExport_AddValidationData() {
        InternalAdGroup internalAdGroup1 = activeInternalAdGroup(campaignInfo.getCampaignId(), 0L)
                .withName("internalGroup name");
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(internalAdGroup1, campaignInfo);
        steps.bannerSteps().createActiveInternalBanner(adGroupInfo);

        InternalAdExportParameters exportParameters = createExportParameters(campaignInfo.getCampaignId());
        createWorkbookAndCompareWithExpectedData(exportParameters, "export-internal-ad_with_validation_data.xlsx");
    }

    @Test
    public void checkInternalAdExcelExport_TemplateWithHiddenResources() {
        InternalAdGroup internalAdGroup1 = activeInternalAdGroup(campaignInfo.getCampaignId(), 0L)
                .withName("internalGroup name");
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(internalAdGroup1, campaignInfo);
        OldInternalBanner internalBanner = activeInternalBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withTemplateId(TemplatePlaceRepositoryMockUtils.PLACE_2_TEMPLATE_2_WITH_HIDDEN_RESOURCE)
                .withTemplateVariables(List.of());
        steps.bannerSteps().createActiveInternalBanner(adGroupInfo, internalBanner);

        InternalAdExportParameters exportParameters = createExportParameters(campaignInfo.getCampaignId());
        createWorkbookAndCompareWithExpectedData(exportParameters, "export-internal-ad_with_hidden_columns.xlsx");
    }

    @Test
    public void checkInternalAdExcelExport_WhenCampaignIsEmpty() {
        InternalAdExportParameters exportParameters = createExportParameters(campaignInfo.getCampaignId());

        createWorkbookAndCompareWithExpectedData(exportParameters, "export-internal-ad-empty_campaign.xlsx");
    }

    @Test
    public void checkInternalAdExcelExport_WhenAdGroupsBelongToDifferentCampaigns() {
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        AdGroupInfo adGroupWithNewCampaign = steps.adGroupSteps().createActiveInternalAdGroup();
        InternalAdExportParameters exportParameters = createExportParameters(null)
                .withAdGroupIds(Set.of(adGroup.getAdGroupId(), adGroupWithNewCampaign.getAdGroupId()));

        var expectedValidationException = ExcelValidationException
                .create(ADS_OR_AD_GROUPS_BELONG_TO_DIFFERENT_CAMPAIGNS);
        thrown.expect(equalTo(expectedValidationException));
        internalAdExcelService.createWorkbook(exportParameters);
    }

    @Test
    public void checkInternalAdExcelExport_WhenCampaignNotFound() {
        InternalAdExportParameters exportParameters = createExportParameters(NOT_EXISTING_OBJECT_ID);

        var expectedValidationException = ExcelValidationException.create(CAMPAIGN_NOT_FOUND);
        thrown.expect(equalTo(expectedValidationException));
        internalAdExcelService.createWorkbook(exportParameters);
    }

    @Test
    public void checkInternalAdExcelExport_WhenAdGroupsNotFound() {
        InternalAdExportParameters exportParameters = createExportParameters(null)
                .withAdGroupIds(Set.of(NOT_EXISTING_OBJECT_ID));

        var expectedValidationException = ExcelValidationException.create(OBJECT_NOT_FOUND);
        thrown.expect(equalTo(expectedValidationException));
        internalAdExcelService.createWorkbook(exportParameters);
    }

    @Test
    public void checkInternalAdExcelExport_WhenAdsNotFound() {
        InternalAdExportParameters exportParameters = createExportParameters(null)
                .withAdIds(Set.of(NOT_EXISTING_OBJECT_ID));

        var expectedValidationException = ExcelValidationException.create(OBJECT_NOT_FOUND);
        thrown.expect(equalTo(expectedValidationException));
        internalAdExcelService.createWorkbook(exportParameters);
    }

    @Test
    public void checkInternalAdExcelExport_WhenAllIdsFieldsIsNull() {
        InternalAdExportParameters exportParametersWithAllIdsFieldsIsNull = createExportParameters(null);

        thrown.expect(ExcelRuntimeException.class);
        thrown.expectMessage("not found ids in exportParameters for adGroupsSelectionCriteria");
        internalAdExcelService.createWorkbook(exportParametersWithAllIdsFieldsIsNull);
    }

    @Test
    public void checkRetrieveCampaign_WhenCampaignNotFound() {
        var expectedValidationException = ExcelValidationException.create(CAMPAIGN_NOT_FOUND);
        thrown.expect(equalTo(expectedValidationException));

        internalAdExcelService.retrieveCampaign(campaignInfo.getClientId(), NOT_EXISTING_OBJECT_ID);
    }

    @Test
    public void checkInternalAdTemplateExcelExport() {
        ClientId clientId = campaignInfo.getClientId();
        Long campaignId = campaignInfo.getCampaignId();
        Long placeId = TestCampaigns.DEFAULT_PLACE_ID_FOR_INTERNAL_CAMPAIGNS;
        Long templateId = TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_WITH_AGE_VARIABLE;

        CampaignInfoWithWorkbook result = internalAdExcelService.createWorkbookForEmptyCampaign(
                clientId, campaignId, placeId, templateId);

        Workbook workbook = result.getWorkbook();
        compareWithExpectedData(workbook, ETALON_FILE_NAME_FOR_TEMPLATE_EXPORT);
    }

    @Test
    public void checkInternalAdTemplateExcelExport_WhenCampaignNotFound() {
        var expectedValidationException = ExcelValidationException.create(CAMPAIGN_NOT_FOUND);
        thrown.expect(equalTo(expectedValidationException));

        internalAdExcelService.createWorkbookForEmptyCampaign(campaignInfo.getClientId(),
                NOT_EXISTING_OBJECT_ID,
                TestCampaigns.DEFAULT_PLACE_ID_FOR_INTERNAL_CAMPAIGNS,
                TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_WITH_AGE_VARIABLE);
    }

    @Test
    public void checkInternalAdTemplateExcelExport_WhenPlaceIsInvalid() {
        var expectedValidationException = ExcelValidationException.create(INVALID_PLACE_ID);
        thrown.expect(equalTo(expectedValidationException));

        internalAdExcelService.createWorkbookForEmptyCampaign(campaignInfo.getClientId(),
                campaignInfo.getCampaignId(),
                NOT_EXISTING_OBJECT_ID,
                TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_WITH_AGE_VARIABLE);
    }

    @Test
    public void checkInternalAdTemplateExcelExport_WhenTemplateNotFound() {
        var expectedValidationException = ExcelValidationException.create(TEMPLATE_NOT_FOUND);
        thrown.expect(equalTo(expectedValidationException));

        internalAdExcelService.createWorkbookForEmptyCampaign(campaignInfo.getClientId(),
                campaignInfo.getCampaignId(),
                TestCampaigns.DEFAULT_PLACE_ID_FOR_INTERNAL_CAMPAIGNS,
                NOT_EXISTING_OBJECT_ID);
    }

    private InternalAdExportParameters createExportParameters(@Nullable Long campaignId) {
        return new InternalAdExportParameters()
                .withClientId(campaignInfo.getClientId())
                .withExportAdGroupsWithAds(true)
                .withCampaignIds(campaignId != null ? Set.of(campaignId) : null)
                .withHideEmptyColumns(false);
    }

    private void addAdGroupAdditionalTargetings(long adGroupId) {
        var additionalTargetings = List.of(
                validInternalNetworkTargeting().withAdGroupId(adGroupId),
                new DesktopInstalledAppsAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                        .withAdGroupId(adGroupId)
                        .withValue(Set.of(1L, 4L)),
                new DesktopInstalledAppsAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
                        .withAdGroupId(adGroupId)
                        .withValue(Set.of(5L)),
                new DesktopInstalledAppsAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
                        .withAdGroupId(adGroupId)
                        .withValue(Set.of(2L, 3L, 6L)),
                validYandexUidTargeting().withAdGroupId(adGroupId),
                validYandexUidFiltering().withAdGroupId(adGroupId),
                new OsFamiliesAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                        .withAdGroupId(adGroupId)
                        .withValue(List.of(
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.min(OS_FAMILY_VALID_VALUES)))),
                new OsFamiliesAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
                        .withAdGroupId(adGroupId)
                        .withValue(List.of(
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.min(OS_FAMILY_VALID_VALUES))
                                        .withMaxVersion("33.4")
                                        .withMinVersion("1.0"),
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.max(OS_FAMILY_VALID_VALUES))))
        );

        MassResult<Long> result = additionalTargetingService.createAddOperation(Applicability.FULL,
                additionalTargetings, campaignInfo.getClientId()).prepareAndApply();
        checkState(!hasValidationIssues(result), "expect operation result without errors");
    }

    private void addAdGroupAdditionalTargetingsWithDeviceName(long adGroupId) {
        var additionalTargetings = List.of(
                validInternalNetworkTargeting().withAdGroupId(adGroupId),
                validYandexUidTargeting().withAdGroupId(adGroupId),
                validYandexUidFiltering().withAdGroupId(adGroupId),
                new DeviceNamesAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                        .withAdGroupId(adGroupId)
                        .withValue(List.of("deviceName"))
        );

        MassResult<Long> result = additionalTargetingService.createAddOperation(Applicability.FULL,
                additionalTargetings, campaignInfo.getClientId()).prepareAndApply();
        checkState(!hasValidationIssues(result), "expect operation result without errors");
    }

    private void addRetargetingCondition() {
        var condition = defaultRetCondition(campaignInfo.getClientId());
        retargetingConditionService.addRetargetingConditions(List.of(condition),
                campaignInfo.getClientId());
    }

    private void createWorkbookAndCompareWithExpectedData(InternalAdExportParameters exportParameters,
                                                          String expectedFileName) {
        Workbook workbook = internalAdExcelService.createWorkbook(exportParameters).getWorkbook();
        compareWithExpectedData(workbook, expectedFileName);
    }

    private void createWorkbookAndCompareWithExpectedData(ExcelFetchedData excelFetchedData,
                                                          String expectedFileName, boolean hideEmptyColumns) {
        Workbook workbook = internalAdExcelService
                .createWorkbook(campaignInfo.getClientId(), excelFetchedData, hideEmptyColumns).getWorkbook();
        compareWithExpectedData(workbook, expectedFileName);
    }

    private ExcelFetchedData getDataFromExcelFile(Workbook workbook) {
        ExcelFetchedData dataFromExcelFile = internalAdExcelService
                .getDataFromExcelFile(campaignInfo.getClientId(), workbookToInputStream(workbook));

        // при чтении из эксель для дистриб кампании игнорим приоритет, но в эталонном файле он есть т.к. обязательный
        // столбец, поэтому проставляем сами
        dataFromExcelFile.getAdGroupsSheet().getObjects().forEach(r -> r.getAdGroup().setLevel(AD_GROUP_LEVEL));

        return dataFromExcelFile;
    }

    private static void compareWithExpectedData(Workbook workbook, String expectedFileName) {
        compareWithExpectedData(workbook, expectedFileName, null);
    }

    static void compareWithExpectedData(Workbook workbook, String expectedFileName,
                                        @Nullable CompareStrategy defaultCompareStrategy) {
//        writeWorkbookToFile(workbook, expectedFileName);

        Workbook expectedWorkBook = getExpectedWorkBook(expectedFileName);

        assertThat(workbook.getNumberOfSheets())
                .as("Compare number of sheets")
                .isEqualTo(expectedWorkBook.getNumberOfSheets());

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet actualSheet = workbook.getSheetAt(i);
            Sheet expectedSheet = expectedWorkBook.getSheetAt(i);

            assertThat(actualSheet.getSheetName())
                    .as("Compare name of sheets")
                    .isEqualTo(expectedSheet.getSheetName());

            List<List<String>> actual = MapperTestUtils.sheetToLists(actualSheet, COLUMN_COUNT);
            List<List<String>> expected = MapperTestUtils.sheetToLists(expectedSheet, COLUMN_COUNT);

            CompareStrategy compareStrategy =
                    nvl(defaultCompareStrategy, getCompareStrategy(actualSheet.getSheetName()));
            assertThat(actual)
                    .as("Compare content of sheet: " + actualSheet.getSheetName())
                    .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));

            int columnCounts = expected.get(0).size();
            for (int col = 0; col < columnCounts; col++) {
                assertThat(actualSheet.isColumnHidden(col))
                        .withFailMessage("wrong hidden for column %s, expected hidden is %s", col,
                                expectedSheet.isColumnHidden(col))
                        .isEqualTo(expectedSheet.isColumnHidden(col));
            }
        }
    }

    private static CompareStrategy getCompareStrategy(String sheetName) {
        if (!sheetName.startsWith(AD_GROUP_SHEET_NAME) && !sheetName.startsWith(AD_SHEET_NAME_PREFIX)) {
            return DefaultCompareStrategies.allFields();
        }

        BeanFieldPath[] fieldPaths = sheetName.startsWith(AD_SHEET_NAME_PREFIX)
                ? FIELD_PATHS_FOR_ADS_SHEET
                : FIELD_PATHS_FOR_AD_GROUPS_SHEET;

        return DefaultCompareStrategies.allFields()
                .forFields(fieldPaths)
                .useDiffer(new ChangeableObjectIdDiffer());
    }

    /**
     * записывает workbook в файл. Пригодится если надо обновить ожидаемые файлы в {@link #RESOURCE_DIRECTORY}
     * важно: предварительно path (/Users/xy6er/repo/arcadia/direct/l...) править на соответствующий
     * то что некоторые тесты упадут с ошибкой "Attempting to write" - это норм) надо опять закомментировать вызов
     * этого метода и перезапустить тесты на обновленных файлах
     */
    private static void writeWorkbookToFile(Workbook workbook, String fileName) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            workbook.write(os);
            Path path = Paths.get("/Users/xy6er/",
                    "arc/arcadia/direct/libs-internal/excel-processing/src/test/resources/",
                    RESOURCE_DIRECTORY, fileName);

            Files.write(path, os.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Can't write to file", e);
        }
    }

    private static InputStream workbookToInputStream(Workbook workbook) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            workbook.write(os);
            return new ByteArrayInputStream(os.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Can't convert workbook to InputStream", e);
        }
    }

    private static File getResourceAsFile(String resourcePath) throws Exception {
        return new File(ClassLoader.getSystemResource(resourcePath).toURI());
    }

    private static Workbook getExpectedWorkBook(String fileName) {
        try {
            File resourceAsFile = getResourceAsFile(RESOURCE_DIRECTORY + fileName);
            return new XSSFWorkbook(resourceAsFile);
        } catch (Exception e) {
            throw new RuntimeException("Can't get expected workbook", e);
        }
    }

}
