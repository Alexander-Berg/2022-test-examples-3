package ru.yandex.direct.web.entity.uac.service

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.uac.model.CampaignStatuses
import ru.yandex.direct.core.entity.uac.model.Content
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.feature.FeatureName.UAC_MERGE_EQUALS_ASSETS_ENABLED
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.controller.UacCampaignRequestsCommon
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_HTML5
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_IMAGE
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_SITELINK
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_TEXT
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_TITLE
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_VIDEO
import ru.yandex.grut.objects.proto.client.Schema

/**
 * Проверка удаления одинаковых ассетов в одной групповой заявке (и заявке на кампанию) при ее создании
 */
@GrutDirectWebTest
@RunWith(JUnitParamsRunner::class)
class GrutUacCampaignAddServiceTest : GrutUacCampaignAddServiceBaseTest() {

    fun testCases(): List<List<Any?>> {
        return listOf(
            listOf(
                "Заявка на группу со всеми ассетами по одному, без фичи склеивания",
                false,
                listOf("title"),
                listOf("text"),
                listOf("imageHash"),
                listOf("sourceUrl"),
                listOf("sourceUrl")
            ),
            listOf(
                "Заявка на группу со всеми ассетами по два, без фичи склеивания",
                false,
                listOf("title1", "title2"),
                listOf("text1", "text2"),
                listOf("imageHash1", "imageHash2"),
                listOf("sourceUrl1", "sourceUrl2"),
                listOf("sourceUrl1", "sourceUrl2")
            ),
            listOf(
                "Заявка на группу со всеми ассетами по два повторяющихся, без фичи склеивания",
                false,
                listOf("title", "title"),
                listOf("text", "text"),
                listOf("imageHash", "imageHash"),
                listOf("sourceUrl", "sourceUrl"),
                listOf("sourceUrl", "sourceUrl")
            ),
            listOf(
                "Заявка на группу со всеми ассетами по одному, с фичей склеивания",
                true,
                listOf("title"),
                listOf("text"),
                listOf("imageHash"),
                listOf("sourceUrl"),
                listOf("sourceUrl")
            ),
            listOf(
                "Заявка на группу со всеми ассетами по два, с фичей склеивания",
                true,
                listOf("title1", "title2"),
                listOf("text1", "text2"),
                listOf("imageHash1", "imageHash2"),
                listOf("sourceUrl1", "sourceUrl2"),
                listOf("sourceUrl1", "sourceUrl2")
            ),
            listOf(
                "Заявка на группу со всеми ассетами по два повторяющихся, с фичей склеивания",
                true,
                listOf("title", "title"),
                listOf("text", "text"),
                listOf("imageHash", "imageHash"),
                listOf("sourceUrl", "sourceUrl"),
                listOf("sourceUrl", "sourceUrl")
            ),
        )
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "testCases")
    fun `checking for removal of duplicate assets`(
        description: String,
        uacMergeEqualsAssetsEnabled: Boolean,
        titles: List<String>,
        texts: List<String>,
        imagesHash: List<String>,
        videoSourceUrl: List<String>,
        html5SourceUrl: List<String>,
    ) {
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, UAC_MERGE_EQUALS_ASSETS_ENABLED, uacMergeEqualsAssetsEnabled)

        val request = UacCampaignRequestsCommon.createCampaignRequest(
            uid = clientInfo.uid,
            titles = titles,
            texts = texts,
            contentIds = createMediaAssets(imagesHash, videoSourceUrl, html5SourceUrl),
        )

        val contents = grutUacCampaignAddService.getContents(request.contentIds ?: emptyList())

        val createDataContainer = uacModifyCampaignDataContainerFactory.dataContainerFromCreateRequest(
            directCampaign.id.toIdString(),
            request.toInternal(null, null, null),
            null, null, null, false,
            adGroupBriefRequests = listOf(
                UacCampaignRequestsCommon.createAdGroupBriefRequest(
                    titles = titles,
                    texts = texts,
                    contentIds = contents.map { it.id },
                    sitelinks = request.sitelinks,
                )
            ),
        )
        grutUacCampaignAddService.saveBrief(
            directCampaign.id,
            operator,
            subjectUser,
            createDataContainer,
            contents,
            CampaignStatuses(Status.DRAFT, TargetStatus.STOPPED),
        )

