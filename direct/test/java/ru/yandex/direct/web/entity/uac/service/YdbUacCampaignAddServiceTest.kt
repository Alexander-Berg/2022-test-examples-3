package ru.yandex.direct.web.entity.uac.service

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.apache.commons.lang.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.assertj.core.util.BigDecimalComparator
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds
import ru.yandex.direct.core.entity.region.validation.RegionIdDefectIds
import ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefectIds
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.model.DeviceType
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.LimitPeriodType
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.Sitelink
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.UacCampaignOptions
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.model.UacStrategyData
import ru.yandex.direct.core.entity.uac.model.UacStrategyName
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectCampaign
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.TestDomain
import ru.yandex.direct.core.testing.data.TestOrganizations
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacAccountSteps
import ru.yandex.direct.core.testing.steps.uac.UacContentSteps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.regions.Region
import ru.yandex.direct.result.Result
import ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveInteger
import ru.yandex.direct.testing.matchers.hasErrorOrWarning
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.entity.uac.model.CreateCampaignInternalRequest
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.direct.web.entity.uac.model.UacCampaign

private const val LONG_TEXT_WITH_NARROW_SYMBOLS =
    "Всем привет! Меня зовут Валерия, я творческий фотохудожник из Санкт-Петербурга! Добр!!!"
