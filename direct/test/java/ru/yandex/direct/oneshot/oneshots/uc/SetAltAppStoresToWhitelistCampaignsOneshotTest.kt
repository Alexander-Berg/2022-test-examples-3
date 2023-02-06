package ru.yandex.direct.oneshot.oneshots.uc

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyInt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository
import ru.yandex.direct.core.entity.bs.resync.queue.service.BsResyncService
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppRepository
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaignWithSystemFields
import ru.yandex.direct.core.testing.data.TestMobileApps
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.MobileAppInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.info.campaign.MobileContentCampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtCluster
import ru.yandex.direct.ytwrapper.model.YtField
import ru.yandex.direct.ytwrapper.model.YtOperator
import ru.yandex.direct.ytwrapper.model.YtTableRow
import java.util.EnumSet
import java.util.function.Consumer


@OneshotTest
@RunWith(JUnitParamsRunner::class)
internal class SetAltAppStoresToWhitelistCampaignsOneshotTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var mobileAppRepository: MobileAppRepository

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var bsResyncService: BsResyncService

    @Autowired
    private lateinit var bannerRelationsRepository: BannerRelationsRepository

    @Autowired
    private lateinit var mobileContentRepository: MobileContentRepository

    private lateinit var defaultUser: UserInfo

    lateinit var ytProvider: YtProvider
    lateinit var operator: YtOperator

    lateinit var oneshot: SetAltAppStoresToWhitelistCampaignsOneshot

    private lateinit var clientInfo: ClientInfo
    private lateinit var mobileAppInfo: MobileAppInfo

    private var whitelistAltAppStores = EnumSet.of(
        MobileAppAlternativeStore.XIAOMI_GET_APPS,
        MobileAppAlternativeStore.HUAWEI_APP_GALLERY
    )

    @Before
    fun init() {
        ytProvider = mock()
        operator = mock()
        bsResyncService = mock()
        mobileAppRepository = mock()
        mobileContentRepository = mock()

        whenever(bsResyncService.addObjectsToResync(any())).thenReturn(1L)

        oneshot = SetAltAppStoresToWhitelistCampaignsOneshot(
            ytProvider,
            dslContextProvider,
            mobileAppRepository,
            mobileContentRepository,
            bsResyncService,
            bannerRelationsRepository
        )

        defaultUser = steps.userSteps().createDefaultUser()
        clientInfo = defaultUser.clientInfo!!
        mobileAppInfo =
            steps.mobileAppSteps().createMobileApp(clientInfo, TestMobileApps.DEFAULT_STORE_URL)

        whenever(ytProvider.getOperator(any())).thenReturn(operator)
    }

    @Suppress("unused")
    fun testData() = listOf(
        listOf(
            EnumSet.of(
                MobileAppAlternativeStore.XIAOMI_GET_APPS,
                MobileAppAlternativeStore.HUAWEI_APP_GALLERY,
                MobileAppAlternativeStore.VIVO_APP_STORE,
                MobileAppAlternativeStore.SAMSUNG_GALAXY_STORE
            ),
            EnumSet.of(
                MobileAppAlternativeStore.XIAOMI_GET_APPS,
                MobileAppAlternativeStore.HUAWEI_APP_GALLERY,
                MobileAppAlternativeStore.VIVO_APP_STORE,
                MobileAppAlternativeStore.SAMSUNG_GALAXY_STORE
            ),
            TestMobileApps.DEFAULT_STORE_URL,
            false
        ),
        listOf(
            EnumSet.of(
                MobileAppAlternativeStore.XIAOMI_GET_APPS,
                MobileAppAlternativeStore.HUAWEI_APP_GALLERY
            ),
            whitelistAltAppStores,
            TestMobileApps.DEFAULT_STORE_URL,
            false
        ),
        listOf(
            EnumSet.noneOf(MobileAppAlternativeStore::class.java),
            whitelistAltAppStores,
            TestMobileApps.DEFAULT_STORE_URL,
            false
        ),
        listOf(null, whitelistAltAppStores, TestMobileApps.DEFAULT_STORE_URL, false),
        listOf(
            EnumSet.of(
                MobileAppAlternativeStore.HUAWEI_APP_GALLERY
            ),
            EnumSet.of(
                MobileAppAlternativeStore.HUAWEI_APP_GALLERY
            ),
            "https://play.google.com/store/apps/details?id=com.joom",
            true
        ),
        listOf(
            null,
            null,
            "https://play.google.com/store/apps/details?id=com.joom",
            true
        )
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("Old altAppStores: {0}")
    fun executeSuccess(
        oldAltAppStores: EnumSet<MobileAppAlternativeStore>?,
        expectedAltAppStores: EnumSet<MobileAppAlternativeStore>?,
        storeUrl: String,
        isUrlFromApkList: Boolean,
    ) {
        val data = mutableListOf<Long>();
        val mobileAppId =
            if (isUrlFromApkList) {
                val otherMobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, storeUrl)
                otherMobileAppInfo.mobileAppId
            } else {
                data.add(mobileAppInfo.mobileAppId)
                mobileAppInfo.mobileAppId
            }

        val campaign = defaultMobileContentCampaignWithSystemFields(clientInfo)
            .withMobileAppId(mobileAppId)
            .withAlternativeAppStores(oldAltAppStores)

        val campaignInfo: MobileContentCampaignInfo = steps.mobileContentCampaignSteps()
            .createCampaign(clientInfo, campaign)

        val cid = campaignInfo.campaignId

        mockData(data)
        oneshot.execute(UacAltAppStoresParam(YtCluster.HAHN, "tablePath"), null, clientInfo.shard)

        val actualValue =
            campaignTypedRepository.getTypedCampaigns(defaultUser.shard, listOf(cid)).first() as MobileContentCampaign

        Assertions.assertThat(actualValue.alternativeAppStores).`as`("altAppStores").isEqualTo(expectedAltAppStores)
    }

    @Test
    fun executeMultipleAppIdsSuccess() {
        val mobileAppId = mobileAppInfo.mobileAppId
        val campaign = defaultMobileContentCampaignWithSystemFields(clientInfo)
            .withMobileAppId(mobileAppId)
            .withAlternativeAppStores(
                EnumSet.noneOf(
                    MobileAppAlternativeStore::class.java
                )
            )

        val campaignInfo: MobileContentCampaignInfo = steps.mobileContentCampaignSteps()
            .createCampaign(clientInfo, campaign)

        val campaign2 = defaultMobileContentCampaignWithSystemFields(clientInfo)
            .withMobileAppId(mobileAppId)
            .withAlternativeAppStores(
                EnumSet.noneOf(
                    MobileAppAlternativeStore::class.java
                )
            )

        val campaignInfo2: MobileContentCampaignInfo = steps.mobileContentCampaignSteps()
            .createCampaign(clientInfo, campaign2)

        val cid = campaignInfo.campaignId
        val cid2 = campaignInfo2.campaignId

        mockData(listOf(mobileAppId))
        oneshot.execute(UacAltAppStoresParam(YtCluster.HAHN, "tablePath"), null, clientInfo.shard)

        val actualValue =
            campaignTypedRepository.getTypedCampaigns(
                defaultUser.shard,
                listOf(cid, cid2)
            ) as List<MobileContentCampaign>

        val soft = SoftAssertions()
        actualValue.forEach { mobileContentCampaign ->
            soft.assertThat(mobileContentCampaign.alternativeAppStores).`as`("altAppStores")
                .isEqualTo(whitelistAltAppStores)
        }

        val campaignsAltAppStores = actualValue
            .map { it.alternativeAppStores }
        soft.assertThat(campaignsAltAppStores)
            .`as`("altAppStores")
            .hasSize(2)
            .containsExactlyInAnyOrder(whitelistAltAppStores, whitelistAltAppStores)

        soft.assertAll()
    }

    private fun mockData(mobileAppIds: List<Long>) {
        whenever(operator.exists(any()))
            .thenReturn(true)

        doAnswer {
            val downloadDeeplink = YtField("downloadDeeplink", String::class.javaObjectType)
            val regionName = YtField("regionName", String::class.javaObjectType)
            val bundleId = YtField("bundleId", String::class.javaObjectType)

            val row = YtTableRow(listOf(downloadDeeplink, regionName, bundleId))
            row.setValue(downloadDeeplink, "mimarket://details?id=com.alibaba.aliexpresshd&cardType=1")
            row.setValue(regionName, "ru")
            row.setValue(bundleId, mobileAppInfo.mobileContentInfo.storeContentId)
            val consumer = it.getArgument(2, Consumer::class.java) as Consumer<YtTableRow>
            consumer.accept(row)
        }.whenever(operator).readTable(any(), any(), any<Consumer<YtTableRow>>())

        whenever(
            mobileAppRepository.getMobileAppIdsByMobileContentIds(
                anyInt(),
                anyCollection(),
            )
        ).thenReturn(mobileAppIds)
    }
}
