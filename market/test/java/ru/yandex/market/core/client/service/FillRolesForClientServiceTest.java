package ru.yandex.market.core.client.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.client.FillRolesForClientService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.SystemActionContext;

/**
 * Тесты для {@link FillRolesForClientService}.
 */
@DbUnitDataSet(before = "csv/FillRolesForClientServiceTest.before.csv")
public class FillRolesForClientServiceTest extends FunctionalTest {

    @Autowired
    private FillRolesForClientService fillRolesForClientService;

    @Autowired
    private ProtocolService protocolService;

    /**
     * Заполнение ролей по клиенту. Клиент используется в бизнесе
     */
    @Test
    @DbUnitDataSet(after = "csv/FillRolesForClientServiceTest.before.csv")
    void testFillRolesForClientInBusiness() {
        fillRolesForClientTransactional(111, 40001);
    }

    /**
     * Заполнение ролей по клиенту. Клиент не используется в бизнесе
     */
    @Test
    @DbUnitDataSet(after = "csv/FillRolesForClientServiceTest.FillRolesWithoutBusiness.after.csv")
    void testFillRolesForClientNotInBusiness() {
        fillRolesForClientTransactional(211, 40010);
    }

    private void fillRolesForClientTransactional(long clientId, long uid) {
        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.CREATE_CONTACT_LINK, "Filling roles for client"),
                (transactionStatus, actionId) ->
                        fillRolesForClientService.fillRolesForClient(actionId, uid, clientId)
        );
    }
}
