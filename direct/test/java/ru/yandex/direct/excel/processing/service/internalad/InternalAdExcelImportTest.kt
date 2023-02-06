package ru.yandex.direct.excel.processing.service.internalad

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.TimeAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.service.AdGroupAdditionalTargetingService
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate
import ru.yandex.direct.core.entity.banner.model.InternalBanner
import ru.yandex.direct.core.entity.banner.model.TemplateVariable
import ru.yandex.direct.core.entity.banner.service.BannerService
import ru.yandex.direct.core.entity.campaign.repository.CampaignMappings
import ru.yandex.direct.core.entity.internalads.model.InternalAdsProductOption
import ru.yandex.direct.core.entity.internalads.service.InternalAdsProductService
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.NewInternalBannerInfo
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_WITH_AGE_VARIABLE
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils
import ru.yandex.direct.core.testing.steps.InternalBannerSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.excel.processing.configuration.ExcelProcessingTest
import ru.yandex.direct.excel.processing.model.ObjectType
import ru.yandex.direct.excel.processing.model.internalad.ExcelFetchedData
import ru.yandex.direct.excel.processing.model.internalad.ExcelImportResult
import ru.yandex.direct.excel.processing.model.internalad.InternalAdGroupRepresentation
import ru.yandex.direct.excel.processing.model.internalad.InternalBannerRepresentation
import ru.yandex.direct.excel.processing.utils.createSheetFetchedData
import ru.yandex.direct.excel.processing.utils.getDefaultInternalAdGroupRepresentation
import ru.yandex.direct.excel.processing.utils.getDefaultInternalBannerRepresentation
import ru.yandex.direct.excel.processing.utils.getDefaultSheetDescriptor
import ru.yandex.direct.multitype.entity.LimitOffset
import ru.yandex.direct.regions.Region
import ru.yandex.direct.result.MassResult
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import javax.annotation.ParametersAreNonnullByDefault

@ExcelProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
@ParametersAreNonnullByDefault
class InternalAdExcelImportTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var internalBannerSteps: InternalBannerSteps;

    @Autowired
    private lateinit var internalAdExcelImportService: InternalAdExcelImportService

    @Autowired
    private lateinit var adGroupsService: AdGroupService

    @Autowired
    private lateinit var bannerService: BannerService

    @Autowired
    private lateinit var internalAdsProductService: InternalAdsProductService

    @Autowired
    private lateinit var additionalTargetingService: AdGroupAdditionalTargetingService

    private val adGroupCompareStrategy: CompareStrategy = DefaultCompareStrategies.onlyExpectedFields()
    private val bannerCompareStrategy: CompareStrategy =
        DefaultCompareStrategies.allFieldsExcept(BeanFieldPath.newPath("\\d+", "lastChange"))

    private lateinit var campaignInfo: CampaignInfo
    private var campaignId = 0L
    private var operatorUid = 0L
    private lateinit var uidAndClientId: UidAndClientId
    private lateinit var excelFetchedData: ExcelFetchedData
    private lateinit var adGroupRepresentation: InternalAdGroupRepresentation
    private lateinit var bannerRepresentation: InternalBannerRepresentation
    private lateinit var additionalTargetingsWhichWillNotBeDeleted: List<AdGroupAdditionalTargeting>

    @Before
    fun initTestData() {
        val clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct()

        campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo)
        campaignId = campaignInfo.campaignId
        operatorUid = clientInfo.uid
        uidAndClientId = UidAndClientId.of(clientInfo.uid, clientInfo.clientId)

        val sheetDescriptor = getDefaultSheetDescriptor(campaignId)
        adGroupRepresentation = getDefaultInternalAdGroupRepresentation(campaignId)
        val adGroupsSheet = createSheetFetchedData(sheetDescriptor, listOf(adGroupRepresentation))
        bannerRepresentation = getDefaultInternalBannerRepresentation(
            adGroupRepresentation.adGroup,
            sheetDescriptor.templateId
        )
        val adsSheet = createSheetFetchedData(adGroupsSheet.sheetDescriptor, listOf(bannerRepresentation))
        excelFetchedData = ExcelFetchedData.create(adGroupsSheet, listOf(adsSheet))

        additionalTargetingsWhichWillNotBeDeleted = getAdditionalTargetingsWhichWillNotBeDeleted()
    }

    private fun getAdditionalTargetingsWhichWillNotBeDeleted(): List<AdGroupAdditionalTargeting> {
        val timeAdGroupAdditionalTargeting = TimeAdGroupAdditionalTargeting()
            .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
            .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
            .withValue(listOf(CampaignMappings.timeTargetFromDb("2BbCbDb3BcCcDc4BdCdDd")))
        return listOf(timeAdGroupAdditionalTargeting)
    }

    @Test
    fun checkImportFromExcel_WhenAddNewAdGroupWithNewBanner() {
        val result = importFromExcel()

        with(result) {
            checkHasNoValidationIssues()
            checkSuccessfullyImportAdGroups()
            checkSuccessfullyImportBanners()
        }
    }

    @Test
    fun checkImportFromExcel_WhenAddNewAdGroupWithNewBanner_AgeVariableValidation() {
        prepareAgGroupAndBannerWithAgeVariable()
        val result = importFromExcel(validationOnly = true)

        with(result) {
            checkHasNoValidationIssues()
            checkNotImportAdGroups()
            checkNotImportBanners()
        }
    }

    @Test
    fun checkImportFromExcel_WhenValidationOnly() {
        val result = importFromExcel(validationOnly = true)

        with(result) {
            checkHasNoValidationIssues()
            checkNotImportAdGroups()
            checkNotImportBanners()
        }
    }

    @Test
    fun checkImportFromExcel_WhenUpdateAdGroupAndBanner() {
        prepareAdGroupAndBannerForUpdate()
        val result = importFromExcel()

        with(result) {
            checkHasNoValidationIssues()
            checkSuccessfullyImportAdGroups()
            checkSuccessfullyImportBanners(
                expectedBanner = bannerRepresentation.banner
                    .withStatusBsSynced(StatusBsSynced.NO)
                    .withStatusModerate(BannerStatusModerate.READY)
                    .withStatusPostModerate(BannerStatusPostModerate.NO)
            )
        }
    }

    @Test
    fun checkImportFromExcel_WhenImportOnlyAdGroup() {
        val result = importFromExcel(objectTypesForImport = setOf(ObjectType.AD_GROUP))

        with(result) {
            checkHasNoValidationIssues()
            checkSuccessfullyImportAdGroups()
            checkNotImportBanners()
        }
    }

    @Test
    fun checkImportFromExcel_WhenImportOnlyAdGroup_WithMinusCrimeaRegion() {
        adGroupRepresentation.adGroup.geo = listOf(Region.RUSSIA_REGION_ID, -Region.CRIMEA_REGION_ID)
        val result = importFromExcel(objectTypesForImport = setOf(ObjectType.AD_GROUP))

        with(result) {
            checkHasNoValidationIssues()
            //проставим ожидаемый geo, который должны сохранить в базу
            adGroupRepresentation.adGroup.geo = listOf(Region.RUSSIA_REGION_ID)
            checkSuccessfullyImportAdGroups()
            checkNotImportBanners()
        }
    }

    @Test
    fun checkImportFromExcel_WhenImportOnlyBanner() {
        prepareAdGroupAndBannerForUpdate()
        val result = importFromExcel(objectTypesForImport = setOf(ObjectType.AD))

        with(result) {
            checkHasNoValidationIssues()
            checkSuccessfullyImportBanners()
            checkNotUpdateAdGroups()
        }
    }

    @Test
    fun checkImportFromExcel_WhenUpdateAdGroup_WithTargetings_WhichWeCanEditOnlyFromFront() {
        val adGroupInfo = steps.adGroupSteps().createAdGroup(adGroupRepresentation.adGroup, campaignInfo)
        steps.adGroupAdditionalTargetingSteps()
            .addValidTargetingsToAdGroup(adGroupInfo, additionalTargetingsWhichWillNotBeDeleted)

        val result = importFromExcel()
        with(result) {
            checkHasNoValidationIssues()
            checkSuccessfullyImportAdGroups()
            checkNotDeleteSomeTargetins()
        }
    }

    private fun importFromExcel(
        validationOnly: Boolean = false,
        objectTypesForImport: Set<ObjectType> = setOf(ObjectType.AD_GROUP, ObjectType.AD)
    ): ExcelImportResult = internalAdExcelImportService
        .importFromExcel(operatorUid, uidAndClientId, validationOnly, objectTypesForImport, excelFetchedData)

    private fun prepareAdGroupAndBannerForUpdate() {
        val adGroupInfo = steps.adGroupSteps().createAdGroup(adGroupRepresentation.adGroup, campaignInfo)
        internalBannerSteps.createInternalBanner(
            NewInternalBannerInfo().withAdGroupInfo(adGroupInfo).withBanner(bannerRepresentation.banner)
        )
        adGroupRepresentation.adGroup.name = "new name " + RandomStringUtils.randomAlphabetic(11)
        bannerRepresentation.banner.description = "new description " + RandomStringUtils.randomAlphabetic(7)
    }

    private fun prepareAgGroupAndBannerWithAgeVariable() {
        bannerRepresentation.banner.withTemplateId(PLACE_1_TEMPLATE_WITH_AGE_VARIABLE)
            .withTemplateVariables(
                listOf(
                    TemplateVariable().withTemplateResourceId(
                        TemplateResourceRepositoryMockUtils.TEMPLATE_6_RESOURCE_AGE
                    )
                        .withInternalValue(null)
                )
            )
        excelFetchedData.adGroupsSheet.sheetDescriptor.templateId = PLACE_1_TEMPLATE_WITH_AGE_VARIABLE

        adGroupRepresentation.adGroup.geo = listOf(Region.KAZAKHSTAN_REGION_ID)

        val product = internalAdsProductService.getProduct(uidAndClientId.clientId)
        product.options = setOf(InternalAdsProductOption.SOFTWARE)
        internalAdsProductService.updateProduct(product)
    }

    private fun AdGroupService.getAdGroupsByCampaignId(campaignId: Long): MutableList<AdGroup> =
        this.getAdGroupsBySelectionCriteria(
            AdGroupsSelectionCriteria().withCampaignIds(campaignId),
            LimitOffset.maxLimited(), false
        )

    private fun ExcelImportResult.checkHasNoValidationIssues() {
        assertThat(this.adGroupsResult.validationResult.flattenErrors())
            .isEmpty()
        assertThat(this.adsResult.validationResult.flattenErrors())
            .isEmpty()
        assertThat(this.hasValidationIssues())
            .isFalse()
    }

    private fun ExcelImportResult.checkSuccessfullyImportAdGroups() {
        val adGroups = adGroupsService.getAdGroupsByCampaignId(campaignId)

        assertThat(adGroups)
            .`is`(
                matchedBy(
                    beanDiffer(listOf(adGroupRepresentation.adGroup))
                        .useCompareStrategy(adGroupCompareStrategy)
                )
            )
            .extracting<Long> { it.id }
            .isEqualTo(this.getAdGroupIds())
    }

    private fun ExcelImportResult.checkNotImportAdGroups() {
        val adGroups = adGroupsService.getAdGroupsByCampaignId(campaignId)

        assertThat(adGroups)
            .isEmpty()
        assertThat(this.getAdGroupIds())
            //null - особенность псевдо-операции для групп если вызвать в режиме валидации - prepareAndCancel
            //Когда делали не нашли нормального способа поправить, оставили как есть т.к. ничего не ломает. Фронт в том числе
            .isEqualTo(listOf(null))
    }

    private fun ExcelImportResult.checkNotUpdateAdGroups() {
        val adGroups = adGroupsService.getAdGroupsByCampaignId(campaignId)
        assertThat(adGroups)
            .hasSize(1)
            .extracting<String> { it.name }
            .doesNotContain(adGroupRepresentation.adGroup.name)
        assertThat(this.getAdGroupIds())
            .isEmpty()
    }

    private fun ExcelImportResult.checkSuccessfullyImportBanners(
        expectedBanner: InternalBanner = bannerRepresentation.banner
    ) {
        val banners = bannerService.getBannersByCampaignIds(setOf(campaignId))

        assertThat(banners)
            .`is`(
                matchedBy(
                    beanDiffer(listOf(expectedBanner))
                        .useCompareStrategy(bannerCompareStrategy)
                )
            )
            .extracting<Long> { it.id }
            .isEqualTo(this.getBannerIds())
    }

    private fun ExcelImportResult.checkNotImportBanners() {
        val banners = bannerService.getBannersByCampaignIds(setOf(campaignId))

        assertThat(banners)
            .isEmpty()
        assertThat(this.getBannerIds())
            .isEmpty()
    }

    private fun ExcelImportResult.checkNotDeleteSomeTargetins() {
        val targetings =
            additionalTargetingService.getTargetingsByAdGroupIds(uidAndClientId.clientId, this.getAdGroupIds())
        assertThat(targetings).isEqualTo(additionalTargetingsWhichWillNotBeDeleted)
    }

    private fun ExcelImportResult.checkDeleteAllTargetings() {
        val targetings =
            additionalTargetingService.getTargetingsByAdGroupIds(uidAndClientId.clientId, this.getAdGroupIds())
        assertThat(targetings).isEmpty()
    }

    private fun ExcelImportResult.getAdGroupIds() = getSuccessfullyResultIds(this.adGroupsResult)

    private fun ExcelImportResult.getBannerIds() = getSuccessfullyResultIds(this.adsResult)

    private fun getSuccessfullyResultIds(massResult: MassResult<Long>): List<Long> =
        massResult.result?.filter { it.isSuccessful }?.map { it.result } ?: emptyList()
}
