package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.banner.model.Banner
import ru.yandex.direct.core.entity.banner.model.DynamicBanner
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDirectAd
import ru.yandex.direct.core.entity.uac.createDirectAdGroup
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAd
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAdGroup
import ru.yandex.direct.core.testing.data.TestFullGoals
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestLalSegmentRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.model.createUpdateAdsContainers
import ru.yandex.direct.jobs.uac.repository.AbstractUacRepositoryJobTest
import ru.yandex.direct.test.utils.assertj.Conditions

private const val COUNTER_ID = 123
private const val SITE_VISIT_GOAL_ID = Goal.METRIKA_COUNTER_LOWER_BOUND + COUNTER_ID

@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerCreateJobServiceCreateBannerEcomTest : AbstractUacRepositoryJobTest() {
    companion object {
        private val KEYWORDS = listOf("keyword1", "keyword2")
    }

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var campaignRepository: CampaignTypedRepository

    @Autowired
    private lateinit var campaignService: CampaignService

    @Autowired
    private lateinit var bannerCreateJobService: YdbBannerCreateJobService

    @Autowired
    private lateinit var uacYdbDirectAdRepository: UacYdbDirectAdRepository

    @Autowired
    private lateinit var uacYdbDirectAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    private lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var testLalSegmentRepository: TestLalSegmentRepository

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var metrikaHelper: MetrikaHelperStub

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var masterCampaignInfo: CampaignInfo
    private lateinit var dynamicCampaignInfo: CampaignInfo
    private lateinit var adGroupInfo: AdGroupInfo
    private lateinit var uacCampaign: UacYdbCampaign
    private lateinit var titleCampaignContent: UacYdbCampaignContent
    private lateinit var textCampaignContent: UacYdbCampaignContent
    private lateinit var masterCampaign: TextCampaign
    private lateinit var dynamicSubCampaign: DynamicCampaign
    private var feedId: Long = 0

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!

        steps.trustedRedirectSteps().addValidCounters()

        metrikaClient.addUserCounter(userInfo.uid, COUNTER_ID)
        metrikaClient.addCounterGoal(COUNTER_ID, SITE_VISIT_GOAL_ID.toInt())
        metrikaClient.addGoals(userInfo.uid, setOf(TestFullGoals.defaultGoal(SITE_VISIT_GOAL_ID)))
        val lalSegment = TestFullGoals.defaultGoalByType(GoalType.LAL_SEGMENT).apply {
            parentId = SITE_VISIT_GOAL_ID
        }
        testLalSegmentRepository.addAll(listOf(lalSegment))

        feedId = steps.feedSteps().createDefaultFeed(clientInfo).feedId

        val (uacCampaignInfo, campaignInfosByType) = uacCampaignSteps.createEcomUcCampaigns(clientInfo)
        val campaignInfo = uacCampaignInfo.campaign

        val campaigns = campaignRepository.getTypedCampaigns(campaignInfo.shard, setOf(campaignInfo.campaignId))
        masterCampaign = campaigns[0] as TextCampaign

        val subCampaignsIds = campaignService.getSubCampaignIdsWithMasterIds(setOf(masterCampaign.id), clientInfo.clientId!!).keys
        dynamicSubCampaign = campaignRepository.getTypedCampaigns(campaignInfo.shard, subCampaignsIds)
                .map { it as CommonCampaign }
                .filter { it.type == CampaignType.DYNAMIC }
                .map { it as DynamicCampaign }
                .first()

        masterCampaignInfo = campaignInfosByType[CampaignType.TEXT]!!
        dynamicCampaignInfo = campaignInfosByType[CampaignType.DYNAMIC]!!

        uacCampaign = createYdbCampaign(
                advType = AdvType.TEXT,
                href = "https://market.yandex.ru/",
                counterIds = listOf(COUNTER_ID),
                keywords = KEYWORDS,
                isEcom = true,
                feedId = feedId
        )

        titleCampaignContent = createDefaultTitleContent(uacCampaign.id)
        textCampaignContent = createDefaultTextContent(uacCampaign.id)
    }

    @Test
    fun createDynamicBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveDynamicFeedAdGroup(dynamicCampaignInfo)

        val uacDirectAdGroup = createDefaultDirectAdGroup(uacCampaign.id, adGroupInfo.adGroupId)


        val containers = createUpdateAdsContainers(
            userInfo.uid,
            clientInfo.client!!,
            uacCampaign,
            uacAdGroupBrief = null,
            campaign = masterCampaign,
        )
        val contentsByType = ContentsByType(titleCampaignContent, textCampaignContent,
            null, null, null, null, null, null)
        val banner = DynamicBanner()
            .withBody(textCampaignContent.text)
            .withHref(uacCampaign.storeUrl)
        bannerCreateJobService.createBanners(containers[0], mapOf(CampaignType.DYNAMIC to dynamicSubCampaign), listOf(BannerWithSourceContents(banner, contentsByType)),
                null, null, null, AdGroupIdToBannersCnt(adGroupInfo.adGroupId, 0), AdGroupType.DYNAMIC)

        val actualUacAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        val actualBanners = bannerTypedRepository.getBannersByGroupIds(clientInfo.shard, listOf(adGroupInfo.adGroupId))

        val expectBanner = DynamicBanner()
                .withHref(uacCampaign.storeUrl)
                .withBody(textCampaignContent.text)

        val expectUacDirectAd = createDirectAd(
                titleContentId = titleCampaignContent.id,
                textContentId = textCampaignContent.id,
                directAdGroupId = actualUacAdGroups.firstNotNullOfOrNull { it.id },
                directAdId = actualBanners.firstNotNullOfOrNull { it.id },
        )

        checkResults(actualBanners, actualUacAdGroups, expectBanner, uacDirectAdGroup, expectUacDirectAd)
    }

    private fun checkResults(
            actualBanners: Collection<Banner>,
            actualUacAdGroups: Collection<UacYdbDirectAdGroup>,
            expectBanner: Banner,
            expectUacAdGroup: UacYdbDirectAdGroup,
            expectUacDirectAd: UacYdbDirectAd,
    ) {
        val actualUacAds = uacYdbDirectAdRepository.getByDirectAdGroupId(actualUacAdGroups.map { it.id }, 0L, 1000L)

        // Проверяем баннер в mysql
        val soft = SoftAssertions()
        soft.assertThat(actualBanners)
                .`as`("количество баннеров в mysql")
                .hasSize(1)
        soft.assertThat(actualBanners.firstOrNull())
                .`as`("баннер в mysql")
                .`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(expectBanner).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())))

        // Проверяем группу в ydb
        soft.assertThat(actualUacAdGroups)
                .`as`("количество групп в ydb")
                .hasSize(1)
        soft.assertThat(actualUacAdGroups.firstOrNull())
                .`as`("группа в ydb")
                .`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(expectUacAdGroup).useCompareStrategy(
                        DefaultCompareStrategies.allFieldsExcept(BeanFieldPath.newPath("id")))))

        // Проверяем баннер в ydb
        soft.assertThat(actualUacAds)
                .`as`("количество баннеров в ydb")
                .hasSize(1)
        soft.assertThat(actualUacAds.firstOrNull())
                .`as`("баннер в ydb")
                .`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(expectUacDirectAd).useCompareStrategy(
                        DefaultCompareStrategies.allFieldsExcept(BeanFieldPath.newPath("id")))))
        soft.assertAll()
    }

    private fun createDefaultTitleContent(uacCampaignId: String): UacYdbCampaignContent {
        val titleCampaignContent = createCampaignContent(
                campaignId = uacCampaignId,
                type = MediaType.TITLE,
                text = "title",
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(titleCampaignContent))
        return titleCampaignContent
    }

    private fun createDefaultTextContent(uacCampaignId: String): UacYdbCampaignContent {
        val textCampaignContent = createCampaignContent(
                campaignId = uacCampaignId,
                type = MediaType.TEXT,
                text = "text",
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(textCampaignContent))
        return textCampaignContent
    }

    private fun createDefaultDirectAdGroup(uacCampaignId: String, adGroupId: Long): UacYdbDirectAdGroup {
        val uacDirectAdGroup = createDirectAdGroup(
                directCampaignId = uacCampaignId,
                directAdGroupId = adGroupId,
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)
        return uacDirectAdGroup
    }
}