        // Получаем заявку на кампании и список id ассетов на ней
        val grutCampaign = grutApiService.briefGrutApi.getBrief(directCampaign.id)
        val assetIdsInCampaign = grutCampaign!!.spec.briefAssetLinks.linksList
            .map { it.id }
        val grutAssetsInCampaign = grutApiService.assetGrutApi.getAssets(assetIdsInCampaign)

        // Получаем данные по ассетам прикрепленным к заявке кампании
        val (
            titleAssetIdToTitleInCampaign, textAssetIdToTextInCampaign, imageAssetIdToDataInCampaign,
            videoAssetIdToDataInCampaign, html5AssetIdToDataInCampaign, sitelinkAssetIdToDataInCampaign,
        ) = getAssetsDataForCheck(grutAssetsInCampaign)

        // Получаем групповую заявку и список id ассетов на ней
        val uacAdGroupBriefs = grutApiService.adGroupBriefGrutApi.selectAdGroupBriefsByCampaignId(directCampaign.id)
        val uacAssetIdsInAdGroup = uacAdGroupBriefs
            .mapNotNull { it.assetLinks }
            .flatten()
            .map { it.id.toIdLong() }


        // Получаем данные по ассетам прикрепленным к групповым заявкам
        val grutAssetsInAdGroupBrief = grutApiService.assetGrutApi.getAssets(uacAssetIdsInAdGroup)
        val (
            titleAssetIdToTitleInAdGroup, textAssetIdToTextInAdGroup, imageAssetIdToDataInAdGroup,
            videoAssetIdToDataInAdGroup, html5AssetIdToDataInAdGroup, sitelinkAssetIdToDataInAdGroup,
        ) = getAssetsDataForCheck(grutAssetsInAdGroupBrief)

        val soft = SoftAssertions()
        soft.assertThat(uacAdGroupBriefs)
            .`as`("Есть одна групповая заявка")
            .hasSize(1)

        // Проверяем схожесть прикрепленных ассетов к групповой заявке и на кампании
        soft.assertThat(titleAssetIdToTitleInCampaign == titleAssetIdToTitleInAdGroup)
            .`as`("Title ассеты на кампании и группе")
            .isEqualTo(uacMergeEqualsAssetsEnabled)
        soft.assertThat(textAssetIdToTextInCampaign == textAssetIdToTextInAdGroup)
            .`as`("Text ассеты на кампании и группе")
            .isEqualTo(uacMergeEqualsAssetsEnabled)
        soft.assertThat(imageAssetIdToDataInCampaign)
            .`as`("Image ассеты на кампании и группе")
            .isEqualTo(imageAssetIdToDataInAdGroup)
        soft.assertThat(videoAssetIdToDataInCampaign)
            .`as`("Video ассеты на кампании и группе")
            .isEqualTo(videoAssetIdToDataInAdGroup)
        soft.assertThat(html5AssetIdToDataInCampaign)
            .`as`("Html5 ассеты на кампании и группе")
            .isEqualTo(html5AssetIdToDataInAdGroup)
        soft.assertThat(sitelinkAssetIdToDataInCampaign == sitelinkAssetIdToDataInAdGroup)
            .`as`("Sitelinks ассеты на кампании и группе")
            .isEqualTo(uacMergeEqualsAssetsEnabled)

