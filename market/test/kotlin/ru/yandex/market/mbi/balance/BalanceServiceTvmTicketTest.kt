package ru.yandex.market.mbi.balance

import org.apache.xmlrpc.XmlRpcException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.common.util.xmlrpc.XmlRPCServiceCommand
import ru.yandex.market.mbi.balance.service.Balance2XmlRPCServiceFactory
import ru.yandex.market.mbi.balance.service.BalanceServiceExecutor
import ru.yandex.market.mbi.balance.xmlrps.Balance2Operations
import ru.yandex.market.mbi.partner1p.FunctionalTest
import java.util.function.Supplier

/**
 * Тест для проверки корректной связи с балансом с использованием
 * Tvm 2. [BalanceService] использует [BalanceServiceExecutor]
 * для отправки комманд. Поэтому проверяется этот класс.
 *
 */
class BalanceServiceTvmTicketTest : FunctionalTest() {
    @Autowired
    lateinit var balance2XmlRPCServiceFactory: Balance2XmlRPCServiceFactory

    @Autowired
    lateinit var balanceServiceExecutor: BalanceServiceExecutor

    private var tvmTicketProvider: Supplier<String> = mock()

    private val command =
        XmlRPCServiceCommand { service: Balance2Operations ->
            service.GetClientContracts(-1).size
        }

    @BeforeEach
    open fun setup() {
        whenever(tvmTicketProvider.get()).thenReturn("test")
        balance2XmlRPCServiceFactory!!.setTvmTicketProvider(tvmTicketProvider)
    }

    /**
     * Тест проверяющий добавление хедера с Service Ticket при выполнении XmlRpcCommand,
     * конкретно проверяется вызов [Supplier.get] внутри метода
     * [org.apache.xmlrpc.client.XmlRpcTransport.sendRequest].
     *
     * В [BalanceTvmConfig] в качестве провайдера используется [ru.yandex.inside.passport.tvm2.Tvm2].
     */
    @Test
    fun shouldInvokeTvmTicketProvider() {
        try {
            balanceServiceExecutor!!.execCommand(command, "GetClientContracts")
        } catch (exception: XmlRpcException) {
            /*
             * Выполняется обращение к НЕ замоканному балансу, происходит создание реквеста
             * в котором вызывается требуемый метод, но сам реквест не валиден, поэтому
             * при попытке его отправить происходит исключение ошибки выполнения XmlRpcException
             */
        } finally {
            verify(tvmTicketProvider, times(1)).get()
        }
    }
}
