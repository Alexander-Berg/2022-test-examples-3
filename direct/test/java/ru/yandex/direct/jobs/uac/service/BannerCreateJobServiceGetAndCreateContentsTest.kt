package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.uac.STORE_URL
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.createDirectContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.direct_content.DirectContentStatus
import ru.yandex.direct.core.entity.uac.model.direct_content.DirectContentType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.TypedCampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.repository.AbstractUacRepositoryJobTest

/**
 * Проверяем функцию createBanner в BannerCreateJobService
 */
@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerCreateJobServiceGetAndCreateContentsTest : AbstractUacRepositoryJobTest() {
    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbDirectContentRepository: UacYdbDirectContentRepository

    @Autowired
    private lateinit var bannerCreateJobService: YdbBannerCreateJobService

    @Autowired
    private lateinit var typedCampaignStepsUnstubbed: TypedCampaignStepsUnstubbed

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var campaignInfo: TypedCampaignInfo
    private lateinit var adGroupInfo: AdGroupInfo
    private lateinit var uacAppInfo: UacYdbAppInfo
    private lateinit var uacAccount: UacYdbAccount
    private lateinit var uacCampaign: UacYdbCampaign

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!

        steps.trustedRedirectSteps().addValidCounters()

        val mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL)
        campaignInfo = typedCampaignStepsUnstubbed.createMobileContentCampaign(userInfo, clientInfo,
            TestCampaigns.defaultMobileContentCampaignWithSystemFields(clientInfo)
                .withStrategy(TestCampaignsStrategy.defaultAutobudgetStrategy())
                .withMobileAppId(mobileAppInfo.mobileAppId))

        adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo.toCampaignInfo())

        uacAppInfo = defaultAppInfo()
        uacYdbAppInfoRepository.saveAppInfo(uacAppInfo)

        uacCampaign = createYdbCampaign(appId = uacAppInfo.id)
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        uacAccount = steps.uacAccountSteps().createAccount(clientInfo)
    }

    /**
     * Если нет контента с картинкой в direct_content -> создается новый
     */
    @Test
    fun imageDirectContentNotExist_CreateNewOne() {
        val imageContent = createDefaultImageContent(accountId = uacAccount.id)
        uacYdbContentRepository.saveContents(listOf(imageContent))

        val uacCampaignContent = createCampaignContent(
            type = MediaType.IMAGE,
            contentId = imageContent.id,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(uacCampaignContent))

        val resultDirectContents = bannerCreateJobService
            .getAndCreateContents(clientInfo.clientId!!, listOf(uacCampaignContent),)

        val expectDirectContent = createDirectContent(
            id = uacCampaignContent.id,
            status = DirectContentStatus.CREATED,
            type = DirectContentType.IMAGE,
            directImageHash = imageContent.directImageHash,
            directVideoId = null,
        )

        assertThat(resultDirectContents).containsOnly(expectDirectContent)
    }

    /**
     * Если нет контента с видео в direct_content -> создается новый
     */
    @Test
    fun videoDirectContentNotExist_CreateNewOne() {
        val videoContent = createDefaultVideoContent(accountId = uacAccount.id)
        uacYdbContentRepository.saveContents(listOf(videoContent))

        val uacCampaignContent = createCampaignContent(
            type = MediaType.VIDEO,
            contentId = videoContent.id,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(uacCampaignContent))

        val resultDirectContents = bannerCreateJobService
            .getAndCreateContents(clientInfo.clientId!!, listOf(uacCampaignContent),)

        val expectDirectContent = createDirectContent(
            id = uacCampaignContent.id,
            status = DirectContentStatus.CREATED,
            type = DirectContentType.VIDEO,
            directVideoId = videoContent.meta["creative_id"].toString().toLongOrNull(),
        )

        assertThat(resultDirectContents).containsOnly(expectDirectContent)
    }

    /**
     * Если есть весть контент с видео и картинками в direct_content -> новые не создаются
     */
    @Test
    fun existAllDirectContents_DoNotCreateNew() {
        val uacCampaignContents = listOf(MediaType.IMAGE, MediaType.VIDEO)
            .map {
                createCampaignContent(
                    type = it,
                )
            }
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)

        val uacDirectContents = uacCampaignContents
            .map {
                createDirectContent(
                    id = it.id
                )
            }
        uacYdbDirectContentRepository.addDirectContent(uacDirectContents)

        val resultDirectContents = bannerCreateJobService.getAndCreateContents(
            clientInfo.clientId!!,
            uacCampaignContents,
        )

        assertThat(resultDirectContents).containsOnlyElementsOf(uacDirectContents)
    }
}