        val expectTitles = if (uacMergeEqualsAssetsEnabled) titles.distinct() else titles
        val expectTexts = if (uacMergeEqualsAssetsEnabled) texts.distinct() else texts
        var expectImageImageHashes = contents
            .filter { it.type == MediaType.IMAGE }
            .map { it.directImageHash }
        expectImageImageHashes =
            if (uacMergeEqualsAssetsEnabled) expectImageImageHashes.distinct() else expectImageImageHashes
        var expectVideoSourceUrl = contents
            .filter { it.type == MediaType.VIDEO }
            .map { it.sourceUrl }
        expectVideoSourceUrl =
            if (uacMergeEqualsAssetsEnabled) expectVideoSourceUrl.distinct() else expectVideoSourceUrl
        var expectHtml5SourceUrl = contents
            .filter { it.type == MediaType.HTML5 }
            .map { it.sourceUrl }
        expectHtml5SourceUrl =
            if (uacMergeEqualsAssetsEnabled) expectHtml5SourceUrl.distinct() else expectHtml5SourceUrl

        // Проверяем ассеты на заявке кампании
        soft.assertThat(titleAssetIdToTitleInCampaign)
            .`as`("Количество title ассетов")
            .hasSize(expectTitles.size)
        soft.assertThat(titleAssetIdToTitleInCampaign.values.map { it.toString() })
            .`as`("Title ассеты")
            .containsAll(expectTitles)

        soft.assertThat(textAssetIdToTextInCampaign)
            .`as`("Количество text ассетов")
            .hasSize(expectTexts.size)
        soft.assertThat(textAssetIdToTextInCampaign.values.map { it.toString() })
            .`as`("Text ассеты")
            .containsAll(expectTexts)

        soft.assertThat(imageAssetIdToDataInCampaign)
            .`as`("Количество image ассетов")
            .hasSize(expectImageImageHashes.size)
        soft.assertThat(imageAssetIdToDataInCampaign.values.map { (it as Pair<*, *>).first })
            .`as`("Image ассеты")
            .containsAll(expectImageImageHashes)

        soft.assertThat(videoAssetIdToDataInCampaign)
            .`as`("Количество video ассетов")
            .hasSize(expectVideoSourceUrl.size)
        soft.assertThat(videoAssetIdToDataInCampaign.values.map { (it as Pair<*, *>).first })
            .`as`("Video ассеты")
            .containsAll(expectVideoSourceUrl)

        soft.assertThat(html5AssetIdToDataInCampaign)
            .`as`("Количество html5 ассетов")
            .hasSize(expectHtml5SourceUrl.size)
        soft.assertThat(html5AssetIdToDataInCampaign.values.map { (it as Pair<*, *>).first })
            .`as`("Html5 ассеты")
            .containsAll(expectHtml5SourceUrl)

