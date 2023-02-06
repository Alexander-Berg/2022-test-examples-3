package ru.yandex.direct.core.entity.uac.service

import com.nhaarman.mockitokotlin2.any
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anySet
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.entity.moderationdiag.convert
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiagData
import ru.yandex.direct.core.entity.uac.createImageCampaignContent
import ru.yandex.direct.core.entity.uac.createModerationDiagModel
import ru.yandex.direct.core.entity.uac.createTextCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbRejectReasonRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbRejectReason
import ru.yandex.direct.i18n.Language

private val LANGUAGE = Language.RU
private const val TITLE = "Некорректное упоминание сервисов Яндекса"
private const val DESCRIPTION = """
    <p>Реклама не должна создавать впечатление, 
    что ее инициатором является Яндекс или один из сервисов Яндекса.</p> 
    <p><a href=\\\"https://yandex.{tld}/{legal}/{general_adv_rules}/index.html\\\" target=\\\"_blank\\\" rel=\\\"noopener\\\">
    Требования к рекламным материалам</a></p>
    """
private const val FORMATTED_DESCRIPTION = """
    <p>Реклама не должна создавать впечатление, 
    что ее инициатором является Яндекс или один из сервисов Яндекса.</p> 
    <p><a href=\\\"https://yandex.ru/legal/general_adv_rules/index.html\\\" target=\\\"_blank\\\" rel=\\\"noopener\\\">
    Требования к рекламным материалам</a></p>
    """

class UacRejectReasonServiceTest {
    @Mock
    lateinit var uacYdbRejectReasonRepository: UacYdbRejectReasonRepository
    @InjectMocks
    lateinit var uacRejectReasonService: UacRejectReasonService

    @Before
    fun init() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun getRejectReasons_noReasons() {
        val textContent = createTextCampaignContent()
        val imageContent = createImageCampaignContent()

        val rejectReasonsById = uacRejectReasonService.getRejectReasons(listOf(textContent, imageContent), LANGUAGE)
        assertThat(rejectReasonsById).containsAllEntriesOf(mapOf(
                textContent.id to null,
                imageContent.id to null
        ))
    }

    @Test
    fun getRejectReasons_noYdbReasons() {
        val moderationDiagData = convert(createModerationDiagModel())
        val textContent = createTextCampaignContent(rejectReasons = listOf(moderationDiagData))
        val imageContent = createImageCampaignContent()

        val rejectReasonsById = uacRejectReasonService.getRejectReasons(listOf(textContent, imageContent), LANGUAGE)
        assertThat(rejectReasonsById).containsAllEntriesOf(mapOf(
                textContent.id to listOf(moderationDiagData),
                imageContent.id to null
        ))
    }

    @Test
    fun getRejectReasons_withYdbReasons() {
        val moderationDiagData = convert(createModerationDiagModel())
        val textContent = createTextCampaignContent(rejectReasons = listOf(moderationDiagData))
        val imageContent = createImageCampaignContent()

        `when`(uacYdbRejectReasonRepository.getRejectReasons(anySet(), any()))
                .thenReturn(listOf(UacYdbRejectReason(
                    id = "123",
                    diagId = moderationDiagData.diagId!!.toInt(),
                    title = TITLE,
                    description = DESCRIPTION,
                    lang = LANGUAGE.langString,
                )))

        val rejectReasonsById = uacRejectReasonService.getRejectReasons(listOf(textContent, imageContent), LANGUAGE)
        val expectedRejectReasonsById = mapOf(
                textContent.id to listOf(ModerationDiagData(
                        showDetailsUrl = moderationDiagData.showDetailsUrl,
                        diagId = moderationDiagData.diagId,
                        badReason = moderationDiagData.badReason,
                        unbanIsProhibited = moderationDiagData.unbanIsProhibited,
                        shortText = TITLE,
                        token = moderationDiagData.token,
                        diagText = FORMATTED_DESCRIPTION,
                        allowFirstAid = moderationDiagData.allowFirstAid,
                )),
                imageContent.id to null
        )
        assertThat(rejectReasonsById).containsAllEntriesOf(expectedRejectReasonsById)
    }
}
