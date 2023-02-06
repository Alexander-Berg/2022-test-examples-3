package ru.yandex.direct.web.entity.uac.service

import java.time.LocalDateTime.now
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.Sitelink
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.service.GrutUacContentService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.test.utils.checkContains
import ru.yandex.direct.test.utils.checkEmpty
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotEquals
import ru.yandex.direct.test.utils.checkNull
import ru.yandex.direct.test.utils.checkSize
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.grut.client.GrutClient

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class GrutUacAssetLinkUpdateServiceTest {

    @Autowired
    private lateinit var grutUacAssetLinkUpdateService: GrutUacAssetLinkUpdateService

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutClient: GrutClient

    @Autowired
    private lateinit var grutUacContentService: GrutUacContentService

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        grutSteps.createClient(clientInfo)
    }

    @Test
    fun oldAssetLinkIdGeneration_CreateAsset() {
        val oldText = "old text"
        val uacCampaign = createYdbCampaign().copy(
            assetLinks = listOf()
        )
        val updateRequest = emptyPatchInternalRequest().copy(
            texts = listOf(oldText),
        )

        val updatedAssetLinks = grutUacAssetLinkUpdateService.getUpdatedAssetLinks(
            uacCampaign, updateRequest,
            clientInfo.clientId!!,
            false
        )
        updatedAssetLinks.apply {
            checkSize(1)
            get(0).apply {
                id.checkEquals(contentId)
            }
        }
    }

    @Test
    fun newAssetLinkIdGeneration_CreateAsset() {
        val oldText = "old text"
        val uacCampaign = createYdbCampaign().copy(
            assetLinks = listOf()
        )
        val updateRequest = emptyPatchInternalRequest().copy(
            texts = listOf(oldText),
        )

        val updatedAssetLinks = grutUacAssetLinkUpdateService.getUpdatedAssetLinks(
            uacCampaign, updateRequest,
            clientInfo.clientId!!,
            true
        )
        updatedAssetLinks.apply {
            checkSize(1)
            get(0).apply {
                id.checkNotEquals(contentId)
            }
        }
    }

    @Test
    fun newAssetLinkIdGeneration_CreateRemovedAsset() {
        val oldNewImageAssetId = grutSteps.createDefaultImageAsset(clientInfo.clientId!!)

        val removedAt = now()
        val createdAt = removedAt.minusDays(1)

        val uacCampaign = createYdbCampaign(advType = AdvType.TEXT).copy(
            assetLinks = listOf(
                UacYdbCampaignContent(
                    id = oldNewImageAssetId,
                    campaignId = "",
                    order = 0,
                    status = CampaignContentStatus.DELETED,
                    type = MediaType.IMAGE,
                    createdAt = createdAt,
                    removedAt = removedAt,
                    contentId = oldNewImageAssetId
                )
            )
        )

        val updateRequest = emptyPatchInternalRequest().copy(
            contentIds = listOf(oldNewImageAssetId),
        )

        val updatedAssetLinks = grutUacAssetLinkUpdateService.getUpdatedAssetLinks(uacCampaign, updateRequest,
            clientInfo.clientId!!, true
        )
        updatedAssetLinks.checkSize(2)
        val notRemovedAssetLinks = updatedAssetLinks.filter { it.removedAt == null }
        notRemovedAssetLinks.checkSize(1)
        notRemovedAssetLinks[0].apply {
            id.checkNotEquals(contentId)
        }
    }

    @Test
    fun assetOrderTest() {
        val texts = List(5) {
            "text $it"
        }
        val sitelinks = List(5) {
            Sitelink(
                "title $it",
                "https://ya.ru",
                "desc $it"
            )
        }
        val textAssetIds = texts.map {
            grutSteps.createTextAsset(clientInfo.clientId!!, it)
        }
        val sitelinkAssetIds = sitelinks.map {
            grutSteps.createSitelinkAsset(clientInfo.clientId!!, it)
        }
        val uacCampaign = createYdbCampaign(startedAt = now()).copy(
            assetLinks = textAssetIds.subList(0, 5).mapIndexed { index, assetId ->
                UacYdbCampaignContent(
                    campaignId = "",
                    order = index,
                    status = CampaignContentStatus.CREATED,
                    type = MediaType.TEXT,
                    contentId = assetId
                )
            } + sitelinkAssetIds.subList(0, 5).mapIndexed { index, assetId ->
                UacYdbCampaignContent(
                    campaignId = "",
                    order = index,
                    status = CampaignContentStatus.CREATED,
                    type = MediaType.SITELINK,
                    contentId = assetId
                )
            }
        )
        val textAssetIdToOrder = listOf(textAssetIds[1], textAssetIds[0], textAssetIds[3], textAssetIds[4])
            .mapIndexed { index, assetId -> assetId to index }
            .toMap()
        val sitelinkAssetIdToOrder = listOf(sitelinkAssetIds[1], sitelinkAssetIds[0], sitelinkAssetIds[3], sitelinkAssetIds[4])
            .mapIndexed { index, assetId -> assetId to index }
            .toMap()
        val assetIdToOrder = textAssetIdToOrder + sitelinkAssetIdToOrder
        val updateRequest = emptyPatchInternalRequest().copy(
            texts = listOf(texts[1], texts[0], texts[3], texts[4]),
            sitelinks = listOf(sitelinks[1], sitelinks[0], sitelinks[3], sitelinks[4])
        )

        val updatedAssetLinks = grutUacAssetLinkUpdateService.getUpdatedAssetLinks(
            uacCampaign, updateRequest,
            clientInfo.clientId!!
        )

        updatedAssetLinks.checkSize(10)
        val notRemovedAssetLinks = updatedAssetLinks.filter { it.removedAt == null }
        notRemovedAssetLinks.checkSize(8)
        notRemovedAssetLinks.forEach {
            assetIdToOrder[it.contentId].checkEquals(it.order)
        }
    }

    @Test
    fun restoreDeletedSitelinkAsset_SitelinkRestored() {
        val sitelinkTitle = "sitelink title"
        val sitelinkHref = "https://ya.ru"
        val assetId = grutSteps.createSitelinkAsset(clientInfo.clientId!!, Sitelink(sitelinkTitle, sitelinkHref, null))

        val removedAt = now()
        val createdAt = removedAt.minusDays(1)

        val uacCampaign = createYdbCampaign().copy(
            assetLinks = listOf(
                UacYdbCampaignContent(
                    campaignId = "",
                    order = 0,
                    status = CampaignContentStatus.DELETED,
                    type = MediaType.TEXT,
                    createdAt = createdAt,
                    removedAt = removedAt,
                    contentId = assetId,
                )
            )
        )

        val updateRequest = emptyPatchInternalRequest().copy(
            sitelinks = listOf(Sitelink(sitelinkTitle, sitelinkHref, null)),
        )

        val updatedAssetLinks = grutUacAssetLinkUpdateService.getUpdatedAssetLinks(
            uacCampaign, updateRequest,
            clientInfo.clientId!!
        )
        updatedAssetLinks.apply {
            checkSize(1)
            get(0).apply {
                contentId.checkEquals(assetId)
                this.removedAt.checkNull()
                status.checkEquals(CampaignContentStatus.CREATED)
            }
        }
    }

    @Test
    fun emptyUpdateRequest() {
        val textAssetId = grutSteps.createDefaultTextAsset(clientInfo.clientId!!)
        val titleAssetId = grutSteps.createDefaultTitleAsset(clientInfo.clientId!!)
        val sitelinkAssetId = grutSteps.createSitelinkAsset(clientInfo.clientId!!)
        val imageAssetId = grutSteps.createDefaultImageAsset(clientInfo.clientId!!)
        val videoAssetId = grutSteps.createDefaultVideoAsset(clientInfo.clientId!!)
        val html5AssetId = grutSteps.createDefaultHtml5Asset(clientInfo.clientId!!)

        val createdAt = now().minusDays(1)

        val assetLinks = listOf(
            UacYdbCampaignContent(
                campaignId = "",
                order = 0,
                status = CampaignContentStatus.CREATED,
                type = MediaType.TEXT,
                createdAt = createdAt,
                contentId = textAssetId
            ),
            UacYdbCampaignContent(
                campaignId = "",
                order = 0,
                status = CampaignContentStatus.CREATED,
                type = MediaType.TITLE,
                createdAt = createdAt,
                contentId = titleAssetId
            ),
            UacYdbCampaignContent(
                campaignId = "",
                order = 0,
                status = CampaignContentStatus.CREATED,
                type = MediaType.SITELINK,
                createdAt = createdAt,
                contentId = sitelinkAssetId,
            ),
            UacYdbCampaignContent(
                campaignId = "",
                order = 0,
                status = CampaignContentStatus.CREATED,
                type = MediaType.IMAGE,
                createdAt = createdAt,
                contentId = imageAssetId,
            ),
            UacYdbCampaignContent(
                campaignId = "",
                order = 0,
                status = CampaignContentStatus.CREATED,
                type = MediaType.VIDEO,
                createdAt = createdAt,
                contentId = videoAssetId,
            ),
            UacYdbCampaignContent(
                campaignId = "",
                order = 0,
                status = CampaignContentStatus.CREATED,
                type = MediaType.HTML5,
                createdAt = createdAt,
                contentId = html5AssetId,
            ),
        )

        val uacCampaign = createYdbCampaign(assetLinks = assetLinks)

        val updateRequest = emptyPatchInternalRequest()

        val updatedAssetLinks = grutUacAssetLinkUpdateService.getUpdatedAssetLinks(
            uacCampaign, updateRequest,
            clientInfo.clientId!!
        )

        updatedAssetLinks.toSet().checkEquals(assetLinks.toSet())
    }

    @Test
    fun deleteAssetInDraftCampaignTest() {
        val oldText = "old text"
        val textAssetId = grutSteps.createTextAsset(clientInfo.clientId!!, oldText)

        val uacCampaign = createYdbCampaign(
            assetLinks = listOf(
                UacYdbCampaignContent(
                    campaignId = "",
                    order = 0,
                    status = CampaignContentStatus.CREATED,
                    type = MediaType.TEXT,
                    contentId = textAssetId
                )
            ),
        )

        val updateRequest = emptyPatchInternalRequest().copy(
            texts = emptyList(),
        )

        grutUacAssetLinkUpdateService.getUpdatedAssetLinks(uacCampaign, updateRequest, clientInfo.clientId!!)
            .checkEmpty()
    }

    @Test
    fun allTransitionsInRmpCampaignWithNewAssetLinkIdGenerationTest() {
        val texts = List(5) {
            "text $it"
        }
        val textAssetIds = texts.map {
            grutSteps.createTextAsset(clientInfo.clientId!!, it)
        }
        val uacCampaign = createYdbCampaign(advType = AdvType.MOBILE_CONTENT, startedAt = now()).copy(
            assetLinks = textAssetIds.subList(0, 2).map { assetId ->
                UacYdbCampaignContent(
                    campaignId = "",
                    order = 0,
                    status = CampaignContentStatus.CREATED,
                    type = MediaType.TEXT,
                    contentId = assetId
                )
            } + textAssetIds.subList(2, 4).map { assetId ->
                UacYdbCampaignContent(
                    campaignId = "",
                    order = 0,
                    status = CampaignContentStatus.DELETED,
                    removedAt = now(),
                    type = MediaType.TEXT,
                    contentId = assetId
                )
            }
        )

        val updateRequest = emptyPatchInternalRequest().copy(
            texts = listOf(texts[0], texts[2], texts[4]),
        )

        val updatedAssetLinks = grutUacAssetLinkUpdateService.getUpdatedAssetLinks(
            uacCampaign, updateRequest,
            clientInfo.clientId!!,
            true
        )

        val (removedAssetLinks, notRemovedAssetLinks) = updatedAssetLinks.partition { it.removedAt != null }
        removedAssetLinks.checkSize(2)
        notRemovedAssetLinks.checkSize(3)
        removedAssetLinks.map { it.contentId!! }.checkContains(textAssetIds[1], textAssetIds[3])
        notRemovedAssetLinks.map { it.contentId!! }.checkContains(textAssetIds[0], textAssetIds[2])
    }

    @Test
    fun allTransitionInTextCampaignWithNewAssetLinkIdGenerationTest() {
        val texts = List(5) {
            "text $it"
        }
        val textAssetIds = texts.map {
            grutSteps.createTextAsset(clientInfo.clientId!!, it)
        }
        val uacCampaign = createYdbCampaign(advType = AdvType.TEXT, startedAt = now()).copy(
            assetLinks = textAssetIds.subList(0, 2).map { assetId ->
                UacYdbCampaignContent(
                    campaignId = "",
                    order = 0,
                    status = CampaignContentStatus.CREATED,
                    type = MediaType.TEXT,
                    contentId = assetId
                )
            } + textAssetIds.subList(2, 4).map { assetId ->
                UacYdbCampaignContent(
                    campaignId = "",
                    order = 0,
                    status = CampaignContentStatus.DELETED,
                    removedAt = now(),
                    type = MediaType.TEXT,
                    contentId = assetId
                )
            }
        )

        val updateRequest = emptyPatchInternalRequest().copy(
            texts = listOf(texts[0], texts[2], texts[4]),
        )

        val updatedAssetLinks = grutUacAssetLinkUpdateService.getUpdatedAssetLinks(
            uacCampaign, updateRequest,
            clientInfo.clientId!!,
            true
        )

        val (removedAssetLinks, notRemovedAssetLinks) = updatedAssetLinks.partition { it.removedAt != null }
        removedAssetLinks.checkSize(3)
        notRemovedAssetLinks.checkSize(3)
        removedAssetLinks.map { it.contentId!! }.checkContains(textAssetIds[1], textAssetIds[2], textAssetIds[3])
        notRemovedAssetLinks.map { it.contentId!! }.checkContains(textAssetIds[0])
    }
}
