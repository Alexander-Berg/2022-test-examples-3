package ru.yandex.direct.jobs.uac.service

import java.time.Instant
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService
import ru.yandex.direct.core.entity.banner.model.BannerMeasurer
import ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem
import ru.yandex.direct.core.entity.banner.model.ButtonAction
import ru.yandex.direct.core.entity.banner.model.CpmBanner
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.entity.client.model.ClientMeasurerSettings
import ru.yandex.direct.core.entity.client.model.ClientMeasurerSystem
import ru.yandex.direct.core.entity.client.model.MediascopeClientMeasurerSettings
import ru.yandex.direct.core.entity.client.repository.ClientMeasurerSettingsRepository
import ru.yandex.direct.core.entity.client.service.MediascopeClientSettingsService
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository
import ru.yandex.direct.core.entity.uac.UacCommonUtils.CREATIVE_ID_KEY
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.createDirectContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.CampaignStatuses
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.direct_content.DirectContentType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.CpmAssetButton
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacBannerMeasurerSystem
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacButtonAction
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacCpmAsset
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacMeasurer
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAdGroup
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestCampaigns.simpleStrategy
import ru.yandex.direct.core.testing.data.TestFullGoals
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.TypedCampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository
import ru.yandex.direct.core.testing.steps.CreativeSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed
import ru.yandex.direct.feature.FeatureName.UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.model.createUpdateAdsContainers
import ru.yandex.direct.jobs.uac.repository.AbstractUacRepositoryJobTest
import ru.yandex.direct.regions.Region.KAZAKHSTAN_REGION_ID
import ru.yandex.direct.utils.JsonUtils.toJson

