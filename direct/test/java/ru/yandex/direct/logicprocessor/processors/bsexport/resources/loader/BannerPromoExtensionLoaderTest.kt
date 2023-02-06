package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.adv.direct.banner.resources.PromoExtension
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.testPromoExtension
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate
import ru.yandex.direct.ess.common.utils.TablesEnum
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration


@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class BannerPromoExtensionLoaderTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var loader: BannerPromoExtensionLoader

    private lateinit var clientInfo: ClientInfo

    private fun bannerResourceType() = BannerResourceType.BANNER_PROMO_EXTENSION

    @BeforeEach
    fun beforeEach() {
        clientInfo = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun exportAbsentBanner() {
        val loadedResources = loader.loadResources(
            1, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(Long.MAX_VALUE)
                    .setAdditionalTable(TablesEnum.CAMPAIGN_PROMOACTIONS)
                    .setResourceType(bannerResourceType())
                    .build()
            )
        )
        Assertions.assertThat(loadedResources.resources).isEmpty()
    }

    @Test
    fun exportTextBannerWithoutChangesInPromoExtensionWithStatusYes() {
        val promoExtensionInfo = steps.promoExtensionSteps()
            .createPromoExtension(clientInfo, defaultPromoExtensionWithStatus(PromoactionsStatusmoderate.Yes))

        val campaignInfo = steps.textCampaignSteps().createCampaign(
            clientInfo,
            TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
                .withPromoExtensionId(promoExtensionInfo.promoExtensionId)
        )
        val bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo)

        val loadedResources = loader.loadResources(
            clientInfo.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(campaignInfo.campaignId)
                    .setAdditionalTable(TablesEnum.CAMPAIGN_PROMOACTIONS)
                    .setResourceType(bannerResourceType())
                    .build()
            )
        )

        Assertions.assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<PromoExtension>()
                    .setBid(bannerInfo.bannerId)
                    .setPid(bannerInfo.adGroupId)
                    .setCid(bannerInfo.campaignId)
                    .setBsBannerId(bannerInfo.banner.bsBannerId)
                    .setOrderId(campaignInfo.orderId)
                    .setResource(
                        PromoExtension.newBuilder()
                            .setPromoExtensionId(promoExtensionInfo.promoExtensionId)
                            .setHref(promoExtensionInfo.promoExtension.href)
                            .setDescription(promoExtensionInfo.promoExtension.compoundDescription)
                            .setType(promoExtensionInfo.promoExtension.type.ordinal)
                            .build()
                    )
                    .build()
            )
    }

    @Test
    fun exportTextBannerWithoutChangesInPromoExtensionWithStatusNo() {
        val promoExtensionInfo = steps.promoExtensionSteps()
            .createPromoExtension(clientInfo, defaultPromoExtensionWithStatus(PromoactionsStatusmoderate.No))

        val campaignInfo = steps.textCampaignSteps().createCampaign(
            clientInfo,
            TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
                .withPromoExtensionId(promoExtensionInfo.promoExtensionId)
        )
        val bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo)

        val loadedResources = loader.loadResources(
            clientInfo.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(campaignInfo.campaignId)
                    .setAdditionalTable(TablesEnum.CAMPAIGN_PROMOACTIONS)
                    .setResourceType(bannerResourceType())
                    .build()
            )
        )

        Assertions.assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<PromoExtension>()
                    .setBid(bannerInfo.bannerId)
                    .setPid(bannerInfo.adGroupId)
                    .setCid(bannerInfo.campaignId)
                    .setBsBannerId(bannerInfo.banner.bsBannerId)
                    .setOrderId(campaignInfo.orderId)
                    .setResource(null)
                    .build()
            )
    }

    @Test
    fun exportTextBannerWithoutChangesInPromoExtensionWithStatusReady() {
        val promoExtensionInfo = steps.promoExtensionSteps()
            .createPromoExtension(clientInfo, defaultPromoExtensionWithStatus(PromoactionsStatusmoderate.Ready))

        val campaignInfo = steps.textCampaignSteps().createCampaign(
            clientInfo,
            TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
                .withPromoExtensionId(promoExtensionInfo.promoExtensionId)
        )
        val bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo)

        val loadedResources = loader.loadResources(
            clientInfo.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(campaignInfo.campaignId)
                    .setAdditionalTable(TablesEnum.CAMPAIGN_PROMOACTIONS)
                    .setResourceType(bannerResourceType())
                    .build()
            )
        )

        Assertions.assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<PromoExtension>()
                    .setBid(bannerInfo.bannerId)
                    .setPid(bannerInfo.adGroupId)
                    .setCid(bannerInfo.campaignId)
                    .setBsBannerId(bannerInfo.banner.bsBannerId)
                    .setOrderId(campaignInfo.orderId)
                    .setResource(null)
                    .build()
            )
    }

    @Test
    fun exportTextBannerWithOnePromoExtension() {
        val promoExtensionInfo = steps.promoExtensionSteps()
            .createPromoExtension(clientInfo, defaultPromoExtensionWithStatus(PromoactionsStatusmoderate.Yes))

        val campaignInfo = steps.textCampaignSteps().createCampaign(
            clientInfo,
            TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
                .withPromoExtensionId(promoExtensionInfo.promoExtensionId)
        )
        val bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo)

        val loadedResources = loader.loadResources(
            clientInfo.shard,
            listOf(
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(bannerInfo.bannerId)
                    .setAdditionalTable(TablesEnum.BANNERS)
                    .setResourceType(bannerResourceType())
                    .build(),
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(promoExtensionInfo.promoExtensionId)
                    .setAdditionalTable(TablesEnum.PROMOACTIONS)
                    .setResourceType(bannerResourceType())
                    .build(),
            ),
        )

        Assertions.assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<PromoExtension>()
                    .setBid(bannerInfo.bannerId)
                    .setPid(bannerInfo.adGroupId)
                    .setCid(bannerInfo.campaignId)
                    .setBsBannerId(bannerInfo.banner.bsBannerId)
                    .setOrderId(campaignInfo.orderId)
                    .setResource(
                        PromoExtension.newBuilder()
                            .setPromoExtensionId(promoExtensionInfo.promoExtensionId)
                            .setHref(promoExtensionInfo.promoExtension.href)
                            .setDescription(promoExtensionInfo.promoExtension.compoundDescription)
                            .setType(promoExtensionInfo.promoExtension.type.ordinal)
                            .build()
                    )
                    .build()
            )
    }

    @Test
    fun exportTwoTextBannersWithDifferentPromoExtensions() {
        val promoExtensionInfo1 = steps.promoExtensionSteps()
            .createPromoExtension(clientInfo, defaultPromoExtensionWithStatus(PromoactionsStatusmoderate.Yes))

        val campaignInfo1 = steps.textCampaignSteps().createCampaign(
            clientInfo,
            TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
                .withPromoExtensionId(promoExtensionInfo1.promoExtensionId)
        )
        val bannerInfo1 = steps.bannerSteps().createActiveTextBanner(campaignInfo1)

        val promoExtensionInfo2 = steps.promoExtensionSteps()
            .createPromoExtension(clientInfo, defaultPromoExtensionWithStatus(PromoactionsStatusmoderate.Yes))

        val campaignInfo2 = steps.textCampaignSteps().createCampaign(
            clientInfo,
            TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
                .withPromoExtensionId(promoExtensionInfo2.promoExtensionId)
        )
        val bannerInfo2 = steps.bannerSteps().createActiveTextBanner(campaignInfo2)

        val loadedResources = loader.loadResources(
            clientInfo.shard,
            listOf(
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(bannerInfo1.bannerId)
                    .setAdditionalTable(TablesEnum.BANNERS)
                    .setResourceType(bannerResourceType())
                    .build(),
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(bannerInfo2.bannerId)
                    .setAdditionalTable(TablesEnum.BANNERS)
                    .setResourceType(bannerResourceType())
                    .build(),
            ),
        )

        Assertions.assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<PromoExtension>()
                    .setBid(bannerInfo1.bannerId)
                    .setPid(bannerInfo1.adGroupId)
                    .setCid(bannerInfo1.campaignId)
                    .setBsBannerId(bannerInfo1.banner.bsBannerId)
                    .setOrderId(campaignInfo1.orderId)
                    .setResource(
                        PromoExtension.newBuilder()
                            .setPromoExtensionId(promoExtensionInfo1.promoExtensionId)
                            .setHref(promoExtensionInfo1.promoExtension.href)
                            .setDescription(promoExtensionInfo1.promoExtension.compoundDescription)
                            .setType(promoExtensionInfo1.promoExtension.type.ordinal)
                            .build()
                    )
                    .build(),
                BannerResource.Builder<PromoExtension>()
                    .setBid(bannerInfo2.bannerId)
                    .setPid(bannerInfo2.adGroupId)
                    .setCid(bannerInfo2.campaignId)
                    .setBsBannerId(bannerInfo2.banner.bsBannerId)
                    .setOrderId(campaignInfo2.orderId)
                    .setResource(
                        PromoExtension.newBuilder()
                            .setPromoExtensionId(promoExtensionInfo2.promoExtensionId)
                            .setHref(promoExtensionInfo2.promoExtension.href)
                            .setDescription(promoExtensionInfo2.promoExtension.compoundDescription)
                            .setType(promoExtensionInfo2.promoExtension.type.ordinal)
                            .build()
                    )
                    .build(),
            )
    }

    @Test
    fun exportTwoTextBannerWithOnePromoExtension() {
        val promoExtensionInfo = steps.promoExtensionSteps()
            .createPromoExtension(clientInfo, defaultPromoExtensionWithStatus(PromoactionsStatusmoderate.Yes))

        val campaignInfo1 = steps.textCampaignSteps().createCampaign(
            clientInfo,
            TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
                .withPromoExtensionId(promoExtensionInfo.promoExtensionId)
        )
        val bannerInfo1 = steps.bannerSteps().createActiveTextBanner(campaignInfo1)

        val campaignInfo2 = steps.textCampaignSteps().createCampaign(
            clientInfo,
            TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
                .withPromoExtensionId(promoExtensionInfo.promoExtensionId)
        )
        val bannerInfo2 = steps.bannerSteps().createActiveTextBanner(campaignInfo2)

        val loadedResources = loader.loadResources(
            clientInfo.shard,
            listOf(
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(bannerInfo1.bannerId)
                    .setAdditionalTable(TablesEnum.BANNERS)
                    .setResourceType(bannerResourceType())
                    .build(),
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(bannerInfo2.bannerId)
                    .setAdditionalTable(TablesEnum.BANNERS)
                    .setResourceType(bannerResourceType())
                    .build(),
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(promoExtensionInfo.promoExtensionId)
                    .setAdditionalTable(TablesEnum.PROMOACTIONS)
                    .setResourceType(bannerResourceType())
                    .build(),
            ),
        )

        Assertions.assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<PromoExtension>()
                    .setBid(bannerInfo1.bannerId)
                    .setPid(bannerInfo1.adGroupId)
                    .setCid(bannerInfo1.campaignId)
                    .setBsBannerId(bannerInfo1.banner.bsBannerId)
                    .setOrderId(campaignInfo1.orderId)
                    .setResource(
                        PromoExtension.newBuilder()
                            .setPromoExtensionId(promoExtensionInfo.promoExtensionId)
                            .setHref(promoExtensionInfo.promoExtension.href)
                            .setDescription(promoExtensionInfo.promoExtension.compoundDescription)
                            .setType(promoExtensionInfo.promoExtension.type.ordinal)
                            .build()
                    )
                    .build(),
                BannerResource.Builder<PromoExtension>()
                    .setBid(bannerInfo2.bannerId)
                    .setPid(bannerInfo2.adGroupId)
                    .setCid(bannerInfo2.campaignId)
                    .setBsBannerId(bannerInfo2.banner.bsBannerId)
                    .setOrderId(campaignInfo2.orderId)
                    .setResource(
                        PromoExtension.newBuilder()
                            .setPromoExtensionId(promoExtensionInfo.promoExtensionId)
                            .setHref(promoExtensionInfo.promoExtension.href)
                            .setDescription(promoExtensionInfo.promoExtension.compoundDescription)
                            .setType(promoExtensionInfo.promoExtension.type.ordinal)
                            .build()
                    )
                    .build(),
            )
    }

    @Test
    fun exportTextBannerWithChangesOnlyInPromoExtensionWithStatusSent() {
        val promoExtensionInfo = steps.promoExtensionSteps()
            .createPromoExtension(clientInfo, defaultPromoExtensionWithStatus(PromoactionsStatusmoderate.Sent))

        val campaignInfo = steps.textCampaignSteps().createCampaign(
            clientInfo,
            TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
                .withPromoExtensionId(promoExtensionInfo.promoExtensionId)
        )
        steps.bannerSteps().createActiveTextBanner(campaignInfo)

        val loadedResources = loader.loadResources(
            clientInfo.shard,
            listOf(
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(promoExtensionInfo.promoExtensionId)
                    .setAdditionalTable(TablesEnum.PROMOACTIONS)
                    .setResourceType(bannerResourceType())
                    .build(),

                ),
        )

        Assertions.assertThat(loadedResources.resources).isEmpty()
    }

    // Отправляем разные баннеры с разными промоакциями. Ожидаем, что оба придут
    @Test
    fun exportTwoTextBannersWithPromoExtensionsWithStatusSent() {
        val promoExtensionInfo = steps.promoExtensionSteps()
            .createPromoExtension(clientInfo, defaultPromoExtensionWithStatus(PromoactionsStatusmoderate.Sent))

        val campaignInfo = steps.textCampaignSteps().createCampaign(
            clientInfo,
            TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
                .withPromoExtensionId(promoExtensionInfo.promoExtensionId)
        )
        val bannerInfo1 = steps.bannerSteps().createActiveTextBanner(campaignInfo)

        val bannerInfo2 = steps.bannerSteps().createActiveTextBanner(campaignInfo)

        val loadedResources = loader.loadResources(
            clientInfo.shard,
            listOf(
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(bannerInfo1.bannerId)
                    .setAdditionalTable(TablesEnum.BANNERS)
                    .setResourceType(bannerResourceType())
                    .build(),
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(bannerInfo2.bannerId)
                    .setAdditionalTable(TablesEnum.BANNERS)
                    .setResourceType(bannerResourceType())
                    .build(),
            ),
        )

        Assertions.assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<PromoExtension>()
                    .setBid(bannerInfo1.bannerId)
                    .setPid(bannerInfo1.adGroupId)
                    .setCid(bannerInfo1.campaignId)
                    .setBsBannerId(bannerInfo1.banner.bsBannerId)
                    .setOrderId(campaignInfo.orderId)
                    .setResource(null)
                    .build(),
                BannerResource.Builder<PromoExtension>()
                    .setBid(bannerInfo2.bannerId)
                    .setPid(bannerInfo2.adGroupId)
                    .setCid(bannerInfo2.campaignId)
                    .setBsBannerId(bannerInfo2.banner.bsBannerId)
                    .setOrderId(campaignInfo.orderId)
                    .setResource(null)
                    .build(),
            )
    }

    @Test
    fun exportTextBannerWithPromoExtensionWithoutHref() {
        val promoExtensionInfo = steps.promoExtensionSteps()
            .createPromoExtension(
                clientInfo, testPromoExtension(
                    id = null,
                    clientId = clientInfo.clientId!!,
                    description = "промоакция",
                    href = null,
                    startDate = null,
                    finishDate = null,
                    statusModerate = PromoactionsStatusmoderate.Yes,
                )
            )

        val campaignInfo = steps.textCampaignSteps().createCampaign(
            clientInfo,
            TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
                .withPromoExtensionId(promoExtensionInfo.promoExtensionId)
        )
        val adGroup = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo)

        val bannerInfo =
            steps.bannerSteps().createActiveTextBanner(adGroup, "Title", "titleExtension", "body", "https://ya.ru")
        val loadedResources = loader.loadResources(
            clientInfo.shard,
            listOf(
                BsExportBannerResourcesObject.Builder()
                    .setAdditionalId(bannerInfo.bannerId)
                    .setAdditionalTable(TablesEnum.BANNERS)
                    .setResourceType(bannerResourceType())
                    .build(),
            ),
        )

        Assertions.assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<PromoExtension>()
                    .setBid(bannerInfo.bannerId)
                    .setPid(bannerInfo.adGroupId)
                    .setCid(bannerInfo.campaignId)
                    .setBsBannerId(bannerInfo.banner.bsBannerId)
                    .setOrderId(campaignInfo.orderId)
                    .setResource(
                        PromoExtension.newBuilder()
                            .setPromoExtensionId(promoExtensionInfo.promoExtensionId)
                            .setDescription(promoExtensionInfo.promoExtension.compoundDescription)
                            .setType(promoExtensionInfo.promoExtension.type.ordinal)
                            .setHref(bannerInfo.banner.href)
                            .build()
                    )
                    .build(),
            )
    }

    private fun defaultPromoExtensionWithStatus(status: PromoactionsStatusmoderate): ru.yandex.direct.core.entity.promoextension.model.PromoExtension {
        return testPromoExtension(
            id = null,
            clientId = clientInfo.clientId!!,
            description = "промоакция",
            href = "https://ya.ru",
            startDate = null,
            finishDate = null,
            statusModerate = status,
        )
    }
}
