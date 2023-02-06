package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.grut.objects.proto.Banner

/**
 * Проверка работы джобы UpdateAdsJob при повторном вызове
 */
@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrutUpdateAdsJobForAdGroupBriefAfterUpdateCheckBanners2Test : GrutUpdateAdsJobForAdGroupBriefBaseTest() {

    /**
     * Проверяем созданные баннеры в груте при повторной обработке джобой
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

        doubleRunJob(
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
        val actualGrutBanners = grutApiService.briefBannerGrutApi.getBanners(actualDirectBanners.map { it.id })
            .filter { it.spec.status != Banner.TBannerSpec.EBannerStatus.BSS_DELETED }

        assertThat(actualGrutBanners)
            .`as`("Количество баннеров в груте одинаково с mysql")
            .hasSize(actualDirectBanners.size)
    }
}
