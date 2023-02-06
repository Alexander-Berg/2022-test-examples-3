package ru.yandex.direct.intapi.entity.bs.export

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.intapi.configuration.GrutIntApiTest
import ru.yandex.grut.objects.proto.Banner.TBannerSpec.EBannerStatus
import ru.yandex.grut.objects.proto.MediaType.EMediaType

@GrutIntApiTest
@RunWith(SpringJUnit4ClassRunner::class)
class GrutBsExportControllerGetCampaignsAssetHashesTest
    : GrutBsExportControllerGetCampaignsAssetHashesBaseTest() {

    @Before
    fun before() {
        super.init(false)
    }

    /**
     * Проверяем получение хешей ассетов по одной кампании
     */
    @Test
    fun test() {
        val campaignId = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val adGroupId = grutSteps.createAdGroup(campaignId)

        val imageContentId = createAsset(EMediaType.MT_IMAGE)
        val videoContentId = createAsset(EMediaType.MT_VIDEO)
        val textContentId = createAsset(EMediaType.MT_TEXT)
        val titleContentId = createAsset(EMediaType.MT_TITLE)
        val html5ContentId = createAsset(EMediaType.MT_HTML5)
        val directAdId = createBanner(campaignId, adGroupId,
            listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId))
        setAssetIdsToCampaign(campaignId,
            listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId))

        val result = performRequest(listOf(campaignId))

        val expected = mapOf(
            campaignId.toIdString() to mapOf(
                directAdId.toIdString() to mapOf(
                    "TitleAssetHash" to titleContentId,
                    "TextBodyAssetHash" to textContentId,
                    "ImageAssetHash" to imageContentId,
                    "VideoAssetHash" to videoContentId,
                    "Html5AssetHash" to html5ContentId,
                )
            )
        )

        Assertions.assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    @Test
    fun diffenentAssetIdAndAssetLinkIdTest() {
        val campaignId = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val adGroupId = grutSteps.createAdGroup(campaignId)

        val imageContentId = createAsset(EMediaType.MT_IMAGE)
        val videoContentId = createAsset(EMediaType.MT_VIDEO)
        val textContentId = createAsset(EMediaType.MT_TEXT)
        val titleContentId = createAsset(EMediaType.MT_TITLE)
        val html5ContentId = createAsset(EMediaType.MT_HTML5)
        val uacCampaignContents = listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId).map {
            createCampaignContent(contentId = it)
        }
        val directAdId = createBanner(campaignId, adGroupId,
            listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId),
            uacCampaignContents.map { it.id })
        grutSteps.setCustomAssetLinksToCampaign(campaignId, uacCampaignContents)

        val result = performRequest(listOf(campaignId))

        val assetIdToAssetLinkId = uacCampaignContents.associate { it.contentId!! to it.id }
        val expected = mapOf(
            campaignId.toIdString() to mapOf(
                directAdId.toIdString() to mapOf(
                    "TitleAssetHash" to assetIdToAssetLinkId[titleContentId],
                    "TextBodyAssetHash" to assetIdToAssetLinkId[textContentId],
                    "ImageAssetHash" to assetIdToAssetLinkId[imageContentId],
                    "VideoAssetHash" to assetIdToAssetLinkId[videoContentId],
                    "Html5AssetHash" to assetIdToAssetLinkId[html5ContentId],
                )
            )
        )

        Assertions.assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    @Test
    fun equalAssetIdAndAssetLinkIdTest() {
        val campaignId = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val adGroupId = grutSteps.createAdGroup(campaignId)

        val imageContentId = createAsset(EMediaType.MT_IMAGE)
        val videoContentId = createAsset(EMediaType.MT_VIDEO)
        val textContentId = createAsset(EMediaType.MT_TEXT)
        val titleContentId = createAsset(EMediaType.MT_TITLE)
        val html5ContentId = createAsset(EMediaType.MT_HTML5)
        val uacCampaignContents = listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId).map {
            createCampaignContent(contentId = it, id = it)
        }
        val directAdId = createBanner(campaignId, adGroupId,
            listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId),
            uacCampaignContents.map { it.id })
        grutSteps.setCustomAssetLinksToCampaign(campaignId, uacCampaignContents)

        val result = performRequest(listOf(campaignId))

        val expected = mapOf(
            campaignId.toIdString() to mapOf(
                directAdId.toIdString() to mapOf(
                    "TitleAssetHash" to titleContentId,
                    "TextBodyAssetHash" to textContentId,
                    "ImageAssetHash" to imageContentId,
                    "VideoAssetHash" to videoContentId,
                    "Html5AssetHash" to html5ContentId,
                )
            )
        )

        Assertions.assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    /**
     * Проверяем что ничего не получаем, если у кампании нет ассетов
     */
    @Test
    fun testWithoutAssets() {
        val directCampaignId = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        grutSteps.createAdGroup(directCampaignId)

        val result = performRequest(listOf(directCampaignId))

        val expected = mapOf<String, Any>()

        Assertions.assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    /**
     * Проверяем получение хешей ассетов для 2ух переданных кампаний
     */
    @Test
    fun testWithTwoCampaignsAndOneDeletedBanner() {
        val directCampaignIdA = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val directCampaignIdB = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val directAdGroupIdA = grutSteps.createAdGroup(directCampaignIdA)
        val directAdGroupIdB = grutSteps.createAdGroup(directCampaignIdB)

        val textContentIdA1 = createAsset(EMediaType.MT_TEXT)
        val textContentIdA2 = createAsset(EMediaType.MT_TEXT)
        val textContentIdB = createAsset(EMediaType.MT_TEXT)
        val textContentIdBDeleted = createAsset(EMediaType.MT_TEXT)
        val titleContentIdA1 = createAsset(EMediaType.MT_TITLE)
        val titleContentIdA2 = createAsset(EMediaType.MT_TITLE)
        val titleContentIdB = createAsset(EMediaType.MT_TITLE)
        val titleContentIdBDeleted = createAsset(EMediaType.MT_TITLE)

        val directAdIdA1 = createBanner(directCampaignIdA, directAdGroupIdA, listOf(textContentIdA1, titleContentIdA1))
        val directAdIdA2 = createBanner(directCampaignIdA, directAdGroupIdA, listOf(textContentIdA2, titleContentIdA2))
        val directAdIdB = createBanner(directCampaignIdB, directAdGroupIdB, listOf(textContentIdB, titleContentIdB))
        createBanner(directCampaignIdB, directAdGroupIdB, listOf(textContentIdBDeleted, titleContentIdBDeleted), status = EBannerStatus.BSS_DELETED)


        setAssetIdsToCampaign(directCampaignIdA, listOf(textContentIdA1, titleContentIdA1, textContentIdA2, titleContentIdA2))
        setAssetIdsToCampaign(directCampaignIdB, listOf(textContentIdB, titleContentIdB, textContentIdBDeleted, titleContentIdBDeleted))
        val result = performRequest(listOf(directCampaignIdA, directCampaignIdB))

        val expected = mapOf(
            directCampaignIdA.toString() to mapOf(
                directAdIdA1.toString() to mapOf(
                    "TitleAssetHash" to titleContentIdA1,
                    "TextBodyAssetHash" to textContentIdA1,
                ),
                directAdIdA2.toString() to mapOf(
                    "TitleAssetHash" to titleContentIdA2,
                    "TextBodyAssetHash" to textContentIdA2,
                )
            ),
            directCampaignIdB.toString() to mapOf(
                directAdIdB.toString() to mapOf(
                    "TitleAssetHash" to titleContentIdB,
                    "TextBodyAssetHash" to textContentIdB,
                ),
            )
        )

        Assertions.assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }
}
