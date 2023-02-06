package ru.yandex.direct.web.entity.uac.service

import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit
import java.util.Locale
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames.DO_NOT_RESTORE_REMOVED_ASSETS_IN_UAC_TGO
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.uac.createMediaCampaignContent
import ru.yandex.direct.core.entity.uac.createSitelinkCampaignContent
import ru.yandex.direct.core.entity.uac.createTextCampaignContent
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus.CREATED
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus.DELETED
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.Sitelink
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacAccountSteps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.core.testing.steps.uac.UacContentSteps
import ru.yandex.direct.feature.FeatureName.UC_MULTIPLE_ADS_ENABLED
import ru.yandex.direct.regions.Region
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.entity.uac.model.PatchCampaignInternalRequest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
open class YdbUacCampaignUpdateServiceForTextCampaignTest {

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacContentSteps: UacContentSteps

    @Autowired
    private lateinit var uacAccountSteps: UacAccountSteps

    @Autowired
    private lateinit var uacCampaignUpdateService: WebYdbUacCampaignUpdateService

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var steps: Steps

    val regions = listOf(Region.RUSSIA_REGION_ID)

    private lateinit var uacAccount: UacYdbAccount
    private lateinit var uacCampaign: UacYdbCampaign
    private var directCampaignId = 0L
    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.UAC_UPDATE_ADS)
        uacAccount = uacAccountSteps.createAccount(clientInfo)

        val uacCampaignInfo = uacCampaignSteps.createTextCampaign(
            clientInfo,
            status = Status.STARTED,
        )
        directCampaignId = uacCampaignInfo.uacDirectCampaign.directCampaignId
        uacCampaign = uacCampaignInfo.uacCampaign

        val campaignTitle2 = createTextCampaignContent(
            campaignId = uacCampaign.id,
            mediaType = MediaType.TITLE
        )
        val campaignTitle3 = createTextCampaignContent(
            campaignId = uacCampaign.id,
            mediaType = MediaType.TITLE,
            status = DELETED,
            removedAt = now()
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(campaignTitle2, campaignTitle3))

        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)

        ppcPropertiesSupport.remove(DO_NOT_RESTORE_REMOVED_ASSETS_IN_UAC_TGO)
        steps.featureSteps().addClientFeature(clientInfo.clientId, UC_MULTIPLE_ADS_ENABLED, true)
    }

    @Test
    fun updateTitlesTest() {
        val oldCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)

        val titleContentToKeep = oldCampaignContents
            .first { it.type == MediaType.TITLE && it.status == CREATED }
        val titleContentToRemove = oldCampaignContents
            .first { it.type == MediaType.TITLE && it.text != titleContentToKeep.text && it.status == CREATED }
        val titleContentToUpdate = oldCampaignContents
            .first { it.type == MediaType.TITLE && it.status != CREATED }
        val newTitle = "Новый текст 1"
        val newTitle2 = "Новый текст 2"

        val newTitles = listOf(newTitle, newTitle2, titleContentToKeep.text!!, titleContentToUpdate.text!!)

        val result = uacCampaignUpdateService.updateCampaign(
            uacCampaign, directCampaignId,
            getPatchCampaignInternalRequest(
                titles = newTitles,
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
            multipleAdsInUc = true,
        )

        val soft = SoftAssertions()
        soft.assertThat(result.isSuccessful).isTrue
        soft.assertThat(result.validationResult).isNull()

        val updatedTitleContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
            .filter { it.type == MediaType.TITLE }

        val assetIdToStatus = updatedTitleContents.associateBy({ it.id }, { it.status })
        val textToStatus = updatedTitleContents.associateBy({ it.text }, { it.status })

        soft.assertThat(assetIdToStatus)
            .`as`("Id и статусы ассетов")
            .containsEntry(titleContentToKeep.id, CREATED)
            .containsEntry(titleContentToRemove.id, DELETED)
            .containsEntry(titleContentToUpdate.id, CREATED)
        soft.assertThat(textToStatus)
            .`as`("Текст и статусы ассетов")
            .containsEntry(newTitle, CREATED)
            .containsEntry(newTitle2, CREATED)
        soft.assertThat(textToStatus.keys)
            .`as`("Текст ассетов")
            .containsOnlyElementsOf(newTitles + titleContentToRemove.text)
        soft.assertAll()
    }

    /**
     * Проверяем что при включенной проперти do_not_restore_removed_assets_in_uac текстовые ассеты не восстанавливаются
     */
    @Test
    fun updateTitlesWithoutRestoreDeleted() {
        ppcPropertiesSupport.get(DO_NOT_RESTORE_REMOVED_ASSETS_IN_UAC_TGO).set(true)

        val deletedCampaignContentText = createTextCampaignContent(
            campaignId = uacCampaign.id,
            mediaType = MediaType.TEXT,
            removedAt = now(),
            status = DELETED,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(deletedCampaignContentText))

        val oldCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
        val titleContentDeleted = oldCampaignContents
            .first { it.type == MediaType.TITLE && it.status == DELETED }
        val textContentDeleted = oldCampaignContents
            .first { it.type == MediaType.TEXT && it.status == DELETED }
        val titleContentToKeep = oldCampaignContents
            .first { it.type == MediaType.TITLE && it.status == CREATED }
        val textContentToKeep = oldCampaignContents
            .first { it.type == MediaType.TEXT && it.status == CREATED }

        val result = uacCampaignUpdateService.updateCampaign(
            uacCampaign, directCampaignId,
            getPatchCampaignInternalRequest(
                titles = listOf("new title", titleContentDeleted.text!!, titleContentToKeep.text!!),
                texts = listOf("new text", textContentDeleted.text!!, textContentToKeep.text!!),
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
            multipleAdsInUc = true,
        )

        val soft = SoftAssertions()
        soft.assertThat(result.isSuccessful).isTrue
        soft.assertThat(result.validationResult).isNull()

        val assetIdToStatusAndRemoveDate = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
            .associateBy { it.id }
            .mapValues { Pair(it.value.status, it.value.removedAt) }

        val expectSize = oldCampaignContents.size + 4

        soft.assertThat(assetIdToStatusAndRemoveDate)
            .`as`("Количество ассетов кампании")
            .hasSize(expectSize)
        soft.assertThat(assetIdToStatusAndRemoveDate)
            .`as`("Удаленные ассеты не восстановились")
            .containsEntry(textContentDeleted.id, Pair(DELETED, textContentDeleted.removedAt))
            .containsEntry(titleContentDeleted.id, Pair(DELETED, titleContentDeleted.removedAt))
        soft.assertAll()
    }

    @Test
    fun updateMediaContentTest() {
        val imageContentIdToKeep = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val imageContentIdToDelete = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val imageContentIdToRestore = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val imageContentIdToAdd = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val videoContentIdToKeep = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id
        val videoContentIdToDelete = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id

        val imageCampaignContents = listOf(
            imageContentIdToKeep,
            imageContentIdToDelete,
            imageContentIdToRestore
        ).mapIndexed { index, contentId ->
            createMediaCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.IMAGE,
                contentId = contentId,
                status = if (index == 2) DELETED else CREATED,
                removedAt = if (index == 2) now() else null,
            )
        }
        val videoCampaignContents = listOf(
            videoContentIdToKeep,
            videoContentIdToDelete,
        ).map {
            createMediaCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.VIDEO,
                contentId = it,
                status = CREATED,
            )
        }
        uacYdbCampaignContentRepository.addCampaignContents(imageCampaignContents + videoCampaignContents)

        val result = uacCampaignUpdateService.updateCampaign(
            uacCampaign, directCampaignId,
            getPatchCampaignInternalRequest(
                contentIds = listOf(
                    imageContentIdToKeep, imageContentIdToRestore, imageContentIdToAdd,
                    videoContentIdToKeep
                ),
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
            multipleAdsInUc = true,
        )

        val soft = SoftAssertions()
        soft.assertThat(result.isSuccessful).isTrue
        soft.assertThat(result.validationResult).isNull()

        val updateCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
        val actualImageContentIds = updateCampaignContents
            .filter { it.type == MediaType.IMAGE && it.removedAt == null }
            .map { it.contentId }
        val actualVideoContentIds = updateCampaignContents
            .filter { it.type == MediaType.VIDEO && it.removedAt == null }
            .map { it.contentId }

        soft.assertThat(actualImageContentIds)
            .`as`("Id image контента")
            .containsOnly(imageContentIdToKeep, imageContentIdToRestore, imageContentIdToAdd)
        soft.assertThat(actualVideoContentIds)
            .`as`("Id video контента")
            .containsOnly(videoContentIdToKeep)
        soft.assertAll()
    }

    /**
     * Проверяем что при включенной проперти do_not_restore_removed_assets_in_uac медийные ассеты не восстанавливаются
     */
    @Test
    fun updateMediaContentWithoutRestoreDeleted() {
        ppcPropertiesSupport.get(DO_NOT_RESTORE_REMOVED_ASSETS_IN_UAC_TGO).set(true)

        val imageContentIdToKeep = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val deletedImageContentIdToKeep = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val imageContentIdToDelete = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val imageContentIdToAdd = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val videoContentIdToKeep = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id
        val deletedVideoContentIdToKeep = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id
        val videoContentIdToDelete = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id

        val removeDate = now().truncatedTo(ChronoUnit.SECONDS)

        val imageCampaignContents = listOf(
            imageContentIdToKeep,
            deletedImageContentIdToKeep,
            imageContentIdToDelete,
        ).mapIndexed { index, contentId ->
            createMediaCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.IMAGE,
                contentId = contentId,
                status = if (index == 1) DELETED else CREATED,
                removedAt = if (index == 1) removeDate else null,
            )
        }
        val videoCampaignContents = listOf(
            videoContentIdToKeep,
            deletedVideoContentIdToKeep,
            videoContentIdToDelete,
        ).mapIndexed { index, contentId ->
            createMediaCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.VIDEO,
                contentId = contentId,
                status = if (index == 1) DELETED else CREATED,
                removedAt = if (index == 1) removeDate else null,
            )
        }
        uacYdbCampaignContentRepository.addCampaignContents(imageCampaignContents + videoCampaignContents)
        val assetsWithContent = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
            .filter { it.type == MediaType.VIDEO || it.type == MediaType.IMAGE }
        val deletedImageAssetId = assetsWithContent
            .filter { it.removedAt != null && it.type == MediaType.IMAGE }
            .map { it.id }
            .firstOrNull()
        val deletedVideoAssetId = assetsWithContent
            .filter { it.removedAt != null && it.type == MediaType.VIDEO }
            .map { it.id }
            .firstOrNull()

        val result = uacCampaignUpdateService.updateCampaign(
            uacCampaign, directCampaignId,
            getPatchCampaignInternalRequest(
                contentIds = listOf(
                    imageContentIdToKeep, deletedImageContentIdToKeep, imageContentIdToAdd,
                    videoContentIdToKeep, deletedVideoContentIdToKeep
                ),
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
            multipleAdsInUc = true,
        )

        val soft = SoftAssertions()
        soft.assertThat(result.isSuccessful).isTrue
        soft.assertThat(result.validationResult).isNull()

        val assetIdToStatusAndRemoveDate = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
            .filter { it.type == MediaType.VIDEO || it.type == MediaType.IMAGE }
            .associateBy { it.id }
            .mapValues { Pair(it.value.status, it.value.removedAt) }

        val expectSize = assetsWithContent.size + 3

        soft.assertThat(assetIdToStatusAndRemoveDate)
            .`as`("Количество ассетов кампании")
            .hasSize(expectSize)
        soft.assertThat(assetIdToStatusAndRemoveDate)
            .`as`("Удаленные ассеты не восстановились")
            .containsEntry(deletedImageAssetId, Pair(DELETED, removeDate))
            .containsEntry(deletedVideoAssetId, Pair(DELETED, removeDate))
        soft.assertAll()
    }

    @Test
    fun updateSitelinksTest() {
        val sitelinkToKeep = Sitelink("title1", "https://ya.ru", "description1")
        val sitelinkToDelete = Sitelink("title2", "https://google.ru", "description2")
        val sitelinkToRestore = Sitelink("title3", "https://mail.ru", "description3")
        val sitelinkToAdd = Sitelink("title4", "https://rambler.ru", "description4")

        val sitelinkContentIdToKeep = createSitelinkCampaignContent(
            uacCampaign.id,
            sitelink = sitelinkToKeep,
        )
        val sitelinkContentIdToDelete = createSitelinkCampaignContent(
            uacCampaign.id,
            sitelink = sitelinkToDelete,
        )
        val sitelinkContentIdToRestore = createSitelinkCampaignContent(
            uacCampaign.id,
            sitelink = sitelinkToRestore,
            status = DELETED,
            removedAt = now(),
        )
        uacYdbCampaignContentRepository.addCampaignContents(
            listOf(sitelinkContentIdToKeep, sitelinkContentIdToDelete, sitelinkContentIdToRestore))

        val result = uacCampaignUpdateService.updateCampaign(
            uacCampaign, directCampaignId,
            getPatchCampaignInternalRequest(
                sitelinks = listOf(sitelinkToKeep, sitelinkToRestore, sitelinkToAdd),
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
            multipleAdsInUc = true,
        )

        val soft = SoftAssertions()
        soft.assertThat(result.isSuccessful).isTrue
        soft.assertThat(result.validationResult).isNull()

        val updateCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
        val actualSitelinkContentIds = updateCampaignContents
            .filter { it.type == MediaType.SITELINK }
            .associateBy({ it.id }, { it.status })

        soft.assertThat(actualSitelinkContentIds)
            .`as`("Cайтлинки")
            .hasSize(4)
            .containsEntry(sitelinkContentIdToKeep.id, CREATED)
            .containsEntry(sitelinkContentIdToRestore.id, CREATED)
            .containsEntry(sitelinkContentIdToDelete.id, DELETED)
        soft.assertAll()
    }

    /**
     * Проверяем что при включенной проперти do_not_restore_removed_assets_in_uac сайтлинки не восстанавливаются
     */
    @Test
    fun updateSitelinksWithoutRestoreDeleted() {
        ppcPropertiesSupport.get(DO_NOT_RESTORE_REMOVED_ASSETS_IN_UAC_TGO).set(true)

        val sitelinkToKeep = Sitelink("title1", "https://ya.ru", "description1")
        val sitelinkToDelete = Sitelink("title2", "https://google.ru", "description2")
        val deletedSitelinkToKeep = Sitelink("title3", "https://mail.ru", "description3")
        val sitelinkToAdd = Sitelink("title4", "https://rambler.ru", "description4")

        val sitelinkContentIdToKeep = createSitelinkCampaignContent(
            uacCampaign.id,
            sitelink = sitelinkToKeep,
        )
        val sitelinkContentIdToDelete = createSitelinkCampaignContent(
            uacCampaign.id,
            sitelink = sitelinkToDelete,
        )
        val deletedSitelinkContentIdToKeep = createSitelinkCampaignContent(
            uacCampaign.id,
            sitelink = deletedSitelinkToKeep,
            status = DELETED,
            removedAt = now(),
        )
        uacYdbCampaignContentRepository.addCampaignContents(
            listOf(sitelinkContentIdToKeep, sitelinkContentIdToDelete, deletedSitelinkContentIdToKeep))

        val result = uacCampaignUpdateService.updateCampaign(
            uacCampaign, directCampaignId,
            getPatchCampaignInternalRequest(
                sitelinks = listOf(sitelinkToKeep, deletedSitelinkToKeep, sitelinkToAdd),
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
            multipleAdsInUc = true,
        )

        val soft = SoftAssertions()
        soft.assertThat(result.isSuccessful).isTrue
        soft.assertThat(result.validationResult).isNull()

        val updateCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
        val actualSitelinkContentIds = updateCampaignContents
            .filter { it.type == MediaType.SITELINK }
            .associateBy({ it.id }, { it.status })

        soft.assertThat(actualSitelinkContentIds)
            .`as`("Cайтлинки")
            .hasSize(5)
            .containsEntry(sitelinkContentIdToKeep.id, CREATED)
            .containsEntry(deletedSitelinkContentIdToKeep.id, DELETED)
            .containsEntry(sitelinkContentIdToDelete.id, DELETED)
        soft.assertAll()
    }

    private fun getPatchCampaignInternalRequest(
        contentIds: List<String>? = null,
        sitelinks: List<Sitelink>? = null,
        titles: List<String> = listOf("title"),
        texts: List<String> = listOf("title"),
    ): PatchCampaignInternalRequest = emptyPatchInternalRequest().copy(
        contentIds = contentIds,
        keywords = listOf("keyword"),
        regions = regions,
        sitelinks = sitelinks,
        titles = titles,
        texts = texts,
    )
}