private const val TOO_LONG_TEXT =
    "Всем привет! Меня зовут Валерия, я творческий фотохудожник из Санкт-Петербурга! Добро"

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class YdbUacCampaignAddServiceTest {

    private val logger = LoggerFactory.getLogger(YdbUacCampaignAddServiceTest::class.java)

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var uacAccountSteps: UacAccountSteps

    @Autowired
    private lateinit var uacContentSteps: UacContentSteps

    @Autowired
    private lateinit var ydbUacCampaignAddService: YdbUacCampaignAddService

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var organizationClient: OrganizationsClientStub

    private val counterId = nextPositiveInteger()
    private val goalId = nextPositiveInteger()

    private lateinit var clientInfo: ClientInfo
    private lateinit var operator: User

    private lateinit var account: UacYdbAccount
    private lateinit var imageContentId: String
    private lateinit var videoContentId: String

    private var hyperGeoId: Long? = null
    private var permalinkId: Long? = null
    private var phoneId: Long? = null
    private val strategy = UacStrategy(
        UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
        UacStrategyData(
            BigDecimal.TEN, false, BigDecimal.TEN, LocalDate.MAX,
            LocalDate.MIN, BigDecimal.ZERO, BigDecimal.ZERO, 0
        )
    )

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        operator = clientInfo.chiefUserInfo?.user!!
        TestAuthHelper.setDirectAuthentication(operator)

        account = uacAccountSteps.createAccount(clientInfo)
        imageContentId = uacContentSteps.createImageContent(clientInfo, account.id).id
        videoContentId = uacContentSteps.createVideoContent(clientInfo, account.id).id

        metrikaClient.addUserCounter(operator.uid, counterId)
        metrikaClient.addCounterGoal(counterId, goalId)

        hyperGeoId = steps.hyperGeoSteps().createHyperGeo(clientInfo).id
        val organization = TestOrganizations.defaultOrganization(clientInfo.clientId!!)
        permalinkId = organization.permalinkId
        organizationClient.addUidsByPermalinkId(permalinkId, listOf(clientInfo.uid))
        phoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientInfo, permalinkId).id
    }

    @Test
    fun `add text campaign success`() {
        val texts = listOf(
            "Banner text",
        )
        val titles = listOf(
            "Banner title",
        )
        val sitelinks = listOf(
            Sitelink(
                title = "Sitelink title",
                href = "https://" + TestDomain.randomDomain(),
                description = null,
            )
        )

        val createCampaignRequest = createTextCampaignRequest(
            texts = texts,
            titles = titles,
            contentIds = listOf(imageContentId, videoContentId),
            counters = listOf(counterId),
            goalId = goalId.toLong(),
            phoneId = phoneId,
            permalinkId = permalinkId,
            hyperGeoId = hyperGeoId,
            sitelinks = sitelinks,
            strategy = null,
            retargetingCondition = null,
        )

        val result = ydbUacCampaignAddService.addUacCampaign(operator, operator, createCampaignRequest)

        assertSoftly {
            it.assertCampaignCreated(createCampaignRequest, result)
            it.assertHasContents(
                result,
                MediaType.IMAGE,
                UacYdbCampaignContent(
                    campaignId = result.result.id,
                    contentId = imageContentId,
                    type = MediaType.IMAGE,
                    order = 0,
                    status = CampaignContentStatus.CREATED,
                )
            )
            it.assertHasContents(
                result,
                MediaType.VIDEO,
                UacYdbCampaignContent(
                    campaignId = result.result.id,
                    contentId = videoContentId,
                    type = MediaType.VIDEO,
                    order = 1,  // потому что image и video имеют общую нумерацию
                    status = CampaignContentStatus.CREATED,
                )
            )
            it.assertHasContents(
                result,
                MediaType.TEXT,
                UacYdbCampaignContent(
                    campaignId = result.result.id,
                    text = texts.first(),
                    type = MediaType.TEXT,
                    order = 0,
                    status = CampaignContentStatus.CREATED,
                )
            )
            it.assertHasContents(
                result,
                MediaType.TITLE,
                UacYdbCampaignContent(
                    campaignId = result.result.id,
                    text = titles.first(),
                    type = MediaType.TITLE,
                    order = 0,
                    status = CampaignContentStatus.CREATED,
                )
            )
            it.assertHasContents(
                result,
                MediaType.SITELINK,
                UacYdbCampaignContent(
                    campaignId = result.result.id,
                    type = MediaType.SITELINK,
                    order = 0,
                    status = CampaignContentStatus.CREATED,
                    sitelink = sitelinks.first(),
                )
            )
        }
    }

    @Test
    fun `add text campaign with no contents fails`() {
        val createCampaignRequest = createTextCampaignRequest(
            texts = listOf(),
            titles = listOf(),
            contentIds = listOf(),
        )

        val result = ydbUacCampaignAddService.addUacCampaign(operator, operator, createCampaignRequest)

        assertSoftly {
            it.assertThat(result.validationResult).hasErrorOrWarning(
                path(field(CreateCampaignRequest::texts)),
                CollectionDefectIds.Size.SIZE_MUST_BE_IN_INTERVAL
            )
            it.assertThat(result.validationResult).hasErrorOrWarning(
                path(field(CreateCampaignRequest::titles)),
                CollectionDefectIds.Size.SIZE_MUST_BE_IN_INTERVAL
            )
        }
    }

    @Test
    fun `add text campaign with too long text`() {
        val createCampaignRequest = createTextCampaignRequest(
            texts = listOf(TOO_LONG_TEXT),
        )

        val result = ydbUacCampaignAddService.addUacCampaign(operator, operator, createCampaignRequest)

        assertThat(result.validationResult).hasErrorOrWarning(
            path(field(CreateCampaignRequest::texts), index(0)),
            BannerDefectIds.String.TEXT_LENGTH_WITHOUT_TEMPLATE_MARKER_CANNOT_BE_MORE_THAN_MAX
        )
    }

    @Test
    fun `add text campaign with narrow symbols in text`() {
        val createCampaignRequest = createTextCampaignRequest(
            texts = listOf(LONG_TEXT_WITH_NARROW_SYMBOLS),
        )

        val result = ydbUacCampaignAddService.addUacCampaign(operator, operator, createCampaignRequest)

        assertThat(result.validationResult).isNull()
    }

    @Test
    fun `add text campaign with empty regions fails`() {
        val createCampaignRequest = createTextCampaignRequest(
            regions = listOf(),
        )

        val result = ydbUacCampaignAddService.addUacCampaign(operator, operator, createCampaignRequest)

        assertThat(result.validationResult).hasErrorOrWarning(
            path(field(CreateCampaignRequest::regions)),
            RegionIdDefectIds.Gen.EMPTY_REGIONS
        )
    }

    @Test
    fun `add text campaign with wrong regions fails`() {
        val createCampaignRequest = createTextCampaignRequest(
            regions = listOf(225, 226),
        )

        val result = ydbUacCampaignAddService.addUacCampaign(operator, operator, createCampaignRequest)

        assertThat(result.validationResult).hasErrorOrWarning(
            path(field(CreateCampaignRequest::regions)),
            RegionIdDefectIds.RegionIds.INCORRECT_REGIONS
        )
    }

    @Test
    fun `add campaign with converting video`() {
        val videoContentWithoutCreative = createDefaultVideoContent(
            accountId = account.id,
            creativeId = null,
        )
        uacContentSteps.createVideoContent(videoContentWithoutCreative)
        val createCampaignRequest = createTextCampaignRequest(
            contentIds = listOf(videoContentWithoutCreative.id),
        )

        val result = ydbUacCampaignAddService.addUacCampaign(operator, operator, createCampaignRequest)

        assertThat(result.validationResult).hasErrorOrWarning(
            path(field(CreateCampaignRequest::contentIds), index(0)),
            DefectIds.INVALID_VALUE
        )
    }

    @Test
    fun `add campaign with too long tracking params`() {
        val sitelinkHref = "https://" + TestDomain.randomDomain()
        val trackingParams = "abc=" + RandomStringUtils.randomAlphanumeric(1024).lowercase()

        val sitelink = Sitelink(
            title = "Sitelink title",
            href = sitelinkHref,
            description = null,
        )

        val createCampaignRequest = createTextCampaignRequest(
            sitelinks = listOf(sitelink),
            trackingParams = trackingParams,
        )

        val result = ydbUacCampaignAddService.addUacCampaign(operator, operator, createCampaignRequest)

        assertThat(result.validationResult).hasErrorOrWarning(
            path(field("complexBanners"), index(0), field("sitelinkSet"), field("sitelinks"), index(0), field("href")),
            SitelinkDefectIds.Strings.SITELINK_HREF_LENGTH_CANNOT_BE_MORE_THAN_MAX
        )
    }

    private fun SoftAssertions.assertCampaignCreated(
        request: CreateCampaignInternalRequest,
        result: Result<UacCampaign>,
    ) {
        assertThat(result.result).`as`("Result is not null").isNotNull
        assertThat(result.isSuccessful).`as`("Result is successful").isTrue
        if (!result.isSuccessful) {
            logger.error("Failed to create campaign: ${result.validationResult?.flattenErrors()}")
        }

        val uacCampaign = result.result
        if (uacCampaign != null) {
            assertTextUacYdbCampaign(uacCampaign.id, account.id, request)
            assertUacYdbDirectCampaign(uacCampaign.id, uacCampaign.directId)
        }
    }

    private fun SoftAssertions.assertTextUacYdbCampaign(
        campaignId: String,
        accountId: String,
        request: CreateCampaignInternalRequest,
    ) {
        val actualCampaign = uacYdbCampaignRepository.getCampaign(campaignId)

        val expectedCampaign = UacYdbCampaign(
            id = campaignId,
            name = request.displayName,
            cpa = request.cpa,
            weekLimit = request.weekLimit,
            regions = request.regions,
            minusRegions = request.minusRegions,
            storeUrl = request.href,
            appId = null,
            targetId = null,
            trackingUrl = null,
            account = accountId,
            impressionUrl = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            startedAt = null,
            advType = request.advType,
            targetStatus = TargetStatus.STOPPED,
            contentFlags = null,
            options = request.limitPeriod?.let { UacCampaignOptions(it) } ?: UacCampaignOptions(),
            skadNetworkEnabled = null,
            adultContentEnabled = null,
            hyperGeoId = request.hyperGeoId,
            keywords = request.keywords,
            minusKeywords = request.minusKeywords,
            socdem = request.socdem,
            deviceTypes = request.deviceTypes,
            inventoryTypes = request.inventoryTypes,
            goals = request.goals,
            counters = request.counters,
            permalinkId = request.permalinkId,
            phoneId = request.phoneId,
            calltrackingSettingsId = request.calltrackingSettingsId,
            timeTarget = null,
            strategy = null,
            retargetingCondition = null,
            videosAreNonSkippable = null,
            brandSurveyId = null,
            briefSynced = true,
            showsFrequencyLimit = null,
            strategyPlatform = null,
            isEcom = null,
            crr = null,
            feedId = null,
            feedFilters = null,
            trackingParams = null,
            cpmAssets = null,
            campaignMeasurers = null,
            uacBrandsafety = null,
            uacDisabledPlaces = null,
            recommendationsManagementEnabled = null,
            priceRecommendationsManagementEnabled = null,
        )

        assertThat(actualCampaign)
            .`as`("Created ydb campaign")
            .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal::class.java)
            .isEqualToIgnoringGivenFields(
                expectedCampaign,
                UacYdbCampaign::createdAt.name,
                UacYdbCampaign::updatedAt.name,
            )

        assertThat(actualCampaign?.createdAt)
            .`as`("Created at")
            .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.HOURS))

        assertThat(actualCampaign?.updatedAt)
            .`as`("Updated at")
            .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.HOURS))
    }

    private fun SoftAssertions.assertUacYdbDirectCampaign(
        campaignId: String,
        directCampaignId: Long?,
    ) {
        val actualDirectCampaign = uacYdbDirectCampaignRepository.getDirectCampaignById(campaignId)

        val expectedDirectCampaign = UacYdbDirectCampaign(
            id = campaignId,
            directCampaignId = directCampaignId!!,
            status = DirectCampaignStatus.DRAFT,
            syncedAt = LocalDateTime.now(),
            rejectReasons = null,
        )

        assertThat(actualDirectCampaign)
            .`as`("Created ydb direct campaign")
            .isEqualToIgnoringGivenFields(
                expectedDirectCampaign,
                UacYdbDirectCampaign::syncedAt.name,
            )

        assertThat(actualDirectCampaign?.syncedAt)
            .`as`("Synced at")
            .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.HOURS))
    }

    private fun SoftAssertions.assertHasContents(
        result: Result<UacCampaign>,
        type: MediaType,
        expected: UacYdbCampaignContent
    ) {
        val campaignId = result.result?.id ?: return
        val actualContents = uacYdbCampaignContentRepository.getCampaignContents(campaignId)
        val actualWithType = actualContents.filter { it.type == type && it.order == expected.order }
        assertThat(actualWithType)
            .hasSize(1)
        assertThat(actualWithType.firstOrNull())
            .isEqualToIgnoringGivenFields(
                expected,
                UacYdbCampaignContent::id.name,
                UacYdbCampaignContent::createdAt.name
            )
    }

    private fun createTextCampaignRequest(
        texts: List<String> = listOf("Some banner text"),
        titles: List<String> = listOf("Some banner title"),
        regions: List<Long> = listOf(Region.MOSCOW_REGION_ID, Region.BY_REGION_ID),
        contentIds: List<String> = listOf(),
        counters: List<Int> = listOf(),
        goalId: Long? = null,
        phoneId: Long? = null,
        permalinkId: Long? = null,
        hyperGeoId: Long? = null,
        sitelinks: List<Sitelink> = listOf(
            Sitelink(
                title = "Sitelink title",
                href = "https://" + TestDomain.randomDomain(),
                description = null,
            )
        ),
        strategy: UacStrategy? = null,
        retargetingCondition: UacRetargetingCondition? = null,
        trackingParams: String? = null,
    ) = CreateCampaignInternalRequest(
        displayName = "Test campaign ${RandomStringUtils.randomAlphanumeric(5)}",
        href = "https://www.yandex.ru/company",
        texts = texts,
        titles = titles,
        regions = regions,
        minusRegions = null,
        contentIds = contentIds,
        advType = AdvType.TEXT,
        weekLimit = BigDecimal.valueOf(2300),
        limitPeriod = LimitPeriodType.WEEK,
        socdem = Socdem(
            genders = listOf(Gender.MALE),
            ageLower = AgePoint.AGE_0,
            ageUpper = AgePoint.AGE_35,
            incomeLower = null,
            incomeUpper = null,
        ),
        goals = goalId?.let {
            listOf(
                UacGoal(
                    goalId = it,
                    conversionValue = null,
                )
            )
        },
        hyperGeoId = hyperGeoId,
        keywords = listOf(
            "Ключевая фраза 1",
            "Ключевая фраза 2",
        ),
        minusKeywords = listOf(
            "Минус слово 1",
            "Минус слово 2",
        ),
        deviceTypes = setOf(DeviceType.ALL),
        inventoryTypes = null,
        counters = counters,
        sitelinks = sitelinks,
        permalinkId = permalinkId,
        phoneId = phoneId,
        calltrackingSettingsId = null,
        appId = null,
        trackingUrl = null,
        impressionUrl = null,
        targetId = null,
        skadNetworkEnabled = null,
        adultContentEnabled = null,
        cpa = null,
        timeTarget = null,
        strategy = strategy,
        retargetingCondition = retargetingCondition,
        videosAreNonSkippable = null,
        zenPublisherId = null,
        brandSurveyId = null,
        brandSurveyName = null,
        showsFrequencyLimit = null,
        strategyPlatform = null,
        adjustments = null,
        isEcom = null,
        crr = null,
        feedId = null,
        feedFilters = null,
        trackingParams = trackingParams,
        cpmAssets = null,
        campaignMeasurers = null,
        uacBrandsafety = null,
        uacDisabledPlaces = null,
        widgetPartnerId = null,
        source = null,
        mobileAppId = null,
        isRecommendationsManagementEnabled = null,
        isPriceRecommendationsManagementEnabled = null,
        relevanceMatch = null,
        showTitleAndBody = null,
        altAppStores = null,
        bizLandingId = null,
        searchLift = null,
    )
}
