package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.jobs.configuration.GrutJobsTest

@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrutUpdateAdsJobForAdGroupBriefKeywordsTest : GrutUpdateAdsJobForAdGroupBriefBaseTest() {

    /**
     * Проверяем ключевые фразы
     */
    @ParameterizedTest(name = "titles: {0}, texts: {1}, images: {2}, videos: {3}, html5: {4}")
    @MethodSource("parametersForTextCampaign")
    fun testCreateKeywords(
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

        val actualAdGroupBriefGrutModel = grutApiService.adGroupBriefGrutApi.getAdGroupBrief(adGroupBriefId)!!

        val keywordPhrasesByAdGroupId = keywordRepository
            .getKeywordsByAdGroupIds(shard, actualAdGroupBriefGrutModel.adGroupIds!!)
            .mapValues {
                it.value.map { keyword -> keyword.phrase }.toSet()
            }
        val expectKeywordPhrasesByAdGroupId = actualAdGroupBriefGrutModel.adGroupIds!!
            .associateBy({ it }) { EXPECTED_KEYWORDS_FOR_ADGROUP }

        assertThat(keywordPhrasesByAdGroupId)
            .`as`("Фразы в группах")
            .isEqualTo(expectKeywordPhrasesByAdGroupId)
    }
}
