package ru.yandex.market.contact.sync;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.exception.ExceptionCollector;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.SystemActionContext;

/**
 * Тесты для {@link SyncClientBalanceHandler}.
 */
class SyncClientBalanceHandlerTest extends FunctionalTest {

    @Autowired
    private ContactService contactService;
    @Autowired
    private ProtocolService protocolService;
    @Autowired
    private BalanceContactService balanceContactService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private SyncClientBalanceHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SyncClientBalanceHandler(contactService, protocolService, jdbcTemplate);
    }

    @Test
    @DisplayName("Нормальная обработка без исключения")
    @DbUnitDataSet(
            before = "SyncClientBalanceHandlerTest.testProcessAllClients.before.csv",
            after = "SyncClientBalanceHandlerTest.testProcessAllClients.after.csv"
    )
    void testProcessAllClients() {
        Mockito.when(balanceContactService.getUidsByClient(10002L))
                .thenReturn(List.of(401L));

        SystemActionContext actionContext = new SystemActionContext(ActionType.TEST_ACTION);
        long actionId = protocolService.createAction(actionContext);
        SyncHandlerContext context = new SyncHandlerContext(actionId, actionContext, new ExceptionCollector());
        try (ExceptionCollector ignored = context.getExceptionCollector()) {
            handler.synchronizeContacts(context);
        }
    }

    @Test
    @DisplayName("Исключение во время обработки одного клиента не прерывает обработку остальных")
    @DbUnitDataSet(
            before = "SyncClientBalanceHandlerTest.testProcessAllClients.before.csv",
            after = "SyncClientBalanceHandlerTest.testException.after.csv"
    )
    void testException() {
        Mockito.when(balanceContactService.getUidsByClient(10001L))
                .thenThrow(new RuntimeException());

        SystemActionContext actionContext = new SystemActionContext(ActionType.TEST_ACTION);
        long actionId = protocolService.createAction(actionContext);
        SyncHandlerContext context = new SyncHandlerContext(actionId, actionContext, new ExceptionCollector());

        Assertions.assertThrows(RuntimeException.class, () -> {
            try (ExceptionCollector ignored = context.getExceptionCollector()) {
                handler.synchronizeContacts(context);
            }
        });
    }
}
