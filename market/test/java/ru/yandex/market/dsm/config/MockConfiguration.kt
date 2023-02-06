package ru.yandex.market.dsm.config

import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import ru.yandex.kikimr.persqueue.producer.AsyncProducer
import ru.yandex.market.dsm.balance.service.BalanceService
import ru.yandex.market.dsm.balance.service.ExternalBalanceService
import ru.yandex.market.dsm.balance.xmlrpc.model.ClientStructure
import ru.yandex.market.dsm.balance.xmlrpc.model.LinkIntegrationToClientStructure
import ru.yandex.market.dsm.balance.xmlrpc.model.OfferStructure
import ru.yandex.market.dsm.balance.xmlrpc.model.PersonStructure
import ru.yandex.market.dsm.external.CheckSelfemployedStatusFakeClient
import ru.yandex.market.dsm.external.pro.ProProfileIntegrationService
import ru.yandex.market.dsm.external.tracker.TrackerSessionFactory
import ru.yandex.market.tms.quartz2.util.QrtzLogTableCleaner
import ru.yandex.market.tpl.common.logbroker.producer.LogbrokerProducerFactory
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient
import ru.yandex.passport.tvmauth.TvmClient
import ru.yandex.startrek.client.Issues
import ru.yandex.startrek.client.Session
import ru.yandex.startrek.client.Transitions
import java.util.concurrent.CompletableFuture

@Configuration
class MockConfiguration {

    @Primary
    @Bean
    fun mockedProProfileIntegrationService() = mock(ProProfileIntegrationService::class.java)

    @ConditionalOnProperty(name = ["quartzStubsEnabled"])
    @Bean
    fun mockedQrtzLogTableCleaner() = mock(QrtzLogTableCleaner::class.java)

    @Primary
    @Bean
    fun mockedTvmClient() = mock(TvmClient::class.java)

    @Primary
    @Bean
    fun mockedLogbrokerProducerFactory(mockedAsyncProducer: AsyncProducer): LogbrokerProducerFactory {
        val mockedLogbrokerProducerFactory = mock(
            LogbrokerProducerFactory::class.java
        )
        Mockito.`when`(mockedLogbrokerProducerFactory.createProducer(ArgumentMatchers.any()))
            .thenReturn(mockedAsyncProducer)
        Mockito.`when`(mockedLogbrokerProducerFactory.createProducerSupportMultiThreadByHost(ArgumentMatchers.any()))
            .thenReturn(mockedAsyncProducer)
        return mockedLogbrokerProducerFactory
    }

    @Primary
    @Bean("mockBlackboxClient")
    fun blackboxClient() = mock(BlackboxClient::class.java)

    @Primary
    @Bean
    fun trackerIssuesMock() = mock(Issues::class.java)

    @Primary
    @Bean
    fun trackerTransitionsMock() = mock(Transitions::class.java)

    @Primary
    @Bean
    fun trackerSessionMock(trackerIssues: Issues, trackerTransitions: Transitions): Session {
        val trackerSession = mock(Session::class.java)
        Mockito.`when`(trackerSession.issues()).thenReturn(trackerIssues)
        Mockito.`when`(trackerSession.transitions()).thenReturn(trackerTransitions)
        return trackerSession
    }

    @Primary
    @Bean
    fun trackerSessionFactoryMock(trackerSession: Session): TrackerSessionFactory {
        val mockedTrackerSessionFactory = mock(
            TrackerSessionFactory::class.java
        )
        Mockito.`when`(mockedTrackerSessionFactory.createForSelfemployed()).thenReturn(trackerSession)
        return mockedTrackerSessionFactory;
    }

    @Bean
    @Primary
    fun checkSelfemployedStatusFakeClientMock() = mock(CheckSelfemployedStatusFakeClient::class.java)

    @Bean
    fun mockedAsyncProducer(): AsyncProducer {
        val mock = mock(AsyncProducer::class.java)
        Mockito.`when`(mock.write(ArgumentMatchers.any())).thenReturn(CompletableFuture.completedFuture(null))
        return mock
    }

    @Bean
    fun externalBalanceService(): BalanceService {
        val mock = mock(ExternalBalanceService::class.java)
        Mockito.`when`(
            mock.createClient(
                Mockito.any<Long>(Long::class.java),
                Mockito.any<ClientStructure>(ClientStructure::class.java)
            )
        )
            .thenReturn(1L)
        Mockito.`when`(
            mock.createPerson(
                Mockito.any<Long>(Long::class.java),
                Mockito.any<PersonStructure>(PersonStructure::class.java)
            )
        )
            .thenReturn(1)
        Mockito.`when`(
            mock.createOffer(
                Mockito.any<Long>(Long::class.java),
                Mockito.any<OfferStructure>(OfferStructure::class.java)
            )
        )
            .thenReturn(1)
        Mockito.`when`(
            mock.linkIntegrationToClient(
                Mockito.any<Long>(Long::class.java),
                Mockito.any<LinkIntegrationToClientStructure>(LinkIntegrationToClientStructure::class.java)
            )
        )
            .thenReturn(0L)
        return mock
    }
}
