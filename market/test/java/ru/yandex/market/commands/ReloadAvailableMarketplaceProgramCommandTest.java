package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.marketplace.AvailableMarketplaceProgramInfo;
import ru.yandex.market.core.partner.marketplace.AvailableMarketplaceProgramService;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Тесты для {@link ReloadAvailableMarketplaceProgramCommand}.
 */
public class ReloadAvailableMarketplaceProgramCommandTest extends FunctionalTest {

    private Terminal terminal = Mockito.mock(Terminal.class);

    private NamedParameterJdbcTemplate ytJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AvailableMarketplaceProgramService availableMarketplaceProgramService;

    private ReloadAvailableMarketplaceProgramCommand reloadAvailableMarketplaceProgramCommand;

    @BeforeEach
    void setUp() {
        Mockito.when(terminal.getWriter()).thenReturn(new PrintWriter(ByteStreams.nullOutputStream()));
        reloadAvailableMarketplaceProgramCommand = new ReloadAvailableMarketplaceProgramCommand(
                availableMarketplaceProgramService,
                transactionTemplate,
                ytJdbcTemplate
        );
    }

    @Test
    @DbUnitDataSet(
            before = "ReloadAvailableMarketplaceProgramCommandTest.before.csv",
            after = "ReloadAvailableMarketplaceProgramCommandTest.after.csv")
    void testReload() {
        Mockito.when(ytJdbcTemplate.query(
                Mockito.anyString(),
                Mockito.any(RowMapper.class))
        ).thenReturn(List.of(
                new AvailableMarketplaceProgramInfo.Builder()
                        .setPartnerId(3L)
                        .setIsDropship(true)
                        .setIsFulfillment(true)
                        .setExpectedOrders(0)
                        .build(),
                new AvailableMarketplaceProgramInfo.Builder()
                        .setPartnerId(4L)
                        .setIsDropship(true)
                        .setIsFulfillment(false)
                        .setExpectedOrders(10)
                        .build(),
                new AvailableMarketplaceProgramInfo.Builder()
                        .setPartnerId(5L)
                        .setIsDropship(true)
                        .setIsFulfillment(true)
                        .setExpectedOrders(134)
                        .build()
        ));
        reloadAvailableMarketplaceProgramCommand.executeCommand(command(), terminal);
    }

    private static CommandInvocation command() {
        return new CommandInvocation(
                "reload-available-marketplace-programs",
                new String[]{"path"},
                Map.of()
        );
    }
}
