package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.OrderCheckpointDao;
import ru.yandex.market.core.billing.matchers.OrderCheckpointMatcher;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.core.order.model.CheckpointType;
import ru.yandex.market.core.order.model.OrderCheckpoint;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.shop.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link ImportOrderCheckpointsCommand}
 */
@DbUnitDataSet(before = "ImportOrderCheckpointsCommandTest.before.csv")
class ImportOrderCheckpointsCommandTest extends FunctionalTest {

    private final Terminal terminal = mock(Terminal.class);

    private final NamedParameterJdbcTemplate ytJdbcTemplate = mock(NamedParameterJdbcTemplate.class);

    @Autowired
    OrderCheckpointDao orderCheckpointDao;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    OrderService orderService;

    private ImportOrderCheckpointsCommand importOrderCheckpointsCommand;

    @BeforeEach
    void setUp() {
        reset(terminal);
        when(terminal.getWriter()).thenReturn(mock(PrintWriter.class));

        reset(ytJdbcTemplate);

        importOrderCheckpointsCommand = new ImportOrderCheckpointsCommand(
                null,
                orderCheckpointDao,
                namedParameterJdbcTemplate,
                ytJdbcTemplate,
                transactionTemplate,
                orderService);
    }

    @Test
    void testFromDateImport() {
        mockYtData().thenReturn(
                List.of(
                        OrderCheckpoint.of(
                                CheckpointType.SORTING_CENTER_RETURN_ARRIVED,
                                LocalDateTime.of(2020, 6, 28, 12, 0),
                                1,
                                11,
                                PartnerType.SORTING_CENTER,
                                131L
                        ),
                        OrderCheckpoint.of(
                                CheckpointType.SORTING_CENTER_RETURN_PREPARING_SENDER,
                                LocalDateTime.of(2020, 6, 26, 10, 0),
                                1,
                                12,
                                PartnerType.SORTING_CENTER,
                                131L
                        ),
                        OrderCheckpoint.of(
                                CheckpointType.LOST,
                                LocalDateTime.of(2020, 6, 25, 10, 0),
                                1,
                                13,
                                PartnerType.SORTING_CENTER,
                                131L
                        ),
                        OrderCheckpoint.of(
                                CheckpointType.ORDER_SORTING_CENTER_RETURN_PREPARED_FOR_UTILIZE,
                                LocalDateTime.of(2020, 6, 25, 10, 0),
                                1,
                                14,
                                PartnerType.SORTING_CENTER,
                                131L
                        )
                )
        );

        importOrderCheckpointsCommand.executeCommand(commandFromDate(LocalDate.of(2020, 6, 25)), terminal);

        List<OrderCheckpoint> checkpoints = orderCheckpointDao.getOrderCheckpoints(1);
        Assertions.assertEquals(4, checkpoints.size());
        assertThat(
                checkpoints,
                containsInAnyOrder(
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(1),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.SORTING_CENTER_RETURN_ARRIVED),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 28, 12, 0)),
                                        OrderCheckpointMatcher.hasId(11),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.SORTING_CENTER),
                                        OrderCheckpointMatcher.hasPartnerId(131L)
                                )).build(),
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(1),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.SORTING_CENTER_RETURN_PREPARING_SENDER),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 26, 10, 0)),
                                        OrderCheckpointMatcher.hasId(12),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.SORTING_CENTER),
                                        OrderCheckpointMatcher.hasPartnerId(131L)
                                )).build(),
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(1),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.LOST),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 25, 10, 0)),
                                        OrderCheckpointMatcher.hasId(13),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.SORTING_CENTER),
                                        OrderCheckpointMatcher.hasPartnerId(131L)
                                )).build(),
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(1),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.ORDER_SORTING_CENTER_RETURN_PREPARED_FOR_UTILIZE),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 25, 10, 0)),
                                        OrderCheckpointMatcher.hasId(14),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.SORTING_CENTER),
                                        OrderCheckpointMatcher.hasPartnerId(131L)
                                )).build()
                )
        );
    }

    @Test
    void testFromImport() {
        mockYtData().thenReturn(
                List.of(
                        OrderCheckpoint.of(
                                CheckpointType.SORTING_CENTER_RETURN_ARRIVED,
                                LocalDateTime.of(2020, 6, 28, 12, 0),
                                2,
                                20,
                                PartnerType.SORTING_CENTER,
                                131L
                        ),
                        OrderCheckpoint.of(
                                CheckpointType.LOST,
                                LocalDateTime.of(2020, 6, 27, 12, 0),
                                2,
                                21,
                                PartnerType.SORTING_CENTER,
                                131L
                        ),
                        OrderCheckpoint.of(
                                CheckpointType.ORDER_SORTING_CENTER_RETURN_PREPARED_FOR_UTILIZE,
                                LocalDateTime.of(2020, 6, 27, 12, 0),
                                2,
                                22,
                                PartnerType.SORTING_CENTER,
                                131L
                        )
                )
        );

        importOrderCheckpointsCommand.executeCommand(commandFrom(1), terminal);

        List<OrderCheckpoint> checkpoints = orderCheckpointDao.getOrderCheckpoints(2);
        Assertions.assertEquals(3, checkpoints.size());
        assertThat(
                checkpoints,
                containsInAnyOrder(
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(2),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.SORTING_CENTER_RETURN_ARRIVED),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 28, 12, 0)),
                                        OrderCheckpointMatcher.hasId(20),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.SORTING_CENTER),
                                        OrderCheckpointMatcher.hasPartnerId(131L)
                                )).build(),
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(2),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.LOST),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 27, 12, 0)),
                                        OrderCheckpointMatcher.hasId(21),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.SORTING_CENTER),
                                        OrderCheckpointMatcher.hasPartnerId(131L)
                                )).build(),
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(2),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.ORDER_SORTING_CENTER_RETURN_PREPARED_FOR_UTILIZE),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 27, 12, 0)),
                                        OrderCheckpointMatcher.hasId(22),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.SORTING_CENTER),
                                        OrderCheckpointMatcher.hasPartnerId(131L)
                                )).build()
                )
        );
    }

    @Test
    void testImportSpecific() {
        mockYtData().thenReturn(
                List.of(
                        OrderCheckpoint.of(
                                CheckpointType.SORTING_CENTER_RETURN_ARRIVED,
                                LocalDateTime.of(2020, 6, 28, 12, 0),
                                2,
                                20,
                                PartnerType.SORTING_CENTER,
                                131L
                        ),
                        OrderCheckpoint.of(
                                CheckpointType.LOST,
                                LocalDateTime.of(2020, 6, 27, 12, 0),
                                2,
                                21,
                                PartnerType.DELIVERY,
                                131L
                        )
                )
        );

        importOrderCheckpointsCommand.executeCommand(command(List.of(2L, 3L)), terminal);

        List<OrderCheckpoint> checkpoints = orderCheckpointDao.getOrderCheckpoints(2);
        Assertions.assertEquals(2, checkpoints.size());
        assertThat(
                checkpoints,
                containsInAnyOrder(
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(2),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.SORTING_CENTER_RETURN_ARRIVED),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 28, 12, 0)),
                                        OrderCheckpointMatcher.hasId(20),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.SORTING_CENTER),
                                        OrderCheckpointMatcher.hasPartnerId(131L)
                                )).build(),
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(2),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.LOST),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 27, 12, 0)),
                                        OrderCheckpointMatcher.hasId(21),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.DELIVERY),
                                        OrderCheckpointMatcher.hasPartnerId(131L)
                                )).build()
                )
        );
    }

    @Test
    void testImportWithDeleteOld() {
        mockYtData().thenReturn(
                List.of(
                        OrderCheckpoint.of(
                                CheckpointType.SORTING_CENTER_RETURN_ARRIVED,
                                LocalDateTime.of(2020, 6, 28, 12, 0),
                                3,
                                20,
                                PartnerType.SORTING_CENTER,
                                131L
                        ),
                        OrderCheckpoint.of(
                                CheckpointType.LOST,
                                LocalDateTime.of(2020, 6, 27, 12, 0),
                                3,
                                21,
                                PartnerType.SORTING_CENTER,
                                131L
                        )
                )
        );

        // Перед синхронизацией, у заказа id=3 должен быть 1 чекпоинт
        List<OrderCheckpoint> checkpointsBeforeSync = orderCheckpointDao.getOrderCheckpoints(3);
        assertThat(checkpointsBeforeSync, hasSize(1));
        assertThat(checkpointsBeforeSync.get(0), OrderCheckpointMatcher.hasId(1000));

        importOrderCheckpointsCommand.executeCommand(commandWithDeleteOld(List.of(3L)), terminal);

        List<OrderCheckpoint> checkpoints = orderCheckpointDao.getOrderCheckpoints(3);
        assertThat(
                checkpoints,
                containsInAnyOrder(
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(3),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.SORTING_CENTER_RETURN_ARRIVED),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 28, 12, 0)),
                                        OrderCheckpointMatcher.hasId(20),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.SORTING_CENTER),
                                        OrderCheckpointMatcher.hasPartnerId(131L)
                                )).build(),
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(3),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.LOST),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 27, 12, 0)),
                                        OrderCheckpointMatcher.hasId(21),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.SORTING_CENTER),
                                        OrderCheckpointMatcher.hasPartnerId(131L)
                                )).build()
                )
        );
    }

    private OngoingStubbing<List<?>> mockYtData() {
        return when(ytJdbcTemplate.query(
                anyString(),
                any(SqlParameterSource.class),
                any(RowMapper.class))
        );
    }

    private static CommandInvocation commandFrom(long orderId) {
        return new CommandInvocation(
                "import-order-checkpoints",
                new String[]{},
                Map.of("from", Long.toString(orderId))
        );
    }

    private static CommandInvocation commandFromDate(LocalDate date) {
        return new CommandInvocation(
                "import-order-checkpoints",
                new String[]{},
                Map.of("dateFrom", date.format(DateTimeFormatter.ISO_DATE))
        );
    }

    private static CommandInvocation commandWithDeleteOld(List<Long> orderIds) {
        Map<String, String> options = new HashMap<>();
        options.put("deleteOld", null);
        return command(orderIds, options);
    }

    private static CommandInvocation command(List<Long> orderIds) {
        return command(orderIds, Map.of());
    }

    private static CommandInvocation command(List<Long> orderIds, Map<String, String> options) {
        String[] args = orderIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new);

        return new CommandInvocation(
                "import-order-checkpoints",
                args,
                options
        );
    }
}
