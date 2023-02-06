package ru.yandex.market.logistics.yard_v2.domain.service.action

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard_v2.domain.dto.action.ActionProcessingResult
import ru.yandex.market.logistics.yard_v2.domain.entity.ActionEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.AudioNotificationEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.EntityParam
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClient
import ru.yandex.market.logistics.yard_v2.domain.service.AudioSynthesisService
import ru.yandex.market.logistics.yard_v2.facade.ClientFacade
import ru.yandex.market.logistics.yard_v2.repository.mapper.AudioNotificationMapper

class GenerateAudioNotificationActionUnitTest : SoftAssertionSupport() {

    private var clientFacade: ClientFacade? = null
    private var audioSynthesisService: AudioSynthesisService? = null
    private var audioNotificationMapper: AudioNotificationMapper? = null

    @BeforeEach
    fun init() {
        clientFacade = Mockito.mock(ClientFacade::class.java)
        audioSynthesisService = Mockito.mock(AudioSynthesisService::class.java)
        audioNotificationMapper = Mockito.mock(AudioNotificationMapper::class.java)
    }

    @Test
    fun generateSuccessful() {
        assertLogic(
            "base64", "{" +
                "\"key1\": \"value1\"," +
                "\"key2\": \"value2\"," +
                "\"ticketCode\": \"Р001\"," +
                "\"window\": \"7\"" +
                "}"
        )
        verify(audioSynthesisService!!).synthesize("Талон с номером Р.001 приглашается к окну 7")
        verify(audioNotificationMapper!!).persist(AudioNotificationEntity(clientId = 1, file = "base64"))
    }

    @Test
    fun generationFailedDueToServiceEmptyResponse() {
        assertLogic(
            "", "{" +
                "\"key1\": \"value1\"," +
                "\"key2\": \"value2\"," +
                "\"ticketCode\": \"Р001\"," +
                "\"window\": \"7\"" +
                "}"
        )
        verify(audioNotificationMapper!!, never()).persist(any())
    }

    @Test
    fun generationFailedDueToMissingMetaParams() {
        assertLogic("base64", "")
        verify(audioNotificationMapper!!, never()).persist(any())
    }

    private fun assertLogic(base64Audio: String, meta: String) {
        Mockito.`when`(clientFacade!!.getByIdOrThrow(1)).thenReturn(
            YardClient(
                id = 1,
                meta = ObjectMapper().readTree(
                    meta
                )
            )
        )
        Mockito.`when`(audioSynthesisService!!.synthesize(any())).thenReturn(base64Audio)

        val result = GenerateAudioNotificationAction(clientFacade!!, audioSynthesisService!!, audioNotificationMapper!!)
            .run(
                1, ActionEntity(
                    params = mutableListOf(
                        EntityParam(
                            "AUDIO_NOTIFICATION_TEMPLATE",
                            "Талон с номером [ticketCode] приглашается к окну [window]"
                        )
                    )
                )
            )

        softly.assertThat(result).isEqualTo(ActionProcessingResult.success())
    }
}
