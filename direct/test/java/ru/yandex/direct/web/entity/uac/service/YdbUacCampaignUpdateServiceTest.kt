package ru.yandex.direct.web.entity.uac.service

import java.time.LocalDateTime.now
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.createMediaCampaignContent
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.uac.UacAccountSteps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.core.testing.steps.uac.UacContentSteps
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotEquals
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class YdbUacCampaignUpdateServiceTest : UacCampaignUpdateServiceTestBase() {

    @Autowired
    override lateinit var uacCampaignUpdateServiceWeb: WebYdbUacCampaignUpdateService

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacContentSteps: UacContentSteps

    @Autowired
    private lateinit var uacAccountSteps: UacAccountSteps

    @Autowired
    private lateinit var uacYdbDirectCampaignRepository: UacYdbDirectCampaignRepository

    private lateinit var uacAccount: UacYdbAccount

    override fun getUacCampaign(id: String) = uacYdbCampaignRepository.getCampaign(id)

    override fun createUacCampaign(clientInfo: ClientInfo, draft: Boolean, impressionUrl: String?, briefSynced: Boolean?) {
        val uacCampaignInfo = uacCampaignSteps.createMobileAppCampaign(clientInfo, draft = draft, impressionUrl = impressionUrl, briefSynced = briefSynced)
        uacCampaign = uacCampaignInfo.uacCampaign
        directCampaignId = uacCampaignInfo.uacDirectCampaign.directCampaignId
    }

    @Before
    fun ydbBefore() {
        uacAccount = uacAccountSteps.createAccount(clientInfo)
        createUacCampaign(clientInfo, draft = false, briefSynced = null)
    }

    @Test
    fun updateAppIdTest() {
        val uacAppInfo = defaultAppInfo()
        uacYdbAppInfoRepository.saveAppInfo(uacAppInfo)
        val storeUrl = googlePlayAppInfoGetter.appPageUrl(uacAppInfo.appId, uacAppInfo.region, uacAppInfo.language)
        steps.mobileAppSteps().createMobileApp(clientInfo, storeUrl)

        createUacCampaign(clientInfo = clientInfo, draft = true)

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                appId = uacAppInfo.id
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        val newDirectCampaignId =
            uacYdbDirectCampaignRepository.getDirectCampaignById(uacCampaign.id)!!.directCampaignId
        val oldDirectCampaignId = directCampaignId

        newDirectCampaignId.checkNotEquals(oldDirectCampaignId)
        rmpCampaignService.getMobileContentCampaign(clientInfo.clientId!!, oldDirectCampaignId)!!.statusEmpty
            .checkEquals(true)
        rmpCampaignService.getMobileContentCampaign(clientInfo.clientId!!, newDirectCampaignId)!!.statusEmpty
            .checkEquals(false)
    }

    @Test
    fun updateTextsTest() {
        val newTexts = listOf("Новый текст 1", "Новый текст 2", "Новый текст 3")

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                texts = newTexts
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
            .filter { it.type == MediaType.TEXT }
            .filter { it.status == CampaignContentStatus.CREATED }
            .sortedBy { it.order }
            .map { it.text }
            .checkEquals(newTexts)
    }

    @Test
    fun updateImageContentsTest() {
        val imageContentIds = listOf(uacContentSteps.createImageContent(clientInfo, uacAccount.id)).map { it.id }

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                contentIds = imageContentIds
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
            .filter { it.type == MediaType.IMAGE }
            .filter { it.status == CampaignContentStatus.CREATED }
            .map { it.contentId }
            .checkEquals(imageContentIds)
    }

    @Test
    fun updateTitlesTest() {
        val oldCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
        val titleContentToKeep = oldCampaignContents
            .first { it.type == MediaType.TITLE && it.status == CampaignContentStatus.CREATED }
        val titleContentToRemove = oldCampaignContents
            .first { it.type == MediaType.TITLE && it.text != titleContentToKeep.text && it.status == CampaignContentStatus.CREATED }
        val titleContentToUpdate = oldCampaignContents
            .first { it.type == MediaType.TITLE && it.status != CampaignContentStatus.CREATED }
        val newTitle = "Новый текст 1"
        val newTitle2 = "Новый текст 2"

        val newTitles = listOf(newTitle, newTitle2, titleContentToKeep.text!!, titleContentToUpdate.text!!)

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                titles = newTitles
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        val updatedTitleContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
            .filter { it.type == MediaType.TITLE }

        updatedTitleContents
            .filter { it.status == CampaignContentStatus.CREATED }
            .sortedBy { it.order }
            .map { it.text }
            .checkEquals(newTitles)
        updatedTitleContents.find { it.id == titleContentToKeep.id }!!.status
            .checkEquals(CampaignContentStatus.CREATED)
        updatedTitleContents.find { it.id == titleContentToRemove.id }!!.status
            .checkEquals(CampaignContentStatus.DELETED)
        updatedTitleContents.find { it.id == titleContentToUpdate.id }!!.status
            .checkEquals(CampaignContentStatus.CREATED)
        updatedTitleContents.find { it.text == newTitle }!!.status
            .checkEquals(CampaignContentStatus.CREATED)
        updatedTitleContents.find { it.text == newTitle2 }!!.status
            .checkEquals(CampaignContentStatus.CREATED)
    }

    @Test
    fun updateTitlesOrderTest() {
        val oldCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)

        val newTitles = oldCampaignContents
            .filter { it.type == MediaType.TITLE }
            .filter { it.status == CampaignContentStatus.CREATED }
            .sortedByDescending { it.order }
            .map { it.text!! }

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                titles = newTitles
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
            .filter { it.type == MediaType.TITLE }
            .filter { it.status == CampaignContentStatus.CREATED }
            .sortedBy { it.order }
            .map { it.text }
            .checkEquals(newTitles)
    }

    @Test
    fun updateSitelinksOrderTest() {
        val oldCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)

        val newSitelinks = oldCampaignContents
            .filter { it.type == MediaType.SITELINK }
            .filter { it.status == CampaignContentStatus.CREATED }
            .sortedByDescending { it.order }
            .map { it.sitelink!! }

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                sitelinks = newSitelinks
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
            .filter { it.type == MediaType.SITELINK }
            .filter { it.status == CampaignContentStatus.CREATED }
            .sortedBy { it.order }
            .map { it.sitelink }
            .checkEquals(newSitelinks)
    }

    @Test
    fun updateMediaOrderTest() {
        val oldMediaContentIds = listOf(
            uacContentSteps.createImageContent(clientInfo, uacAccount.id),
            uacContentSteps.createVideoContent(clientInfo, uacAccount.id),
            uacContentSteps.createImageContent(clientInfo, uacAccount.id),
            uacContentSteps.createImageContent(clientInfo, uacAccount.id)
        ).map { it.id }

        uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                contentIds = oldMediaContentIds
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        val oldCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)

        val newMediaContentIds = oldCampaignContents
            .filter { it.contentId != null }
            .sortedByDescending { it.order }
            .map { it.contentId!! }

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                contentIds = newMediaContentIds
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
            .filter { it.contentId != null}
            .filter { it.status == CampaignContentStatus.CREATED }
            .sortedBy { it.order }
            .map { it.contentId }
            .checkEquals(newMediaContentIds)
    }

    @Test
    fun updateMediaContentTest() {
        val imageContentIdToKeep = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val imageContentIdToDelete = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val imageContentIdToRestore = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val imageContentIdToAdd = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val videoContentIdToKeep = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id
        val videoContentIdToDelete = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id
        val videoContentIdToRestore = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id
        val videoContentIdToAdd = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id

        val imageCampaignContents = listOf(
            imageContentIdToKeep,
            imageContentIdToDelete,
            imageContentIdToRestore
        ).mapIndexed { index, contentId ->
            createMediaCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.IMAGE,
                order = index,
                contentId = contentId,
                status = if (index == 2) {
                    CampaignContentStatus.DELETED
                } else {
                    CampaignContentStatus.CREATED
                },
                removedAt = if (index == 2) {
                    now()
                } else {
                    null
                },
            )
        }
        val videoCampaignContents = listOf(
            videoContentIdToKeep,
            videoContentIdToDelete,
            videoContentIdToRestore
        ).mapIndexed { index, contentId ->
            createMediaCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.VIDEO,
                order = index,
                contentId = contentId,
                status = if (index == 2) {
                    CampaignContentStatus.DELETED
                } else {
                    CampaignContentStatus.CREATED
                },
                removedAt = if (index == 2) {
                    now()
                } else {
                    null
                },
            )
        }
        uacYdbCampaignContentRepository.addCampaignContents(imageCampaignContents + videoCampaignContents)

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                contentIds = listOf(
                    imageContentIdToKeep, imageContentIdToRestore, imageContentIdToAdd,
                    videoContentIdToKeep, videoContentIdToRestore, videoContentIdToAdd
                )
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        val updateCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaign.id)
        updateCampaignContents
            .filter { it.type == MediaType.IMAGE }
            .filter { it.status == CampaignContentStatus.CREATED }
            .sortedBy { it.order }
            .map { it.contentId }
            .checkEquals(listOf(imageContentIdToKeep, imageContentIdToRestore, imageContentIdToAdd))
        updateCampaignContents
            .filter { it.type == MediaType.VIDEO }
            .filter { it.status == CampaignContentStatus.CREATED }
            .sortedBy { it.order }
            .map { it.contentId }
            .checkEquals(listOf(videoContentIdToKeep, videoContentIdToRestore, videoContentIdToAdd))
    }
}
