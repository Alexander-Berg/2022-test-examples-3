package ru.yandex.direct.jobs.uac.service

import java.time.LocalDateTime
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository
import ru.yandex.direct.core.entity.uac.STORE_URL
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDirectContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.TypedCampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.model.createUpdateAdsContainers
import ru.yandex.direct.rbac.RbacRole

@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerCreateJobServiceAgencyTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var bannerCreateJobService: YdbBannerCreateJobService

    @Autowired
    private lateinit var uacYdbAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var bannerRelationsRepository: BannerRelationsRepository

    @Autowired
    private lateinit var typedCampaignStepsUnstubbed: TypedCampaignStepsUnstubbed

    @Autowired
    private lateinit var uacYdbDirectContentRepository: UacYdbDirectContentRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbDirectCampaignRepository: UacYdbDirectCampaignRepository

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var agencyClientInfo: ClientInfo
    private lateinit var uacAccount: UacYdbAccount

    @BeforeEach
    fun before() {
        agencyClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY)
        clientInfo = steps.clientSteps().createDefaultClientUnderAgency(agencyClientInfo)
        userInfo = agencyClientInfo.chiefUserInfo!!

        uacAccount = steps.uacAccountSteps().createAccount(clientInfo)
    }

    @Test
    fun createNewAds_TextCampaign() {
        val textCampaign = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo).apply {
            strategy = TestCampaigns.simpleStrategy()
            agencyId = agencyClientInfo.clientId!!.asLong()
            agencyUid = agencyClientInfo.uid
        }
        val campaignInfo = typedCampaignStepsUnstubbed.createTextCampaign(userInfo, clientInfo, textCampaign)

        val uacCampaign = createYdbCampaign(
            advType = AdvType.TEXT,
            href = "https://www.yandex.ru/company")
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        uacYdbDirectCampaignRepository.saveDirectCampaign(
            UacYdbDirectCampaign(
                id = uacCampaign.id, directCampaignId = campaignInfo.id,
                status = DirectCampaignStatus.CREATED,
                syncedAt = LocalDateTime.now(),
                rejectReasons = null,
            )
        )

        createAndAssertAds(campaignInfo, uacCampaign, TextBanner::class.java)
    }

    @Test
    fun createNewAds_MobileCampaign() {
        val mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL)
        val mobileCampaign = TestCampaigns.defaultMobileContentCampaignWithSystemFields(clientInfo).apply {
            strategy = TestCampaignsStrategy.defaultAutobudgetStrategy()
            mobileAppId = mobileAppInfo.mobileAppId
            agencyId = agencyClientInfo.clientId!!.asLong()
            agencyUid = agencyClientInfo.uid
        }
        val campaignInfo = typedCampaignStepsUnstubbed.createMobileContentCampaign(
            userInfo, clientInfo, mobileCampaign)

        val uacAppInfo = defaultAppInfo()
        uacYdbAppInfoRepository.saveAppInfo(uacAppInfo)

        steps.trustedRedirectSteps().addValidCounters()
        val uacCampaign = createYdbCampaign(appId = uacAppInfo.id)
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        uacYdbDirectCampaignRepository.saveDirectCampaign(
            UacYdbDirectCampaign(
                id = uacCampaign.id, directCampaignId = campaignInfo.id,
                status = DirectCampaignStatus.CREATED,
                syncedAt = LocalDateTime.now(),
                rejectReasons = null,
            )
        )

        createAndAssertAds(campaignInfo, uacCampaign, MobileAppBanner::class.java)
    }

    private fun createAndAssertAds(
        campaignInfo: TypedCampaignInfo, uacCampaign: UacYdbCampaign,
        bannerClass: Class<out BannerWithAdGroupId>
    ) {
        val uacCampaignContents = createCampaignContents(uacCampaign.id)
        val containers = createUpdateAdsContainers(
            userInfo.uid,
            clientInfo.client!!,
            uacCampaign = uacCampaign,
            uacAdGroupBrief = null,
            campaign = campaignInfo.campaign,
        )
        bannerCreateJobService.createNewAdsAndUpdateExist(
            userInfo.clientInfo!!.client!!,
            containers,
            uacCampaign,
            uacDirectAdGroups = listOf(),
            uacAssetsByGroupBriefId = mapOf(null as Long? to uacCampaignContents),
            isItCampaignBrief = true,
        )

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(bannerClass))

        Assertions.assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создалась одна группа")
            .hasSize(1)
        Assertions.assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создался один баннер")
            .hasSize(1)
    }

    private fun createCampaignContents(uacCampaignId: String): List<UacYdbCampaignContent> {
        val uacCampaignContents = listOf(
            createCampaignContent(
                campaignId = uacCampaignId,
                type = MediaType.TITLE,
                text = "title",
            ),
            createCampaignContent(
                campaignId = uacCampaignId,
                type = MediaType.TEXT,
                text = "text",
            ),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))
        return uacCampaignContents
    }
}
