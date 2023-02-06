package ru.yandex.market.checkout.checkouter.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class FillRefundLastHistoryIdCommandTest extends AbstractPaymentTestBase {

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(FillRefundLastHistoryIdCommand.class);
    private static final String CLEAR_REFUND_LAST_HISTORY_ID = "update refund set last_history_id = null where id = ?";
    private static final String SELECT_ID = "select id from refund where last_history_id = null and id in (?,?,?)";

    private InMemoryAppender inMemoryAppender;
    private Level oldLevel;

    @Autowired
    private FillRefundLastHistoryIdCommand command;

    @BeforeEach
    void prepare() {
        inMemoryAppender = new InMemoryAppender();
        LOGGER.addAppender(inMemoryAppender);
        inMemoryAppender.clear();
        inMemoryAppender.start();
        oldLevel = LOGGER.getLevel();
        LOGGER.setLevel(Level.DEBUG);
    }

    @AfterEach
    public void tearDown() {
        LOGGER.detachAppender(inMemoryAppender);
        LOGGER.setLevel(oldLevel);
    }

    @Test
    void shouldFillOnlyEmptyRefundLastHistoryId() throws Exception {

        TestTerminal testTerminal = new TestTerminal(new ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream());

        long refund = createRefundedOrder();
        long refund1 = createRefundedOrder();
        long refund2 = createRefundedOrder();

        transactionTemplate.execute(ts -> masterJdbcTemplate.update(CLEAR_REFUND_LAST_HISTORY_ID, refund1));
        transactionTemplate.execute(ts -> masterJdbcTemplate.update(CLEAR_REFUND_LAST_HISTORY_ID, refund2));

        command.executeCommand(new CommandInvocation("", null, Map.of("batchSize", "5")), testTerminal);

        assertFalse(masterJdbcTemplate.queryForRowSet(SELECT_ID, refund, refund1, refund2).next());
        assertFalse(inMemoryAppender.getRaw().isEmpty());
        assertThat(
                inMemoryAppender.getRaw().get(0).getFormattedMessage(),
                containsString("Filling successful! Filled 2 rows")
        );
    }

    private long createRefundedOrder() throws Exception {
        createUnpaidBlueOrder();
        paymentHelper.payForOrder(order());
        reloadOrder();
        paymentTestHelper.clearPayment();
        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order());
        reloadOrder();
        return refundTestHelper.makeFullRefund();
    }
}
