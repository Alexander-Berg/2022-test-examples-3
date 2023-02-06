package ru.yandex.direct.logicprocessor.processors.mysql2grut

import junitparams.naming.TestCaseName
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.banner.model.BannerImageOpts
import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate
import ru.yandex.direct.core.entity.bs.common.service.BsBannerIdCalculator.calculateBsBannerId
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestBanners
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.BannerImageInfo
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.TextBannerInfo
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository
import ru.yandex.direct.core.testing.repository.TestBannerRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class BannerImageReplicationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationApiService: GrutApiService

    @Autowired
    private lateinit var testBannerImageRepository: TestBannerImageRepository

    @Autowired
    private lateinit var testBannerRepository: TestBannerRepository

    private lateinit var clientInfo: ClientInfo
    private lateinit var campaignInfo: CampaignInfo
    private lateinit var adGroupInfo: AdGroupInfo
    private lateinit var bannerImageInfo: BannerImageInfo<*>
    private lateinit var bannerInfo: TextBannerInfo

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        replicationApiService.clientGrutDao.createOrUpdateClients(
            listOf(
                ClientGrutModel(
                    clientInfo.client!!,
                    listOf()
                )
            )
        )

        val campaign = TestCampaigns.activeTextCampaign(null, null)
        campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo)
        bannerInfo = steps.bannerSteps().createBanner(TestBanners.activeTextBanner().withBsBannerId(0), adGroupInfo)
        bannerImageInfo = steps.bannerSteps().createBannerImage(
            bannerInfo,
            steps.bannerSteps().createBannerImageFormat(clientInfo),
            TestBanners.defaultBannerImage(bannerInfo.bannerId, RandomStringUtils.randomAlphanumeric(16))
                .withStatusModerate(OldStatusBannerImageModerate.YES)
                .withBsBannerId(0L)
        )
        processor.withShard(campaignInfo.shard)
    }

    @AfterEach
    fun after() {
        replicationApiService.clientGrutDao.deleteObjects(listOf(clientInfo.clientId!!.asLong()))
    }

    private fun checkBsBannerIds() = listOf(
        arrayOf(0L, 0L, setOf(BannerImageOpts.SINGLE_AD_TO_BS)),
        arrayOf(12345L, 0L, setOf(BannerImageOpts.SINGLE_AD_TO_BS)),
        arrayOf(12345L, 6789L, setOf(BannerImageOpts.SINGLE_AD_TO_BS)),
        arrayOf(0L, 0L, emptySet<BannerImageOpts>()),
        arrayOf(12345L, 0L, emptySet<BannerImageOpts>()),
        arrayOf(12345L, 6789L, emptySet<BannerImageOpts>()),
        arrayOf(0L, 0L, null),
        arrayOf(12345L, 0L, null),
        arrayOf(12345L, 6789L, null),
    )

    /**
     * Проверяем что выставляется корректный bsBannerId для картиночной версии баннера,
     * в зависимости от наличия флага single_ad_to_bs и bsBannerId, bsImageBannerId
     */
    @ParameterizedTest
    @MethodSource("checkBsBannerIds")
    @TestCaseName("bsBannerId: {0}, bsImageBannerId: {1}, opts: {2}")
    fun testBsBannerIdOnImageBanner(
        bsBannerId: Long,
        bsImageBannerId: Long,
        opts: Set<BannerImageOpts>?,
    ) {
        testBannerRepository.updateBannerId(bannerInfo.shard, bannerInfo, bsBannerId)
        testBannerImageRepository.updateBannerIdByImageAndHash(bannerInfo.shard, bannerImageInfo, bsImageBannerId)
        testBannerImageRepository.updateImageOpts(bannerImageInfo.shard, bannerImageInfo.bannerInfo.bannerId, opts)

        assertThatCode {
            processor.process(
                listOf(
                    Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId),
                    Mysql2GrutReplicationObject(adGroupId = adGroupInfo.adGroupId),
                    Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)
                )
            )
        }.doesNotThrowAnyException()

        val expectBsImageBannerId =
            if (bsImageBannerId > 0L) bsImageBannerId
            else if (opts.isNullOrEmpty()) calculateBsBannerId(bannerImageInfo.bannerImageId)
            else if (bsBannerId > 0L) bsBannerId
            else calculateBsBannerId(bannerImageInfo.bannerInfo.bannerId)

        val sourceBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        assertThat(sourceBanner).isNotNull
        assertThat(sourceBanner!!.meta.imageBannerId).isEqualTo(expectBsImageBannerId)
    }
}
