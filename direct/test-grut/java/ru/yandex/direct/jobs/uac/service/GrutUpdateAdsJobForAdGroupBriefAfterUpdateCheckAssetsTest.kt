package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.Assertions.assertThat
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
class GrutUpdateAdsJobForAdGroupBriefAfterUpdateCheckAssetsTest : GrutUpdateAdsJobForAdGroupBriefBaseTest() {

    /**
     * Проверяем ассеты в баннерах при повторной обработке джобой
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

        val (_, grutTAssetsNew) = doubleRunJob(
            adGroupBriefId,
            titleAssetCount,
            textAssetCount,
            imageAssetCount,
            withVideoAsset,
            withHtml5Asset,
        )

        val actualDirectBanners = bannerTypedRepository.getBannersByCampaignIds(shard, listOf(directCampaignId))
            .filterIsInstance<TextBanner>()
            .filter { !it.statusArchived }
        val actualAssetsInBanners = actualDirectBanners
            .map { BannerAssets(it.title, it.body, it.imageHash, it.creativeId) }

        val expectAssetsInBanners = getAssetsCombinations(grutTAssetsNew)

        assertThat(actualAssetsInBanners)
            .`as`("Список ассетов в баннерах")
            .hasSize(expectAssetsInBanners.size)
            .containsAll(expectAssetsInBanners)
    }
}
