package ru.yandex.market.pvz.tms.command.migration;

import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.pvz.core.domain.order.OrderCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderQueryService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_1_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_1;
import static ru.yandex.market.pvz.tms.command.migration.UpdateReportPaymentSumCommand.COMMAND_NAME;

@TransactionlessEmbeddedDbTest
@Import({
        SetBillingAtForOldOrders.class
})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SetBillingAtForOldOrdersTest {

    private final JdbcTemplate jdbcTemplate;
    private final TestOrderFactory orderFactory;
    private final OrderQueryService orderQueryService;
    private final OrderCommandService orderCommandService;

    private final SetBillingAtForOldOrders command;

    @Test
    void migrate() {
        Order order = orderFactory.createSimpleFashionOrder();

        order = orderFactory.receiveOrder(order.getId());
        order = orderFactory.verifyOrder(order.getId());
        order = orderFactory.partialDeliver(order.getId(), List.of(UIT_1_1, UIT_2_1));

        orderCommandService.commitPartialDeliver(order.getId());
        orderCommandService.commitDeliver(order.getId());

        jdbcTemplate.update("update orders set billing_at=null");
        assertThat(orderQueryService.get(order.getId()).getBillingAt()).isNull();

        command.executeCommand(
                new CommandInvocation(COMMAND_NAME, new String[]{}, Collections.emptyMap()),
                mock(Terminal.class, Mockito.RETURNS_DEEP_STUBS));
        OrderParams orderParams = orderQueryService.get(order.getId());
        assertThat(orderParams.getBillingAt()).isEqualTo(orderParams.getDeliveredAt());
    }

}
