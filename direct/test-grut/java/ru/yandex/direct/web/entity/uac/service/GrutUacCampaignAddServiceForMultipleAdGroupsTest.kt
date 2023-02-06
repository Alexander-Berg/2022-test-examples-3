package ru.yandex.direct.web.entity.uac.service

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.uac.model.CampaignStatuses
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.feature.FeatureName.UAC_MERGE_EQUALS_ASSETS_ENABLED
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.controller.UacCampaignRequestsCommon
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_HTML5
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_IMAGE
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_TEXT
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_TITLE
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_VIDEO

/**
 * Проверка склейки ассетов у нескольких групповых заявкок при создании кампании
 */
@GrutDirectWebTest
@RunWith(JUnitParamsRunner::class)
class GrutUacCampaignAddServiceForMultipleAdGroupsTest : GrutUacCampaignAddServiceBaseTest() {

    fun testCases(): List<List<Any?>> {
        return listOf(
            listOf(
                "С фичей склеивания",
                true,
            ),
            listOf(
                "Без фичи склеивания",
                false,
            ),
        )
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "testCases")
    fun `check for merging duplicate assets`(
        description: String,
        uacMergeEqualsAssetsEnabled: Boolean,
    ) {
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, UAC_MERGE_EQUALS_ASSETS_ENABLED, uacMergeEqualsAssetsEnabled)

        val titles = listOf("title1", "title2")
        val titlesForSecondGroup = listOf("title1")
        val texts = listOf("text1", "text2")
        val textsForSecondGroup = listOf("text1")
        val imageHashes = listOf("imagesHash1", "imagesHash2")
        val imageHashesForSecondGroup = listOf("imagesHash1")
        val videoSourceUrls = listOf("videoSourceUrl1", "videoSourceUrl2")
        val videoSourceUrlsForSecondGroup = listOf("videoSourceUrl1")
        val html5SourceUrls = listOf("html5SourceUrl1", "html5SourceUrl2")
        val html5SourceUrlsForSecondGroup = listOf("html5SourceUrl1")

        val mediaAssets = createMediaAssets(imageHashes, videoSourceUrls, html5SourceUrls)
        val mediaAssetsForSecondGroup = createMediaAssets(
            imageHashesForSecondGroup, videoSourceUrlsForSecondGroup, html5SourceUrlsForSecondGroup
        )
        val request = UacCampaignRequestsCommon.createCampaignRequest(
            uid = clientInfo.uid,
            titles = titles,
            texts = texts,
            contentIds = mediaAssets,
        )

        val contents = grutUacCampaignAddService.getContents(mediaAssets)
        val contentsForSecondGroup = grutUacCampaignAddService.getContents(mediaAssetsForSecondGroup)

        // Создаем контейнер с двумя групповыми заявками
        val createDataContainer = uacModifyCampaignDataContainerFactory.dataContainerFromCreateRequest(
            directCampaign.id.toIdString(),
            request.toInternal(null, null, null),
            null, null, null, false,
        ).copy(
            adGroupBriefRequests = listOf(
                UacCampaignRequestsCommon.createAdGroupBriefRequest(
                    titles = titles,
                    texts = texts,
                    contentIds = contents.map { it.id },
                    sitelinks = request.sitelinks,
                ),
                UacCampaignRequestsCommon.createAdGroupBriefRequest(
                    titles = titlesForSecondGroup,
                    texts = textsForSecondGroup,
                    contentIds = contentsForSecondGroup.map { it.id },
                ),
            )
        )
        grutUacCampaignAddService.saveBrief(
            directCampaign.id,
            operator,
            subjectUser,
            createDataContainer,
            contents + contentsForSecondGroup,
            CampaignStatuses(Status.DRAFT, TargetStatus.STOPPED),
        )

        // Получаем заявку на кампании и список id ассетов на ней
        val grutCampaign = grutApiService.briefGrutApi.getBrief(directCampaign.id)
        val assetIdsInCampaign = grutCampaign!!.spec.briefAssetLinks.linksList
            .map { it.id }
        val grutAssetsInCampaign = grutApiService.assetGrutApi.getAssets(assetIdsInCampaign)
        val campaignBriefTitles = grutAssetsInCampaign
            .filter { it.meta.mediaType == MT_TITLE }
            .map { it.spec.title }
        val campaignBriefText = grutAssetsInCampaign
            .filter { it.meta.mediaType == MT_TEXT }
            .map { it.spec.text }
        val campaignBriefImageHashes = grutAssetsInCampaign
            .filter { it.meta.mediaType == MT_IMAGE }
            .map { it.spec.image.directImageHash }
        val campaignBriefVideoUrls = grutAssetsInCampaign
            .filter { it.meta.mediaType == MT_VIDEO }
            .map { it.spec.video.mdsInfo.sourceUrl }
        val campaignBriefHtml5Urls = grutAssetsInCampaign
            .filter { it.meta.mediaType == MT_HTML5 }
            .map { it.spec.html5.mdsInfo.sourceUrl }

