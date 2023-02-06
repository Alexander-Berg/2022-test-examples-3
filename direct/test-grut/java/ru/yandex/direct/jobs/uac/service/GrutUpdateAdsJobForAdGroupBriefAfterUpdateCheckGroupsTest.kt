package ru.yandex.direct.jobs.uac.service

import kotlin.math.max
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Проверка работы джобы UpdateAdsJob при повторном вызове
 */
class GrutUpdateAdsJobForAdGroupBriefAfterUpdateCheckGroupsTest : GrutUpdateAdsJobForAdGroupBriefBaseTest() {

    /**
     * Проверяем добавление групп при повторной обработке джобой
     */
    @ParameterizedTest(name = "titles: {0}, texts: {1}, images: {2}, videos: {3}, html5: {4}")
    @MethodSource("parametersForTextCampaign")
    fun testCreateAdGroupsAfterChangeBrief(
        titleAssetCount: Int,
        textAssetCount: Int,
        imageAssetCount: Int,
        withVideoAsset: Boolean,
        withHtml5Asset: Boolean,
    ) {
        setupTextCampaign()
        val adGroupBriefId = createAdGroupBriefGrutModel().id!!

        val (grutTAssetsOld, grutTAssetsNew) = doubleRunJob(
            adGroupBriefId,
            titleAssetCount,
            textAssetCount,
            imageAssetCount,
            withVideoAsset,
            withHtml5Asset,
        )

        val expectAssetsInOldBanners = getAssetsCombinations(grutTAssetsOld)
        val expectAssetsInNewBanners = getAssetsCombinations(grutTAssetsNew)
        val maxBannerCounts = max(expectAssetsInOldBanners.size, expectAssetsInNewBanners.size)
        val expectGroupCount = (maxBannerCounts + MAX_COUNT_OF_BANNERS_IN_GROUP - 1) / MAX_COUNT_OF_BANNERS_IN_GROUP

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
}