@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerCreateJobServiceCpmBannerTest : AbstractUacRepositoryJobTest() {

    @Autowired
    private lateinit var testCryptaSegmentRepository: TestCryptaSegmentRepository

    @Autowired
    private lateinit var creativeSteps: CreativeSteps

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbDirectContentRepository: UacYdbDirectContentRepository

    @Autowired
    private lateinit var bannerCreateJobService: YdbBannerCreateJobService

    @Autowired
    private lateinit var typedCampaignStepsUnstubbed: TypedCampaignStepsUnstubbed

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var bannerRelationsRepository: BannerRelationsRepository

    @Autowired
    private lateinit var retargetingRepository: RetargetingRepository

    @Autowired
    private lateinit var retargetingConditionRepository: RetargetingConditionRepository

    @Autowired
    private lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    private lateinit var adGroupService: AdGroupService

    @Autowired
    private lateinit var uacYdbDirectAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    private lateinit var clientMeasurerSettingsRepository: ClientMeasurerSettingsRepository

    @Autowired
    private lateinit var mediascopeClientSettingsService: MediascopeClientSettingsService

    @Autowired
    private lateinit var steps: Steps

    private lateinit var ydbContents: List<UacYdbContent>;

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var campaignInfo: TypedCampaignInfo
    private lateinit var uacAccount: UacYdbAccount
    private lateinit var uacCampaign: UacYdbCampaign

    private val campaignStatuses = CampaignStatuses(Status.STARTED, TargetStatus.STARTED)

    @AfterEach
    fun cleanup() {
        testCryptaSegmentRepository.clean();
    }

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!

        steps.trustedRedirectSteps().addValidCounters()

        val cpmBannerCampaign = TestCampaigns.defaultCpmBannerCampaignWithSystemFields()
            .withUid(clientInfo.uid)
            .withClientId(clientInfo.clientId?.asLong())
            .apply {
                strategy = simpleStrategy() //TODO startegy
            }

        val allGoals = TestFullGoals.defaultCryptaGoals()
        testCryptaSegmentRepository.addAll(allGoals)
        testCryptaSegmentRepository.addAll(
            listOf(
                2499000001L, 2499000002L, 2499000003L, 2499000004L, 2499000005L,
                2499000006L, 2499000007L, 2499000008L, 2499000009L, 2499000010L,
                2499000011L, 2499000012L, 2499000013L,
            ).map {
                TestFullGoals.defaultGoalByTypeAndId(
                    it,
                    GoalType.SOCIAL_DEMO
                )
            }
        )

        testCryptaSegmentRepository.addAll(listOf(2499000122L, 2499000123L).map {
            TestFullGoals.defaultGoalByTypeAndId(
                it,
                GoalType.FAMILY
            )
        }
        )

        campaignInfo = typedCampaignStepsUnstubbed.createCpmBannerCampaign(userInfo, clientInfo, cpmBannerCampaign)

        uacAccount = steps.uacAccountSteps().createAccount(clientInfo)
        ydbContents = createContents()

        val clientMeasurerSettingsList = listOf(
            ClientMeasurerSettings()
                .withClientId(clientInfo.clientId?.asLong())
                .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                .withSettings(mediascopeClientSettingsService.encryptSettings(toJson(MediascopeClientMeasurerSettings()
                    .withAccessToken("1")
                    .withRefreshToken("2")
                    .withTmsecprefix("prefix")
                    .withExpiresAt(Instant.now().epochSecond + 8000)
                )))
        )
        clientMeasurerSettingsRepository.insertOrUpdate(clientInfo.shard, clientMeasurerSettingsList)
    }

    @Test
    fun createNewAds_WithAutoRetargeting() {
        steps.featureSteps().addClientFeature(userInfo.clientId, UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)

        uacCampaign = createYdbCampaign(
            advType = AdvType.CPM_BANNER,
            href = "https://www.yandex.ru/company",
            socdem = Socdem(
                genders = listOf(Gender.MALE),
                ageLower = AgePoint.AGE_35,
                ageUpper = AgePoint.AGE_INF,
                incomeLower = Socdem.IncomeGrade.MIDDLE,
                incomeUpper = Socdem.IncomeGrade.PREMIUM,
            ),
        )
        uacYdbDirectCampaignRepository.saveDirectCampaign(
            UacYdbDirectCampaign(
                id = uacCampaign.id, directCampaignId = campaignInfo.id,
                status = DirectCampaignStatus.CREATED,
                syncedAt = LocalDateTime.now(),
                rejectReasons = null,
            )
        )

        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents: List<UacYdbCampaignContent> = linkContents();

        runCreateAdsFunction(uacCampaignContents, uacCampaign)

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(CpmBanner::class.java)
        )

        assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создалась одна группа")
            .hasSize(1)

        assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создалось два баннера в группе")
            .hasSize(2)

        val retargetings = retargetingRepository.getRetargetingsByCampaigns(
            clientInfo.shard, listOf(campaignInfo.id)
        )

        assertThat(retargetings)
            .`as`("создался один ретаргетинг")
            .hasSize(1)

        val retargetingConditions = retargetingConditionRepository.getConditions(
            clientInfo.shard, retargetings.map { it.retargetingConditionId })

        assertThat(retargetingConditions)
            .`as`("создалось одно условие ретаргетинга")
            .hasSize(1)

        val retargetingCondition = retargetingConditions[0]
        assertThat(retargetingCondition.autoRetargeting).isFalse
        assertThat(retargetingCondition.rules).hasSize(3)

        val goalIds: List<List<Long>> = retargetingCondition.rules.map { rule ->
            rule.goals.map { it.id }
        }

        val expectedGoals: List<List<Long>> = listOf(
            listOf(2499000006, 2499000007, 2499000008),
            listOf(2499000001),
            listOf(2499000010, 2499000011L, 2499000012, 2499000013),
        )

        assertThat(goalIds).containsExactlyElementsOf(expectedGoals)
    }

    private fun createContents(): List<UacYdbContent> {
        val videoContents: List<UacYdbContent> = listOf(
            createDefaultVideoContent(accountId = uacAccount.id),
            createDefaultVideoContent(accountId = uacAccount.id)
        )

        creativeSteps.addDefaultCpmVideoAdditionCreative(clientInfo, videoContents[0].meta[CREATIVE_ID_KEY] as Long)
        creativeSteps.addDefaultCpmVideoAdditionCreative(clientInfo, videoContents[1].meta[CREATIVE_ID_KEY] as Long)

        uacYdbContentRepository.saveContents(videoContents)

        return videoContents;
    }

    private fun linkContents(): List<UacYdbCampaignContent> {

        val uacCampaignContents: List<UacYdbCampaignContent> = listOf(
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.VIDEO,
                id = ydbContents[0].id
            ),
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.VIDEO,
                id = ydbContents[1].id
            ),
        )

        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)

        //val uacDirectContent = createDirectContent(type = DirectContentType.VIDEO)

        uacYdbDirectContentRepository.addDirectContent(
            listOf(
                createDirectContent(
                    type = DirectContentType.VIDEO,
                    id = uacCampaignContents[0].id,
                    directVideoId = ydbContents[0].meta[CREATIVE_ID_KEY] as Long
                ),
                createDirectContent(
                    type = DirectContentType.VIDEO,
                    id = uacCampaignContents[1].id,
                    directVideoId = ydbContents[1].meta[CREATIVE_ID_KEY] as Long
                )
            )
        )

        return uacCampaignContents;
    }

    @Test
    fun createNewAds_WithAutoRetargetingWithAllGenders() {
        steps.featureSteps().addClientFeature(userInfo.clientId, UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)

        uacCampaign = createYdbCampaign(
            advType = AdvType.CPM_BANNER,
            href = "https://www.yandex.ru/company",
            socdem = Socdem(
                genders = listOf(Gender.MALE, Gender.FEMALE),
                ageLower = AgePoint.AGE_35,
                ageUpper = AgePoint.AGE_INF,
                incomeLower = Socdem.IncomeGrade.MIDDLE,
                incomeUpper = Socdem.IncomeGrade.PREMIUM,
            ),
        )

        uacYdbDirectCampaignRepository.saveDirectCampaign(
            UacYdbDirectCampaign(
                id = uacCampaign.id, directCampaignId = campaignInfo.id,
                status = DirectCampaignStatus.CREATED,
                syncedAt = LocalDateTime.now(),
                rejectReasons = null,
            )
        )

        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents: List<UacYdbCampaignContent> = linkContents();

        runCreateAdsFunction(uacCampaignContents, uacCampaign)

        val retargetings = retargetingRepository.getRetargetingsByCampaigns(
            clientInfo.shard, listOf(campaignInfo.id)
        )

        assertThat(retargetings)
            .`as`("создался один ретаргетинг")
            .hasSize(1)

        val retargetingConditions = retargetingConditionRepository.getConditions(
            clientInfo.shard, retargetings.map { it.retargetingConditionId })

        assertThat(retargetingConditions)
            .`as`("создалось одно условие ретаргетинга")
            .hasSize(1)

        val retargetingCondition = retargetingConditions[0]
        assertThat(retargetingCondition.autoRetargeting).isFalse
        assertThat(retargetingCondition.rules).hasSize(2)

        val goalIds: List<List<Long>> = retargetingCondition.rules.map { rule ->
            rule.goals.map { it.id }
        }

        val expectedGoals: List<List<Long>> = listOf(
            listOf(2499000006, 2499000007, 2499000008),
            listOf(2499000010, 2499000011L, 2499000012, 2499000013),
        )

        assertThat(goalIds).containsExactlyElementsOf(expectedGoals)
    }

    @Test
    fun createNewAds_WithAutoRetargetingWithAllGendersAndInterests() {
        steps.featureSteps().addClientFeature(userInfo.clientId, UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)

        uacCampaign = createYdbCampaign(
            advType = AdvType.CPM_BANNER,
            href = "https://www.yandex.ru/company",
            socdem = Socdem(
                genders = listOf(Gender.MALE, Gender.FEMALE),
                ageLower = AgePoint.AGE_35,
                ageUpper = AgePoint.AGE_INF,
                incomeLower = Socdem.IncomeGrade.MIDDLE,
                incomeUpper = Socdem.IncomeGrade.PREMIUM,
            ),
            retargetingCondition = UacRetargetingCondition(
                conditionRules = listOf(
                    UacRetargetingConditionRule(
                        type = UacRetargetingConditionRule.RuleType.OR,
                        goals = listOf(
                            UacRetargetingConditionRuleGoal(id = 2499000123L),
                            UacRetargetingConditionRuleGoal(id = 2499000122L)
                        )
                    )
                )
            )
        )

        uacYdbDirectCampaignRepository.saveDirectCampaign(
            UacYdbDirectCampaign(
                id = uacCampaign.id, directCampaignId = campaignInfo.id,
                status = DirectCampaignStatus.CREATED,
                syncedAt = LocalDateTime.now(),
                rejectReasons = null,
            )
        )

        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents: List<UacYdbCampaignContent> = linkContents();

        runCreateAdsFunction(uacCampaignContents, uacCampaign)

        val retargetings = retargetingRepository.getRetargetingsByCampaigns(
            clientInfo.shard, listOf(campaignInfo.id)
        )

        assertThat(retargetings)
            .`as`("создался один ретаргетинг")
            .hasSize(1)

        val retargetingConditions = retargetingConditionRepository.getConditions(
            clientInfo.shard, retargetings.map { it.retargetingConditionId })

        assertThat(retargetingConditions)
            .`as`("создалось одно условие ретаргетинга")
            .hasSize(1)

        val retargetingCondition = retargetingConditions[0]
        assertThat(retargetingCondition.autoRetargeting).isFalse
        assertThat(retargetingCondition.rules).hasSize(3)

        val goalIds: List<List<Long>> = retargetingCondition.rules.map { rule ->
            rule.goals.map { it.id }
        }

        val expectedGoals: List<List<Long>> = listOf(
            listOf(2499000006, 2499000007, 2499000008),
            listOf(2499000010, 2499000011L, 2499000012, 2499000013),
            listOf(2499000123L, 2499000122L)
        )

        assertThat(goalIds).containsExactlyElementsOf(expectedGoals)
    }

    @Test
    fun createNewAds_WithCpmAssets() {
        steps.featureSteps().addClientFeature(userInfo.clientId, UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)

        uacCampaign = createYdbCampaign(
            advType = AdvType.CPM_BANNER,
            href = "https://www.yandex.ru/company",

            socdem = Socdem(
                genders = listOf(Gender.MALE, Gender.FEMALE),
                ageLower = AgePoint.AGE_35,
                ageUpper = AgePoint.AGE_INF,
                incomeLower = Socdem.IncomeGrade.MIDDLE,
                incomeUpper = Socdem.IncomeGrade.PREMIUM,
            ),
            retargetingCondition = UacRetargetingCondition(
                conditionRules = listOf(
                    UacRetargetingConditionRule(
                        type = UacRetargetingConditionRule.RuleType.OR,
                        goals = listOf(
                            UacRetargetingConditionRuleGoal(id = 2499000123L),
                            UacRetargetingConditionRuleGoal(id = 2499000122L)
                        )
                    )
                )
            ),
            cpmAssets = mapOf(
                ydbContents[0].id to UacCpmAsset(
                    title = "asset title",
                    body = "asset body",
                    bannerHref = "",
                    pixels = listOf(
                        "https://amc.yandex.ru/show?cmn_id=4&plt_id=4&crv_id=4&evt_tp=impression&ad_type=banner&vv_crit=mrc&rnd=%Random%",
                        "https://mc.yandex.ru/pixel/2555327861230035827?rnd=%aw_random%"
                    ),
                    measurers = listOf(
                        UacMeasurer(
                            UacBannerMeasurerSystem.WEBORAMA,
                            "{\"account\": 1234, \"tte\":964, \"aap\": 172348973}"
                        ),
                        UacMeasurer(UacBannerMeasurerSystem.MEDIASCOPE, ""),
                    ),
                    logoImageHash = null,
                    titleExtension = "title extension",
                    button = CpmAssetButton(action = UacButtonAction.BUY, customText = "", href = "http://yandex.ru"),
                )
            )
        )

        uacYdbDirectCampaignRepository.saveDirectCampaign(
            UacYdbDirectCampaign(
                id = uacCampaign.id, directCampaignId = campaignInfo.id,
                status = DirectCampaignStatus.CREATED,
                syncedAt = LocalDateTime.now(),
                rejectReasons = null,
            )
        )

        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents: List<UacYdbCampaignContent> = linkContents()

        runCreateAdsFunction(uacCampaignContents, uacCampaign)

        val banners = bannerTypedRepository.getBannersByCampaignIdsAndClass(
            clientInfo.shard,
            listOf(campaignInfo.campaign.id),
            CpmBanner::class.java
        )

        assertThat(banners)
            .`as`("создалось ровно два баннера")
            .hasSize(2)

        val creativeIdWithAssets = ydbContents[0].meta[CREATIVE_ID_KEY] as Long;
        val bannerWithAssets = banners.findLast { it.creativeId == creativeIdWithAssets }

        assertThat(bannerWithAssets).isNotNull

        assertThat(bannerWithAssets?.title).isEqualTo("asset title")
        assertThat(bannerWithAssets?.body).isEqualTo("asset body")
        assertThat(bannerWithAssets?.measurers).isEqualTo(
            listOf(
                BannerMeasurer()
                    .withParams("")
                    .withBannerMeasurerSystem(BannerMeasurerSystem.MEDIASCOPE)
                    .withHasIntegration(false),
                BannerMeasurer()
                    .withParams("{\"aap\": 172348973, \"tte\": 964, \"account\": 1234}")
                    .withBannerMeasurerSystem(BannerMeasurerSystem.WEBORAMA)
                    .withHasIntegration(false),
            )
        )
        assertThat(bannerWithAssets?.pixels).isEqualTo(
            listOf("https://amc.yandex.ru/show?cmn_id=4&plt_id=4&crv_id=4&evt_tp=impression&ad_type=banner&vv_crit=mrc&rnd=%Random%",
                "https://mc.yandex.ru/pixel/2555327861230035827?rnd=%aw_random%"
            )
        )
    }

    @Test
    fun createNewAdsAndUpdate_WithCpmAssets() {
        steps.featureSteps().addClientFeature(userInfo.clientId, UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)

        uacCampaign = createYdbCampaign(
            advType = AdvType.CPM_BANNER,
            href = "https://www.yandex.ru/company",

            socdem = Socdem(
                genders = listOf(Gender.MALE, Gender.FEMALE),
                ageLower = AgePoint.AGE_35,
                ageUpper = AgePoint.AGE_INF,
                incomeLower = Socdem.IncomeGrade.MIDDLE,
                incomeUpper = Socdem.IncomeGrade.PREMIUM,
            ),
            retargetingCondition = UacRetargetingCondition(
                conditionRules = listOf(
                    UacRetargetingConditionRule(
                        type = UacRetargetingConditionRule.RuleType.OR,
                        goals = listOf(
                            UacRetargetingConditionRuleGoal(id = 2499000123L),
                            UacRetargetingConditionRuleGoal(id = 2499000122L)
                        )
                    )
                )
            ),
            cpmAssets = mapOf(
                ydbContents[0].id to UacCpmAsset(
                    title = "asset title",
                    body = "asset body",
                    bannerHref = null,
                    pixels = listOf(
                        "https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=%25aw_random%25"
                    ),
                    measurers = listOf(
                        UacMeasurer(
                            UacBannerMeasurerSystem.WEBORAMA,
                            "{\"account\": 1234, \"tte\":964, \"aap\": 172348973}"
                        )
                    ),
                    logoImageHash = null,
                    titleExtension = "title extension",
                    button = CpmAssetButton(action = UacButtonAction.BUY, customText = null, href = "http://yandex.ru"),
                )
            )
        )

        uacYdbDirectCampaignRepository.saveDirectCampaign(
            UacYdbDirectCampaign(
                id = uacCampaign.id, directCampaignId = campaignInfo.id,
                status = DirectCampaignStatus.CREATED,
                syncedAt = LocalDateTime.now(),
                rejectReasons = null,
            )
        )

        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents: List<UacYdbCampaignContent> = linkContents()

        runCreateAdsFunction(uacCampaignContents, uacCampaign)

        val campaignModified = uacCampaign.copy(
            cpmAssets = mapOf(
                ydbContents[0].id to UacCpmAsset(
                    title = "asset title2",
                    body = "asset body2",
                    bannerHref = null,
                    pixels = listOf(
                        "https://ads.adfox.ru/12345/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=%25aw_random%25"
                    ),
                    measurers = listOf(
                        UacMeasurer(
                            UacBannerMeasurerSystem.MOAT, "{}"
                        )
                    ),
                    logoImageHash = null,
                    titleExtension = "title extension",
                    button = CpmAssetButton(action = UacButtonAction.APPLY, customText = null, href = "http://google.ru"),
                )
            )
        )

        runCreateAdsFunction(uacCampaignContents, campaignModified)

        val banners = bannerTypedRepository.getBannersByCampaignIdsAndClass(
            clientInfo.shard,
            listOf(campaignInfo.campaign.id),
            CpmBanner::class.java
        )

        assertThat(banners)
            .`as`("создалось ровно два баннера")
            .hasSize(2)

        val creativeIdWithAssets = ydbContents[0].meta[CREATIVE_ID_KEY] as Long;
        val bannerWithAssets = banners.findLast { it.creativeId == creativeIdWithAssets }

        assertThat(bannerWithAssets).isNotNull

        assertThat(bannerWithAssets?.title).isEqualTo("asset title2")
        assertThat(bannerWithAssets?.body).isEqualTo("asset body2")
        assertThat(bannerWithAssets?.buttonAction).isEqualTo(ButtonAction.APPLY)
        assertThat(bannerWithAssets?.buttonHref).isEqualTo("http://google.ru")
        assertThat(bannerWithAssets?.measurers).isEqualTo(
            listOf(
                BannerMeasurer()
                    .withParams("{}")
                    .withBannerMeasurerSystem(BannerMeasurerSystem.MOAT)
                    .withHasIntegration(false)
            )
        )
        assertThat(bannerWithAssets?.pixels).isEqualTo(
            listOf(
                "https://ads.adfox.ru/12345/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=%25aw_random%25"
            )
        )
    }

    @Test
    fun update_geo() {
        uacCampaign = createYdbCampaign(
            advType = AdvType.CPM_BANNER,
            href = "https://www.yandex.ru/company",
            socdem = Socdem(
                genders = listOf(Gender.MALE, Gender.FEMALE),
                ageLower = AgePoint.AGE_35,
                ageUpper = AgePoint.AGE_INF,
                incomeLower = Socdem.IncomeGrade.MIDDLE,
                incomeUpper = Socdem.IncomeGrade.PREMIUM,
            ),
        )

        uacYdbDirectCampaignRepository.saveDirectCampaign(
            UacYdbDirectCampaign(
                id = uacCampaign.id, directCampaignId = campaignInfo.id,
                status = DirectCampaignStatus.CREATED,
                syncedAt = LocalDateTime.now(),
                rejectReasons = null,
            )
        )

        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents: List<UacYdbCampaignContent> = linkContents()

        runCreateAdsFunction(uacCampaignContents, uacCampaign)

        val campaignModified = uacCampaign.copy(regions = listOf(KAZAKHSTAN_REGION_ID))
        val uacYdbDirectAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        runCreateAdsFunction(uacCampaignContents, campaignModified, uacYdbDirectAdGroups)

        val groups = adGroupService.getSimpleAdGroupsByCampaignIds(
            clientInfo.clientId!!,
            listOf(campaignInfo.campaign.id))[campaignInfo.campaign.id]
        assertThat(groups)
            .`as`("Одна группа")
            .hasSize(1)
        val groupGeo = groups?.get(0)?.geo
        assertThat(groupGeo).isEqualTo(listOf(KAZAKHSTAN_REGION_ID))
    }

    private fun runCreateAdsFunction(
        uacCampaignContents: List<UacYdbCampaignContent>,
        uacCampaign: UacYdbCampaign,
        uacDirectAdGroups: List<UacYdbDirectAdGroup> = listOf(),
    ) {
        val containers = createUpdateAdsContainers(
            userInfo.uid,
            clientInfo.client!!,
            uacCampaign = uacCampaign,
            uacAdGroupBrief = null,
            campaign = campaignInfo.campaign as CpmBannerCampaign,
        )
        bannerCreateJobService.createNewAdsAndUpdateExist(
            userInfo.clientInfo!!.client!!,
            containers,
            uacCampaign,
            uacDirectAdGroups = uacDirectAdGroups,
            uacAssetsByGroupBriefId = mapOf(null as Long? to uacCampaignContents),
            isItCampaignBrief = true,
        )
    }
}
