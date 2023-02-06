package ru.yandex.direct.web.entity.uac.repository

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.uac.createImageCampaignContent
import ru.yandex.direct.core.entity.uac.createSitelinkCampaignContent
import ru.yandex.direct.core.entity.uac.createTextCampaignContent
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.generateUniqueRandomId
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.web.configuration.DirectWebTest
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacYdbCampaignContentRepositoryTest : AbstractUacRepositoryTest() {

    @Autowired
    private lateinit var campaignContentRepository: UacYdbCampaignContentRepository

    private lateinit var campaignId: String
    private lateinit var textCampaignContent: UacYdbCampaignContent
    private lateinit var imageCampaignContent: UacYdbCampaignContent
    private lateinit var sitelinkCampaignContent: UacYdbCampaignContent

    @Before
    fun before() {
        campaignId = generateUniqueRandomId()
        textCampaignContent = createTextCampaignContent(campaignId)
        imageCampaignContent = createImageCampaignContent(campaignId)
        sitelinkCampaignContent = createSitelinkCampaignContent(campaignId)

        campaignContentRepository.addCampaignContents(
            listOf(
                textCampaignContent,
                imageCampaignContent,
                sitelinkCampaignContent
            )
        )
    }

    @Test
    fun getCampaignContentsTest() {
        val actualContents = campaignContentRepository.getCampaignContents(campaignId)
        assertThat(actualContents).containsExactlyInAnyOrder(
            textCampaignContent,
            imageCampaignContent,
            sitelinkCampaignContent
        )
    }

    @Test
    fun deleteByCampaignIdTest() {
        campaignContentRepository.deleteByCampaignId(campaignId)

        val actualContents = campaignContentRepository.getCampaignContents(campaignId)
        assertThat(actualContents).isEmpty()
    }

    @Test
    fun deleteTextTest() {
        campaignContentRepository.delete(listOf(textCampaignContent.id))

        val actualContents = campaignContentRepository.getCampaignContents(campaignId)
        assertThat(actualContents).containsExactlyInAnyOrder(imageCampaignContent, sitelinkCampaignContent)
    }

    @Test
    fun deleteImageTest() {
        campaignContentRepository.delete(listOf(imageCampaignContent.id))

        val actualContents = campaignContentRepository.getCampaignContents(campaignId)
        assertThat(actualContents).containsExactlyInAnyOrder(textCampaignContent, sitelinkCampaignContent)
    }

    @Test
    fun deleteSitelinkTest() {
        campaignContentRepository.delete(listOf(sitelinkCampaignContent.id))

        val actualContents = campaignContentRepository.getCampaignContents(campaignId)
        assertThat(actualContents).containsExactlyInAnyOrder(textCampaignContent, imageCampaignContent)
    }

    fun provideContents() = arrayOf(
        arrayOf(
            "text default content",
            createTextCampaignContent(order = 1)
        ),
        arrayOf(
            "text content with createdAt",
            createTextCampaignContent(
                createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) - Duration.ofHours(1)
            )
        ),
        arrayOf(
            "text content with removedAt",
            createTextCampaignContent(removedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        ),
        arrayOf(
            "text content with status",
            createTextCampaignContent(status = CampaignContentStatus.REJECTED)
        ),
        arrayOf(
            "text content with rejectReasons",
            createTextCampaignContent(rejectReasons = listOf())
        ),
        arrayOf(
            "text content with order",
            createImageCampaignContent(order = 2)
        ),
        arrayOf(
            "image content with createdAt",
            createImageCampaignContent(
                createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) - Duration.ofHours(1)
            )
        ),
        arrayOf(
            "image content with removedAt",
            createImageCampaignContent(removedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        ),
        arrayOf(
            "image content with status",
            createImageCampaignContent(status = CampaignContentStatus.REJECTED)
        ),
        arrayOf(
            "image content with rejecteReasons",
            createImageCampaignContent(rejectReasons = listOf())
        ),
        arrayOf(
            "sitelink default content",
            createSitelinkCampaignContent()
        ),
    )

    @Test
    @TestCaseName("saveAndGetTest({0})")
    @Parameters(method = "provideContents")
    fun saveAndGetTest(caseName: String, campaignContent: UacYdbCampaignContent) {
        campaignContentRepository.addCampaignContents(listOf(campaignContent))

        val actualContent = campaignContentRepository.getCampaignContents(campaignContent.campaignId)
        assertThat(actualContent).containsExactly(campaignContent)
    }
}