        // Получаем групповые заявки и список id ассетов на ней
        val uacAdGroupBriefs = grutApiService.adGroupBriefGrutApi.selectAdGroupBriefsByCampaignId(directCampaign.id)
        val adGroupBriefIdToAssets = uacAdGroupBriefs
            .associateBy({ it.id!! }) { it.assetLinks }
        val adGroupBriefAssetIds = adGroupBriefIdToAssets.values
            .filterNotNull()
            .flatten()
            .map { it.id.toIdLong() }

        val soft = SoftAssertions()
        soft.assertThat(uacAdGroupBriefs)
            .`as`("Есть две групповых заявки")
            .hasSize(2)
        val assetsCountInAdgroups = uacAdGroupBriefs
            .map { it.assetLinks?.size ?: 0 }
        soft.assertThat(assetsCountInAdgroups)
            .`as`("Количество ассетов по группам (по 2 в первой + sitelink, по 1му во второй + sitelink)")
            .isEqualTo(listOf(11, 6))

        // Получаем уникальные id ассетов, прикрепленным к групповым заявкам
        val grutAssetsInAdGroupBriefs = grutApiService.assetGrutApi.getAssets(adGroupBriefAssetIds)
        val typeToAdGroupBriefIds = grutAssetsInAdGroupBriefs
            .groupBy { it.meta.mediaType }
            .mapValues { it.value.map { grutAsset -> grutAsset.meta.id }.toSet() }

        val expectUniqAssetsCountInType = if (uacMergeEqualsAssetsEnabled) 2 else 3
        soft.assertThat(typeToAdGroupBriefIds[MT_TITLE])
            .`as`("Количество уникальных title ассетов")
            .hasSize(expectUniqAssetsCountInType)
        soft.assertThat(typeToAdGroupBriefIds[MT_TEXT])
            .`as`("Количество уникальных text ассетов")
            .hasSize(expectUniqAssetsCountInType)
        soft.assertThat(typeToAdGroupBriefIds[MT_IMAGE])
            .`as`("Количество уникальных image ассетов")
            .hasSize(expectUniqAssetsCountInType)
        soft.assertThat(typeToAdGroupBriefIds[MT_VIDEO])
            .`as`("Количество уникальных video ассетов")
            .hasSize(expectUniqAssetsCountInType)
        soft.assertThat(typeToAdGroupBriefIds[MT_HTML5])
            .`as`("Количество уникальных html5 ассетов")
            .hasSize(expectUniqAssetsCountInType)

        val expectImageHashes: List<String> = imageHashes + (
            if (uacMergeEqualsAssetsEnabled) listOf()
            else imageHashesForSecondGroup)
        val expectVideoUrls: List<String> = videoSourceUrls + (
            if (uacMergeEqualsAssetsEnabled) listOf()
            else videoSourceUrlsForSecondGroup)
        val expectHtml5SourceUrls: List<String> = html5SourceUrls + (
            if (uacMergeEqualsAssetsEnabled) listOf()
            else html5SourceUrlsForSecondGroup)

        // Проверяем что прикрепленные ассеты к групповой заявке те же что и на кампании
        // Для кампании ассеты берутся от первой групповой заявки (которая с полным набором)
        soft.assertThat(campaignBriefTitles)
            .`as`("Title ассеты на кампании и группе")
            .hasSize(titles.size)
            .containsAll(titles)
        soft.assertThat(campaignBriefText)
            .`as`("Text ассеты на кампании и группе")
            .hasSize(texts.size)
            .containsAll(texts)
        soft.assertThat(campaignBriefImageHashes)
            .`as`("Image ассеты на кампании и группе")
            .hasSize(expectImageHashes.size)
            .containsAll(expectImageHashes)
        soft.assertThat(campaignBriefVideoUrls)
            .`as`("Video ассеты на кампании и группе")
            .hasSize(expectVideoUrls.size)
            .containsAll(expectVideoUrls)
        soft.assertThat(campaignBriefHtml5Urls)
            .`as`("Html5 ассеты на кампании и группе")
            .hasSize(expectHtml5SourceUrls.size)
            .containsAll(expectHtml5SourceUrls)

        soft.assertAll()
    }
}
