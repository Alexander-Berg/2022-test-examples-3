package ru.yandex.market.partner.mvc.controller.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link OrdersControllerV2#getOrderItemsTransactions}
 */
class OrdersControllerV2ItemsTransactionsTest extends FunctionalTest {

    @Autowired
    private EnvironmentService environmentService;

    @Test
    @DisplayName("Детализация транзакций по заказу.")
    @DbUnitDataSet(before = "OrdersControllerV2ItemsTransactionsTest.before.csv")
    void testPaymentsDetailsTransactionsOnly() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "v2/campaigns/10245732/orders/1898847/items/transactions/details");
        JsonTestUtil.assertEquals(response, this.getClass(),
                "OrdersControllerV2ItemsTransactionsTest.json");
    }

    @Test
    @DisplayName("Нет детализация по новой схеме управлению выплатами, если отключено")
    @DbUnitDataSet(before = "OrdersControllerV2ItemsTransactionsNewFlowTest.before.csv")
    void testPaymentsDetailsTransactionNewFlowDisabled() {
        // тк environmentService = mock, то по-умолчанию он будет false присылать
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "v2/campaigns/10245732/orders/1898848/items/transactions/details");
        JsonTestUtil.assertEquals(response, this.getClass(),
                "OrdersControllerV2ItemsTransactionsNewFlowTestDisabled.json");
    }

    @Test
    @DisplayName("Детализация транзакций заказа по новой схеме управлению выплатами.")
    @DbUnitDataSet(before = "OrdersControllerV2ItemsTransactionsNewFlowTest.before.csv")
    void testPaymentsDetailsTransactionNewFlow() {
        environmentService.setValue("mbi.control.enabled.partner.report", "true");
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "v2/campaigns/10245732/orders/1898848/items/transactions/details");
        JsonTestUtil.assertEquals(response, this.getClass(),
                "OrdersControllerV2ItemsTransactionsNewFlowTest.json");
    }
}
