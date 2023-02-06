package ru.yandex.market.integration.npd.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.integration.npd.service.dto.AlreadyBoundError
import ru.yandex.market.integration.npd.service.dto.Arg
import ru.yandex.market.integration.npd.service.dto.Message
import ru.yandex.market.integration.npd.service.dto.SmzPlatformError

class FnsErrorsParserTest {

    @Test
    fun testAlreadyBound() {
        val xml = "" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "\t<soap:Body>\n" +
            "\t\t<GetMessageResponse xmlns=\"urn://x-artefacts-gnivc-ru/inplat/servin/OpenApiAsyncMessageConsumerService/types/1.0\">\n" +
            "\t\t\t<ProcessingStatus>COMPLETED</ProcessingStatus>\n" +
            "\t\t\t<Message>\n" +
            "\t\t\t\t<SmzPlatformError xmlns=\"urn://x-artefacts-gnivc-ru/ais3/SMZ/SmzPartnersIntegrationService/types/1.0\" xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns2=\"urn://x-artefacts-gnivc-ru/ais3/SMZ/SmzPartnersValidationService/types/1.0\">\n" +
            "\t\t\t\t\t<Code>TAXPAYER_ALREADY_BOUND</Code>\n" +
            "\t\t\t\t\t<Message>Налогоплательщик с ИНН 782800030897 уже привязан к партнеру</Message>\n" +
            "\t\t\t\t\t<Args>\n" +
            "\t\t\t\t\t\t<Key>INN</Key>\n" +
            "\t\t\t\t\t\t<Value>782800030897</Value>\n" +
            "\t\t\t\t\t</Args>\n" +
            "\t\t\t\t</SmzPlatformError>\n" +
            "\t\t\t</Message>\n" +
            "\t\t</GetMessageResponse>\n" +
            "\t</soap:Body>\n" +
            "</soap:Envelope>"
        val error = FnsErrorsParser.parseAlreadyBoundError(xml)
        Assertions.assertEquals(
            AlreadyBoundError(
                "COMPLETED",
                Message(
                    SmzPlatformError(
                        "TAXPAYER_ALREADY_BOUND",
                        "Налогоплательщик с ИНН 782800030897 уже привязан к партнеру",
                        Arg("INN", "782800030897")
                    )
                )
            ),
            error
        )
    }
}
