package ru.yandex.market.abo.bpmn

import org.mockito.Mockito.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.market.partner.notification.client.PartnerNotificationClient
import ru.yandex.market.abo.api.client.AboAPI
import ru.yandex.market.abo.bpmn.mbi.MbiModerationApiClient

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 25.03.2022
 */
@Configuration
@Profile("functionalTest")
open class TestBeansConfig {
    @Bean("moderationApiClient")
    open fun moderationApiClientMock(): MbiModerationApiClient = mock(MbiModerationApiClient::class.java)

    @Bean("aboPublicClient")
    open fun aboPublicClientMock(): AboAPI = mock(AboAPI::class.java)

    @Bean("partnerNotificationClient")
    open fun partnerNotificationClient(): PartnerNotificationClient = mock(PartnerNotificationClient::class.java)
}
