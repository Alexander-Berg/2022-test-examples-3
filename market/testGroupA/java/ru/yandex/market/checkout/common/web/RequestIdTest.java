package ru.yandex.market.checkout.common.web;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RequestIdTest extends AbstractPaymentTestBase {

    private static final String REQUEST_FIELD_NAME = "request_id";
    private static final Collection<String> TABLES_TO_CHECK = List.of(
            "order_history",
            "payment_history",
            "return_history",
            "refund_history"
    );

    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private ReturnService returnService;

    @Test
    public void testRecordsHaveRequestId() {
        RequestContextHolder.createNewContext();
        returnHelper.mockSupplierInfo();
        returnHelper.mockShopInfo();
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());
        returnHelper.mockActualDelivery(order.get());
        Return ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        returnService.createAndDoRefunds(ret, order());

        TABLES_TO_CHECK.forEach(tableName -> {
            long count = masterJdbcTemplate.queryForList("SELECT " + REQUEST_FIELD_NAME + " FROM " + tableName)
                    .stream().peek(map -> {
                        assertNotNull(map.get(REQUEST_FIELD_NAME), "Error for table " + REQUEST_FIELD_NAME);
                    }).count();
            assertTrue(count > 0, "Error for table " + REQUEST_FIELD_NAME);
        });
    }
}
