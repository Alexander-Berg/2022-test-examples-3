package ru.yandex.market.core.balance;

import java.util.Collections;
import java.util.function.Supplier;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.xmlrpc.XmlRPCServiceCommand;
import ru.yandex.market.common.balance.xmlrpc.Balance2Operations;
import ru.yandex.market.common.balance.xmlrpc.Balance2XmlRPCServiceFactory;
import ru.yandex.market.core.FunctionalTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
/**
 *  Тест для проверки корректной связи с балансом с использованием
 *  Tvm 2. {@link BalanceService} использует {@link BalanceServiceExecutor}
 *  для отправки комманд. Поэтому проверяется этот класс.
 *
 */
public class BalanceServiceTvmTicketTest extends FunctionalTest {

    @Autowired
    private Balance2XmlRPCServiceFactory balance2XmlRPCServiceFactory;

    @Autowired
    private BalanceServiceExecutor balanceServiceExecutor;

    @Mock
    private Supplier<String> tvmTicketProvider;

    private final XmlRPCServiceCommand<Balance2Operations, Integer> command =
            service -> service.GetClientByIdBatch(Collections.emptyList()).size();

    @BeforeEach
    void initMocks() {
        tvmTicketProvider = mock(Supplier.class);
        when(tvmTicketProvider.get()).thenReturn("test");
        balance2XmlRPCServiceFactory.setTvmTicketProvider(tvmTicketProvider);
    }

    /**
     * Тест проверяющий добавление хедера с Service Ticket при выполнении XmlRpcCommand,
     * конкретно проверяется вызов {@link Supplier#get()} внутри метода
     * {@link org.apache.xmlrpc.client.XmlRpcTransport#sendRequest(XmlRpcRequest)}.
     *
     * В {@link BalanceTvmConfig} в качестве провайдера используется {@link ru.yandex.inside.passport.tvm2.Tvm2}.
     */
    @Test
    void shouldInvokeTvmTicketProvider() {
        try {
            balanceServiceExecutor.execCommand(command, "FindClient");
        } catch (XmlRpcException exception) {
            /*
             * Выполняется обращение к НЕ замоканному балансу, происходит создание реквеста
             * в котором вызывается требуемый метод, но сам реквест не валиден, поэтому
             * при попытке его отправить происходит исключение ошибки выполнения XmlRpcException
             */
        } finally {
            verify(tvmTicketProvider, times(1)).get();
        }
    }
}
