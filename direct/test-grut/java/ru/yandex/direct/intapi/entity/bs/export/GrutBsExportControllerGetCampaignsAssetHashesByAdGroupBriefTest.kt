package ru.yandex.direct.intapi.entity.bs.export

import org.assertj.core.api.Assertions.assertThat
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
class GrutBsExportControllerGetCampaignsAssetHashesByAdGroupBriefTest
    : GrutBsExportControllerGetCampaignsAssetHashesBaseTest() {

    @Before
    fun before() {
        super.init(true)
    }

    /**
     * Проверяем получение хешей ассетов по одной заявочной группе
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
        val directAdId = createBanner(
            campaignId, adGroupId,
            listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId)
        )

        createAdGroupBrief(
            campaignId,
            listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId),
        )

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

        assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    /**
     * Проверяем получение хешей ассетов с разными id и contentId в баннерах и ассетах
     */
    @Test
    fun differentAssetIdAndAssetLinkIdTest() {
        val campaignId = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val adGroupId = grutSteps.createAdGroup(campaignId)

        val imageContentId = createAsset(EMediaType.MT_IMAGE)
        val videoContentId = createAsset(EMediaType.MT_VIDEO)
        val textContentId = createAsset(EMediaType.MT_TEXT)
        val titleContentId = createAsset(EMediaType.MT_TITLE)
        val html5ContentId = createAsset(EMediaType.MT_HTML5)
        val uacCampaignContents =
            listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId).map {
                createCampaignContent(
                    campaignId = campaignId.toIdString(),
                    contentId = it,
                )
            }
        val directAdId = createBanner(campaignId, adGroupId,
            listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId),
            uacCampaignContents.map { it.id })

        createAdGroupBriefWithCustomAssets(
            campaignId,
            uacCampaignContents,
        )

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

        assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    /**
     * Проверяем получение хешей ассетов с одинаковыми id и contentId в баннерах и ассетах
     */
    @Test
    fun equalAssetIdAndAssetLinkIdTest() {
        val campaignId = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val adGroupId = grutSteps.createAdGroup(campaignId)

        val imageContentId = createAsset(EMediaType.MT_IMAGE)
        val videoContentId = createAsset(EMediaType.MT_VIDEO)
        val textContentId = createAsset(EMediaType.MT_TEXT)
        val titleContentId = createAsset(EMediaType.MT_TITLE)
        val html5ContentId = createAsset(EMediaType.MT_HTML5)

        val uacCampaignContents =
            listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId).map {
                createCampaignContent(
                    campaignId = campaignId.toIdString(),
                    contentId = it,
                    id = it,
                )
            }
        val directAdId = createBanner(campaignId, adGroupId,
            listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId),
            uacCampaignContents.map { it.id })

        createAdGroupBriefWithCustomAssets(
            campaignId,
            uacCampaignContents,
        )

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

        assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    /**
     * Проверяем что ничего не получаем, если в групповой заявке нет ассетов
     */
    @Test
    fun testWithoutAssets() {
        val directCampaignId = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        grutSteps.createAdGroup(directCampaignId)

        createAdGroupBrief(
            directCampaignId,
            emptyList(),
        )

        val result = performRequest(listOf(directCampaignId))

        val expected = mapOf<String, Any>()

        assertThat(result)
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
        createBanner(
            directCampaignIdB,
            directAdGroupIdB,
            listOf(textContentIdBDeleted, titleContentIdBDeleted),
            status = EBannerStatus.BSS_DELETED
        )

        createAdGroupBrief(
            directCampaignIdA,
            listOf(textContentIdA1, titleContentIdA1, textContentIdA2, titleContentIdA2),
        )
        createAdGroupBrief(
            directCampaignIdB,
            listOf(textContentIdB, titleContentIdB, textContentIdBDeleted, titleContentIdBDeleted),
        )

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

        assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    /**
     * Проверяем получение хешей ассетов из 2ух групповых заявок в одной кампании
     */
    @Test
    fun testTwoAdGroupBriefIntoOneCampaign() {
        val directCampaignId = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val directAdGroupIdA = grutSteps.createAdGroup(directCampaignId)
        val directAdGroupIdB = grutSteps.createAdGroup(directCampaignId)

        val textContentIdA1 = createAsset(EMediaType.MT_TEXT)
        val textContentIdA2 = createAsset(EMediaType.MT_TEXT)
        val textContentIdB = createAsset(EMediaType.MT_TEXT)
        val textContentIdBDeleted = createAsset(EMediaType.MT_TEXT)
        val titleContentIdA1 = createAsset(EMediaType.MT_TITLE)
        val titleContentIdA2 = createAsset(EMediaType.MT_TITLE)
        val titleContentIdB = createAsset(EMediaType.MT_TITLE)
        val titleContentIdBDeleted = createAsset(EMediaType.MT_TITLE)

        val directAdIdA1 = createBanner(directCampaignId, directAdGroupIdA, listOf(textContentIdA1, titleContentIdA1))
        val directAdIdA2 = createBanner(directCampaignId, directAdGroupIdA, listOf(textContentIdA2, titleContentIdA2))
        val directAdIdA3 = createBanner(directCampaignId, directAdGroupIdB, listOf(textContentIdB, titleContentIdB))
        createBanner(
            directCampaignId,
            directAdGroupIdB,
            listOf(textContentIdBDeleted, titleContentIdBDeleted),
            status = EBannerStatus.BSS_DELETED
        )

        createAdGroupBrief(
            directCampaignId,
            listOf(textContentIdA1, titleContentIdA1, textContentIdA2, titleContentIdA2),
        )
        createAdGroupBrief(
            directCampaignId,
            listOf(textContentIdB, titleContentIdB, textContentIdBDeleted, titleContentIdBDeleted),
        )

        val result = performRequest(listOf(directCampaignId))

        val expected = mapOf(
            directCampaignId.toString() to mapOf(
                directAdIdA1.toString() to mapOf(
                    "TitleAssetHash" to titleContentIdA1,
                    "TextBodyAssetHash" to textContentIdA1,
                ),
                directAdIdA2.toString() to mapOf(
                    "TitleAssetHash" to titleContentIdA2,
                    "TextBodyAssetHash" to textContentIdA2,
                ),
                directAdIdA3.toString() to mapOf(
                    "TitleAssetHash" to titleContentIdB,
                    "TextBodyAssetHash" to textContentIdB,
                ),
            )
        )

        assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    /**
     * Проверяем получение хешей ассетов из 2ух групповых заявок с одинаковыми ассетами
     */
    @Test
    fun testTwoAdGroupBriefIntoOneCampaignWithEqualsAssets() {
        val directCampaignId = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val directAdGroupId1 = grutSteps.createAdGroup(directCampaignId)
        val directAdGroupId2 = grutSteps.createAdGroup(directCampaignId)

        val textContentId = createAsset(EMediaType.MT_TEXT)
        val textContentIdDeleted = createAsset(EMediaType.MT_TEXT)
        val titleContentId = createAsset(EMediaType.MT_TITLE)
        val titleContentIdDeleted = createAsset(EMediaType.MT_TITLE)

        val directAdId1 = createBanner(directCampaignId, directAdGroupId1, listOf(textContentId, titleContentId))
        val directAdId2 = createBanner(directCampaignId, directAdGroupId1, listOf(textContentId, titleContentId))
        createBanner(
            directCampaignId,
            directAdGroupId2,
            listOf(textContentIdDeleted, titleContentIdDeleted),
            status = EBannerStatus.BSS_DELETED
        )

        createAdGroupBrief(
            directCampaignId,
            listOf(textContentId, titleContentId, textContentIdDeleted),
        )
        createAdGroupBrief(
            directCampaignId,
            listOf(textContentId, titleContentId, titleContentIdDeleted),
        )

        val result = performRequest(listOf(directCampaignId))

        val expected = mapOf(
            directCampaignId.toString() to mapOf(
                directAdId1.toString() to mapOf(
                    "TitleAssetHash" to titleContentId,
                    "TextBodyAssetHash" to textContentId,
                ),
                directAdId2.toString() to mapOf(
                    "TitleAssetHash" to titleContentId,
                    "TextBodyAssetHash" to textContentId,
                ),
            )
        )

        assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    /**
     * Проверяем получение хешей ассетов для 2ух переданных кампаний с фичей и без
     */
    @Test
    fun testTwoCampaignsWithAndWithoutFeature() {
        val userInfoWithourtFeature = userSteps.createDefaultUser()
        grutSteps.createClient(userInfoWithourtFeature.clientInfo!!)

        val directCampaignId1 = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val directCampaignId2 = grutSteps.createTextCampaign(userInfoWithourtFeature.clientInfo!!)
        val directAdGroupId1 = grutSteps.createAdGroup(directCampaignId1)
        val directAdGroupId2 = grutSteps.createAdGroup(directCampaignId2)

        val textContentId1 = createAsset(EMediaType.MT_TEXT)
        val textContentId2 = createAsset(EMediaType.MT_TEXT)
        val titleContentId1 = createAsset(EMediaType.MT_TITLE)
        val titleContentId2 = createAsset(EMediaType.MT_TITLE)

        val directAdId1 = createBanner(directCampaignId1, directAdGroupId1, listOf(textContentId1, titleContentId1))
        val directAdId2 = createBanner(directCampaignId2, directAdGroupId2, listOf(textContentId2, titleContentId2))

        createAdGroupBrief(
            directCampaignId1,
            listOf(textContentId1, titleContentId1),
        )
        setAssetIdsToCampaign(directCampaignId2, listOf(textContentId2, titleContentId2))

        val result = performRequest(listOf(directCampaignId1, directCampaignId2))

        val expected = mapOf(
            directCampaignId1.toString() to mapOf(
                directAdId1.toString() to mapOf(
                    "TitleAssetHash" to titleContentId1,
                    "TextBodyAssetHash" to textContentId1,
                ),
            ),
            directCampaignId2.toString() to mapOf(
                directAdId2.toString() to mapOf(
                    "TitleAssetHash" to titleContentId2,
                    "TextBodyAssetHash" to textContentId2,
                ),
            )
        )

        assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    /**
     * Проверяем получение хешей ассетов когда нет групповой заявки - получаем хеши от заявки на кампании
     */
    @Test
    fun testWithFeatureButWithourAdGroupBrief() {
        val campaignId = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val adGroupId = grutSteps.createAdGroup(campaignId)

        val imageContentId = createAsset(EMediaType.MT_IMAGE)
        val videoContentId = createAsset(EMediaType.MT_VIDEO)
        val textContentId = createAsset(EMediaType.MT_TEXT)
        val titleContentId = createAsset(EMediaType.MT_TITLE)
        val html5ContentId = createAsset(EMediaType.MT_HTML5)
        val directAdId = createBanner(
            campaignId, adGroupId,
            listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId)
        )
        setAssetIdsToCampaign(
            campaignId,
            listOf(imageContentId, videoContentId, textContentId, titleContentId, html5ContentId)
        )

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

        assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    /**
     * Проверяем получение хешей ассетов для 2ух переданных кампаний с фичей, при этом
     * одна кампания с групповой заявкой, вторая без групповой заявки -> получаем хеши от заявки на группу и кампанию
     */
    @Test
    fun testTwoCampaignsWithFeatureWithAndWithoutAdGroupBrief() {
        val directCampaignId1 = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val directCampaignId2 = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val directAdGroupId1 = grutSteps.createAdGroup(directCampaignId1)
        val directAdGroupId2 = grutSteps.createAdGroup(directCampaignId2)

        val textContentId1 = createAsset(EMediaType.MT_TEXT)
        val textContentId2 = createAsset(EMediaType.MT_TEXT)
        val titleContentId1 = createAsset(EMediaType.MT_TITLE)
        val titleContentId2 = createAsset(EMediaType.MT_TITLE)

        val directAdId1 = createBanner(directCampaignId1, directAdGroupId1, listOf(textContentId1, titleContentId1))
        val directAdId2 = createBanner(directCampaignId2, directAdGroupId2, listOf(textContentId2, titleContentId2))

        createAdGroupBrief(
            directCampaignId1,
            listOf(textContentId1, titleContentId1),
        )
        setAssetIdsToCampaign(directCampaignId2, listOf(textContentId2, titleContentId2))

        val result = performRequest(listOf(directCampaignId1, directCampaignId2))

        val expected = mapOf(
            directCampaignId1.toString() to mapOf(
                directAdId1.toString() to mapOf(
                    "TitleAssetHash" to titleContentId1,
                    "TextBodyAssetHash" to textContentId1,
                ),
            ),
            directCampaignId2.toString() to mapOf(
                directAdId2.toString() to mapOf(
                    "TitleAssetHash" to titleContentId2,
                    "TextBodyAssetHash" to textContentId2,
                ),
            )
        )

        assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }
}
