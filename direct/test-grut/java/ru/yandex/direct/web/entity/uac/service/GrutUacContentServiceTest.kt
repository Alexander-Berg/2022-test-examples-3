package ru.yandex.direct.web.entity.uac.service

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.uac.AssetConstants.ASSET_TEXT
import ru.yandex.direct.core.entity.uac.AssetConstants.ASSET_TITLE
import ru.yandex.direct.core.entity.uac.AssetConstants.DIRECT_IMAGE_HASH
import ru.yandex.direct.core.entity.uac.AssetConstants.FILENAME
import ru.yandex.direct.core.entity.uac.AssetConstants.MDS_URL
import ru.yandex.direct.core.entity.uac.AssetConstants.SITELINK_DESCRIPTION
import ru.yandex.direct.core.entity.uac.AssetConstants.SITELINK_HREF
import ru.yandex.direct.core.entity.uac.AssetConstants.SITELINK_TITLE
import ru.yandex.direct.core.entity.uac.AssetConstants.SOURCE_URL
import ru.yandex.direct.core.entity.uac.AssetConstants.THUMB
import ru.yandex.direct.core.entity.uac.AssetConstants.VIDEO_DURATION
import ru.yandex.direct.core.entity.uac.converter.UacGrutAssetsConverter.toImageAsset
import ru.yandex.direct.core.entity.uac.converter.UacGrutAssetsConverter.toUacYdbContent
import ru.yandex.direct.core.entity.uac.converter.UacGrutAssetsConverter.toVideoAsset
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.getExpectedImageContentMeta
import ru.yandex.direct.core.entity.uac.getExpectedVideoContentMeta
import ru.yandex.direct.core.entity.uac.model.AssetContainer
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.model.Content
import ru.yandex.direct.core.entity.uac.model.Html5Asset
import ru.yandex.direct.core.entity.uac.model.ImageAsset
import ru.yandex.direct.core.entity.uac.model.LinkedAsset
import ru.yandex.direct.core.entity.uac.model.MdsInfo
import ru.yandex.direct.core.entity.uac.model.MediaAsset
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.SitelinkAsset
import ru.yandex.direct.core.entity.uac.model.TextAsset
import ru.yandex.direct.core.entity.uac.model.TitleAsset
import ru.yandex.direct.core.entity.uac.model.VideoAsset
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.samples.CONTENT_HTML5_META
import ru.yandex.direct.core.entity.uac.samples.CONTENT_IMAGE_META
import ru.yandex.direct.core.entity.uac.samples.CONTENT_VIDEO_META
import ru.yandex.direct.core.entity.uac.service.GrutUacContentService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.utils.fromJson
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import java.time.LocalDateTime.now

@GrutDirectWebTest
@RunWith(JUnitParamsRunner::class)
class GrutUacContentServiceTest {
    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var uacContentService: GrutUacContentService

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutApiService: GrutApiService

