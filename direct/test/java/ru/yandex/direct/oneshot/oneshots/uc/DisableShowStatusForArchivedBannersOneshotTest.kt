package ru.yandex.direct.oneshot.oneshots.uc

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.banner.service.BannerService
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestBannerRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables.BANNERS
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusarch
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusshow
import ru.yandex.direct.dbschema.ppc.enums.CampaignsSource
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.test.utils.TestUtils.assumeThat

@OneshotTest
@RunWith(JUnitParamsRunner::class)
internal class DisableShowStatusForArchivedBannersOneshotTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var campaignRepository: CampaignRepository

    @Autowired
    private lateinit var bannerService: BannerService

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var oneshot: DisableShowStatusForArchivedBannersOneshot

    @Autowired
    private lateinit var testBannerRepository: TestBannerRepository

    private lateinit var clientInfo: ClientInfo

    @Before
    fun init() {
        clientInfo = steps.clientSteps().createDefaultClient()
    }

    fun uacCampaignParameters() = listOf(
        listOf(true, true, true),
        listOf(true, false, true),
        listOf(true, false, false),
        listOf(false, false, true),
        listOf(false, false, false),
        listOf(false, false, false),
    )

    /**
     * Проверка изменения статуса показа архивных баннеров у uac ТГО кампаний
     */
    @Test
    @Parameters(method = "uacCampaignParameters")
    @TestCaseName("status show of archived1 {0}, status show of archived2 {1}, status show of active {2}")
    fun testUacTextCampaign(
        isShowedArchiveBanner1: Boolean,
        isShowedArchiveBanner2: Boolean,
        showStatusOfActiveBanner: Boolean,
    ) {
        val campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        campaignRepository.updateCampaignSource(clientInfo.shard, listOf(campaignInfo.campaignId), CampaignsSource.uac)
        val adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo)

        val archivedBannerInfo1 = steps.bannerSteps().createDefaultArchivedBanner(adGroupInfo)
        val archivedBannerInfo2 = steps.bannerSteps().createDefaultArchivedBanner(adGroupInfo)
        val activeBannerInfo = steps.bannerSteps().createActiveTextBanner(adGroupInfo)

        setBannerStatusShow(archivedBannerInfo1.bannerId, isShowedArchiveBanner1)
        setBannerStatusShow(archivedBannerInfo2.bannerId, isShowedArchiveBanner2)
        setBannerStatusShow(activeBannerInfo.bannerId, showStatusOfActiveBanner)

        oneshot.execute(null, null, clientInfo.shard)

        val actualBannerIdToStatusShow = bannerService.getBannersByAdGroupIds(listOf(adGroupInfo.adGroupId))[adGroupInfo.adGroupId]!!
            .associateBy({ it.id }, { Pair(it.statusShow, it.statusArchived) })

        Assertions.assertThat(actualBannerIdToStatusShow)
            .`as`("Статус показа архивных uac баннеров меняется на false")
            .hasSize(3)
            .containsEntry(archivedBannerInfo1.bannerId, Pair(false, true))
            .containsEntry(archivedBannerInfo2.bannerId, Pair(false, true))
            .containsEntry(activeBannerInfo.bannerId, Pair(showStatusOfActiveBanner, false))
    }

    /**
     * Проверка изменения статуса показа архивных баннеров у uac РМП кампаний
     */
    @Test
    @Parameters(method = "uacCampaignParameters")
    @TestCaseName("status show of archived1 {0}, status show of archived2 {1}, status show of active {2}")
    fun testUacMobileContentCampaign(
        showStatusOfArchiveBanner1: Boolean,
        showStatusOfArchiveBanner2: Boolean,
        showStatusOfActiveBanner: Boolean,
    ) {
        val campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo)
        campaignRepository.updateCampaignSource(clientInfo.shard, listOf(campaignInfo.campaignId), CampaignsSource.uac)
        val adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo)

        val archivedBannerInfo1 = steps.bannerSteps().createActiveMobileAppBanner(adGroupInfo)
        val archivedBannerInfo2 = steps.bannerSteps().createActiveMobileAppBanner(adGroupInfo)
        val activeBannerInfo = steps.bannerSteps().createActiveMobileAppBanner(adGroupInfo)
        testBannerRepository.updateStatusArchive(clientInfo.shard, archivedBannerInfo1.bannerId, BannersStatusarch.Yes)
        testBannerRepository.updateStatusArchive(clientInfo.shard, archivedBannerInfo2.bannerId, BannersStatusarch.Yes)

        setBannerStatusShow(archivedBannerInfo1.bannerId, showStatusOfArchiveBanner1)
        setBannerStatusShow(archivedBannerInfo2.bannerId, showStatusOfArchiveBanner2)
        setBannerStatusShow(activeBannerInfo.bannerId, showStatusOfActiveBanner)

        oneshot.execute(null, null, clientInfo.shard)

        val actualBannerIdToStatusShow = bannerService.getBannersByAdGroupIds(listOf(adGroupInfo.adGroupId))[adGroupInfo.adGroupId]!!
            .associateBy({ it.id }, { Pair(it.statusShow, it.statusArchived) })

        Assertions.assertThat(actualBannerIdToStatusShow)
            .`as`("Статус показа архивных uc баннеров не изменяется")
            .hasSize(3)
            .containsEntry(archivedBannerInfo1.bannerId, Pair(showStatusOfArchiveBanner1, true))
            .containsEntry(archivedBannerInfo2.bannerId, Pair(showStatusOfArchiveBanner2, true))
            .containsEntry(activeBannerInfo.bannerId, Pair(showStatusOfActiveBanner, false))
    }

    /**
     * Проверка изменения статуса показа архивных баннеров у uac ТГО кампаний с фильтром по клиенту
     */
    @Test
    fun testForUacTextContentCampaignWithDifferentClients() {
        val clientInfo2 = steps.clientSteps().createDefaultClient()

        assumeThat{sa-> sa.assertThat(clientInfo.shard)
            .isEqualTo(clientInfo2.shard)}

        val campaignInfo1 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        campaignRepository.updateCampaignSource(clientInfo.shard, listOf(campaignInfo1.campaignId), CampaignsSource.uac)
        val adGroupInfo1 = steps.adGroupSteps().createDefaultAdGroup(campaignInfo1)

        val archivedBannerInfo1 = steps.bannerSteps().createDefaultArchivedBanner(adGroupInfo1)

        val campaignInfo2 = steps.campaignSteps().createActiveTextCampaign(clientInfo2)
        campaignRepository.updateCampaignSource(clientInfo.shard, listOf(campaignInfo2.campaignId), CampaignsSource.uac)
        val adGroupInfo2 = steps.adGroupSteps().createDefaultAdGroup(campaignInfo2)

        val archivedBannerInfo2 = steps.bannerSteps().createDefaultArchivedBanner(adGroupInfo2)

        setBannerStatusShow(archivedBannerInfo1.bannerId, true)
        setBannerStatusShow(archivedBannerInfo2.bannerId, true)

        oneshot.execute(InputData(clientInfo.clientId!!.asLong()), null, clientInfo.shard)

        val actualBannerIdToStatusShow = bannerService
            .getBannersByAdGroupIds(listOf(adGroupInfo1.adGroupId, adGroupInfo2.adGroupId)).values
            .flatten()
            .associateBy({ it.id }, { it.statusShow })

        Assertions.assertThat(actualBannerIdToStatusShow)
            .`as`("Статус показа архивных uac баннеров меняется только для переданного клиента")
            .hasSize(2)
            .containsEntry(archivedBannerInfo1.bannerId, false)
            .containsEntry(archivedBannerInfo2.bannerId, true)
    }

    private fun setBannerStatusShow(
        bannerId: Long,
        isShow: Boolean
    ) {
        dslContextProvider.ppc(clientInfo.shard)
            .update(BANNERS)
            .set(BANNERS.STATUS_SHOW, if (isShow) BannersStatusshow.Yes else BannersStatusshow.No)
            .where(BANNERS.BID.eq(bannerId))
            .execute()
    }
}
