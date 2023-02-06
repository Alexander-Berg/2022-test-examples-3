package ru.yandex.direct.jobs.uac.service

import com.google.common.collect.ImmutableMap
import java.time.LocalDateTime
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.banner.model.Banner
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner
import ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction
import ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.DEFAULT_MAX_BANNERS_IN_UAC_TEXT_ADGROUP
import ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_ADGROUP
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository
import ru.yandex.direct.core.entity.uac.STORE_URL
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDirectAd
import ru.yandex.direct.core.entity.uac.createDirectAdGroup
import ru.yandex.direct.core.entity.uac.createDirectContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.AppInfo
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.direct_ad.DirectAdStatus
import ru.yandex.direct.core.entity.uac.model.direct_content.DirectContentType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAd
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAdGroup
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectContent
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaignWithSystemFields
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.TextBannerInfo
import ru.yandex.direct.core.testing.info.TypedCampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.model.createUpdateAdsContainers
import ru.yandex.direct.jobs.uac.repository.AbstractUacRepositoryJobTest
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy

/**
 * Проверяем функцию createBanner в BannerCreateJobService
 */
@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerCreateJobServiceCreateBannerTest : AbstractUacRepositoryJobTest() {
    companion object {
        private val KEYWORDS = listOf("keyword1", "keyword2")
    }

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbDirectContentRepository: UacYdbDirectContentRepository

    @Autowired
    private lateinit var bannerCreateJobService: YdbBannerCreateJobService

    @Autowired
    private lateinit var typedCampaignStepsUnstubbed: TypedCampaignStepsUnstubbed

    @Autowired
    private lateinit var uacYdbDirectAdRepository: UacYdbDirectAdRepository

    @Autowired
    private lateinit var uacYdbDirectAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    private lateinit var uacYdbAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var uacAppInfoService: UacAppInfoService

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var keywordRepository: KeywordRepository

    @Autowired
    private lateinit var adGroupRepository: AdGroupRepository

    @Autowired
    private lateinit var bannerRelationsRepository: BannerRelationsRepository

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var campaignInfo: TypedCampaignInfo
    private lateinit var adGroupInfo: AdGroupInfo
    private lateinit var appInfo: AppInfo
    private lateinit var uacCampaign: UacYdbCampaign
    private lateinit var titleCampaignContent: UacYdbCampaignContent
    private lateinit var textCampaignContent: UacYdbCampaignContent
    private lateinit var imageCampaignContent: UacYdbDirectContent

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!

        steps.trustedRedirectSteps().addValidCounters()

        val mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL)
        campaignInfo = typedCampaignStepsUnstubbed.createMobileContentCampaign(userInfo, clientInfo,
            defaultMobileContentCampaignWithSystemFields(clientInfo)
                .withStrategy(TestCampaignsStrategy.defaultAutobudgetStrategy())
                .withMobileAppId(mobileAppInfo.mobileAppId)
                .withSource(CampaignSource.UAC))

        val uacAppInfo = defaultAppInfo()
        uacYdbAppInfoRepository.saveAppInfo(uacAppInfo)
        appInfo = uacAppInfoService.getAppInfo(uacAppInfo)

        uacCampaign = createYdbCampaign(
            appId = uacAppInfo.id,
            keywords = KEYWORDS,
        )

        uacYdbCampaignRepository.addCampaign(uacCampaign)
        uacYdbDirectCampaignRepository.saveDirectCampaign(
            UacYdbDirectCampaign(
                id = uacCampaign.id, directCampaignId = campaignInfo.id,
                status = DirectCampaignStatus.CREATED,
                syncedAt = LocalDateTime.now(),
                rejectReasons = null,
            )
        )

        titleCampaignContent = createDefaultTitleContent(uacCampaign.id)
        textCampaignContent = createDefaultTextContent(uacCampaign.id)
        imageCampaignContent = createDefaultImageDirectContent()
    }

    /**
     * Проверяем создание баннера, без создания группы
     */
    @Test
    fun createBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo.toCampaignInfo())

        val uacDirectAdGroup = createDefaultDirectAdGroup(uacCampaign.id, adGroupInfo.adGroupId)

        val container = createUpdateAdsContainers(
            userInfo.uid,
            clientInfo.client!!,
            uacCampaign,
            uacAdGroupBrief = null,
            campaign = campaignInfo.campaign,
        )[0]
        val contentsByType = ContentsByType(titleCampaignContent, textCampaignContent, null, null, null, imageCampaignContent, null, null)
        bannerCreateJobService.createBanners(container, mapOf(), listOf(BannerWithSourceContents(defaultMobileBanner(), contentsByType)), appInfo,
            null, null, AdGroupIdToBannersCnt(adGroupInfo.adGroupId, 0), AdGroupType.MOBILE_CONTENT)

        val actualUacAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        val actualBanners = bannerTypedRepository.getBannersByGroupIds(clientInfo.shard, listOf(adGroupInfo.adGroupId))

        val expectBanner = MobileAppBanner()
            .withTitle(titleCampaignContent.text)
            .withBody(textCampaignContent.text)
            .withImageHash(imageCampaignContent.directImageHash)

        val expectUacDirectAd = createDirectAd(
            titleContentId = titleCampaignContent.id,
            textContentId = textCampaignContent.id,
            directImageContentId = imageCampaignContent.id,
            directAdGroupId = actualUacAdGroups.firstNotNullOfOrNull { it.id },
            directAdId = actualBanners.firstNotNullOfOrNull { it.id },
        )

        checkResults(actualBanners, actualUacAdGroups, expectBanner, uacDirectAdGroup, expectUacDirectAd)
    }

    fun createBannerParameters() = arrayOf(
        arrayOf(0, 1000, 1, 1),
        arrayOf(1000, 1000, 1, 2),
        arrayOf(499, 500, 1, 1),
        arrayOf(501, 500, 1, 2),
        arrayOf(0, 10, 1, 1),
        arrayOf(10, 10, 1, 2),
        arrayOf(499, 500, 2, 2),
        arrayOf(10, 10, 10, 2),
        arrayOf(10, 10, 20, 3),
        arrayOf(10, 10, 21, 4),
        arrayOf(0, MAX_BANNERS_IN_ADGROUP + 1, MAX_BANNERS_IN_ADGROUP + 1, 1),
        arrayOf(MAX_BANNERS_IN_ADGROUP + 1, MAX_BANNERS_IN_ADGROUP + 1, MAX_BANNERS_IN_ADGROUP + 1, 2),
    )

    /**
     * Проверяем создание баннеров с лимитами на размер группы
     */
    @ParameterizedTest(name = "{0}, макс. баннеров в группе = {1}, добавляемое количество баннеров = {2}, " +
        "ожидаемое количество групп = {3}")
    @MethodSource("createBannerParameters")
    fun createBannerWithGroupLimit(bannersInGroup: Int,
                                   maxBannersInGroup: Int,
                                   bannersToAddCount: Int,
                                   adGroupsSize: Int) {
        ppcPropertiesSupport.set(PpcPropertyNames.MAX_BANNERS_IN_UAC_AD_GROUP, maxBannersInGroup.toString())

        adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo.toCampaignInfo())
        createDefaultDirectAdGroup(uacCampaign.id, adGroupInfo.adGroupId)

        val container = createUpdateAdsContainers(
            userInfo.uid,
            clientInfo.client!!,
            uacCampaign,
            uacAdGroupBrief = null,
            campaign = campaignInfo.campaign,
        )[0]
        val contentsByType = ContentsByType(titleCampaignContent, textCampaignContent, null, null, null, imageCampaignContent, null, null)
        val bannersList = List(bannersToAddCount) { BannerWithSourceContents(defaultMobileBanner(), contentsByType) }
        bannerCreateJobService.createBanners(container, mapOf(), bannersList, appInfo,
            null, null, AdGroupIdToBannersCnt(adGroupInfo.adGroupId, bannersInGroup),
            AdGroupType.MOBILE_CONTENT)

        val actualUacAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        val actualAdGroupIds = actualUacAdGroups.map { it.directAdGroupId }
        val actualGroupIdToPhrases = keywordRepository
            .getKeywordTextsByAdGroupIds(clientInfo.shard, clientInfo.clientId, actualAdGroupIds)
            .mapValues {
                it.value.map { k -> k.phrase }
            }
        val actualBannersCount = uacYdbDirectAdRepository.getCountAdsByAdGroupIds(actualUacAdGroups.map { it.id })
        val actualBannersTotal = actualBannersCount.values.sum()

        // Первая группа без фраз
        val expectAdGroupIdToKeywords = actualAdGroupIds
            .filter { it != adGroupInfo.adGroupId }
            .associateBy({ it }, { KEYWORDS })

        val soft = SoftAssertions()
        soft.assertThat(actualUacAdGroups)
            .`as`("количество групп в ydb")
            .hasSize(adGroupsSize)
        soft.assertThat(actualGroupIdToPhrases)
            .`as`("Фразы у групп")
            .isEqualTo(expectAdGroupIdToKeywords)
        soft.assertThat(actualBannersTotal)
            .`as`("Всего объявлений")
            .isEqualTo(bannersToAddCount)
        soft.assertAll()
    }

    fun createBannerParametersForTgo() = arrayOf(
        arrayOf(0, 1000, 1, 1),
        arrayOf(DEFAULT_MAX_BANNERS_IN_UAC_TEXT_ADGROUP - 1, DEFAULT_MAX_BANNERS_IN_UAC_TEXT_ADGROUP, 1, 1),
        arrayOf(DEFAULT_MAX_BANNERS_IN_UAC_TEXT_ADGROUP, DEFAULT_MAX_BANNERS_IN_UAC_TEXT_ADGROUP, 1, 2),
        arrayOf(DEFAULT_MAX_BANNERS_IN_UAC_TEXT_ADGROUP + 1, DEFAULT_MAX_BANNERS_IN_UAC_TEXT_ADGROUP, 1, 2),
        arrayOf(10, 10, 1, 2),
        arrayOf(0, MAX_BANNERS_IN_ADGROUP + 1, MAX_BANNERS_IN_ADGROUP + 1, 1),
        arrayOf(MAX_BANNERS_IN_ADGROUP + 1, MAX_BANNERS_IN_ADGROUP + 1, MAX_BANNERS_IN_ADGROUP + 1, 2),
    )

    /**
     * Проверяем создание баннера ТГО с лимитами на размер группы
     */
    @ParameterizedTest(name = "{0}, макс. баннеров в группе = {1}, добавляемое количество баннеров = {2}, " +
        "ожидаемое количество групп = {3}")
    @MethodSource("createBannerParametersForTgo")
    fun createBannerWithGroupLimitForTgo(bannersInGroup: Int,
                                         maxBannersInGroup: Int,
                                         bannersToAddCount: Int,
                                         adGroupsSize: Int) {
        ppcPropertiesSupport.set(PpcPropertyNames.MAX_BANNERS_IN_UAC_TEXT_AD_GROUP, maxBannersInGroup.toString())


        val textCampaign = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo).apply {
            strategy = TestCampaigns.simpleStrategy()
            source = CampaignSource.UAC
        }
        val textCampaignInfo = typedCampaignStepsUnstubbed.createTextCampaign(userInfo, clientInfo, textCampaign)

        val adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(textCampaignInfo.toCampaignInfo())

        val uacTextCampaign = createYdbCampaign(
            advType = AdvType.TEXT,
            keywords = KEYWORDS,
        )
        uacYdbCampaignRepository.addCampaign(uacTextCampaign)

        val titleCampaignContent = createDefaultTitleContent(uacTextCampaign.id)
        val textCampaignContent = createDefaultTextContent(uacTextCampaign.id)
        val imageCampaignContent = createDefaultImageDirectContent()

        createDefaultDirectAdGroup(uacTextCampaign.id, adGroupInfo.adGroupId)

        val container = createUpdateAdsContainers(
            userInfo.uid,
            clientInfo.client!!,
            uacTextCampaign,
            uacAdGroupBrief = null,
            campaign = textCampaignInfo.campaign,
        )[0]
        val contentsByType = ContentsByType(titleCampaignContent, textCampaignContent, null, null, null, imageCampaignContent, null, null)
        val bannersList = List(bannersToAddCount) { BannerWithSourceContents(defaultTextBanner(), contentsByType) }
        bannerCreateJobService.createBanners(container, mapOf(), bannersList, appInfo,
            null, null, AdGroupIdToBannersCnt(adGroupInfo.adGroupId, bannersInGroup),
            AdGroupType.BASE)

        val actualUacAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacTextCampaign.id)
        val actualAdGroupIds = actualUacAdGroups.map { it.directAdGroupId }
        val actualGroupIdToPhrases = keywordRepository
            .getKeywordTextsByAdGroupIds(clientInfo.shard, clientInfo.clientId, actualAdGroupIds)
            .mapValues {
                it.value.map { k -> k.phrase }
            }
        val actualBannersCount = uacYdbDirectAdRepository.getCountAdsByAdGroupIds(actualUacAdGroups.map { it.id })
        val actualBannersTotal = actualBannersCount.values.sum()

        // Первая группа без фраз
        val expectAdGroupIdToKeywords = actualAdGroupIds
            .filter { it != adGroupInfo.adGroupId }
            .associateBy({ it }, { KEYWORDS })

        val soft = SoftAssertions()
        soft.assertThat(actualUacAdGroups)
            .`as`("количество групп в ydb")
            .hasSize(adGroupsSize)
        soft.assertThat(actualGroupIdToPhrases)
            .`as`("Фразы у групп")
            .isEqualTo(expectAdGroupIdToKeywords)
        soft.assertThat(actualBannersTotal)
            .`as`("Всего объявлений")
            .isEqualTo(bannersToAddCount)
        soft.assertAll()
    }

    /**
     * Проверяем создание баннера и группы
     */
    @Test
    fun createBannerWithAdGroup() {
        val container = createUpdateAdsContainers(
            userInfo.uid,
            clientInfo.client!!,
            uacCampaign,
            uacAdGroupBrief = null,
            campaign = campaignInfo.campaign,
        )[0]
        val contentsByType = ContentsByType(titleCampaignContent, textCampaignContent, null, null, null, imageCampaignContent, null, null)
        bannerCreateJobService.createBanners(container, mapOf(), listOf(BannerWithSourceContents(defaultMobileBanner(), contentsByType)),
            appInfo, null, null, null, AdGroupType.MOBILE_CONTENT)

        val actualUacAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        val actualBanners = bannerTypedRepository.getBannersByGroupIds(clientInfo.shard,
            listOf(actualUacAdGroups.map { it.directAdGroupId }.first()))

        val expectBanner = MobileAppBanner()
            .withTitle(titleCampaignContent.text)
            .withBody(textCampaignContent.text)
            .withImageHash(imageCampaignContent.directImageHash)

        val expectUacAdGroup = createDirectAdGroup(
            directCampaignId = uacCampaign.id,
            directAdGroupId = actualBanners.map { (it as BannerWithSystemFields).adGroupId }.first()
        )

        val expectUacDirectAd = createDirectAd(
            titleContentId = titleCampaignContent.id,
            textContentId = textCampaignContent.id,
            directImageContentId = imageCampaignContent.id,
            directAdGroupId = actualUacAdGroups.firstNotNullOfOrNull { it.id },
            directAdId = actualBanners.firstNotNullOfOrNull { it.id },
        )

        checkResults(actualBanners, actualUacAdGroups, expectBanner, expectUacAdGroup, expectUacDirectAd)
    }

    /**
     * Проверяем создание баннера ТГО когда в группе есть архивные баннеры и их количество превышает лимит на группу
     */
    @Test
    fun createBannerInGroupWithArchivedBannersForTgo() {
        ppcPropertiesSupport.set(PpcPropertyNames.MAX_BANNERS_IN_UAC_TEXT_AD_GROUP, "1")

        val textCampaign = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo).apply {
            strategy = TestCampaigns.simpleStrategy()
            source = CampaignSource.UAC
        }
        val textCampaignInfo = typedCampaignStepsUnstubbed.createTextCampaign(userInfo, clientInfo, textCampaign)

        val adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(textCampaignInfo.toCampaignInfo())
        val archivedBannersInfo: MutableList<TextBannerInfo> = mutableListOf()
        for (i in 0 until DEFAULT_MAX_BANNERS_IN_UAC_TEXT_ADGROUP) {
            archivedBannersInfo.add(steps.bannerSteps().createDefaultArchivedBanner(adGroupInfo))
        }

        val uacTextCampaign = createYdbCampaign(
            advType = AdvType.TEXT,
            keywords = KEYWORDS,
        )
        uacYdbCampaignRepository.addCampaign(uacTextCampaign)

        val titleCampaignContent = createDefaultTitleContent(uacTextCampaign.id)
        val textCampaignContent = createDefaultTextContent(uacTextCampaign.id)
        val imageCampaignContent = createDefaultImageDirectContent()

        val uacAdGroup = createDefaultDirectAdGroup(uacTextCampaign.id, adGroupInfo.adGroupId)
        createDefaultDirectAd(uacAdGroup.id, archivedBannersInfo.first().bannerId, DirectAdStatus.DELETED)

        val container = createUpdateAdsContainers(
            userInfo.uid,
            clientInfo.client!!,
            uacTextCampaign,
            uacAdGroupBrief = null,
            campaign = textCampaignInfo.campaign,
        )[0]

        val contentsByType = ContentsByType(titleCampaignContent, textCampaignContent, null, null, null, imageCampaignContent, null, null)
        bannerCreateJobService.createBanners(container, mapOf(), listOf(BannerWithSourceContents(defaultTextBanner(), contentsByType)),
            appInfo, null, null, AdGroupIdToBannersCnt(adGroupInfo.adGroupId, 0),
            AdGroupType.BASE)

        val actualUacAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacTextCampaign.id)
        val actualUacBannerStatusToCount = uacYdbDirectAdRepository.getByDirectAdGroupId(actualUacAdGroups.map { it.id }, 0L, 1000L)
            .groupBy { it.status }
            .mapValues { it.value.size }

        val actualAdGroupIds = adGroupRepository
            .getAdGroupIdsByCampaignIds(clientInfo.shard, listOf(textCampaignInfo.id))[textCampaignInfo.id]
        val actualBannerCounter = bannerRelationsRepository
            .getBannersCountersByAdgroups(clientInfo.shard, actualAdGroupIds!!)[actualAdGroupIds.first()]

        val soft = SoftAssertions()
        soft.assertThat(actualUacAdGroups)
            .`as`("количество групп в ydb")
            .hasSize(1)
        soft.assertThat(actualUacBannerStatusToCount)
            .`as`("объявления в ydb")
            .hasSize(2)
            .containsEntry(DirectAdStatus.DELETED, 1)
            .containsEntry(DirectAdStatus.CREATED, 1)
        soft.assertThat(actualAdGroupIds)
            .`as`("количество групп в mysql")
            .hasSize(1)
        soft.assertThat(actualBannerCounter?.left)
            .`as`("количество объявлений в mysql")
            .isEqualTo(1L + DEFAULT_MAX_BANNERS_IN_UAC_TEXT_ADGROUP)
        soft.assertThat(actualBannerCounter?.right)
            .`as`("количество не архивных объявлений в mysql")
            .isEqualTo(1L)
        soft.assertAll()
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
            .`is`(matchedBy(beanDiffer(expectBanner).useCompareStrategy(onlyExpectedFields())))

        // Проверяем группу в ydb
        soft.assertThat(actualUacAdGroups)
            .`as`("количество групп в ydb")
            .hasSize(1)
        soft.assertThat(actualUacAdGroups.firstOrNull())
            .`as`("группа в ydb")
            .`is`(matchedBy(beanDiffer(expectUacAdGroup).useCompareStrategy(
                allFieldsExcept(newPath("id")))))

        // Проверяем баннер в ydb
        soft.assertThat(actualUacAds)
            .`as`("количество баннеров в ydb")
            .hasSize(1)
        soft.assertThat(actualUacAds.firstOrNull())
            .`as`("баннер в ydb")
            .`is`(matchedBy(beanDiffer(expectUacDirectAd).useCompareStrategy(
                allFieldsExcept(newPath("id")))))
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

    private fun createDefaultImageDirectContent(): UacYdbDirectContent {
        val imageHash = steps.bannerSteps().createWideImageFormat(clientInfo).imageHash

        imageCampaignContent = createDirectContent(
            type = DirectContentType.IMAGE,
            directImageHash = imageHash,
        )
        uacYdbDirectContentRepository.addDirectContent(listOf(imageCampaignContent))
        return imageCampaignContent
    }

    private fun createDefaultDirectAdGroup(uacCampaignId: String, adGroupId: Long): UacYdbDirectAdGroup {
        val uacDirectAdGroup = createDirectAdGroup(
            directCampaignId = uacCampaignId,
            directAdGroupId = adGroupId,
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)
        return uacDirectAdGroup
    }

    private fun createDefaultDirectAd(
        uacAdGroupId: String,
        bannerId: Long,
        directAdStatus: DirectAdStatus = DirectAdStatus.CREATED,
    ): UacYdbDirectAd {
        val uacDirectAd = createDirectAd(
            directAdGroupId = uacAdGroupId,
            directAdId = bannerId,
            status = directAdStatus,
        )
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd)
        return uacDirectAd
    }

    private fun defaultMobileBanner() = MobileAppBanner()
        .withTitle(titleCampaignContent.text)
        .withBody(textCampaignContent.text)
        .withImageHash(imageCampaignContent.directImageHash)
        .withPrimaryAction(NewMobileContentPrimaryAction.GET)
        .withHref(uacCampaign.trackingUrl)
        .withImpressionUrl(uacCampaign.impressionUrl)
        .withShowTitleAndBody(uacCampaign.showTitleAndBody)
        .withReflectedAttributes(
            ImmutableMap.of(
                NewReflectedAttribute.PRICE, true,
                NewReflectedAttribute.ICON, true,
                NewReflectedAttribute.RATING, true,
                NewReflectedAttribute.RATING_VOTES, true
            )
        )

    private fun defaultTextBanner() = TextBanner()
        .withTitle(titleCampaignContent.text)
        .withBody(textCampaignContent.text)
        .withHref(uacCampaign.storeUrl)
}
