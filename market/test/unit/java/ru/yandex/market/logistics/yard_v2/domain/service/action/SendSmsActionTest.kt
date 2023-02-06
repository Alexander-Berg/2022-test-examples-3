package ru.yandex.market.logistics.yard_v2.domain.service.action

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard_v2.config.sms.PassportSmsService
import ru.yandex.market.logistics.yard_v2.domain.dto.action.ActionProcessingResult
import ru.yandex.market.logistics.yard_v2.domain.entity.ActionEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.EntityParam
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClient
import ru.yandex.market.logistics.yard_v2.facade.ClientFacade

internal class SendSmsActionTest : SoftAssertionSupport() {

    @Test
    fun run() {

        val smsService: PassportSmsService = Mockito.mock(PassportSmsService::class.java)
        val clientFacade: ClientFacade = Mockito.mock(ClientFacade::class.java)

        val phone = "123456"
        val ticket = "Р001"
        Mockito.`when`(clientFacade.getByIdOrThrow(1)).thenReturn(
            YardClient(
                id = 1,
                phone= phone,
                meta = ObjectMapper().readTree(
                    "{" +
                        "\"ticket\": \"$ticket\"," +
                        "\"window\": \"7\"" +
                        "}"
                )
            )
        )

        val result = SendSmsAction(smsService, clientFacade)
            .run(
                1, ActionEntity(
                    params = mutableListOf(
                        EntityParam(
                            "SMS_NOTIFICATION_TEMPLATE",
                            "Ваш номер талона [ticket]."
                        )
                    )
                )
            )
        Mockito.verify(smsService, Mockito.times(1))
            .sendToPhone(phone, "Ваш номер талона $ticket.")

        softly.assertThat(result).isEqualTo(ActionProcessingResult.success())
    }
}