        soft.assertAll()
    }

    /**
     * Проверка склеивания images по разным полям одного ассета
     */
    @Test
    fun `checking for removal of duplicate images by different fields`() {
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, UAC_MERGE_EQUALS_ASSETS_ENABLED, true)

        val imageData1 = Pair("imageHash1", "sourceUrl1")
        val imageData2 = Pair("imageHash2", "sourceUrl2")
        val imageData3 = Pair(imageData1.first, "sourceUrl3")
        val imageData4 = Pair("imageHash4", imageData2.second)

        val imageAsset1 = createDefaultImageAsset(imageData1)
        val imageAsset2 = createDefaultImageAsset(imageData2)
        val imageAsset3 = createDefaultImageAsset(imageData3)
        val imageAsset4 = createDefaultImageAsset(imageData4)

        val request = UacCampaignRequestsCommon.createCampaignRequest(
            uid = clientInfo.uid,
            titles = listOf("title"),
            texts = listOf("text"),
            contentIds = listOf(imageAsset1, imageAsset2, imageAsset3, imageAsset4)
        )
        val contents = grutUacCampaignAddService.getContents(request.contentIds ?: emptyList())

        val grutAssetsInCampaign = runAndGetAssetsInCampaign(request, contents)

        // Получаем данные по ассетам прикрепленным к заявке кампании
        val (_, _, imageAssetIdToDataInCampaign) = getAssetsDataForCheck(grutAssetsInCampaign)

        // Получаем групповую заявку и список id ассетов на ней
        val uacAdGroupBriefs = grutApiService.adGroupBriefGrutApi.selectAdGroupBriefsByCampaignId(directCampaign.id)
        val uacAssetIdsInAdGroup = uacAdGroupBriefs
            .mapNotNull { it.assetLinks }
            .flatten()
            .map { it.id.toIdLong() }

        // Получаем данные по ассетам прикрепленным к групповым заявкам
        val grutAssetsInAdGroupBrief = grutApiService.assetGrutApi.getAssets(uacAssetIdsInAdGroup)
        val (_, _, imageAssetIdToDataInAdGroup) = getAssetsDataForCheck(grutAssetsInAdGroupBrief)

        val soft = SoftAssertions()
        soft.assertThat(uacAdGroupBriefs)
            .`as`("Есть одна групповая заявка")
            .hasSize(1)

        // Проверяем что прикрепленные ассеты к групповой заявке те же что и на кампании
        soft.assertThat(imageAssetIdToDataInCampaign)
            .`as`("Image ассеты на кампании и группе")
            .isEqualTo(imageAssetIdToDataInAdGroup)

        val cassetIdToContent = contents
            .associateBy { it.id }
        val expectImageData: Map<Long, Pair<String, String>> = mapOf(
            cassetIdToContent[imageAsset1]!!.id.toIdLong() to imageData1,
            cassetIdToContent[imageAsset4]!!.id.toIdLong() to imageData4,
        )

        soft.assertThat(imageAssetIdToDataInCampaign)
            .`as`("Image ассеты")
            .isEqualTo(expectImageData)
        soft.assertAll()
    }

    /**
     * Проверка склеивания video по разным полям одного ассета
     */
    @Test
    fun `checking for removal of duplicate videos by different fields`() {
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, UAC_MERGE_EQUALS_ASSETS_ENABLED, true)

        val videoData1 = Pair("sourceUrl1", "mdsUrl1")
        val videoData2 = Pair("sourceUrl2", "mdsUrl2")
        val videoData3 = Pair(videoData1.first, "mdsUrl3")
        val videoData4 = Pair("sourceUrl4", videoData2.second)

        val videoAsset1 = createDefaultVideoAsset(videoData1)
        val videoAsset2 = createDefaultVideoAsset(videoData2)
        val videoAsset3 = createDefaultVideoAsset(videoData3)
        val videoAsset4 = createDefaultVideoAsset(videoData4)

        val request = UacCampaignRequestsCommon.createCampaignRequest(
            uid = clientInfo.uid,
            titles = listOf("title"),
            texts = listOf("text"),
            contentIds = listOf(videoAsset1, videoAsset2, videoAsset3, videoAsset4)
        )
        val contents = grutUacCampaignAddService.getContents(request.contentIds ?: emptyList())

        val grutAssetsInCampaign = runAndGetAssetsInCampaign(request, contents)

        // Получаем данные по ассетам прикрепленным к заявке кампании
        val (_, _, _, videoAssetIdToDataInCampaign) = getAssetsDataForCheck(grutAssetsInCampaign)

        // Получаем групповую заявку и список id ассетов на ней
        val uacAdGroupBriefs = grutApiService.adGroupBriefGrutApi.selectAdGroupBriefsByCampaignId(directCampaign.id)
        val uacAssetIdsInAdGroup = uacAdGroupBriefs
            .mapNotNull { it.assetLinks }
            .flatten()
            .map { it.id.toIdLong() }

        // Получаем данные по ассетам прикрепленным к групповым заявкам
        val grutAssetsInAdGroupBrief = grutApiService.assetGrutApi.getAssets(uacAssetIdsInAdGroup)
        val (_, _, _, videoAssetIdToDataInAdGroup) = getAssetsDataForCheck(grutAssetsInAdGroupBrief)

        val soft = SoftAssertions()
        soft.assertThat(uacAdGroupBriefs)
            .`as`("Есть одна групповая заявка")
            .hasSize(1)

        // Проверяем что прикрепленные ассеты к групповой заявке те же что и на кампании
        soft.assertThat(videoAssetIdToDataInCampaign)
            .`as`("Video ассеты на кампании и группе")
            .isEqualTo(videoAssetIdToDataInAdGroup)

        val cassetIdToContent = contents
            .associateBy { it.id }
        val expectVideoData: Map<Long, Pair<String, String>> = mapOf(
            cassetIdToContent[videoAsset3]!!.id.toIdLong() to videoData3,
            cassetIdToContent[videoAsset2]!!.id.toIdLong() to videoData2,
        )

        soft.assertThat(videoAssetIdToDataInCampaign)
            .`as`("Video ассеты")
            .isEqualTo(expectVideoData)
        soft.assertAll()
    }

    /**
     * Проверка склеивания html5 по разным полям одного ассета
     */
    @Test
    fun `checking for removal of duplicate html5 by different fields`() {
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, UAC_MERGE_EQUALS_ASSETS_ENABLED, true)

        val html5Data1 = Pair("sourceUrl1", "mdsUrl1")
        val html5Data2 = Pair("sourceUrl2", "mdsUrl2")
        val html5Data3 = Pair(html5Data1.first, "mdsUrl3")
        val html5Data4 = Pair("sourceUrl4", html5Data2.second)

        val html5Asset1 = createDefaultHtml5Asset(html5Data1)
        val html5Asset2 = createDefaultHtml5Asset(html5Data2)
        val html5Asset3 = createDefaultHtml5Asset(html5Data3)
        val html5Asset4 = createDefaultHtml5Asset(html5Data4)

        val request = UacCampaignRequestsCommon.createCampaignRequest(
            uid = clientInfo.uid,
            titles = listOf("title"),
            texts = listOf("text"),
            contentIds = listOf(html5Asset1, html5Asset2, html5Asset3, html5Asset4)
        )
        val contents = grutUacCampaignAddService.getContents(request.contentIds ?: emptyList())

        val grutAssetsInCampaign = runAndGetAssetsInCampaign(request, contents)

        // Получаем данные по ассетам прикрепленным к заявке кампании
        val (_, _, _, _, html5AssetIdToDataInCampaign) = getAssetsDataForCheck(grutAssetsInCampaign)

        // Получаем групповую заявку и список id ассетов на ней
        val uacAdGroupBriefs = grutApiService.adGroupBriefGrutApi.selectAdGroupBriefsByCampaignId(directCampaign.id)
        val uacAssetIdsInAdGroup = uacAdGroupBriefs
            .mapNotNull { it.assetLinks }
            .flatten()
            .map { it.id.toIdLong() }

        // Получаем данные по ассетам прикрепленным к групповым заявкам
        val grutAssetsInAdGroupBrief = grutApiService.assetGrutApi.getAssets(uacAssetIdsInAdGroup)
        val (_, _, _, _, html5AssetIdToDataInAdGroup) = getAssetsDataForCheck(grutAssetsInAdGroupBrief)

        val soft = SoftAssertions()
        soft.assertThat(uacAdGroupBriefs)
            .`as`("Есть одна групповая заявка")
            .hasSize(1)

        // Проверяем что прикрепленные ассеты к групповой заявке те же что и на кампании
        soft.assertThat(html5AssetIdToDataInCampaign)
            .`as`("Html5 ассеты на кампании и группе")
            .isEqualTo(html5AssetIdToDataInAdGroup)

        val assetIdToContent = contents
            .associateBy { it.id }
        val expectHtml5Data: Map<Long, Pair<String, String>> = mapOf(
            assetIdToContent[html5Asset3]!!.id.toIdLong() to html5Data3,
            assetIdToContent[html5Asset2]!!.id.toIdLong() to html5Data2,
        )

        soft.assertThat(html5AssetIdToDataInCampaign)
            .`as`("Html5 ассеты")
            .isEqualTo(expectHtml5Data)
        soft.assertAll()
    }

    private fun runAndGetAssetsInCampaign(
        request: CreateCampaignRequest,
        contents: List<Content>,
    ): List<Schema.TAsset> {
        val createDataContainer = uacModifyCampaignDataContainerFactory.dataContainerFromCreateRequest(
            directCampaign.id.toIdString(),
            request.toInternal(null, null, null),
            null, null, null, false,
            adGroupBriefRequests = listOf(
                UacCampaignRequestsCommon.createAdGroupBriefRequest(
                    titles = request.titles!!,
                    texts = request.texts!!,
                    contentIds = contents.map { it.id },
                )
            ),
        )
        grutUacCampaignAddService.saveBrief(
            directCampaign.id,
            operator,
            subjectUser,
            createDataContainer,
            contents,
            CampaignStatuses(Status.DRAFT, TargetStatus.STOPPED),
        )

        // Получаем заявку на кампании и список id ассетов на ней
        val grutCampaign = grutApiService.briefGrutApi.getBrief(directCampaign.id)
        val assetIdsInCampaign = grutCampaign!!.spec.briefAssetLinks.linksList
            .map { it.id }
        return grutApiService.assetGrutApi.getAssets(assetIdsInCampaign)
    }

    private fun getAssetsDataForCheck(grutAssets: List<Schema.TAsset>): Array<Map<Long, Any>> {
        val actualTitleAssetIdToTitle = mutableMapOf<Long, String>()
        val actualTextAssetIdToText = mutableMapOf<Long, String>()
        val actualImageAssetIdToData = mutableMapOf<Long, Pair<String, String>>()
        val actualVideoAssetIdToData = mutableMapOf<Long, Pair<String, String>>()
        val actualHtml5AssetIdToData = mutableMapOf<Long, Pair<String, String>>()
        val actualSitelinkAssetIdToData = mutableMapOf<Long, String>()
        grutAssets
            .forEach {
                when (it.meta.mediaType) {
                    MT_TITLE -> actualTitleAssetIdToTitle[it.meta.id] = it.spec.title
                    MT_TEXT -> actualTextAssetIdToText[it.meta.id] = it.spec.text
                    MT_IMAGE -> actualImageAssetIdToData[it.meta.id] =
                        Pair(it.spec.image.directImageHash, it.spec.image.mdsInfo.sourceUrl)
                    MT_VIDEO -> actualVideoAssetIdToData[it.meta.id] =
                        Pair(it.spec.video.mdsInfo.sourceUrl, it.spec.video.mdsInfo.mdsUrl)
                    MT_HTML5 -> actualHtml5AssetIdToData[it.meta.id] =
                        Pair(it.spec.html5.mdsInfo.sourceUrl, it.spec.html5.mdsInfo.mdsUrl)
                    MT_SITELINK -> actualSitelinkAssetIdToData[it.meta.id] = it.spec.sitelink.href
                    else -> throw IllegalStateException("Unsupported type: ${it.meta.mediaType}")
                }
            }
        return arrayOf(
            actualTitleAssetIdToTitle, actualTextAssetIdToText, actualImageAssetIdToData,
            actualVideoAssetIdToData, actualHtml5AssetIdToData, actualSitelinkAssetIdToData
        )
    }

    private fun createDefaultImageAsset(
        imageHashAndSourceUrls: Pair<String, String>
    ) = grutSteps.createDefaultImageAsset(
        clientId = subjectUser.clientId,
        imageHash = imageHashAndSourceUrls.first,
        sourceUrl = imageHashAndSourceUrls.second,
    )

    private fun createDefaultVideoAsset(
        sourceAndMdsUrls: Pair<String, String>
    ) = grutSteps.createDefaultVideoAsset(
        clientId = subjectUser.clientId,
        sourceUrl = sourceAndMdsUrls.first,
        mdsUrl = sourceAndMdsUrls.second,
    )

    private fun createDefaultHtml5Asset(
        sourceAndMdsUrls: Pair<String, String>
    ) = grutSteps.createDefaultHtml5Asset(
        clientId = subjectUser.clientId,
        sourceUrl = sourceAndMdsUrls.first,
        mdsUrl = sourceAndMdsUrls.second,
    )

}
