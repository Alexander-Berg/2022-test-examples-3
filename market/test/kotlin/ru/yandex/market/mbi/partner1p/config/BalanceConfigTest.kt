package ru.yandex.market.mbi.partner1p.config

import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.market.mbi.balance.service.Balance2XmlRPCServiceFactory
import ru.yandex.market.mbi.balance.service.BalanceService
import ru.yandex.market.mbi.balance.service.BalanceServiceExecutor
import ru.yandex.market.mbi.partner1p.controller.mapper.ClientContractInfoDtoMapper

/**
 * @author lozovskii@yandex-team.ru
 */
@Profile("functionalTest")
@Configuration
open class BalanceConfigTest(
    @Value("\${balance.test.xmlrpc.url}")
    val balanceRpcUrl: String
) {

    @Bean
    open fun balanceService() = Mockito.mock(BalanceService::class.java)

    @Bean
    open fun clientContractDtoMapper() = ClientContractInfoDtoMapper()

    @Bean
    open fun balance2XmlRPCServiceFactory(): Balance2XmlRPCServiceFactory {
        val factory = Balance2XmlRPCServiceFactory()
        factory.serverUrl = balanceRpcUrl
        factory.setConnectionTimeout(1000)
        factory.setDefaultReplyTimeout(2000)
        return factory
    }

    @Bean
    open fun balanceServiceExecutor(
        balance2XmlRPCServiceFactory: Balance2XmlRPCServiceFactory,
        @Qualifier("repeatMethodCounts") repetMap: HashMap<String, Int>
    ): BalanceServiceExecutor {
        val balanceServiceExecutor = BalanceServiceExecutor()
        balanceServiceExecutor.setServiceFactory(balance2XmlRPCServiceFactory)
        balanceServiceExecutor.setRepeatMap(repetMap)
        return balanceServiceExecutor
    }
}
