package ru.yandex.market.pvz.tms.command.migration;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.tms.command.migration.MoveFbsFlagToOrderAdditionalInfo.COMMAND_NAME;

@TransactionlessEmbeddedDbTest
@Import({
        MoveFbsFlagToOrderAdditionalInfo.class,
})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class MoveFbsFlagToOrderAdditionalInfoTest {

    private final MoveFbsFlagToOrderAdditionalInfo command;
    private final TestOrderFactory orderFactory;
    private final JdbcTemplate jdbcTemplate;

    @MockBean
    private Terminal terminal;

    @MockBean
    private PrintWriter printWriter;

    @Test
    @Disabled
    void checkCommand() {
        createOrder(null, null);
        createOrder(null, null);
        createOrder(null, null);

        createOrder(false, null);
        createOrder(false, null);

        createOrder(true, null);

        createOrder(false, false);
        createOrder(true, true);

        assertRecordsNumberForQuery("select id from order_additional_info oai where oai.fbs is null", 6);

        when(terminal.getWriter()).thenReturn(printWriter);
        command.executeCommand(new CommandInvocation(COMMAND_NAME, new String[]{}, Map.of()), terminal);

        assertRecordsNumberForQuery("select id from order_additional_info oai where oai.fbs", 2);

        assertRecordsNumberForQuery("select id from order_additional_info oai where not oai.fbs", 6);
    }

    private void createOrder(Boolean isDropShip, Boolean fbs) {
        Order order = orderFactory.createSimpleFashionOrder();
        jdbcTemplate.update("update orders set is_drop_ship = ? where id = ?",
                isDropShip, order.getId());
        jdbcTemplate.update("update order_additional_info set fbs = ? where id = ?",
                fbs, order.getId());
    }

    private void assertRecordsNumberForQuery(String sql, int expectedNumber) {
        List<Long> ids = jdbcTemplate.queryForList(sql, Long.class);
        assertThat(ids.size()).isEqualTo(expectedNumber);
    }
}