    private lateinit var clientId: ClientId
    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        userInfo = steps.userSteps().createDefaultUser()
        clientId = userInfo.clientId
        grutSteps.createClient(clientId)
    }

    @Test
    fun getAssetContainerEmptyTest() {
        val result = uacContentService.getAssetContainer(
            assetIds = emptySet(),
            assetOrderById = emptyMap(),
        )
        assertThat(result).isEqualTo(AssetContainer())
    }

    @Test
    fun getAssetContainerBadAssetIdTest() {
        val result = uacContentService.getAssetContainer(
            setOf("1"),
            mapOf("1" to 0)
        )
        assertThat(result).isEqualTo(AssetContainer())
    }

    @Test
    fun getAssetContainerTest() {
        val textAssetId1 = grutSteps.createDefaultTextAsset(clientId)
        val textAssetId2 = grutSteps.createDefaultTextAsset(clientId)
        val titleAssetId = grutSteps.createTitleAsset(clientId)
        val sitelinkAssetId = grutSteps.createSitelinkAsset(clientId)
        val imageAssetId = grutSteps.createDefaultImageAsset(clientId)
        val videoAssetId = grutSteps.createDefaultVideoAsset(clientId)
        val html5AssetId = grutSteps.createDefaultHtml5Asset(clientId)

        val result = uacContentService.getAssetContainer(
            assetIds = setOf(
                textAssetId1, textAssetId2, titleAssetId, sitelinkAssetId, imageAssetId, videoAssetId, html5AssetId
            ),
            assetOrderById = mapOf(
                textAssetId1 to 0,
                textAssetId2 to 1,
                titleAssetId to 2,
                sitelinkAssetId to 3,
                imageAssetId to 4,
                videoAssetId to 5,
                html5AssetId to 6
            ),
            setOf(textAssetId2, sitelinkAssetId, videoAssetId),
        )

        val expected = AssetContainer(
            texts = listOf(
                LinkedAsset(0, false, TextAsset(textAssetId1, clientId.toString(), ASSET_TEXT)),
                LinkedAsset(1, true, TextAsset(textAssetId2, clientId.toString(), ASSET_TEXT)),
            ),
            titles = listOf(
                LinkedAsset(2, false, TitleAsset(titleAssetId, clientId.toString(), ASSET_TITLE)),
            ),
            sitelinks = listOf(
                LinkedAsset(3, true, SitelinkAsset(sitelinkAssetId, clientId.toString(), SITELINK_TITLE, SITELINK_HREF, SITELINK_DESCRIPTION)),
            ),
            images = listOf(
                LinkedAsset(4, false, ImageAsset(
                    id = imageAssetId,
                    clientId = clientId.toString(),
                    mdsInfo = MdsInfo(THUMB, SOURCE_URL, MDS_URL, FILENAME, fromJson(CONTENT_IMAGE_META)),
                    directImageHash = DIRECT_IMAGE_HASH,
                )),
            ),
            videos = listOf(
                LinkedAsset(5, true, VideoAsset(
                    id = videoAssetId,
                    clientId = clientId.toString(),
                    mdsInfo = MdsInfo(THUMB, SOURCE_URL, MDS_URL, FILENAME, fromJson(CONTENT_VIDEO_META)),
                    videoDuration = 15,
                )),
            ),
            html5s = listOf(
                LinkedAsset(6, false, Html5Asset(
                    id = html5AssetId,
                    clientId = clientId.toString(),
                    mdsInfo = MdsInfo(THUMB, SOURCE_URL, MDS_URL, FILENAME, fromJson(CONTENT_HTML5_META)),
                )),
            )
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun getAssetContainerWithAssetLinksTest() {
        val textAssetId1 = grutSteps.createDefaultTextAsset(clientId)
        val textAssetId2 = grutSteps.createDefaultTextAsset(clientId)
        val titleAssetId = grutSteps.createTitleAsset(clientId)
        val sitelinkAssetId = grutSteps.createSitelinkAsset(clientId)
        val imageAssetId = grutSteps.createDefaultImageAsset(clientId)
        val videoAssetId = grutSteps.createDefaultVideoAsset(clientId)
        val html5AssetId = grutSteps.createDefaultHtml5Asset(clientId)

        val campaignContents =
            listOf(textAssetId1, textAssetId2, titleAssetId, sitelinkAssetId, imageAssetId, videoAssetId, html5AssetId)
                .mapIndexed { index, assetId ->
                    UacYdbCampaignContent(
                        contentId = assetId,
                        order = index,
                        campaignId = "",
                        type = null,
                        status = CampaignContentStatus.CREATED,
                        removedAt = null,
                    )
                } +
                listOf(textAssetId1, textAssetId2, titleAssetId, sitelinkAssetId, imageAssetId, videoAssetId, html5AssetId)
                    .mapIndexed { index, assetId ->
                        UacYdbCampaignContent(
                            contentId = assetId,
                            order = index,
                            campaignId = "",
                            type = null,
                            status = CampaignContentStatus.DELETED,
                            removedAt = now(),
                        )
                    }

        val result = uacContentService.getAssetContainer(
            createYdbCampaign(assetLinks = campaignContents)
        )

        val expected = AssetContainer(
            texts = listOf(false, true).flatMap {
                listOf(
                    LinkedAsset(0, it, TextAsset(textAssetId1, clientId.toString(), ASSET_TEXT)),
                    LinkedAsset(1, it, TextAsset(textAssetId2, clientId.toString(), ASSET_TEXT))
                )
            },
            titles = listOf(
                LinkedAsset(2, false, TitleAsset(titleAssetId, clientId.toString(), ASSET_TITLE)),
                LinkedAsset(2, true, TitleAsset(titleAssetId, clientId.toString(), ASSET_TITLE)),
            ),
            sitelinks = listOf(
                LinkedAsset(3, false, SitelinkAsset(sitelinkAssetId, clientId.toString(), SITELINK_TITLE, SITELINK_HREF, SITELINK_DESCRIPTION)),
                LinkedAsset(3, true, SitelinkAsset(sitelinkAssetId, clientId.toString(), SITELINK_TITLE, SITELINK_HREF, SITELINK_DESCRIPTION)),

                ),
            images = listOf(
                LinkedAsset(4, false, ImageAsset(
                    id = imageAssetId,
                    clientId = clientId.toString(),
                    mdsInfo = MdsInfo(THUMB, SOURCE_URL, MDS_URL, FILENAME, fromJson(CONTENT_IMAGE_META)),
                    directImageHash = DIRECT_IMAGE_HASH,
                )),
                LinkedAsset(4, true, ImageAsset(
                    id = imageAssetId,
                    clientId = clientId.toString(),
                    mdsInfo = MdsInfo(THUMB, SOURCE_URL, MDS_URL, FILENAME, fromJson(CONTENT_IMAGE_META)),
                    directImageHash = DIRECT_IMAGE_HASH,
                )),
            ),
            videos = listOf(
                LinkedAsset(5, false, VideoAsset(
                    id = videoAssetId,
                    clientId = clientId.toString(),
                    mdsInfo = MdsInfo(THUMB, SOURCE_URL, MDS_URL, FILENAME, fromJson(CONTENT_VIDEO_META)),
                    videoDuration = 15,
                )),
                LinkedAsset(5, true, VideoAsset(
                    id = videoAssetId,
                    clientId = clientId.toString(),
                    mdsInfo = MdsInfo(THUMB, SOURCE_URL, MDS_URL, FILENAME, fromJson(CONTENT_VIDEO_META)),
                    videoDuration = 15,
                )),
            ),
            html5s = listOf(
                LinkedAsset(6, false, Html5Asset(
                    id = html5AssetId,
                    clientId = clientId.toString(),
                    mdsInfo = MdsInfo(THUMB, SOURCE_URL, MDS_URL, FILENAME, fromJson(CONTENT_HTML5_META)),
                )),
                LinkedAsset(6, true, Html5Asset(
                    id = html5AssetId,
                    clientId = clientId.toString(),
                    mdsInfo = MdsInfo(THUMB, SOURCE_URL, MDS_URL, FILENAME, fromJson(CONTENT_HTML5_META)),
                )),
            )
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun fillMediaContentsFromAssetsEmptyTest() {
        val result = uacContentService.fillMediaContentsFromAssets(emptyList())
        assertThat(result).isEmpty()
    }

    @Test
    fun fillMediaContentsFromAssetsBadTypeTest() {
        data class UnknownMediaAsset(
            override val id: String = "",
            override val clientId: String = "",
            override val mdsInfo: MdsInfo = MdsInfo("", "", "", "", emptyMap()),
        ) : MediaAsset

        assertThatThrownBy {
            uacContentService.fillMediaContentsFromAssets(listOf(UnknownMediaAsset()))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Unsupported asset type: \"UnknownMediaAsset\"")
    }

    @Test
    fun fillMediaContentsFromAssetsTest() {
        val videoAsset = VideoAsset(
            id = "video-asset-id",
            clientId = clientId.toString(),
            mdsInfo = MdsInfo(
                thumb = THUMB,
                sourceUrl = SOURCE_URL,
                mdsUrl = MDS_URL,
                filename = FILENAME,
                meta = fromJson(CONTENT_VIDEO_META),
            ),
            videoDuration = 15,
        )
        val imageAsset = ImageAsset(
            id = "image-asset-id",
            clientId = clientId.toString(),
            mdsInfo = MdsInfo(
                thumb = THUMB,
                sourceUrl = SOURCE_URL,
                mdsUrl = MDS_URL,
                filename = FILENAME,
                meta = fromJson(CONTENT_IMAGE_META),
            ),
            directImageHash = "directImageHash",
        )

        val result = uacContentService.fillMediaContentsFromAssets(listOf(videoAsset, imageAsset))

        assertThat(result).containsExactly(
            Content(
                id = videoAsset.id,
                type = MediaType.VIDEO,
                thumb = videoAsset.mdsInfo.thumb,
                thumbId = "4220162/121e631f-db50-463e-9fd0-dfe799c78df8",
                sourceUrl = videoAsset.mdsInfo.sourceUrl,
                directImageHash = null,
                iw = 1280,
                ih = 720,
                ow = 1280,
                oh = 720,
                mdsUrl = videoAsset.mdsInfo.mdsUrl,
                meta = getExpectedVideoContentMeta(),
                videoDuration = 15,
                filename = videoAsset.mdsInfo.filename,
                tw = 1280,
                th = 720,
            ),
            Content(
                id = imageAsset.id,
                type = MediaType.IMAGE,
                thumb = imageAsset.mdsInfo.thumb,
                thumbId = "4220162/121e631f-db50-463e-9fd0-dfe799c78df8",
                sourceUrl = imageAsset.mdsInfo.sourceUrl,
                directImageHash = imageAsset.directImageHash,
                iw = 850,
                ih = 850,
                ow = 950,
                oh = 950,
                mdsUrl = imageAsset.mdsInfo.mdsUrl,
                meta = getExpectedImageContentMeta(imageAsset.mdsInfo.meta["direct_mds_meta"]),
                videoDuration = null,
                filename = imageAsset.mdsInfo.filename,
                tw = 850,
                th = 850,
            ),
        )
    }

    @Test
    fun saveImageContentTest() {
        val content = UacYdbContent(
            ownerId = clientId.toString(),
            type = MediaType.IMAGE,
            thumb = THUMB,
            sourceUrl = SOURCE_URL,
            mdsUrl = MDS_URL,
            meta = fromJson(CONTENT_IMAGE_META),
            filename = FILENAME,
            accountId = clientId.toString(),
            directImageHash = DIRECT_IMAGE_HASH,
            videoDuration = null,
        )
        val result = uacContentService.insertContent(content)
        assertThat(result.isSuccessful)

        val fetched = getAsset(content.id)
        assertThat(fetched).isNotNull
        assertThat(toUacYdbContent(fetched!!.toImageAsset()))
            .isEqualTo(content)
    }

    @Test
    fun saveVideoContentTest() {
        val content = UacYdbContent(
            ownerId = clientId.toString(),
            type = MediaType.VIDEO,
            thumb = THUMB,
            sourceUrl = SOURCE_URL,
            mdsUrl = MDS_URL,
            meta = fromJson(CONTENT_VIDEO_META),
            filename = FILENAME,
            accountId = clientId.toString(),
            directImageHash = null,
            videoDuration = 10,
        )
        val result = uacContentService.insertContent(content)
        assertThat(result.isSuccessful)

        val fetched = getAsset(content.id)
        assertThat(fetched).isNotNull
        assertThat(toUacYdbContent(fetched!!.toVideoAsset()))
            .isEqualTo(content)
    }

    @Test
    fun deleteContentTest() {
        val assetId = grutSteps.createDefaultImageAsset(clientId)
        assertThat(getAsset(assetId)).isNotNull

        uacContentService.deleteContent(assetId)
        assertThat(getAsset(assetId)).isNull()
    }

    @Test
    fun getDbContentsTest() {
        val textAssetId = grutSteps.createDefaultTextAsset(clientId)
        val titleAssetId = grutSteps.createTitleAsset(clientId)
        val sitelinkAssetId = grutSteps.createSitelinkAsset(clientId)
        val imageAssetId = grutSteps.createDefaultImageAsset(clientId)
        val videoAssetId = grutSteps.createDefaultVideoAsset(clientId)
        val html5AssetId = grutSteps.createDefaultHtml5Asset(clientId)

        val fetchedContents = uacContentService.getDbContents(
            listOf(textAssetId, titleAssetId, sitelinkAssetId, imageAssetId, videoAssetId, html5AssetId)
        )
        assertThat(fetchedContents).containsExactlyInAnyOrder(
            UacYdbContent(
                id = imageAssetId,
                ownerId = clientId.toString(),
                type = MediaType.IMAGE,
                thumb = THUMB,
                sourceUrl = SOURCE_URL,
                mdsUrl = MDS_URL,
                meta = fromJson(CONTENT_IMAGE_META),
                videoDuration = null,
                filename = FILENAME,
                accountId = clientId.toString(),
                directImageHash = DIRECT_IMAGE_HASH,
            ),
            UacYdbContent(
                id = videoAssetId,
                ownerId = clientId.toString(),
                type = MediaType.VIDEO,
                thumb = THUMB,
                sourceUrl = SOURCE_URL,
                mdsUrl = MDS_URL,
                meta = fromJson(CONTENT_VIDEO_META),
                videoDuration = VIDEO_DURATION,
                filename = FILENAME,
                accountId = clientId.toString(),
                directImageHash = null,
            ),
            UacYdbContent(
                id = html5AssetId,
                ownerId = clientId.toString(),
                type = MediaType.HTML5,
                thumb = THUMB,
                sourceUrl = SOURCE_URL,
                mdsUrl = MDS_URL,
                meta = fromJson(CONTENT_HTML5_META),
                videoDuration = null,
                filename = FILENAME,
                accountId = clientId.toString(),
                directImageHash = null,
            ),
        )
    }

    @Test
    fun getDbContentByHash() {
        val hash = "hash"

        grutSteps.createDefaultVideoAsset(clientId)
        grutSteps.createDefaultImageAsset(clientId)
        val imageAssetId = grutSteps.createDefaultImageAsset(clientId, hash)

        val fetchedContent = uacContentService.getDbContentByHash(hash)

        assertThat(fetchedContent).isNotNull
        assertThat(fetchedContent).isEqualTo(
            UacYdbContent(
                id = imageAssetId,
                ownerId = clientId.toString(),
                type = MediaType.IMAGE,
                thumb = THUMB,
                sourceUrl = SOURCE_URL,
                mdsUrl = MDS_URL,
                meta = fromJson(CONTENT_IMAGE_META),
                videoDuration = null,
                filename = FILENAME,
                accountId = clientId.toString(),
                directImageHash = hash,
            )
        )
    }

    @Suppress("unused")
    private fun isFeatureEnabled() = listOf(true, false)

    @Test
    @TestCaseName("Test case for feature enabled: {0}")
    @Parameters(method = "isFeatureEnabled")
    fun getDbContentsByDirectCampaignIdTest(isFeatureEnabled: Boolean) {
        steps.featureSteps().addClientFeature(clientId, FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED, isFeatureEnabled)

        val textAssetId = grutSteps.createDefaultTextAsset(clientId)
        val videoAssetId = grutSteps.createDefaultVideoAsset(clientId)
        val imageAsset1Id = grutSteps.createDefaultImageAsset(clientId)
        val imageAsset2Id = grutSteps.createDefaultImageAsset(clientId)
        val campaignId = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val assetIds = listOf(textAssetId, videoAssetId, imageAsset1Id, imageAsset2Id)
        grutSteps.setAssetLinksToCampaign(campaignId, assetIds)

        val result = uacContentService.getDbContentsByDirectCampaignId(campaignId, clientId, MediaType.IMAGE).toList()
        assertThat(result.size).isEqualTo(2)
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(imageAsset1Id, imageAsset2Id)
    }

    /**
     * Проверяем, что контенты берутся из групповых заявок, а не из заявки на кампанию
     */
    @Test
    fun `getDbContentsByDirectCampaignIdTest ad group briefs enabled`() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED, true)

        val textAssetId = grutSteps.createDefaultTextAsset(clientId)
        val videoAssetId = grutSteps.createDefaultVideoAsset(clientId)
        val campaignImageAsset1Id = grutSteps.createDefaultImageAsset(clientId)
        val campaignImageAsset2Id = grutSteps.createDefaultImageAsset(clientId)
        val campaignId = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val campaignAssetIds = listOf(textAssetId, videoAssetId, campaignImageAsset1Id, campaignImageAsset2Id)
        grutSteps.setAssetLinksToCampaign(campaignId, campaignAssetIds)

        val adGroupBriefId1 = grutSteps.createAdGroupBrief(campaignId)
        val adGroupBrief1ImageAsset1Id = grutSteps.createDefaultImageAsset(clientId)
        val adGroupBrief1ImageAsset2Id = grutSteps.createDefaultImageAsset(clientId)
        val adGroupBrief1AssetIds = listOf(textAssetId, videoAssetId,
            adGroupBrief1ImageAsset1Id, adGroupBrief1ImageAsset2Id)
        grutSteps.setCustomAssetIdsToAdGroupBrief(adGroupBriefId1, adGroupBrief1AssetIds)

        val adGroupBriefId2 = grutSteps.createAdGroupBrief(campaignId)
        val adGroupBrief2ImageAsset1Id = grutSteps.createDefaultImageAsset(clientId)
        val adGroupBrief2ImageAsset2Id = grutSteps.createDefaultImageAsset(clientId)
        val adGroupBrief2AssetIds = listOf(textAssetId, videoAssetId,
            adGroupBrief2ImageAsset1Id, adGroupBrief2ImageAsset2Id)
        grutSteps.setCustomAssetIdsToAdGroupBrief(adGroupBriefId2, adGroupBrief2AssetIds)

        val result = uacContentService.getDbContentsByDirectCampaignId(campaignId, clientId, MediaType.IMAGE).toList()
        assertThat(result.size).isEqualTo(4)
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(
            adGroupBrief1ImageAsset1Id, adGroupBrief1ImageAsset2Id,
            adGroupBrief2ImageAsset1Id, adGroupBrief2ImageAsset2Id,
        )
    }

    private fun getAsset(assetId: String) = grutApiService.assetGrutApi.getAsset(assetId.toIdLong())

}
