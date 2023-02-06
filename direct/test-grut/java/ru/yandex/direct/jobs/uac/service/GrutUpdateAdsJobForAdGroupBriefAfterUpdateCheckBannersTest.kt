package ru.yandex.direct.jobs.uac.service

import kotlin.math.max
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.jobs.configuration.GrutJobsTest

/**
 * Проверка работы джобы UpdateAdsJob при повторном вызове
 */
@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrutUpdateAdsJobForAdGroupBriefAfterUpdateCheckBannersTest : GrutUpdateAdsJobForAdGroupBriefBaseTest() {

    /**
     * Проверяем добавление баннеров при повторной обработке джобой
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

        val actualAdGroupBriefGrutModel = grutApiService.adGroupBriefGrutApi.getAdGroupBrief(adGroupBriefId)!!
        val actualDirectBanners = bannerTypedRepository.getBannersByCampaignIds(shard, listOf(directCampaignId))
            .filterIsInstance<TextBanner>()
            .filter { !it.statusArchived }

        val soft = SoftAssertions()
        soft.assertThat(actualDirectBanners)
            .`as`("Количество баннеров в mysql")
            .hasSize(expectAssetsInNewBanners.size)
        soft.assertThat(actualDirectBanners.map { it.adGroupId }.toSet())
            .`as`("Список групп в баннерах и групповой заявке")
            .hasSize(expectGroupCount)
            .containsAll(actualAdGroupBriefGrutModel.adGroupIds)
        soft.assertAll()
    }
}
