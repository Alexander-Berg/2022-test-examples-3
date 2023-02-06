package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.jobs.configuration.GrutJobsTest

@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrutUpdateAdsJobForAdGroupBriefBannersTest : GrutUpdateAdsJobForAdGroupBriefBaseTest() {

    /**
     * Проверяем добавление баннеров
     */
    @ParameterizedTest(name = "titles: {0}, texts: {1}, images: {2}, videos: {3}, html5: {4}")
    @MethodSource("parametersForTextCampaign")
    fun testCreateBanners(
        titleAssetCount: Int,
        textAssetCount: Int,
        imageAssetCount: Int,
        withVideoAsset: Boolean,
        withHtml5Asset: Boolean,
    ) {
        setupTextCampaign()
        val adGroupBriefId = createAdGroupBriefGrutModel().id!!

        val (grutTAssets, assets) = createAssets(
            titleAssetCount,
            textAssetCount,
            imageAssetCount,
            withVideoAsset,
            withHtml5Asset,
        )
        grutSteps.setCustomAssetLinksToAdGroupBrief(adGroupBriefId, assets)

        updateAdsJob.withShard(shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val actualAdGroupBriefGrutModel = grutApiService.adGroupBriefGrutApi.getAdGroupBrief(adGroupBriefId)!!
        val actualDirectBanners = bannerTypedRepository.getBannersByCampaignIds(shard, listOf(directCampaignId))
            .filterIsInstance<TextBanner>()

        val expectAssetsInBanners = getAssetsCombinations(grutTAssets)
        val expectGroupCount =
            (expectAssetsInBanners.size + MAX_COUNT_OF_BANNERS_IN_GROUP - 1) / MAX_COUNT_OF_BANNERS_IN_GROUP

        val soft = SoftAssertions()
        soft.assertThat(actualDirectBanners)
            .`as`("Количество баннеров в mysql")
            .hasSize(expectAssetsInBanners.size)
        soft.assertThat(actualDirectBanners.map { it.adGroupId }.toSet())
            .`as`("Список групп в баннерах и групповой заявке")
            .hasSize(expectGroupCount)
            .containsAll(actualAdGroupBriefGrutModel.adGroupIds)
        soft.assertAll()
    }

    /**
     * Проверяем созданные баннеры в груте
     */
    @ParameterizedTest(name = "titles: {0}, texts: {1}, images: {2}, videos: {3}, html5: {4}")
    @MethodSource("parametersForTextCampaign")
    fun testCreateBannersInGrut(
        titleAssetCount: Int,
        textAssetCount: Int,
        imageAssetCount: Int,
        withVideoAsset: Boolean,
        withHtml5Asset: Boolean,
    ) {
        setupTextCampaign()
        val adGroupBriefId = createAdGroupBriefGrutModel().id!!

        val (_, assets) = createAssets(
            titleAssetCount,
            textAssetCount,
            imageAssetCount,
            withVideoAsset,
            withHtml5Asset,
        )
        grutSteps.setCustomAssetLinksToAdGroupBrief(adGroupBriefId, assets)

        updateAdsJob.withShard(shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val actualDirectBanners = bannerTypedRepository.getBannersByCampaignIds(shard, listOf(directCampaignId))
        val actualGrutBanners = grutApiService.briefBannerGrutApi.getBanners(actualDirectBanners.map { it.id })

        assertThat(actualGrutBanners)
            .`as`("Количество баннеров в груте одинаково с mysql")
            .hasSize(actualDirectBanners.size)
    }
}
