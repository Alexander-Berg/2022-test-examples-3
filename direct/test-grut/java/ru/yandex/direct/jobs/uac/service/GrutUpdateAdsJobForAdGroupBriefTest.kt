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
class GrutUpdateAdsJobForAdGroupBriefTest : GrutUpdateAdsJobForAdGroupBriefBaseTest() {

    /**
     * Проверяем добавление групп к групповой заявке
     */
    @ParameterizedTest(name = "titles: {0}, texts: {1}, images: {2}, videos: {3}, html5: {4}")
    @MethodSource("parametersForTextCampaign")
    fun testCreateAdGroups(
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

        val expectAssetsInBanners = getAssetsCombinations(grutTAssets)
        val expectGroupCount =
            (expectAssetsInBanners.size + MAX_COUNT_OF_BANNERS_IN_GROUP - 1) / MAX_COUNT_OF_BANNERS_IN_GROUP

        val actualDirectAdGroupIds =
            adGroupService.getAdGroupIdsByCampaignIds(setOf(directCampaignId))[directCampaignId]
                ?.toSet() ?: emptySet()
        val actualAdGroupBriefGrutModel = grutApiService.adGroupBriefGrutApi.getAdGroupBrief(adGroupBriefId)

        val soft = SoftAssertions()
        soft.assertThat(actualDirectAdGroupIds)
            .`as`("Количество групп в mysql")
            .hasSize(expectGroupCount)
        soft.assertThat(actualAdGroupBriefGrutModel!!.adGroupIds)
            .`as`("Список id групп в групповой заявке")
            .hasSize(expectGroupCount)
            .containsAll(actualDirectAdGroupIds)
        soft.assertAll()
    }

    /**
     * Проверяем ассеты в баннерах
     */
    @ParameterizedTest(name = "titles: {0}, texts: {1}, images: {2}, videos: {3}, html5: {4}")
    @MethodSource("parametersForTextCampaign")
    fun testCreateAssetsInBanners(
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

        val actualDirectBanners = bannerTypedRepository.getBannersByCampaignIds(shard, listOf(directCampaignId))
            .filterIsInstance<TextBanner>()
        val actualAssetsInBanners = actualDirectBanners
            .map { BannerAssets(it.title, it.body, it.imageHash, it.creativeId) }

        val expectAssetsInBanners = getAssetsCombinations(grutTAssets)

        assertThat(actualAssetsInBanners)
            .`as`("Список ассетов в баннерах")
            .hasSize(expectAssetsInBanners.size)
            .containsAll(expectAssetsInBanners)
    }
}
