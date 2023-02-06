package ru.yandex.direct.web.entity.excel.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.differ.ListDiffer;
import ru.yandex.autotests.irt.testutils.beandiffer2.differ.SimpleTypeDiffer;
import ru.yandex.direct.common.mds.MdsHolder;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.DesktopInstalledAppsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InternalNetworkAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexUidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.service.AdGroupAdditionalTargetingService;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEngine;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEnginesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.banner.model.old.OldInternalBanner;
import ru.yandex.direct.core.entity.banner.model.old.TemplateVariable;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.InternalBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.excel.processing.model.CampaignInfoWithWorkbook;
import ru.yandex.direct.excel.processing.model.VersionedTargetingInfo;
import ru.yandex.direct.excel.processing.model.internalad.ExcelImportResult;
import ru.yandex.direct.excel.processing.model.internalad.InternalAdExportParameters;
import ru.yandex.direct.excel.processing.service.internalad.InternalAdExcelService;
import ru.yandex.direct.excelmapper.mappers.YesNoBooleanExcelMapper;
import ru.yandex.direct.grid.processing.util.ResultConverterHelper;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.test.utils.differ.SetDiffer;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.excel.ExcelTestUtils;
import ru.yandex.direct.web.entity.excel.model.ExcelFileKey;
import ru.yandex.direct.web.entity.excel.model.UploadedImageInfo;
import ru.yandex.direct.web.entity.excel.model.internalad.AdGroupAdditionalTargetingInfo;
import ru.yandex.direct.web.entity.excel.model.internalad.AdInfo;
import ru.yandex.direct.web.entity.excel.model.internalad.ImageInfo;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportInfo;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportMode;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportRequest;
import ru.yandex.direct.web.entity.excel.model.internalad.TemplateVariableInfo;
import ru.yandex.misc.io.ByteArrayInputStreamSource;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.DistribSoftConstants.DISTRIB_SOFT;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.UaTraitsConstants.BROWSER_ENGINE;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.UaTraitsConstants.BROWSER_ENGINE_VALID_VALUES;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validYandexUidFiltering;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validYandexUidTargeting;
import static ru.yandex.direct.core.testing.data.TestBanners.activeInternalBanner;
import static ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_3_WITH_IMAGE;
import static ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils.RESOURCE_DESCRIPTION;
import static ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils.TEMPLATE_3_RESOURCE_1_REQUIRED_IMAGE;
import static ru.yandex.direct.core.validation.ValidationUtils.hasValidationIssues;
import static ru.yandex.direct.excel.processing.model.internalad.mappers.AdGroupAdditionalTargetingMapperSettings.getTargetingTitle;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.web.entity.excel.ExcelTestData.getDefaultInternalAdImportRequest;
import static ru.yandex.direct.web.entity.excel.service.ExcelWebConverter.toMdsFileKey;
import static ru.yandex.direct.web.entity.excel.service.validation.InternalAdPathConverters.IMPORT_EXCEL_FILE_CONTENT_TYPE_PATH;
import static ru.yandex.direct.web.entity.excel.service.validation.InternalAdPathConverters.toImportExcelFilePath;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class InternalAdExcelWebServiceTest {

    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath("excelFileKey", "fileName"), newPath("excelFileKey", "mdsGroupId"),
                    newPath("ads", "1", "images", "0", "fileName"))
            .useMatcher(not(isEmptyOrNullString()))
            // под 0-ым индексом будет таргетинг со значением String, а для остальных - сет или список значений
            // так приходиться делать из-за того, что тип поля - Object в классе VersionedTargetingInfo
            .forFields(newPath("adGroups", "\\d+", "additionalTargetings", "0", "value"))
            .useDiffer(new SimpleTypeDiffer())
            .forFields(newPath("adGroups", "\\d+", "additionalTargetings", "[1-3]", "value"))
            .useDiffer(new SetDiffer())
            .forFields(newPath("adGroups", "\\d+", "additionalTargetings", "[4-9]", "value"))
            .useDiffer(new ListDiffer());

    @Autowired
    private InternalAdExcelWebService internalAdExcelWebService;

    @Autowired
    private InternalAdExcelService internalAdExcelService;

    @Autowired
    private MdsHolder mdsHolder;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private AdGroupAdditionalTargetingService additionalTargetingService;

    private ClientInfo clientInfo;
    private UidAndClientId uidAndClientId;
    private CampaignInfo campaignInfo;
    private AdGroupInfo emptyGroupInfo;
    private AdGroupInfo adGroupInfo;
    private InternalBannerInfo bannerInfo;
    private InternalBannerInfo bannerWithImageInfo;
    private BannerImageFormat bannerImageFormat;
    private byte[] workbookBytes;
    private InternalNetworkAdGroupAdditionalTargeting internalNetworkTargeting;
    private DesktopInstalledAppsAdGroupAdditionalTargeting desktopAppsTargetingAny;
    private DesktopInstalledAppsAdGroupAdditionalTargeting desktopAppsFiltering;
    private DesktopInstalledAppsAdGroupAdditionalTargeting desktopAppsTargetingAll;
    private YandexUidsAdGroupAdditionalTargeting yandexUidTargeting;
    private YandexUidsAdGroupAdditionalTargeting yandexUidFiltering;
    private BrowserEnginesAdGroupAdditionalTargeting browserEnginesTargeting;
    private BrowserEnginesAdGroupAdditionalTargeting browserEnginesFiltering;

    @Before
    public void initTestData() throws IOException {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();

        uidAndClientId = UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId());
        campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo);

        emptyGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);

        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        addAdGroupAdditionalTargetings(adGroupInfo.getAdGroupId());
        bannerInfo = steps.bannerSteps().createActiveInternalBanner(adGroupInfo);

        bannerImageFormat = steps.bannerSteps().createImageAdImageFormat(clientInfo);
        OldInternalBanner bannerWithImage = activeInternalBanner(campaignInfo.getCampaignId(),
                adGroupInfo.getAdGroupId())
                .withTemplateId(PLACE_1_TEMPLATE_3_WITH_IMAGE)
                .withTemplateVariables(singletonList(
                        new TemplateVariable()
                                .withTemplateResourceId(TEMPLATE_3_RESOURCE_1_REQUIRED_IMAGE)
                                .withInternalValue(bannerImageFormat.getImageHash()))
                );
        bannerWithImageInfo = steps.bannerSteps().createActiveInternalBanner(adGroupInfo, bannerWithImage);

        workbookBytes = getWorkbookAsBytes();
    }


    @Test
    public void checkGetDataFromExcelFileAndUploadFileToMds() {
        InternalAdImportInfo expectedImportInfo = getInternalAdImportInfo();

        checkDataFromExcelFile(InternalAdImportMode.AD_GROUPS_WITH_ADS, expectedImportInfo);
    }

    @Test
    public void checkGetDataFromExcelFileAndUploadFileToMds_OnlyAdGroups() {
        InternalAdImportInfo expectedImportInfo = getInternalAdImportInfo()
                .withAds(null)
                .withAdsCount(null);

        checkDataFromExcelFile(InternalAdImportMode.ONLY_AD_GROUPS, expectedImportInfo);
    }

    @Test
    public void checkGetDataFromExcelFileAndUploadFileToMds_OnlyAds() {
        InternalAdImportInfo expectedImportInfo = getInternalAdImportInfo()
                .withAdGroups(null);

        checkDataFromExcelFile(InternalAdImportMode.ONLY_ADS, expectedImportInfo);
    }

    @Test
    public void checkGetDataFromExcelFile_WhenInvalidImportFile() throws IOException {
        var excelFile = new MockMultipartFile("someFile", new ByteArrayInputStream("invalidData".getBytes()));

        var result = internalAdExcelWebService.getDataFromExcelFileAndUploadFileToMds(
                clientInfo.getClientId(), InternalAdImportMode.ONLY_ADS, excelFile);

        assertThat(result.isSuccessful())
                .isFalse();
        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(
                        validationError(path(field(toImportExcelFilePath(IMPORT_EXCEL_FILE_CONTENT_TYPE_PATH))),
                                CommonDefects.notNull()))));
    }

    private void checkDataFromExcelFile(InternalAdImportMode importMode, InternalAdImportInfo expectedImportInfo) {
        var excelFile = ExcelTestUtils.createMockExcelFile(workbookBytes);

        Result<InternalAdImportInfo> result = internalAdExcelWebService.getDataFromExcelFileAndUploadFileToMds(
                clientInfo.getClientId(), importMode, excelFile);

        InternalAdImportInfo importInfo = result.getResult();

        assertThat(importInfo)
                .isNotNull();
        assertThat(importInfo.getAdGroups() != null || importInfo.getAds() != null)
                .isTrue();

        if (importInfo.getAdGroups() != null) {
            importInfo.getAdGroups()
                    .sort(Comparator.comparing(ru.yandex.direct.web.entity.excel.model.internalad.AdGroupInfo::getId));
        }
        assertThat(importInfo)
                .is(matchedBy(beanDiffer(expectedImportInfo).useCompareStrategy(COMPARE_STRATEGY)));
    }

    @Test
    public void checkImportInternal() {
        InternalAdImportRequest request = getDefaultInternalAdImportRequest()
                .withAdsImages(List.of(new UploadedImageInfo()
                        .withFileName("somePic.jpg")
                        .withImageHash(RandomStringUtils.randomAlphanumeric(11))));
        doReturn(new ByteArrayInputStreamSource(workbookBytes))
                .when(mdsHolder).download(eq(toMdsFileKey(request.getExcelFileKey())));

        Result<ExcelImportResult> result = internalAdExcelWebService.importInternal(
                clientInfo.getUid(), uidAndClientId, request);

        ExcelImportResult excelImportResult = result.getResult();
        assertThat(excelImportResult)
                .isNotNull();

        List<Long> successfullyUpdatedAdGroupIds = ResultConverterHelper
                .getSuccessfullyUpdatedIds(excelImportResult.getAdGroupsResult(), identity());
        assertThat(successfullyUpdatedAdGroupIds)
                .containsExactlyInAnyOrder(emptyGroupInfo.getAdGroupId(), adGroupInfo.getAdGroupId());

        List<Long> successfullyUpdatedAdIds = ResultConverterHelper
                .getSuccessfullyUpdatedIds(excelImportResult.getAdsResult(), identity());
        assertThat(successfullyUpdatedAdIds)
                .containsExactlyInAnyOrder(bannerInfo.getBannerId(), bannerWithImageInfo.getBannerId());
        assertThat(excelImportResult.getExcelFileUrl())
                .isNotEmpty();
    }

    @Test
    public void checkImportInternal_WhenInvalidRequest() {
        //noinspection ConstantConditions
        InternalAdImportRequest request = getDefaultInternalAdImportRequest()
                .withImportMode(null);

        var result = internalAdExcelWebService.importInternal(clientInfo.getUid(), uidAndClientId, request);

        assertThat(result.isSuccessful())
                .isFalse();
        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(
                        validationError(path(field(InternalAdImportRequest.IMPORT_MODE)), CommonDefects.notNull()))));
    }

    private void addAdGroupAdditionalTargetings(long adGroupId) {
        internalNetworkTargeting = new InternalNetworkAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
                .withAdGroupId(adGroupId);
        desktopAppsTargetingAny = new DesktopInstalledAppsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withAdGroupId(adGroupId)
                .withValue(Set.of(1L, 4L, 5L));
        desktopAppsFiltering = new DesktopInstalledAppsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
                .withAdGroupId(adGroupId)
                .withValue(Set.of(2L));
        desktopAppsTargetingAll = new DesktopInstalledAppsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
                .withAdGroupId(adGroupId)
                .withValue(Set.of(3L, 6L));
        yandexUidTargeting = validYandexUidTargeting()
                .withAdGroupId(adGroupId);
        yandexUidFiltering = validYandexUidFiltering()
                .withAdGroupId(adGroupId);
        browserEnginesTargeting = new BrowserEnginesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withAdGroupId(adGroupId)
                .withValue(List.of(
                        new BrowserEngine()
                                .withTargetingValueEntryId(Collections.min(BROWSER_ENGINE_VALID_VALUES))
                                .withMaxVersion("33.4")
                                .withMinVersion("1.0"),
                        new BrowserEngine()
                                .withTargetingValueEntryId(Collections.max(BROWSER_ENGINE_VALID_VALUES))));
        browserEnginesFiltering = new BrowserEnginesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
                .withAdGroupId(adGroupId)
                .withValue(List.of(
                        new BrowserEngine()
                                .withTargetingValueEntryId(Collections.max(BROWSER_ENGINE_VALID_VALUES))
                                .withMinVersion("3.0")));

        MassResult<Long> result = additionalTargetingService.createAddOperation(Applicability.FULL,
                List.of(internalNetworkTargeting,
                        desktopAppsTargetingAny, desktopAppsFiltering, desktopAppsTargetingAll,
                        yandexUidTargeting, yandexUidFiltering,
                        browserEnginesTargeting, browserEnginesFiltering),
                clientInfo.getClientId()).prepareAndApply();
        checkState(!hasValidationIssues(result), "expect operation result without errors");
    }

    private InternalAdImportInfo getInternalAdImportInfo() {
        AdInfo adWithImageInfo = toAdInfo(bannerWithImageInfo, adGroupInfo.getAdGroup().getName())
                .withImages(List.of(toImageInfo(bannerImageFormat)));
        Map<Long, Long> placeIdByCampaignId = campaignService
                .getCampaignInternalPlaces(clientInfo.getClientId(), Set.of(campaignInfo.getCampaignId()));

        return new InternalAdImportInfo()
                .withExcelFileKey(new ExcelFileKey())
                .withClientLogin(clientInfo.getLogin())
                .withCampaignId(campaignInfo.getCampaignId().toString())
                .withCampaignName(campaignInfo.getCampaign().getName())
                .withCampaignArchived(campaignInfo.getCampaign().getArchived())
                .withCampaignType(campaignInfo.getCampaign().getType())
                .withPlaceId(placeIdByCampaignId.get(campaignInfo.getCampaignId()).toString())
                .withAdGroups(toAdGroupsInfo(List.of(emptyGroupInfo, adGroupInfo)))
                .withAds(List.of(toAdInfo(bannerInfo, adGroupInfo.getAdGroup().getName()), adWithImageInfo))
                .withAdsCount(2);
    }

    private byte[] getWorkbookAsBytes() throws IOException {
        var request = new InternalAdExportParameters()
                .withClientId(clientInfo.getClientId())
                .withExportAdGroupsWithAds(true)
                .withCampaignIds(Set.of(campaignInfo.getCampaignId()))
                .withHideEmptyColumns(false);
        CampaignInfoWithWorkbook workbook = internalAdExcelService.createWorkbook(request);

        var outputStream = new ByteArrayOutputStream();
        workbook.getWorkbook().write(outputStream);
        return outputStream.toByteArray();
    }

    private List<ru.yandex.direct.web.entity.excel.model.internalad.AdGroupInfo> toAdGroupsInfo(
            List<AdGroupInfo> adGroupInfos) {
        return mapList(adGroupInfos, adGroup -> {
            InternalAdGroup internalAdGroup = (InternalAdGroup) adGroup.getAdGroup();
            return new ru.yandex.direct.web.entity.excel.model.internalad.AdGroupInfo()
                    .withId(adGroup.getAdGroupId().toString())
                    .withName(internalAdGroup.getName())
                    .withLevel(null)
                    .withStartTime(internalAdGroup.getStartTime())
                    .withFinishTime(internalAdGroup.getFinishTime())
                    .withRf(internalAdGroup.getRf())
                    .withRfReset(internalAdGroup.getRfReset())
                    .withGeoIncluded(internalAdGroup.getGeo())
                    .withGeoExcluded(emptyList())
                    .withRetargetingCondition(null)
                    .withAdditionalTargetings(getAdditionalTargetingsInfo(adGroup));
        });
    }

    private List<AdGroupAdditionalTargetingInfo> getAdditionalTargetingsInfo(AdGroupInfo adGroup) {
        if (adGroup == adGroupInfo) {
            return StreamEx.of(
                    new AdGroupAdditionalTargetingInfo()
                            .withDescription(getTargetingTitle(internalNetworkTargeting))
                            .withValue(YesNoBooleanExcelMapper.booleanToString(
                                    internalNetworkTargeting.getTargetingMode() == AdGroupAdditionalTargetingMode.TARGETING)),
                    new AdGroupAdditionalTargetingInfo()
                            .withDescription(getTargetingTitle(desktopAppsTargetingAny))
                            .withValue(desktopAppsTargetingAny.getValue()
                                    .stream()
                                    .map(DISTRIB_SOFT::get)
                                    .collect(Collectors.toUnmodifiableSet())),
                    new AdGroupAdditionalTargetingInfo()
                            .withDescription(getTargetingTitle(desktopAppsFiltering))
                            .withValue(desktopAppsFiltering.getValue()
                                    .stream()
                                    .map(DISTRIB_SOFT::get)
                                    .collect(Collectors.toUnmodifiableSet())),
                    new AdGroupAdditionalTargetingInfo()
                            .withDescription(getTargetingTitle(desktopAppsTargetingAll))
                            .withValue(desktopAppsTargetingAll.getValue()
                                    .stream()
                                    .map(DISTRIB_SOFT::get)
                                    .collect(Collectors.toUnmodifiableSet())),
                    new AdGroupAdditionalTargetingInfo()
                            .withDescription(getTargetingTitle(yandexUidTargeting))
                            .withValue(yandexUidTargeting.getValue()),
                    new AdGroupAdditionalTargetingInfo()
                            .withDescription(getTargetingTitle(yandexUidFiltering))
                            .withValue(yandexUidFiltering.getValue()),
                    new AdGroupAdditionalTargetingInfo()
                            .withDescription(getTargetingTitle(browserEnginesTargeting))
                            .withValue(toVersionedTargetingInfos(browserEnginesTargeting.getValue())),
                    new AdGroupAdditionalTargetingInfo()
                            .withDescription(getTargetingTitle(browserEnginesFiltering))
                            .withValue(toVersionedTargetingInfos(browserEnginesFiltering.getValue())))
                    .toImmutableList();
        }

        return Collections.emptyList();
    }

    private static List<VersionedTargetingInfo> toVersionedTargetingInfos(List<BrowserEngine> versionedTargetingValues) {
        return mapList(versionedTargetingValues, versionedValue ->
                new VersionedTargetingInfo()
                        .withMaxVersion(versionedValue.getMaxVersion())
                        .withMinVersion(versionedValue.getMinVersion())
                        .withValue(BROWSER_ENGINE.get(versionedValue.getTargetingValueEntryId()))
        );
    }

    private static AdInfo toAdInfo(InternalBannerInfo internalBannerInfo, String adGroupName) {
        checkState(internalBannerInfo.getBanner().getTemplateVariables().size() == 1);
        TemplateVariable templateVariable = internalBannerInfo.getBanner().getTemplateVariables().get(0);

        return new AdInfo()
                .withAdGroupId(internalBannerInfo.getAdGroupId().toString())
                .withAdGroupName(adGroupName)
                .withId(internalBannerInfo.getBanner().getId().toString())
                .withDescription(internalBannerInfo.getBanner().getDescription())
                .withTemplateVariables(List.of(new TemplateVariableInfo()
                        .withDescription(RESOURCE_DESCRIPTION)
                        .withValue(templateVariable.getInternalValue())))
                .withImages(Collections.emptyList());
    }

    private ImageInfo toImageInfo(BannerImageFormat bannerImageFormat) {
        return new ImageInfo()
                .withNeedUpload(false)
                .withImageHash(bannerImageFormat.getImageHash())
                .withHeight(bannerImageFormat.getHeight().intValue())
                .withWidth(bannerImageFormat.getWidth().intValue())
                .withMdsGroupId(bannerImageFormat.getMdsGroupId().toString())
                .withNamespace(bannerImageFormat.getAvatarNamespace().name());
    }

}
