package ru.yandex.direct.web.entity.uac.service

import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.converter.UacGrutAssetsConverter.toMediaType
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toUacYdbCampaign
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdsLong
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignUpdateService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.model.KtModelChanges
import ru.yandex.direct.test.utils.checkContainsInAnyOrder
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkSize
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.grut.objects.proto.client.Schema

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class GrutUacCampaignUpdateServiceTest : UacCampaignUpdateServiceTestBase() {

    @Autowired
    override lateinit var uacCampaignUpdateServiceWeb: WebGrutUacCampaignUpdateService

    @Autowired
    private lateinit var uacCampaignUpdateCoreService: GrutUacCampaignUpdateService

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var grutApiService: GrutApiService

    override fun getUacCampaign(id: String): UacYdbCampaign {
        return grutApiService.briefGrutApi.getBrief(id.toIdLong())!!.toUacYdbCampaign()
    }

    override fun createUacCampaign(clientInfo: ClientInfo, draft: Boolean, impressionUrl: String?, briefSynced: Boolean?) {
        val startedAt = if (draft) null else now().truncatedTo(ChronoUnit.SECONDS)
        val campaignId = grutSteps.createMobileAppCampaign(clientInfo, startedAt = startedAt, impressionUrl = impressionUrl, briefSynced = briefSynced, createInDirect = true)
        uacCampaign = getUacCampaign(campaignId.toIdString())
        directCampaignId = campaignId
    }

    private fun getAssets(uacCampaign: UacYdbCampaign): List<Schema.TAsset> {
        val assetIds = uacCampaign.assetLinks!!.sortedBy { it.order }.mapNotNull { it.contentId }

        return grutApiService.assetGrutApi.getAssets(assetIds.toIdsLong())
    }

    @Before
    fun grutBefore() {
        grutSteps.createClient(clientInfo)

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UC_UAC_CREATE_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)

        createUacCampaign(clientInfo, draft = false, impressionUrl = null, briefSynced = null)
    }

    @Test
    fun updateFromModelChangesEmptyTest() {
        val campaignBefore = grutApiService.briefGrutApi.getBrief(uacCampaign.id.toIdLong())!!.toUacYdbCampaign()

        val changes = KtModelChanges<String, UacYdbCampaign>(uacCampaign.id)

        uacCampaignUpdateCoreService.updateCampaignFromModelChanges(campaignBefore, changes)

        val campaignAfter = grutApiService.briefGrutApi.getBrief(uacCampaign.id.toIdLong())!!.toUacYdbCampaign()

        assertThat(campaignAfter).isEqualTo(campaignBefore)
    }

    @Test
    fun updateFromModelChangesTest() {
        val campaignBefore = grutApiService.briefGrutApi.getBrief(uacCampaign.id.toIdLong())!!.toUacYdbCampaign()

        val now = now().truncatedTo(ChronoUnit.SECONDS)

        val changes = KtModelChanges<String, UacYdbCampaign>(uacCampaign.id)
        changes.process(UacYdbCampaign::appId, "new-app-id")
        changes.process(UacYdbCampaign::updatedAt, now)
        changes.process(UacYdbCampaign::directCampaignStatus, DirectCampaignStatus.CREATED)

        uacCampaignUpdateCoreService.updateCampaignFromModelChanges(campaignBefore, changes)

        val campaignAfter = grutApiService.briefGrutApi.getBrief(uacCampaign.id.toIdLong())!!.toUacYdbCampaign()

        assertThat(campaignAfter).isEqualToIgnoringGivenFields(
            campaignBefore,
            UacYdbCampaign::appId.name,
            UacYdbCampaign::updatedAt.name,
            UacYdbCampaign::directCampaignStatus.name,
        )
        assertThat(campaignAfter.appId).isEqualTo("new-app-id")
        assertThat(campaignAfter.updatedAt).isEqualTo(now)
        assertThat(campaignAfter.directCampaignStatus).isEqualTo(DirectCampaignStatus.CREATED)
    }

    @Test
    fun updateTextAssetsTest() {
        val newTexts = listOf("Новый текст 1", "Новый текст 2", "Новый текст 3")

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                texts = newTexts
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful)
        assertThat(result.validationResult).isNull()

        val campaign = getUacCampaign(uacCampaign.id)!!
        getAssets(campaign).apply {
            val textAssets = filter { it.meta.mediaType.toMediaType() == MediaType.TEXT }
            assertThat(
                textAssets.map { it.spec.text }
            ).containsExactlyElementsOf(newTexts)
        }
    }

    @Test
    fun updateImageAssetsTest() {
        val imageAssetIds = listOf(
            grutSteps.createDefaultImageAsset(clientInfo.clientId!!),
        )

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                contentIds = imageAssetIds
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful)
        assertThat(result.validationResult).isNull()

        val campaign = getUacCampaign(uacCampaign.id)!!
        getAssets(campaign).apply {
            val imageAssets = filter { it.meta.mediaType.toMediaType() == MediaType.IMAGE }
            assertThat(
                imageAssets.map { it.meta.id.toIdString() }
            ).containsExactlyElementsOf(imageAssetIds)
        }
    }

    @Test
    fun updateTitleAssetsTest() {
        val assetId1 = grutSteps.createTitleAsset(clientInfo.clientId!!, "title1")
        val assetId2 = grutSteps.createTitleAsset(clientInfo.clientId!!, "title2")
        val assetId3 = grutSteps.createTitleAsset(clientInfo.clientId!!, "title3")

        val campaignWithAssetLinks = uacCampaign.copy(
            assetLinks = listOf(
                createCampaignContent(campaignId = uacCampaign.id, contentId = assetId1, order = 0, type = null),
                createCampaignContent(campaignId = uacCampaign.id, contentId = assetId2, order = 1, type = null, status = CampaignContentStatus.DELETED, removedAt = now()),
                createCampaignContent(campaignId = uacCampaign.id, contentId = assetId3, order = 2, type = null),
            )
        )

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            campaignWithAssetLinks, directCampaignId,
            emptyPatchInternalRequest().copy(
                titles = listOf("title4", "title5", "title3", "title2")
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )
        assertThat(result.isSuccessful)
        assertThat(result.validationResult).isNull()

        val campaign = getUacCampaign(uacCampaign.id)!!

        campaign.assetLinks!!
            .filter { it.status != CampaignContentStatus.DELETED }
            .sortedBy { it.order }.apply {

                checkSize(4)
                get(0).apply {
                    order.checkEquals(0)
                    assertThat(contentId).isNotIn(listOf(assetId1, assetId2, assetId3))
                }
                get(1).apply {
                    order.checkEquals(1)
                    assertThat(contentId).isNotIn(listOf(assetId1, assetId2, assetId3))
                }
                get(2).apply {
                    order.checkEquals(2)
                    contentId.checkEquals(assetId3)
                }
                get(3).apply {
                    order.checkEquals(3)
                    contentId.checkEquals(assetId2)
                }
            }

        campaign.assetLinks!!.filter { it.status == CampaignContentStatus.DELETED }.apply {
            checkSize(1)
            get(0).contentId.checkEquals(assetId1)
        }
    }

    @Test
    fun updateMediaAssetsTest() {
        val imageIdToKeep = grutSteps.createDefaultImageAsset(clientInfo.clientId!!)
        val imageIdToDelete = grutSteps.createDefaultImageAsset(clientInfo.clientId!!)
        val imageIdToRestore = grutSteps.createDefaultImageAsset(clientInfo.clientId!!)
        val imageIdToAdd = grutSteps.createDefaultImageAsset(clientInfo.clientId!!)

        val videoIdToKeep = grutSteps.createDefaultVideoAsset(clientInfo.clientId!!)
        val videoIdToDelete = grutSteps.createDefaultVideoAsset(clientInfo.clientId!!)
        val videoIdToRestore = grutSteps.createDefaultVideoAsset(clientInfo.clientId!!)
        val videoIdToAdd = grutSteps.createDefaultVideoAsset(clientInfo.clientId!!)

        val campaignWithAssetLinks = uacCampaign.copy(
            assetLinks = listOf(
                createCampaignContent(campaignId = uacCampaign.id, contentId = imageIdToKeep, order = 0, type = null),
                createCampaignContent(campaignId = uacCampaign.id, contentId = imageIdToDelete, order = 1, type = null),
                createCampaignContent(campaignId = uacCampaign.id, contentId = imageIdToRestore, order = 2, type = null, status = CampaignContentStatus.DELETED, removedAt = now()),

                createCampaignContent(campaignId = uacCampaign.id, contentId = videoIdToKeep, order = 0, type = null),
                createCampaignContent(campaignId = uacCampaign.id, contentId = videoIdToDelete, order = 1, type = null),
                createCampaignContent(campaignId = uacCampaign.id, contentId = videoIdToRestore, order = 2, type = null, status = CampaignContentStatus.DELETED, removedAt = now()),
            )
        )

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            campaignWithAssetLinks, directCampaignId,
            emptyPatchInternalRequest().copy(
                contentIds = listOf(
                    imageIdToKeep, imageIdToRestore, imageIdToAdd,
                    videoIdToKeep, videoIdToRestore, videoIdToAdd
                )
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )
        assertThat(result.isSuccessful)
        assertThat(result.validationResult).isNull()

        val campaign = getUacCampaign(uacCampaign.id)!!
        val assetTypeById = getAssets(campaign).associate { it.meta.id.toIdString() to it.meta.mediaType.toMediaType()!! }

        campaign.assetLinks!!
            .filter { it.status == CampaignContentStatus.CREATED && assetTypeById[it.contentId] == MediaType.VIDEO }
            .sortedBy { it.order }
            .map { it.contentId }
            .checkEquals(listOf(videoIdToKeep, videoIdToRestore, videoIdToAdd))

        campaign.assetLinks!!
            .filter { it.status == CampaignContentStatus.CREATED && assetTypeById[it.contentId] == MediaType.IMAGE }
            .sortedBy { it.order }
            .map { it.contentId }
            .checkEquals(listOf(imageIdToKeep, imageIdToRestore, imageIdToAdd))

        campaign.assetLinks!!
            .filter { it.status == CampaignContentStatus.DELETED }
            .map { it.contentId }
            .checkContainsInAnyOrder(imageIdToDelete, videoIdToDelete)
    }
}
